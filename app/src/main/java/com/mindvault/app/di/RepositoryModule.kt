package com.mindvault.app.di

import com.mindvault.app.data.local.dao.AttachmentDao
import com.mindvault.app.data.local.dao.CategoryDao
import com.mindvault.app.data.local.dao.NoteDao
import com.mindvault.app.data.local.dao.NoteLinkDao
import com.mindvault.app.data.local.dao.TagDao
import com.mindvault.app.data.repository.AttachmentRepository
import com.mindvault.app.data.repository.AttachmentRepositoryInterface
import com.mindvault.app.data.repository.CategoryRepository
import com.mindvault.app.data.repository.CategoryRepositoryInterface
import com.mindvault.app.data.repository.NoteLinkRepository
import com.mindvault.app.data.repository.NoteLinkRepositoryInterface
import com.mindvault.app.data.repository.NoteRepository
import com.mindvault.app.data.repository.NoteRepositoryInterface
import com.mindvault.app.data.repository.TagRepository
import com.mindvault.app.data.repository.TagRepositoryInterface
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

    @Provides
    @Singleton
    fun provideTagRepository(dao: TagDao): TagRepositoryInterface = TagRepository(dao)

    @Provides
    @Singleton
    fun provideCategoryRepository(dao: CategoryDao): CategoryRepositoryInterface = CategoryRepository(dao)

    @Provides
    @Singleton
    fun provideNoteLinkRepository(dao: NoteLinkDao): NoteLinkRepositoryInterface = NoteLinkRepository(dao)

    @Provides
    @Singleton
    fun provideAttachmentRepository(repo: AttachmentRepository): AttachmentRepositoryInterface = repo
}
