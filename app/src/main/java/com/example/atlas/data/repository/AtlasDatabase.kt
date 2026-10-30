package com.example.atlas.data.repository

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.atlas.data.local.FamilyDocumentDao
import com.example.atlas.data.local.FamilyDocumentDeletionEntity
import com.example.atlas.data.local.FamilyDocumentEntity
import com.example.atlas.data.local.ShoppingDao
import com.example.atlas.data.local.ShoppingItemDeletionEntity
import com.example.atlas.data.local.ShoppingItemEntity
import com.example.atlas.data.model.CalendarEventEntity

@Database(
    entities = [
        CalendarEventEntity::class,
        ShoppingItemEntity::class,
        ShoppingItemDeletionEntity::class,
        FamilyDocumentEntity::class,
        FamilyDocumentDeletionEntity::class
    ],
    version = 5,
    exportSchema = false
)
abstract class AtlasDatabase : RoomDatabase() {
    abstract fun calendarEventDao(): CalendarEventDao
    abstract fun shoppingDao(): ShoppingDao
    abstract fun familyDocumentDao(): FamilyDocumentDao

    companion object {
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS shopping_items (
                        id TEXT NOT NULL,
                        name TEXT NOT NULL,
                        category TEXT NOT NULL,
                        quantity INTEGER NOT NULL,
                        isBought INTEGER NOT NULL,
                        addedBy TEXT NOT NULL,
                        isSynced INTEGER NOT NULL,
                        PRIMARY KEY(id)
                    )
                    """.trimIndent()
                )
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS shopping_item_deletions (
                        id TEXT NOT NULL,
                        PRIMARY KEY(id)
                    )
                    """.trimIndent()
                )
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    ALTER TABLE calendar_events
                    ADD COLUMN isSynced INTEGER NOT NULL DEFAULT 0
                    """.trimIndent()
                )
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS family_documents (
                        id TEXT NOT NULL,
                        name TEXT NOT NULL,
                        description TEXT NOT NULL,
                        url TEXT NOT NULL,
                        addedBy TEXT NOT NULL,
                        isSynced INTEGER NOT NULL,
                        PRIMARY KEY(id)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS family_document_deletions (
                        id TEXT NOT NULL,
                        PRIMARY KEY(id)
                    )
                    """.trimIndent()
                )
            }
        }

        @Volatile
        private var instance: AtlasDatabase? = null

        fun getInstance(context: Context): AtlasDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AtlasDatabase::class.java,
                    "atlas_database"
                )
                    .addMigrations(MIGRATION_1_2)
                    .addMigrations(MIGRATION_2_3)
                    .addMigrations(MIGRATION_3_4)
                    .addMigrations(MIGRATION_4_5)
                    .build()
                    .also { instance = it }
            }
    }
}
