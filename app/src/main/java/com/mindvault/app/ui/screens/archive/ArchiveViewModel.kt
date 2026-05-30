package com.mindvault.app.ui.screens.archive

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mindvault.app.data.model.Note
import com.mindvault.app.data.repository.NoteRepositoryInterface
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ArchiveUiState(
    val notes: List<Note> = emptyList(),
    val isLoading: Boolean = true,
)

@HiltViewModel
class ArchiveViewModel @Inject constructor(
    private val repository: NoteRepositoryInterface,
) : ViewModel() {

    val uiState: StateFlow<ArchiveUiState> = repository.getArchivedNotes()
        .map { ArchiveUiState(notes = it, isLoading = false) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ArchiveUiState(),
        )

    fun unarchiveNote(id: Long) {
        viewModelScope.launch { repository.toggleArchive(id, isArchived = false) }
    }

    fun deleteNote(id: Long) {
        viewModelScope.launch { repository.softDeleteNote(id) }
    }
}
