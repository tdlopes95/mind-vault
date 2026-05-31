package com.mindvault.app.di

import android.content.Context
import androidx.room.Room
import com.mindvault.app.data.local.MindVaultDatabase
import com.mindvault.app.data.local.dao.AttachmentDao
import com.mindvault.app.data.local.dao.CategoryDao
import com.mindvault.app.data.local.dao.NoteDao
import com.mindvault.app.data.local.dao.NoteLinkDao
import com.mindvault.app.data.local.dao.TagDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): MindVaultDatabase =
        Room.databaseBuilder(context, MindVaultDatabase::class.java, "mindvault.db")
            .addMigrations(MindVaultDatabase.MIGRATION_1_2, MindVaultDatabase.MIGRATION_2_3, MindVaultDatabase.MIGRATION_3_4)
            .build()

    @Provides
    fun provideNoteDao(db: MindVaultDatabase): NoteDao = db.noteDao()

    @Provides
    fun provideTagDao(db: MindVaultDatabase): TagDao = db.tagDao()

    @Provides
    fun provideCategoryDao(db: MindVaultDatabase): CategoryDao = db.categoryDao()

    @Provides
    fun provideNoteLinkDao(db: MindVaultDatabase): NoteLinkDao = db.noteLinkDao()

    @Provides
    fun provideAttachmentDao(db: MindVaultDatabase): AttachmentDao = db.attachmentDao()
}
