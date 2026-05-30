package com.mindvault.app.di

import com.mindvault.app.data.local.dao.NoteDao
import com.mindvault.app.data.repository.NoteRepository
import com.mindvault.app.data.repository.NoteRepositoryInterface
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideNoteRepository(dao: NoteDao): NoteRepositoryInterface = NoteRepository(dao)
}
