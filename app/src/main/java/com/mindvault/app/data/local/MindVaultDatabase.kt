package com.mindvault.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.mindvault.app.data.local.converter.DateConverter
import com.mindvault.app.data.local.dao.CategoryDao
import com.mindvault.app.data.local.dao.NoteDao
import com.mindvault.app.data.local.dao.TagDao
import com.mindvault.app.data.local.entity.CategoryEntity
import com.mindvault.app.data.local.entity.NoteEntity
import com.mindvault.app.data.local.entity.NoteFts
import com.mindvault.app.data.local.entity.NoteTagCrossRef
import com.mindvault.app.data.local.entity.TagEntity

@Database(
    entities = [NoteEntity::class, NoteFts::class, TagEntity::class, NoteTagCrossRef::class, CategoryEntity::class],
    version = 2,
    exportSchema = false,
)
@TypeConverters(DateConverter::class)
abstract class MindVaultDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao
    abstract fun tagDao(): TagDao
    abstract fun categoryDao(): CategoryDao

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
    }
}
