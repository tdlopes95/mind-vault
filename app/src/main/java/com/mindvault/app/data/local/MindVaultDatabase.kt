package com.mindvault.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.mindvault.app.data.local.converter.DateConverter
import com.mindvault.app.data.local.dao.AttachmentDao
import com.mindvault.app.data.local.dao.CategoryDao
import com.mindvault.app.data.local.dao.NoteDao
import com.mindvault.app.data.local.dao.NoteLinkDao
import com.mindvault.app.data.local.dao.TagDao
import com.mindvault.app.data.local.entity.AttachmentEntity
import com.mindvault.app.data.local.entity.CategoryEntity
import com.mindvault.app.data.local.entity.NoteEntity
import com.mindvault.app.data.local.entity.NoteFts
import com.mindvault.app.data.local.entity.NoteLinkEntity
import com.mindvault.app.data.local.entity.NoteTagCrossRef
import com.mindvault.app.data.local.entity.TagEntity

@Database(
    entities = [
        NoteEntity::class,
        NoteFts::class,
        TagEntity::class,
        NoteTagCrossRef::class,
        CategoryEntity::class,
        NoteLinkEntity::class,
        AttachmentEntity::class,
    ],
    version = 4,
    exportSchema = false,
)
@TypeConverters(DateConverter::class)
abstract class MindVaultDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao
    abstract fun tagDao(): TagDao
    abstract fun categoryDao(): CategoryDao
    abstract fun noteLinkDao(): NoteLinkDao
    abstract fun attachmentDao(): AttachmentDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `TagEntity` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `color` INTEGER NOT NULL DEFAULT 0,
                        `createdAt` INTEGER NOT NULL
                    )
                """.trimIndent())
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_TagEntity_name` ON `TagEntity` (`name`)")

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `NoteTagCrossRef` (
                        `noteId` INTEGER NOT NULL,
                        `tagId` INTEGER NOT NULL,
                        PRIMARY KEY (`noteId`, `tagId`),
                        FOREIGN KEY (`noteId`) REFERENCES `notes`(`id`) ON DELETE CASCADE,
                        FOREIGN KEY (`tagId`) REFERENCES `TagEntity`(`id`) ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_NoteTagCrossRef_tagId` ON `NoteTagCrossRef` (`tagId`)")

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `CategoryEntity` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `color` INTEGER NOT NULL DEFAULT 0,
                        `icon` TEXT,
                        `sortOrder` INTEGER NOT NULL DEFAULT 0,
                        `createdAt` INTEGER NOT NULL
                    )
                """.trimIndent())
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_CategoryEntity_name` ON `CategoryEntity` (`name`)")

                db.execSQL("ALTER TABLE `notes` ADD COLUMN `categoryId` INTEGER DEFAULT NULL REFERENCES `CategoryEntity`(`id`) ON DELETE SET NULL")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_notes_categoryId` ON `notes` (`categoryId`)")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `NoteLinkEntity` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `sourceNoteId` INTEGER NOT NULL,
                        `targetNoteId` INTEGER NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        FOREIGN KEY (`sourceNoteId`) REFERENCES `notes`(`id`) ON DELETE CASCADE,
                        FOREIGN KEY (`targetNoteId`) REFERENCES `notes`(`id`) ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_NoteLinkEntity_source_target` ON `NoteLinkEntity` (`sourceNoteId`, `targetNoteId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_NoteLinkEntity_targetNoteId` ON `NoteLinkEntity` (`targetNoteId`)")

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `AttachmentEntity` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `noteId` INTEGER NOT NULL,
                        `fileName` TEXT NOT NULL,
                        `filePath` TEXT NOT NULL,
                        `mimeType` TEXT NOT NULL,
                        `fileSize` INTEGER NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        FOREIGN KEY (`noteId`) REFERENCES `notes`(`id`) ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_AttachmentEntity_noteId` ON `AttachmentEntity` (`noteId`)")

                db.execSQL("ALTER TABLE `notes` ADD COLUMN `isPinned` INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_notes_isDeleted` ON `notes` (`isDeleted`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_notes_isArchived` ON `notes` (`isArchived`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_notes_isFavorite` ON `notes` (`isFavorite`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_notes_isPinned` ON `notes` (`isPinned`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_notes_updatedAt` ON `notes` (`updatedAt`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_notes_isDeleted_isArchived` ON `notes` (`isDeleted`, `isArchived`)")
            }
        }
    }
}
