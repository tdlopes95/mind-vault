package com.mindvault.app.data.local.dao

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.mindvault.app.data.local.MindVaultDatabase
import com.mindvault.app.data.local.entity.NoteLinkEntity
import com.mindvault.app.data.local.entity.NoteEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NoteLinkDaoTest {

    private lateinit var db: MindVaultDatabase
    private lateinit var noteLinkDao: NoteLinkDao
    private lateinit var noteDao: NoteDao

    @Before
    fun setup() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(context, MindVaultDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        noteLinkDao = db.noteLinkDao()
        noteDao = db.noteDao()
    }

    @After
    fun teardown() { db.close() }

    private suspend fun insertNote(title: String): Long =
        noteDao.insertNote(NoteEntity(title = title, content = ""))

    @Test
    fun insertLink_andGetLinkedNotes_bidirectional() = runTest {
        val id1 = insertNote("Note A")
        val id2 = insertNote("Note B")
        noteLinkDao.insertLink(NoteLinkEntity(sourceNoteId = id1, targetNoteId = id2, createdAt = 0))

        // From source perspective
        val linkedFromA = noteLinkDao.getLinkedNotes(id1).first()
        assertEquals(1, linkedFromA.size)
        assertEquals("Note B", linkedFromA[0].title)

        // From target perspective (bidirectional)
        val linkedFromB = noteLinkDao.getLinkedNotes(id2).first()
        assertEquals(1, linkedFromB.size)
        assertEquals("Note A", linkedFromB[0].title)
    }

    @Test
    fun deleteLink_removesFromBothDirections() = runTest {
        val id1 = insertNote("Note A")
        val id2 = insertNote("Note B")
        noteLinkDao.insertLink(NoteLinkEntity(sourceNoteId = id1, targetNoteId = id2, createdAt = 0))

        noteLinkDao.deleteLink(id1, id2)

        assertTrue(noteLinkDao.getLinkedNotes(id1).first().isEmpty())
        assertTrue(noteLinkDao.getLinkedNotes(id2).first().isEmpty())
    }

    @Test
    fun getLinkCount_countsAllDirections() = runTest {
        val id1 = insertNote("Note A")
        val id2 = insertNote("Note B")
        val id3 = insertNote("Note C")
        noteLinkDao.insertLink(NoteLinkEntity(sourceNoteId = id1, targetNoteId = id2, createdAt = 0))
        noteLinkDao.insertLink(NoteLinkEntity(sourceNoteId = id3, targetNoteId = id1, createdAt = 0))

        val count = noteLinkDao.getLinkCount(id1).first()
        assertEquals(2, count)
    }

    @Test
    fun insertDuplicateLink_ignoredDueToUniqueIndex() = runTest {
        val id1 = insertNote("Note A")
        val id2 = insertNote("Note B")
        noteLinkDao.insertLink(NoteLinkEntity(sourceNoteId = id1, targetNoteId = id2, createdAt = 0))
        noteLinkDao.insertLink(NoteLinkEntity(sourceNoteId = id1, targetNoteId = id2, createdAt = 0)) // ignored

        assertEquals(1, noteLinkDao.getLinkCount(id1).first())
    }

    @Test
    fun getLinkedNotes_excludesDeletedNotes() = runTest {
        val id1 = insertNote("Note A")
        val id2 = insertNote("Note B")
        noteLinkDao.insertLink(NoteLinkEntity(sourceNoteId = id1, targetNoteId = id2, createdAt = 0))
        noteDao.softDeleteNote(id2)

        assertTrue(noteLinkDao.getLinkedNotes(id1).first().isEmpty())
    }

    @Test
    fun getBacklinks_returnsOnlySourceNotes() = runTest {
        val id1 = insertNote("Note A")
        val id2 = insertNote("Note B")
        noteLinkDao.insertLink(NoteLinkEntity(sourceNoteId = id1, targetNoteId = id2, createdAt = 0))

        val backlinks = noteLinkDao.getBacklinks(id2).first()
        assertEquals(1, backlinks.size)
        assertEquals("Note A", backlinks[0].title)

        // Note B has no backlinks (nothing links to Note A via getBacklinks for id1)
        val backlinksForA = noteLinkDao.getBacklinks(id1).first()
        assertTrue(backlinksForA.isEmpty())
    }

    @Test
    fun cascadeDelete_noteLinkRemovedWhenNoteDeleted() = runTest {
        val id1 = insertNote("Note A")
        val id2 = insertNote("Note B")
        noteLinkDao.insertLink(NoteLinkEntity(sourceNoteId = id1, targetNoteId = id2, createdAt = 0))

        noteDao.permanentlyDeleteNote(id1)

        // Link should be gone (cascade delete)
        assertTrue(noteLinkDao.getLinkedNotes(id2).first().isEmpty())
    }
}
