package com.mindvault.app.ui.screens.trash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mindvault.app.data.model.Note
import com.mindvault.app.data.repository.AttachmentRepositoryInterface
import com.mindvault.app.data.repository.NoteRepositoryInterface
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TrashUiState(
    val notes: List<Note> = emptyList(),
    val isLoading: Boolean = true,
)

@HiltViewModel
class TrashViewModel @Inject constructor(
    private val repository: NoteRepositoryInterface,
    private val attachmentRepository: AttachmentRepositoryInterface,
) : ViewModel() {

    val uiState: StateFlow<TrashUiState> = repository.getDeletedNotes()
        .map { TrashUiState(notes = it, isLoading = false) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = TrashUiState(),
        )

    fun restoreNote(id: Long) {
        viewModelScope.launch { repository.restoreNote(id) }
    }

    fun permanentlyDeleteNote(id: Long) {
        viewModelScope.launch {
            attachmentRepository.deleteAllAttachmentsForNote(id)
            repository.permanentlyDeleteNote(id)
        }
    }

    fun emptyTrash() {
        viewModelScope.launch {
            repository.getDeletedNotes().first().forEach { note ->
                attachmentRepository.deleteAllAttachmentsForNote(note.id)
                repository.permanentlyDeleteNote(note.id)
            }
        }
    }
}
