package com.example.atlas.data.repository

import com.example.atlas.data.local.FamilyDocumentDao
import com.example.atlas.data.local.FamilyDocumentEntity
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Source
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await

class FamilyDocumentRepository(
    private val documentDao: FamilyDocumentDao,
    private val firestore: FirebaseFirestore
) {
    fun observeDocuments(): Flow<List<FamilyDocumentEntity>> =
        documentDao.observeDocuments()

    suspend fun addDocument(document: FamilyDocumentEntity): DocumentSyncResult {
        documentDao.insertOrUpdate(document.copy(isSynced = false))
        return syncWithFirebase()
    }

    suspend fun deleteDocument(id: String): DocumentSyncResult {
        documentDao.deleteAndQueueForSync(id)
        return syncWithFirebase()
    }

    suspend fun syncWithFirebase(): DocumentSyncResult = syncMutex.withLock {
        try {
            val collection = firestore.collection(FAMILY_DOCUMENTS_COLLECTION)
            documentDao.getPendingDocuments().forEach { document ->
                collection.document(document.id).set(document.toFirestoreMap()).await()
                documentDao.markSyncedIfUnchanged(
                    id = document.id,
                    name = document.name,
                    description = document.description,
                    url = document.url,
                    addedBy = document.addedBy
                )
            }

            documentDao.getPendingDeletions().forEach { deletion ->
                collection.document(deletion.id).delete().await()
                documentDao.removeDeletion(deletion.id)
            }

            val remoteSnapshot = collection.get(Source.SERVER).await()
            val remoteDocuments = remoteSnapshot.documents.map { it.toFamilyDocumentEntity() }
            val remoteIds = remoteDocuments.map { it.id }

            if (remoteIds.isEmpty()) {
                documentDao.deleteAllSyncedDocuments()
            } else {
                documentDao.deleteSyncedDocumentsMissingFromCloud(remoteIds)
            }
            for (document in remoteDocuments) {
                documentDao.mergeRemoteDocument(document)
            }

            DocumentSyncResult.Synced
        } catch (exception: FirebaseFirestoreException) {
            DocumentSyncResult.Pending(
                exception.localizedMessage ?: "Falha ao comunicar com o Firebase."
            )
        } catch (exception: InvalidRemoteDocumentException) {
            DocumentSyncResult.Pending(exception.message ?: "Documento remoto inválido.")
        }
    }

    private fun FamilyDocumentEntity.toFirestoreMap(): Map<String, Any> = mapOf(
        "id" to id,
        "name" to name,
        "description" to description,
        "url" to url,
        "addedBy" to addedBy
    )

    private fun DocumentSnapshot.toFamilyDocumentEntity(): FamilyDocumentEntity {
        val documentName = getString("name")
            ?: throw InvalidRemoteDocumentException(id, "name")
        val documentDescription = getString("description")
            ?: throw InvalidRemoteDocumentException(id, "description")
        val documentUrl = getString("url")
            ?: throw InvalidRemoteDocumentException(id, "url")
        val documentAddedBy = getString("addedBy")
            ?: throw InvalidRemoteDocumentException(id, "addedBy")

        return FamilyDocumentEntity(
            id = id,
            name = documentName,
            description = documentDescription,
            url = documentUrl,
            addedBy = documentAddedBy,
            isSynced = true
        )
    }

    private class InvalidRemoteDocumentException(
        documentId: String,
        field: String
    ) : Exception("O documento $documentId não tem um campo válido: $field.")

    private companion object {
        const val FAMILY_DOCUMENTS_COLLECTION = "family_documents"
        val syncMutex = Mutex()
    }
}

sealed interface DocumentSyncResult {
    data object Synced : DocumentSyncResult
    data class Pending(val reason: String) : DocumentSyncResult
}
