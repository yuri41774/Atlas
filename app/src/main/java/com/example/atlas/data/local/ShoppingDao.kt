package com.example.atlas.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface ShoppingDao {
    @Query("SELECT * FROM shopping_items ORDER BY isBought, name COLLATE NOCASE")
    fun getAllItems(): Flow<List<ShoppingItemEntity>>

    @Query("SELECT * FROM shopping_items WHERE isSynced = 0")
    suspend fun getPendingItems(): List<ShoppingItemEntity>

    @Query("SELECT COUNT(*) FROM shopping_items WHERE isBought = 0")
    fun observePendingCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateItem(item: ShoppingItemEntity)

    @Query(
        """
        UPDATE shopping_items SET isSynced = 1
        WHERE id = :id
          AND name = :name
          AND category = :category
          AND quantity = :quantity
          AND isBought = :isBought
          AND addedBy = :addedBy
          AND isSynced = 0
        """
    )
    suspend fun markSyncedIfUnchanged(
        id: String,
        name: String,
        category: String,
        quantity: Int,
        isBought: Boolean,
        addedBy: String
    )

    @Query(
        """
        UPDATE shopping_items
        SET isBought = :isBought, isSynced = 0
        WHERE id = :id
        """
    )
    suspend fun setBought(id: String, isBought: Boolean)

    @Query("SELECT * FROM shopping_item_deletions")
    suspend fun getPendingDeletions(): List<ShoppingItemDeletionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDeletion(deletion: ShoppingItemDeletionEntity)

    @Query("DELETE FROM shopping_item_deletions WHERE id = :id")
    suspend fun removeDeletion(id: String)

    @Query("DELETE FROM shopping_items WHERE id = :id")
    suspend fun deleteItem(id: String)

    @Transaction
    suspend fun deleteAndQueueForSync(id: String) {
        deleteItem(id)
        insertDeletion(ShoppingItemDeletionEntity(id))
    }

    @Query("DELETE FROM shopping_items WHERE isSynced = 1 AND id NOT IN (:remoteIds)")
    suspend fun deleteSyncedItemsMissingFromCloud(remoteIds: List<String>)

    @Query("DELETE FROM shopping_items WHERE isSynced = 1")
    suspend fun deleteAllSyncedItems()

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertRemoteItemIfMissing(item: ShoppingItemEntity): Long

    @Query("SELECT EXISTS(SELECT 1 FROM shopping_item_deletions WHERE id = :id)")
    suspend fun hasPendingDeletion(id: String): Boolean

    @Query(
        """
        UPDATE shopping_items
        SET name = :name,
            category = :category,
            quantity = :quantity,
            isBought = :isBought,
            addedBy = :addedBy,
            isSynced = 1
        WHERE id = :id AND isSynced = 1
        """
    )
    suspend fun updateSyncedItem(
        id: String,
        name: String,
        category: String,
        quantity: Int,
        isBought: Boolean,
        addedBy: String
    )

    @Transaction
    suspend fun mergeRemoteItem(item: ShoppingItemEntity) {
        if (hasPendingDeletion(item.id)) return

        val insertedRowId = insertRemoteItemIfMissing(item)
        if (insertedRowId == -1L) {
            updateSyncedItem(
                id = item.id,
                name = item.name,
                category = item.category,
                quantity = item.quantity,
                isBought = item.isBought,
                addedBy = item.addedBy
            )
        }
    }
}
