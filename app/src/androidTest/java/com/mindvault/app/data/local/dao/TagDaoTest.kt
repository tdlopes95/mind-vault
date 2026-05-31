package com.mindvault.app.data.local.dao

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.mindvault.app.data.local.MindVaultDatabase
import com.mindvault.app.data.local.entity.NoteEntity
import com.mindvault.app.data.local.entity.NoteTagCrossRef
import com.mindvault.app.data.local.entity.TagEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TagDaoTest {

    private lateinit var db: MindVaultDatabase
    private lateinit var tagDao: TagDao
    private lateinit var noteDao: NoteDao

    @Before
    fun setup() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(context, MindVaultDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        tagDao = db.tagDao()
        noteDao = db.noteDao()
    }

    @After
    fun teardown() { db.close() }

    @Test
    fun insertAndGetAllTags() = runTest {
        tagDao.insertTag(TagEntity(name = "kotlin", createdAt = 0))
        tagDao.insertTag(TagEntity(name = "android", createdAt = 0))

        val tags = tagDao.getAllTags().first()
        assertEquals(2, tags.size)
        assertEquals("android", tags[0].name) // ORDER BY name ASC
        assertEquals("kotlin", tags[1].name)
    }

    @Test
    fun insertDuplicate_ignoredDueToUniqueIndex() = runTest {
        tagDao.insertTag(TagEntity(name = "kotlin", createdAt = 0))
        tagDao.insertTag(TagEntity(name = "kotlin", createdAt = 0)) // ignored

        assertEquals(1, tagDao.getAllTags().first().size)
    }

    @Test
    fun deleteTag_removesFromList() = runTest {
        val id = tagDao.insertTag(TagEntity(name = "kotlin", createdAt = 0))
        val tag = tagDao.getAllTags().first().first()

        tagDao.deleteTag(tag)

        assertTrue(tagDao.getAllTags().first().isEmpty())
    }

    @Test
    fun addTagToNote_andGetTagsForNote() = runTest {
        val noteId = noteDao.insertNote(NoteEntity(title = "Note", content = ""))
        val tagId = tagDao.insertTag(TagEntity(name = "kotlin", createdAt = 0))
        tagDao.addTagToNote(NoteTagCrossRef(noteId = noteId, tagId = tagId))

        val tags = tagDao.getTagsForNote(noteId).first()
        assertEquals(1, tags.size)
        assertEquals("kotlin", tags[0].name)
    }

    @Test
    fun removeTagFromNote_tagNoLongerReturned() = runTest {
        val noteId = noteDao.insertNote(NoteEntity(title = "Note", content = ""))
        val tagId = tagDao.insertTag(TagEntity(name = "kotlin", createdAt = 0))
        tagDao.addTagToNote(NoteTagCrossRef(noteId = noteId, tagId = tagId))
        tagDao.removeTagFromNote(noteId, tagId)

        assertTrue(tagDao.getTagsForNote(noteId).first().isEmpty())
    }

    @Test
    fun removeAllTagsFromNote_clearsAllAssociations() = runTest {
        val noteId = noteDao.insertNote(NoteEntity(title = "Note", content = ""))
        val id1 = tagDao.insertTag(TagEntity(name = "kotlin", createdAt = 0))
        val id2 = tagDao.insertTag(TagEntity(name = "android", createdAt = 0))
        tagDao.addTagToNote(NoteTagCrossRef(noteId = noteId, tagId = id1))
        tagDao.addTagToNote(NoteTagCrossRef(noteId = noteId, tagId = id2))

        tagDao.removeAllTagsFromNote(noteId)

        assertTrue(tagDao.getTagsForNote(noteId).first().isEmpty())
    }

    @Test
    fun searchTags_matchesByNameSubstring() = runTest {
        tagDao.insertTag(TagEntity(name = "kotlin", createdAt = 0))
        tagDao.insertTag(TagEntity(name = "android", createdAt = 0))
        tagDao.insertTag(TagEntity(name = "compose", createdAt = 0))

        val results = tagDao.searchTags("an").first()
        assertEquals(1, results.size)
        assertEquals("android", results[0].name)
    }

    @Test
    fun getNotesForTag_returnsActiveNotesWithTag() = runTest {
        val noteId = noteDao.insertNote(NoteEntity(title = "Tagged Note", content = ""))
        val tagId = tagDao.insertTag(TagEntity(name = "kotlin", createdAt = 0))
        tagDao.addTagToNote(NoteTagCrossRef(noteId = noteId, tagId = tagId))

        val notes = tagDao.getNotesForTag(tagId).first()
        assertEquals(1, notes.size)
        assertEquals("Tagged Note", notes[0].title)
    }

    @Test
    fun getNotesForTag_excludesDeletedNotes() = runTest {
        val noteId = noteDao.insertNote(NoteEntity(title = "Deleted Note", content = ""))
        val tagId = tagDao.insertTag(TagEntity(name = "kotlin", createdAt = 0))
        tagDao.addTagToNote(NoteTagCrossRef(noteId = noteId, tagId = tagId))
        noteDao.softDeleteNote(noteId)

        assertTrue(tagDao.getNotesForTag(tagId).first().isEmpty())
    }
}
