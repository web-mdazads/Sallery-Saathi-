package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        CategoryEntity::class,
        TransactionEntity::class,
        BudgetAllocationEntity::class,
        GoalEntity::class,
        ChatSessionEntity::class,
        ChatMessageEntity::class,
        SaathiNotificationEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class SaathiDatabase : RoomDatabase() {
    abstract fun saathiDao(): SaathiDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `saathi_notifications` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `kind` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `body` TEXT NOT NULL,
                        `amount` REAL NOT NULL,
                        `isDebit` INTEGER NOT NULL,
                        `counterpartyName` TEXT NOT NULL,
                        `bankName` TEXT NOT NULL,
                        `accountLast4` TEXT NOT NULL,
                        `upiRef` TEXT NOT NULL,
                        `suggestedCategoryId` TEXT NOT NULL,
                        `detectedAtMillis` INTEGER NOT NULL,
                        `approvalStatus` TEXT NOT NULL,
                        `isRead` INTEGER NOT NULL,
                        `approvedTransactionId` INTEGER NOT NULL,
                        `dedupKey` TEXT NOT NULL,
                        `rawSmsPreview` TEXT NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `chat_sessions` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `title` TEXT NOT NULL,
                        `lastMessagePreview` TEXT NOT NULL,
                        `messageCount` INTEGER NOT NULL,
                        `createdAtMillis` INTEGER NOT NULL,
                        `updatedAtMillis` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "ALTER TABLE `chat_messages` ADD COLUMN `sessionId` INTEGER NOT NULL DEFAULT 1"
                )
                val now = System.currentTimeMillis()
                db.execSQL(
                    """
                    INSERT INTO `chat_sessions` (`id`, `title`, `lastMessagePreview`, `messageCount`, `createdAtMillis`, `updatedAtMillis`)
                    SELECT 1,
                           'Previous Chat',
                           COALESCE((SELECT `content` FROM `chat_messages` ORDER BY `timestampMillis` DESC, `id` DESC LIMIT 1), ''),
                           (SELECT COUNT(*) FROM `chat_messages`),
                           COALESCE((SELECT MIN(`timestampMillis`) FROM `chat_messages`), $now),
                           COALESCE((SELECT MAX(`timestampMillis`) FROM `chat_messages`), $now)
                    WHERE EXISTS (SELECT 1 FROM `chat_messages`)
                    """.trimIndent()
                )
            }
        }

        @Volatile
        private var INSTANCE: SaathiDatabase? = null

        fun getInstance(context: Context): SaathiDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SaathiDatabase::class.java,
                    "salary_saathi_local.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .fallbackToDestructiveMigration(true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
