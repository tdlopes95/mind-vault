package com.mindvault.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.mindvault.app.data.local.converter.DateConverter
import com.mindvault.app.data.local.dao.NoteDao
import com.mindvault.app.data.local.entity.NoteEntity
import com.mindvault.app.data.local.entity.NoteFts

@Database(
    entities = [NoteEntity::class, NoteFts::class],
    version = 1,
    exportSchema = false,
)
@TypeConverters(DateConverter::class)
abstract class MindVaultDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao
}
