package com.example.atlas.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "shopping_items")
data class ShoppingItemEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val category: String,
    val quantity: Int,
    val isBought: Boolean,
    val addedBy: String,
    val isSynced: Boolean = false
)
