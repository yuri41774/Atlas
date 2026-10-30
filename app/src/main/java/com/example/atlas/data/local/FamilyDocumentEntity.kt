package com.example.atlas.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "family_documents")
data class FamilyDocumentEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val url: String,
    val addedBy: String,
    val isSynced: Boolean = false
)
