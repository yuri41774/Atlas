package com.example.atlas.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface FamilyDocumentDao {
    @Query("SELECT * FROM family_documents ORDER BY name COLLATE NOCASE")
    fun observeDocuments(): Flow<List<FamilyDocumentEntity>>

    @Query("SELECT * FROM family_documents WHERE isSynced = 0")
    suspend fun getPendingDocuments(): List<FamilyDocumentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(document: FamilyDocumentEntity)

    @Query(
        """
        UPDATE family_documents SET isSynced = 1
        WHERE id = :id
          AND name = :name
          AND description = :description
          AND url = :url
          AND addedBy = :addedBy
          AND isSynced = 0
        """
    )
    suspend fun markSyncedIfUnchanged(
        id: String,
        name: String,
        description: String,
        url: String,
        addedBy: String
    )

    @Query("DELETE FROM family_documents WHERE id = :id")
    suspend fun deleteItem(id: String)

    @Query("SELECT * FROM family_document_deletions")
    suspend fun getPendingDeletions(): List<FamilyDocumentDeletionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDeletion(deletion: FamilyDocumentDeletionEntity)

    @Query("DELETE FROM family_document_deletions WHERE id = :id")
    suspend fun removeDeletion(id: String)

    @Transaction
    suspend fun deleteAndQueueForSync(id: String) {
        deleteItem(id)
        insertDeletion(FamilyDocumentDeletionEntity(id))
    }

    @Query("DELETE FROM family_documents WHERE isSynced = 1 AND id NOT IN (:remoteIds)")
    suspend fun deleteSyncedDocumentsMissingFromCloud(remoteIds: List<String>)

    @Query("DELETE FROM family_documents WHERE isSynced = 1")
    suspend fun deleteAllSyncedDocuments()

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertRemoteDocumentIfMissing(document: FamilyDocumentEntity): Long

    @Query("SELECT EXISTS(SELECT 1 FROM family_document_deletions WHERE id = :id)")
    suspend fun hasPendingDeletion(id: String): Boolean

    @Query(
        """
        UPDATE family_documents
        SET name = :name,
            description = :description,
            url = :url,
            addedBy = :addedBy,
            isSynced = 1
        WHERE id = :id AND isSynced = 1
        """
    )
    suspend fun updateSyncedDocument(
        id: String,
        name: String,
        description: String,
        url: String,
        addedBy: String
    )

    @Transaction
    suspend fun mergeRemoteDocument(document: FamilyDocumentEntity) {
        if (hasPendingDeletion(document.id)) return

        if (insertRemoteDocumentIfMissing(document) == -1L) {
            updateSyncedDocument(
                id = document.id,
                name = document.name,
                description = document.description,
                url = document.url,
                addedBy = document.addedBy
            )
        }
    }
}
