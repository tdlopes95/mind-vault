package com.mindvault.app.ui.screens.editor

import androidx.lifecycle.SavedStateHandle
import com.mindvault.app.data.model.Note
import com.mindvault.app.data.repository.NoteRepositoryInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NoteEditorViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRepo: FakeNoteRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepo = FakeNoteRepository()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(noteId: Long? = null): NoteEditorViewModel {
        val handle = SavedStateHandle(mapOf("noteId" to (noteId ?: -1L)))
        return NoteEditorViewModel(fakeRepo, handle)
    }

    @Test
    fun `new note — initial state is blank`() = runTest {
        val vm = createViewModel(noteId = null)
        advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals("", state.title)
        assertEquals("", state.content)
        assertEquals(0, state.color)
        assertFalse(state.isFavorite)
        assertTrue(state.isNewNote)
    }

    @Test
    fun `edit note — loads existing data`() = runTest {
        val seeded = fakeRepo.seed(Note(title = "Loaded", content = "Body", color = 0xFFFFAB91.toInt(), isFavorite = true))
        val vm = createViewModel(noteId = seeded.id)
        advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals("Loaded", state.title)
        assertEquals("Body", state.content)
        assertEquals(0xFFFFAB91.toInt(), state.color)
        assertTrue(state.isFavorite)
        assertFalse(state.isNewNote)
    }

    @Test
    fun `onTitleChanged updates state`() = runTest {
        val vm = createViewModel()
        vm.onTitleChanged("My Title")

        assertEquals("My Title", vm.uiState.value.title)
    }

    @Test
    fun `onContentChanged updates state`() = runTest {
        val vm = createViewModel()
        vm.onContentChanged("Body text")

        assertEquals("Body text", vm.uiState.value.content)
    }

    @Test
    fun `saveNote — creates new note in repo`() = runTest {
        val vm = createViewModel(noteId = null)
        vm.onTitleChanged("My Note")
        vm.onContentChanged("Some content")
        vm.saveNote()
        advanceUntilIdle()

        val notes = fakeRepo.allNotes()
        assertEquals(1, notes.size)
        assertEquals("My Note", notes.first().title)
        assertEquals("Some content", notes.first().content)
    }

    @Test
    fun `saveNote — updates existing note in repo`() = runTest {
        val seeded = fakeRepo.seed(Note(title = "Old Title", content = "Old content", updatedAt = 1000L))
        val vm = createViewModel(noteId = seeded.id)
        advanceUntilIdle()

        vm.onTitleChanged("New Title")
        vm.saveNote()
        advanceUntilIdle()

        val updated = fakeRepo.allNotes().first()
        assertEquals("New Title", updated.title)
        assertTrue(updated.updatedAt > 1000L)
    }

    @Test
    fun `saveNote — skips save when both title and content are empty`() = runTest {
        val vm = createViewModel(noteId = null)
        vm.saveNote()
        advanceUntilIdle()

        assertEquals(0, fakeRepo.allNotes().size)
    }

    @Test
    fun `onColorSelected updates state and persists on save`() = runTest {
        val vm = createViewModel(noteId = null)
        vm.onTitleChanged("Colored note")
        vm.onColorSelected(0xFFFFF59D.toInt())

        assertEquals(0xFFFFF59D.toInt(), vm.uiState.value.color)

        vm.saveNote()
        advanceUntilIdle()

        assertEquals(0xFFFFF59D.toInt(), fakeRepo.allNotes().first().color)
    }

    @Test
    fun `toggleFavorite flips state`() = runTest {
        val vm = createViewModel(noteId = null)
        assertFalse(vm.uiState.value.isFavorite)

        vm.toggleFavorite()
        assertTrue(vm.uiState.value.isFavorite)

        vm.toggleFavorite()
        assertFalse(vm.uiState.value.isFavorite)
    }
}

// ---------------------------------------------------------------------------
// Fake repository
// ---------------------------------------------------------------------------

private class FakeNoteRepository : NoteRepositoryInterface {

    private val notes = mutableListOf<Note>()
    private var nextId = 1L

    /** Inserts a pre-built note, assigning a stable ID. Returns the stored copy. */
    fun seed(note: Note): Note {
        val stored = note.copy(id = nextId++)
        notes.add(stored)
        return stored
    }

    fun allNotes(): List<Note> = notes.toList()

    override fun getActiveNotes(): Flow<List<Note>> = flow { emit(notes.filter { !it.isDeleted && !it.isArchived }) }
    override fun getArchivedNotes(): Flow<List<Note>> = flow { emit(notes.filter { it.isArchived }) }
    override fun getDeletedNotes(): Flow<List<Note>> = flow { emit(notes.filter { it.isDeleted }) }
    override fun getFavoriteNotes(): Flow<List<Note>> = flow { emit(notes.filter { it.isFavorite }) }
    override fun getNoteById(id: Long): Flow<Note?> = flow { emit(notes.find { it.id == id }) }
    override fun searchNotes(query: String): Flow<List<Note>> = flow {
        emit(notes.filter { it.title.contains(query, ignoreCase = true) || it.content.contains(query, ignoreCase = true) })
    }

    override suspend fun insertNote(note: Note): Long {
        val id = nextId++
        notes.add(note.copy(id = id))
        return id
    }

    override suspend fun updateNote(note: Note) {
        val idx = notes.indexOfFirst { it.id == note.id }
        if (idx >= 0) notes[idx] = note
    }

    override suspend fun softDeleteNote(id: Long) {
        val idx = notes.indexOfFirst { it.id == id }
        if (idx >= 0) notes[idx] = notes[idx].copy(isDeleted = true)
    }

    override suspend fun permanentlyDeleteNote(id: Long) {
        notes.removeAll { it.id == id }
    }

    override suspend fun restoreNote(id: Long) {
        val idx = notes.indexOfFirst { it.id == id }
        if (idx >= 0) notes[idx] = notes[idx].copy(isDeleted = false, deletedAt = null)
    }

    override suspend fun toggleFavorite(id: Long, isFavorite: Boolean) {
        val idx = notes.indexOfFirst { it.id == id }
        if (idx >= 0) notes[idx] = notes[idx].copy(isFavorite = isFavorite)
    }

    override suspend fun toggleArchive(id: Long, isArchived: Boolean) {
        val idx = notes.indexOfFirst { it.id == id }
        if (idx >= 0) notes[idx] = notes[idx].copy(isArchived = isArchived)
    }

    override suspend fun purgeOldDeletedNotes(cutoffTimestamp: Long) {
        notes.removeAll { it.isDeleted && (it.deletedAt ?: 0L) < cutoffTimestamp }
    }
}
