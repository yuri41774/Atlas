package com.example.atlas.data.repository

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.atlas.data.model.CalendarEventEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CalendarEventDao {
    @Query(
        """
        SELECT * FROM calendar_events
        WHERE date = :date
        ORDER BY time, title
        """
    )
    fun observeEventsForDate(date: String): Flow<List<CalendarEventEntity>>

    @Query("SELECT * FROM calendar_events WHERE isSynced = 0")
    suspend fun getPendingEvents(): List<CalendarEventEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(event: CalendarEventEntity)

    @Query(
        """
        UPDATE calendar_events SET isSynced = 1
        WHERE id = :id
          AND title = :title
          AND description = :description
          AND date = :date
          AND time = :time
          AND location = :location
          AND createdBy = :createdBy
          AND isSynced = 0
        """
    )
    suspend fun markSyncedIfUnchanged(
        id: String,
        title: String,
        description: String,
        date: String,
        time: String,
        location: String,
        createdBy: String
    )

    @Query("DELETE FROM calendar_events WHERE isSynced = 1 AND id NOT IN (:remoteIds)")
    suspend fun deleteSyncedEventsMissingFromCloud(remoteIds: List<String>)

    @Query("DELETE FROM calendar_events WHERE isSynced = 1")
    suspend fun deleteAllSyncedEvents()

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertRemoteEventIfMissing(event: CalendarEventEntity): Long

    @Query("SELECT EXISTS(SELECT 1 FROM calendar_events WHERE id = :id AND isSynced = 0)")
    suspend fun hasPendingLocalEvent(id: String): Boolean

    @Query(
        """
        UPDATE calendar_events
        SET title = :title,
            description = :description,
            date = :date,
            time = :time,
            location = :location,
            createdBy = :createdBy,
            isSynced = 1
        WHERE id = :id AND isSynced = 1
        """
    )
    suspend fun updateSyncedEvent(
        id: String,
        title: String,
        description: String,
        date: String,
        time: String,
        location: String,
        createdBy: String
    )

    @Transaction
    suspend fun mergeRemoteEvent(event: CalendarEventEntity) {
        if (hasPendingLocalEvent(event.id)) return

        if (insertRemoteEventIfMissing(event) == -1L) {
            updateSyncedEvent(
                id = event.id,
                title = event.title,
                description = event.description,
                date = event.date,
                time = event.time,
                location = event.location,
                createdBy = event.createdBy
            )
        }
    }
}
