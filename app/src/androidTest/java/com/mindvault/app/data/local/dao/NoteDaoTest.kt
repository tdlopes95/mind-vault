package com.mindvault.app.data.local.dao

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.mindvault.app.data.local.MindVaultDatabase
import com.mindvault.app.data.local.entity.NoteEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NoteDaoTest {

    private lateinit var db: MindVaultDatabase
    private lateinit var dao: NoteDao

    @Before
    fun setup() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(context, MindVaultDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.noteDao()
    }

    @After
    fun teardown() {
        db.close()
    }

    // --- Insert & retrieve ---

    @Test
    fun insertAndRetrieveById_allFieldsMatch() = runTest {
        val note = NoteEntity(
            title = "Test Title",
            content = "Test Content",
            color = 0xFF0000,
            isFavorite = true,
            isArchived = false,
            isDeleted = false,
            createdAt = 1000L,
            updatedAt = 2000L,
            deletedAt = null,
        )
        val id = dao.insertNote(note)
        val result = dao.getNoteById(id).first()

        assertNotNull(result)
        assertEquals("Test Title", result!!.title)
        assertEquals("Test Content", result.content)
        assertEquals(0xFF0000, result.color)
        assertTrue(result.isFavorite)
        assertFalse(result.isArchived)
        assertFalse(result.isDeleted)
        assertEquals(1000L, result.createdAt)
        assertEquals(2000L, result.updatedAt)
        assertNull(result.deletedAt)
    }

    // --- getActiveNotes ---

    @Test
    fun getActiveNotes_excludesArchivedAndDeleted_orderedByUpdatedAtDesc() = runTest {
        dao.insertNote(NoteEntity(title = "Active Old", content = "", updatedAt = 1000L))
        dao.insertNote(NoteEntity(title = "Active New", content = "", updatedAt = 3000L))
        dao.insertNote(NoteEntity(title = "Archived", content = "", isArchived = true, updatedAt = 4000L))
        dao.insertNote(NoteEntity(title = "Deleted", content = "", isDeleted = true, deletedAt = 5000L, updatedAt = 5000L))

        val active = dao.getActiveNotes().first()

        assertEquals(2, active.size)
        assertEquals("Active New", active[0].title)
        assertEquals("Active Old", active[1].title)
    }

    // --- Soft delete & restore ---

    @Test
    fun softDelete_noteMovesToDeletedAndLeavesActive() = runTest {
        val id = dao.insertNote(NoteEntity(title = "Note", content = ""))

        dao.softDeleteNote(id)

        assertTrue(dao.getActiveNotes().first().isEmpty())
        val deleted = dao.getDeletedNotes().first()
        assertEquals(1, deleted.size)
        assertTrue(deleted[0].isDeleted)
        assertNotNull(deleted[0].deletedAt)
    }

    @Test
    fun restoreNote_noteMovesBackToActive() = runTest {
        val id = dao.insertNote(NoteEntity(title = "Note", content = ""))
        dao.softDeleteNote(id)

        dao.restoreNote(id)

        assertTrue(dao.getDeletedNotes().first().isEmpty())
        assertEquals(1, dao.getActiveNotes().first().size)
        val restored = dao.getNoteById(id).first()
        assertFalse(restored!!.isDeleted)
        assertNull(restored.deletedAt)
    }

    // --- Toggle favorite ---

    @Test
    fun toggleFavorite_on_noteAppearsInFavorites() = runTest {
        val id = dao.insertNote(NoteEntity(title = "Note", content = ""))

        dao.toggleFavorite(id, true)

        val favorites = dao.getFavoriteNotes().first()
        assertEquals(1, favorites.size)
        assertEquals(id, favorites[0].id)
    }

    @Test
    fun toggleFavorite_off_noteDisappearsFromFavorites() = runTest {
        val id = dao.insertNote(NoteEntity(title = "Note", content = ""))
        dao.toggleFavorite(id, true)

        dao.toggleFavorite(id, false)

        assertTrue(dao.getFavoriteNotes().first().isEmpty())
    }

    // --- Toggle archive ---

    @Test
    fun toggleArchive_on_noteMovesToArchivedAndLeavesActive() = runTest {
        val id = dao.insertNote(NoteEntity(title = "Note", content = ""))

        dao.toggleArchive(id, true)

        assertTrue(dao.getActiveNotes().first().isEmpty())
        val archived = dao.getArchivedNotes().first()
        assertEquals(1, archived.size)
        assertTrue(archived[0].isArchived)
    }

    @Test
    fun toggleArchive_off_noteMovesBackToActive() = runTest {
        val id = dao.insertNote(NoteEntity(title = "Note", content = ""))
        dao.toggleArchive(id, true)

        dao.toggleArchive(id, false)

        assertTrue(dao.getArchivedNotes().first().isEmpty())
        assertEquals(1, dao.getActiveNotes().first().size)
    }

    // --- Search ---

    @Test
    fun searchNotes_matchesByTitle() = runTest {
        dao.insertNote(NoteEntity(title = "Kotlin Tips", content = "Use coroutines"))
        dao.insertNote(NoteEntity(title = "Android Guide", content = "Room database"))
        dao.insertNote(NoteEntity(title = "Cooking", content = "Recipe ideas"))

        val results = dao.searchNotes("Kotlin").first()

        assertEquals(1, results.size)
        assertEquals("Kotlin Tips", results[0].title)
    }

    @Test
    fun searchNotes_matchesByContent() = runTest {
        dao.insertNote(NoteEntity(title = "Kotlin Tips", content = "Use coroutines"))
        dao.insertNote(NoteEntity(title = "Android Guide", content = "Room database"))

        val results = dao.searchNotes("Room").first()

        assertEquals(1, results.size)
        assertEquals("Android Guide", results[0].title)
    }

    @Test
    fun searchNotes_noMatch_returnsEmpty() = runTest {
        dao.insertNote(NoteEntity(title = "Kotlin Tips", content = "Use coroutines"))

        val results = dao.searchNotes("Flutter").first()

        assertTrue(results.isEmpty())
    }

    @Test
    fun searchNotes_excludesDeletedNotes() = runTest {
        dao.insertNote(NoteEntity(title = "Kotlin Tips", content = ""))
        val deletedId = dao.insertNote(NoteEntity(title = "Kotlin Deleted", content = ""))
        dao.softDeleteNote(deletedId)

        val results = dao.searchNotes("Kotlin").first()

        assertEquals(1, results.size)
        assertEquals("Kotlin Tips", results[0].title)
    }

    // --- Purge ---

    @Test
    fun purgeOldDeletedNotes_removesExpiredKeepsRecent() = runTest {
        val now = System.currentTimeMillis()
        val thirtyDaysMs = 30L * 24 * 60 * 60 * 1000
        val cutoff = now - thirtyDaysMs

        dao.insertNote(NoteEntity(title = "Old", content = "", isDeleted = true, deletedAt = cutoff - 1000L, updatedAt = cutoff))
        dao.insertNote(NoteEntity(title = "Recent", content = "", isDeleted = true, deletedAt = now, updatedAt = now))

        dao.purgeOldDeletedNotes(cutoff)

        val remaining = dao.getDeletedNotes().first()
        assertEquals(1, remaining.size)
        assertEquals("Recent", remaining[0].title)
    }

    @Test
    fun purgeOldDeletedNotes_doesNotTouchActiveNotes() = runTest {
        val now = System.currentTimeMillis()
        val cutoff = now - 1000L

        dao.insertNote(NoteEntity(title = "Active", content = "", updatedAt = now - 2000L))

        dao.purgeOldDeletedNotes(cutoff)

        assertEquals(1, dao.getActiveNotes().first().size)
    }

    // --- Update ---

    @Test
    fun updateNote_persistsTitleAndContentChanges() = runTest {
        val id = dao.insertNote(NoteEntity(title = "Original Title", content = "Original Content"))
        val original = dao.getNoteById(id).first()!!

        dao.updateNote(original.copy(title = "Updated Title", content = "Updated Content", updatedAt = original.updatedAt + 1000L))

        val updated = dao.getNoteById(id).first()
        assertEquals("Updated Title", updated!!.title)
        assertEquals("Updated Content", updated.content)
    }

    @Test
    fun updateNote_doesNotAffectOtherNotes() = runTest {
        val id1 = dao.insertNote(NoteEntity(title = "Note 1", content = "Content 1"))
        val id2 = dao.insertNote(NoteEntity(title = "Note 2", content = "Content 2"))
        val note1 = dao.getNoteById(id1).first()!!

        dao.updateNote(note1.copy(title = "Note 1 Updated"))

        val note2 = dao.getNoteById(id2).first()
        assertEquals("Note 2", note2!!.title)
    }
}
