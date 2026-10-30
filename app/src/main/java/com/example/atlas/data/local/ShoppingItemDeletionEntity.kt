package com.example.atlas.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "shopping_item_deletions")
data class ShoppingItemDeletionEntity(
    @PrimaryKey val id: String
)
