package com.example.atlas.data.model

data class ShoppingItem(
    val id: String = "",
    val name: String = "",
    val category: String = "Geral",
    val quantity: Int = 1,
    val isBought: Boolean = false,
    val addedBy: String = "",
    val isSynced: Boolean = false
)
