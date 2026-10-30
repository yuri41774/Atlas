package com.example.atlas.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo

@Entity(tableName = "calendar_events")
data class CalendarEventEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val date: String,
    val time: String,
    val location: String,
    val createdBy: String,
    @ColumnInfo(defaultValue = "0")
    val isSynced: Boolean = false
)
