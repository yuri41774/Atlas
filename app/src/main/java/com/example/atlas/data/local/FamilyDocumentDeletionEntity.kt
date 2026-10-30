package com.example.atlas.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "family_document_deletions")
data class FamilyDocumentDeletionEntity(
    @PrimaryKey val id: String
)
