package com.example.atlas.data.repository

import com.example.atlas.data.model.CalendarEvent
import com.example.atlas.data.model.CalendarEventEntity
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Source
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await

class CalendarEventRepository(
    private val calendarEventDao: CalendarEventDao,
    private val firestore: FirebaseFirestore
) {
    fun observeEventsForDate(date: String): Flow<List<CalendarEvent>> =
        calendarEventDao.observeEventsForDate(date).map { events ->
            events.map { event -> event.toCalendarEvent() }
        }

    suspend fun addEvent(event: CalendarEvent): CalendarSyncResult {
        calendarEventDao.insertOrUpdate(event.toEntity().copy(isSynced = false))
        return syncWithFirebase()
    }

    suspend fun syncWithFirebase(): CalendarSyncResult = syncMutex.withLock {
        try {
            val collection = firestore.collection(CALENDAR_EVENTS_COLLECTION)
            calendarEventDao.getPendingEvents().forEach { event ->
                collection.document(event.id).set(event.toFirestoreMap()).await()
                calendarEventDao.markSyncedIfUnchanged(
                    id = event.id,
                    title = event.title,
                    description = event.description,
                    date = event.date,
                    time = event.time,
                    location = event.location,
                    createdBy = event.createdBy
                )
            }

            val remoteSnapshot = collection.get(Source.SERVER).await()
            val remoteEvents = remoteSnapshot.documents.map { it.toCalendarEventEntity() }
            val remoteIds = remoteEvents.map { event -> event.id }

            if (remoteIds.isEmpty()) {
                calendarEventDao.deleteAllSyncedEvents()
            } else {
                calendarEventDao.deleteSyncedEventsMissingFromCloud(remoteIds)
            }
            for (event in remoteEvents) {
                calendarEventDao.mergeRemoteEvent(event)
            }

            CalendarSyncResult.Synced
        } catch (exception: FirebaseFirestoreException) {
            CalendarSyncResult.Pending(
                exception.localizedMessage ?: "Falha ao comunicar com o Firebase."
            )
        } catch (exception: InvalidRemoteCalendarEventException) {
            CalendarSyncResult.Pending(exception.message ?: "Evento remoto inválido.")
        }
    }

    private fun CalendarEvent.toEntity() = CalendarEventEntity(
        id = id,
        title = title,
        description = description,
        date = date,
        time = time,
        location = location,
        createdBy = createdBy,
        isSynced = false
    )

    private fun CalendarEventEntity.toCalendarEvent() = CalendarEvent(
        id = id,
        title = title,
        description = description,
        date = date,
        time = time,
        location = location,
        createdBy = createdBy
    )

    private fun CalendarEventEntity.toFirestoreMap(): Map<String, Any> = mapOf(
        "id" to id,
        "title" to title,
        "description" to description,
        "date" to date,
        "time" to time,
        "location" to location,
        "createdBy" to createdBy
    )

    private fun DocumentSnapshot.toCalendarEventEntity(): CalendarEventEntity {
        val eventTitle = getString("title")
            ?: throw InvalidRemoteCalendarEventException(id, "title")
        val eventDescription = getString("description")
            ?: throw InvalidRemoteCalendarEventException(id, "description")
        val eventDate = getString("date")
            ?: throw InvalidRemoteCalendarEventException(id, "date")
        val eventTime = getString("time")
            ?: throw InvalidRemoteCalendarEventException(id, "time")
        val eventLocation = getString("location")
            ?: throw InvalidRemoteCalendarEventException(id, "location")
        val eventCreatedBy = getString("createdBy")
            ?: throw InvalidRemoteCalendarEventException(id, "createdBy")

        return CalendarEventEntity(
            id = id,
            title = eventTitle,
            description = eventDescription,
            date = eventDate,
            time = eventTime,
            location = eventLocation,
            createdBy = eventCreatedBy,
            isSynced = true
        )
    }

    private class InvalidRemoteCalendarEventException(
        documentId: String,
        field: String
    ) : Exception("O documento $documentId não tem um campo válido: $field.")

    private companion object {
        const val CALENDAR_EVENTS_COLLECTION = "calendar_events"
        val syncMutex = Mutex()
    }
}

sealed interface CalendarSyncResult {
    data object Synced : CalendarSyncResult
    data class Pending(val reason: String) : CalendarSyncResult
}
