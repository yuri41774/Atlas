package com.example.atlas.data.repository

import com.example.atlas.data.local.ShoppingDao
import com.example.atlas.data.local.ShoppingItemEntity
import com.example.atlas.data.model.ShoppingItem
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Source
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await

class ShoppingRepository(
    private val shoppingDao: ShoppingDao,
    private val firestore: FirebaseFirestore
) {
    val allItems: Flow<List<ShoppingItemEntity>> = shoppingDao.getAllItems()

    fun observeItems(): Flow<List<ShoppingItem>> =
        allItems.map { items -> items.map { item -> item.toShoppingItem() } }

    fun observePendingCount(): Flow<Int> = shoppingDao.observePendingCount()

    suspend fun addItem(item: ShoppingItem): ShoppingSyncResult {
        shoppingDao.insertOrUpdateItem(item.toEntity().copy(isSynced = false))
        return syncWithFirebase()
    }

    suspend fun setBought(id: String, isBought: Boolean): ShoppingSyncResult {
        shoppingDao.setBought(id, isBought)
        return syncWithFirebase()
    }

    suspend fun deleteItem(id: String): ShoppingSyncResult {
        shoppingDao.deleteAndQueueForSync(id)
        return syncWithFirebase()
    }

    suspend fun syncWithFirebase(): ShoppingSyncResult = syncMutex.withLock {
        try {
            val collection = firestore.collection(SHOPPING_ITEMS_COLLECTION)

            shoppingDao.getPendingItems().forEach { item ->
                collection.document(item.id)
                    .set(item.toFirestoreMap())
                    .await()
                shoppingDao.markSyncedIfUnchanged(
                    id = item.id,
                    name = item.name,
                    category = item.category,
                    quantity = item.quantity,
                    isBought = item.isBought,
                    addedBy = item.addedBy
                )
            }

            shoppingDao.getPendingDeletions().forEach { deletion ->
                collection.document(deletion.id).delete().await()
                shoppingDao.removeDeletion(deletion.id)
            }

            val remoteSnapshot = collection.get(Source.SERVER).await()
            val remoteItems = remoteSnapshot.documents.map { document ->
                document.toShoppingItemEntity()
            }
            val remoteIds = remoteItems.map { it.id }

            if (remoteIds.isEmpty()) {
                shoppingDao.deleteAllSyncedItems()
            } else {
                shoppingDao.deleteSyncedItemsMissingFromCloud(remoteIds)
            }
            for (remoteItem in remoteItems) {
                shoppingDao.mergeRemoteItem(remoteItem)
            }

            ShoppingSyncResult.Synced
        } catch (exception: FirebaseFirestoreException) {
            ShoppingSyncResult.Pending(
                exception.localizedMessage ?: "Falha ao comunicar com o Firebase."
            )
        } catch (exception: InvalidRemoteShoppingItemException) {
            ShoppingSyncResult.Pending(exception.message ?: "Item remoto inválido.")
        }
    }

    private fun ShoppingItem.toEntity() = ShoppingItemEntity(
        id = id,
        name = name,
        category = category,
        quantity = quantity,
        isBought = isBought,
        addedBy = addedBy,
        isSynced = isSynced
    )

    private fun ShoppingItemEntity.toShoppingItem() = ShoppingItem(
        id = id,
        name = name,
        category = category,
        quantity = quantity,
        isBought = isBought,
        addedBy = addedBy,
        isSynced = isSynced
    )

    private fun ShoppingItemEntity.toFirestoreMap(): Map<String, Any> = mapOf(
        "id" to id,
        "name" to name,
        "category" to category,
        "quantity" to quantity,
        "isBought" to isBought,
        "addedBy" to addedBy
    )

    private fun com.google.firebase.firestore.DocumentSnapshot.toShoppingItemEntity():
        ShoppingItemEntity {
        val itemName = getString("name")
            ?: throw InvalidRemoteShoppingItemException(id, "name")
        val itemCategory = getString("category")
            ?: throw InvalidRemoteShoppingItemException(id, "category")
        val itemQuantity = getLong("quantity")
            ?.takeIf { it in 1..Int.MAX_VALUE.toLong() }
            ?.toInt()
            ?: throw InvalidRemoteShoppingItemException(id, "quantity")
        val itemIsBought = getBoolean("isBought")
            ?: throw InvalidRemoteShoppingItemException(id, "isBought")
        val itemAddedBy = getString("addedBy")
            ?: throw InvalidRemoteShoppingItemException(id, "addedBy")

        return ShoppingItemEntity(
            id = id,
            name = itemName,
            category = itemCategory,
            quantity = itemQuantity,
            isBought = itemIsBought,
            addedBy = itemAddedBy,
            isSynced = true
        )
    }

    private class InvalidRemoteShoppingItemException(
        documentId: String,
        field: String
    ) : Exception("O documento $documentId não tem um campo válido: $field.")

    private companion object {
        const val SHOPPING_ITEMS_COLLECTION = "shopping_items"
        val syncMutex = Mutex()
    }
}

sealed interface ShoppingSyncResult {
    data object Synced : ShoppingSyncResult
    data class Pending(val reason: String) : ShoppingSyncResult
}
