package com.mindvault.app.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mindvault.app.data.model.Note
import com.mindvault.app.data.repository.NoteRepositoryInterface
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val notes: List<Note> = emptyList(),
    val isLoading: Boolean = true,
    val searchQuery: String = "",
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: NoteRepositoryInterface,
) : ViewModel() {

    private val searchQuery = MutableStateFlow("")

    @OptIn(ExperimentalCoroutinesApi::class)
    private val notesFlow = searchQuery.flatMapLatest { query ->
        if (query.isBlank()) repository.getActiveNotes()
        else repository.searchNotes(query)
    }

    val uiState: StateFlow<HomeUiState> = combine(notesFlow, searchQuery) { notes, query ->
        HomeUiState(notes = notes, isLoading = false, searchQuery = query)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState(),
    )

    fun onSearchQueryChanged(query: String) {
        searchQuery.value = query
    }

    fun deleteNote(id: Long) {
        viewModelScope.launch { repository.softDeleteNote(id) }
    }

    fun toggleFavorite(note: Note) {
        viewModelScope.launch { repository.toggleFavorite(note.id, !note.isFavorite) }
    }

    fun archiveNote(id: Long) {
        viewModelScope.launch { repository.toggleArchive(id, isArchived = true) }
    }

    fun restoreNote(id: Long) {
        viewModelScope.launch { repository.restoreNote(id) }
    }

    fun changeNoteColor(note: Note, color: Int) {
        viewModelScope.launch { repository.updateNote(note.copy(color = color)) }
    }
}
