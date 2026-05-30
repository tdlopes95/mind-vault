package com.mindvault.app.ui.screens.editor

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mindvault.app.data.model.Note
import com.mindvault.app.data.repository.NoteRepositoryInterface
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EditorUiState(
    val title: String = "",
    val content: String = "",
    val color: Int = 0,
    val isFavorite: Boolean = false,
    val isNewNote: Boolean = true,
    val lastEditedTimestamp: Long = System.currentTimeMillis(),
    val shouldNavigateBack: Boolean = false,
)

@HiltViewModel
class NoteEditorViewModel @Inject constructor(
    private val repository: NoteRepositoryInterface,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val initialNoteId: Long? =
        savedStateHandle.get<Long>("noteId")?.takeIf { it != -1L }

    // Tracks the persisted ID: null until first insert, then set after insert
    private var savedNoteId: Long? = initialNoteId

    // Cached loaded note — preserves fields we don't edit (createdAt, isArchived, etc.)
    private var loadedNote: Note? = null

    private val _uiState = MutableStateFlow(EditorUiState(isNewNote = initialNoteId == null))
    val uiState: StateFlow<EditorUiState> = _uiState.asStateFlow()

    init {
        if (initialNoteId != null) {
            viewModelScope.launch {
                repository.getNoteById(initialNoteId).first()?.let { note ->
                    loadedNote = note
                    _uiState.value = EditorUiState(
                        title = note.title,
                        content = note.content,
                        color = note.color,
                        isFavorite = note.isFavorite,
                        isNewNote = false,
                        lastEditedTimestamp = note.updatedAt,
                    )
                }
            }
        }
    }

    fun onTitleChanged(title: String) {
        _uiState.update { it.copy(title = title) }
    }

    fun onContentChanged(content: String) {
        _uiState.update { it.copy(content = content) }
    }

    fun onColorSelected(color: Int) {
        _uiState.update { it.copy(color = color) }
    }

    fun toggleFavorite() {
        _uiState.update { it.copy(isFavorite = !it.isFavorite) }
    }

    fun saveNote() {
        val state = _uiState.value
        if (state.title.isBlank() && state.content.isBlank()) return

        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val currentId = savedNoteId
            if (currentId == null) {
                val id = repository.insertNote(
                    Note(
                        title = state.title,
                        content = state.content,
                        color = state.color,
                        isFavorite = state.isFavorite,
                        createdAt = now,
                        updatedAt = now,
                    )
                )
                savedNoteId = id
                _uiState.update { it.copy(isNewNote = false, lastEditedTimestamp = now) }
            } else {
                val base = loadedNote ?: Note(id = currentId, createdAt = now)
                val updated = base.copy(
                    title = state.title,
                    content = state.content,
                    color = state.color,
                    isFavorite = state.isFavorite,
                    updatedAt = now,
                )
                repository.updateNote(updated)
                loadedNote = updated
                _uiState.update { it.copy(lastEditedTimestamp = now) }
            }
        }
    }

    fun deleteNote() {
        val id = savedNoteId ?: run {
            _uiState.update { it.copy(shouldNavigateBack = true) }
            return
        }
        viewModelScope.launch {
            repository.softDeleteNote(id)
            _uiState.update { it.copy(shouldNavigateBack = true) }
        }
    }

    fun archiveNote() {
        val id = savedNoteId ?: run {
            _uiState.update { it.copy(shouldNavigateBack = true) }
            return
        }
        viewModelScope.launch {
            repository.toggleArchive(id, isArchived = true)
            _uiState.update { it.copy(shouldNavigateBack = true) }
        }
    }
}
