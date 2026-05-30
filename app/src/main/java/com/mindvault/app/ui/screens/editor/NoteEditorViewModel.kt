package com.mindvault.app.ui.screens.editor

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mindvault.app.data.model.Attachment
import com.mindvault.app.data.model.Category
import com.mindvault.app.data.model.Note
import com.mindvault.app.data.model.Tag
import com.mindvault.app.data.repository.AttachmentRepositoryInterface
import com.mindvault.app.data.repository.CategoryRepositoryInterface
import com.mindvault.app.data.repository.NoteLinkRepositoryInterface
import com.mindvault.app.data.repository.NoteRepositoryInterface
import com.mindvault.app.data.repository.TagRepositoryInterface
import com.mindvault.app.domain.analysis.CategorySuggestionEngine
import com.mindvault.app.domain.analysis.RelatedNote
import com.mindvault.app.domain.analysis.RelatedNotesEngine
import com.mindvault.app.domain.analysis.TagSuggestionEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CategorySuggestion(
    val category: Category,
    val confidence: Double,
    val reason: String,
)

data class EditorUiState(
    val title: String = "",
    val content: String = "",
    val color: Int = 0,
    val isFavorite: Boolean = false,
    val isPinned: Boolean = false,
    val isNewNote: Boolean = true,
    val lastEditedTimestamp: Long = System.currentTimeMillis(),
    val shouldNavigateBack: Boolean = false,
    val tags: List<Tag> = emptyList(),
    val allTags: List<Tag> = emptyList(),
    val categoryId: Long? = null,
    val category: Category? = null,
    val allCategories: List<Category> = emptyList(),
    val linkedNotes: List<Note> = emptyList(),
    val availableForLinking: List<Note> = emptyList(),
    val attachments: List<Attachment> = emptyList(),
    val isPreviewMode: Boolean = false,
    val navigateToNoteId: Long? = null,
    // Task 1: tag suggestions
    val suggestedTags: List<String> = emptyList(),
    // Task 2: related notes
    val relatedNotes: List<RelatedNote> = emptyList(),
    val isLoadingRelated: Boolean = false,
    val relatedNotesExpanded: Boolean = false,
    // Task 4: category suggestion
    val categorySuggestion: CategorySuggestion? = null,
)

@HiltViewModel
class NoteEditorViewModel @Inject constructor(
    private val repository: NoteRepositoryInterface,
    private val tagRepository: TagRepositoryInterface,
    private val categoryRepository: CategoryRepositoryInterface,
    private val noteLinkRepository: NoteLinkRepositoryInterface,
    private val attachmentRepository: AttachmentRepositoryInterface,
    private val tagSuggestionEngine: TagSuggestionEngine,
    private val relatedNotesEngine: RelatedNotesEngine,
    private val categorySuggestionEngine: CategorySuggestionEngine,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val initialNoteId: Long? =
        savedStateHandle.get<Long>("noteId")?.takeIf { it != -1L }

    private var savedNoteId: Long? = initialNoteId
    private var loadedNote: Note? = null
    private val dismissedSuggestions = mutableSetOf<String>()
    private var dismissedCategorySuggestion = false
    private var relatedNotesLoaded = false

    private val _uiState = MutableStateFlow(EditorUiState(isNewNote = initialNoteId == null))
    val uiState: StateFlow<EditorUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            tagRepository.getAllTags().collect { tags ->
                _uiState.update { it.copy(allTags = tags) }
            }
        }
        viewModelScope.launch {
            categoryRepository.getAllCategories().collect { cats ->
                _uiState.update { it.copy(allCategories = cats) }
            }
        }
        if (initialNoteId != null) {
            viewModelScope.launch {
                repository.getNoteById(initialNoteId).first()?.let { note ->
                    loadedNote = note
                    val tags = tagRepository.getTagsForNote(initialNoteId).first()
                    val category = note.categoryId?.let { categoryRepository.getCategoryById(it).first() }
                    _uiState.value = EditorUiState(
                        title = note.title,
                        content = note.content,
                        color = note.color,
                        isFavorite = note.isFavorite,
                        isPinned = note.isPinned,
                        isNewNote = false,
                        lastEditedTimestamp = note.updatedAt,
                        tags = tags,
                        categoryId = note.categoryId,
                        category = category,
                        allTags = _uiState.value.allTags,
                        allCategories = _uiState.value.allCategories,
                    )
                }
            }
            viewModelScope.launch {
                noteLinkRepository.getLinkedNotes(initialNoteId).collect { linked ->
                    _uiState.update { it.copy(linkedNotes = linked) }
                }
            }
            viewModelScope.launch {
                attachmentRepository.getAttachmentsForNote(initialNoteId).collect { list ->
                    _uiState.update { it.copy(attachments = list) }
                }
            }
        }
    }

    fun onTitleChanged(title: String) { _uiState.update { it.copy(title = title) } }
    fun onContentChanged(content: String) { _uiState.update { it.copy(content = content) } }
    fun onColorSelected(color: Int) { _uiState.update { it.copy(color = color) } }
    fun toggleFavorite() { _uiState.update { it.copy(isFavorite = !it.isFavorite) } }
    fun togglePreviewMode() { _uiState.update { it.copy(isPreviewMode = !it.isPreviewMode) } }

    fun togglePin() {
        val newPinned = !_uiState.value.isPinned
        _uiState.update { it.copy(isPinned = newPinned) }
        savedNoteId?.let { id ->
            viewModelScope.launch { repository.togglePin(id, newPinned) }
        }
    }

    fun addTag(tag: Tag) {
        _uiState.update { it.copy(tags = (it.tags + tag).distinctBy { t -> t.id }) }
        savedNoteId?.let { id ->
            viewModelScope.launch { tagRepository.addTagToNote(id, tag.id) }
        }
    }

    fun removeTag(tag: Tag) {
        _uiState.update { it.copy(tags = it.tags.filter { t -> t.id != tag.id }) }
        savedNoteId?.let { id ->
            viewModelScope.launch { tagRepository.removeTagFromNote(id, tag.id) }
        }
    }

    fun createAndAddTag(name: String) {
        viewModelScope.launch {
            val tagId = tagRepository.insertTag(name)
            if (tagId > 0) {
                val newTag = tagRepository.getAllTags().first().find { it.id == tagId }
                    ?: Tag(id = tagId, name = name)
                addTag(newTag)
            }
        }
    }

    fun setCategory(category: Category?) {
        _uiState.update { it.copy(categoryId = category?.id, category = category) }
        savedNoteId?.let { id ->
            viewModelScope.launch { repository.assignCategory(id, category?.id) }
        }
    }

    fun loadAvailableForLinking() {
        viewModelScope.launch {
            val all = repository.getActiveNotes().first()
            val linkedIds = _uiState.value.linkedNotes.map { it.id }.toSet()
            val currentId = savedNoteId
            _uiState.update {
                it.copy(
                    availableForLinking = all.filter { note ->
                        note.id != currentId && note.id !in linkedIds
                    }
                )
            }
        }
    }

    fun linkNote(targetNoteId: Long) {
        val sourceId = savedNoteId ?: return
        viewModelScope.launch {
            noteLinkRepository.linkNotes(sourceId, targetNoteId)
        }
    }

    fun unlinkNote(targetNoteId: Long) {
        val sourceId = savedNoteId ?: return
        viewModelScope.launch {
            noteLinkRepository.unlinkNotes(sourceId, targetNoteId)
        }
    }

    fun navigateToLinkedNote(noteId: Long) {
        _uiState.update { it.copy(navigateToNoteId = noteId) }
    }

    fun clearNavigateToNote() {
        _uiState.update { it.copy(navigateToNoteId = null) }
    }

    fun addAttachment(uri: Uri) {
        val noteId = savedNoteId ?: return
        viewModelScope.launch {
            attachmentRepository.addAttachment(noteId, uri)
        }
    }

    fun deleteAttachment(attachment: Attachment) {
        viewModelScope.launch {
            attachmentRepository.deleteAttachment(attachment)
        }
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
                        isPinned = state.isPinned,
                        categoryId = state.categoryId,
                        createdAt = now,
                        updatedAt = now,
                    )
                )
                savedNoteId = id
                state.tags.forEach { tag -> tagRepository.addTagToNote(id, tag.id) }
                _uiState.update { it.copy(isNewNote = false, lastEditedTimestamp = now) }
                viewModelScope.launch {
                    noteLinkRepository.getLinkedNotes(id).collect { linked ->
                        _uiState.update { it.copy(linkedNotes = linked) }
                    }
                }
                viewModelScope.launch {
                    attachmentRepository.getAttachmentsForNote(id).collect { list ->
                        _uiState.update { it.copy(attachments = list) }
                    }
                }
            } else {
                val base = loadedNote ?: Note(id = currentId, createdAt = now)
                val updated = base.copy(
                    title = state.title,
                    content = state.content,
                    color = state.color,
                    isFavorite = state.isFavorite,
                    isPinned = state.isPinned,
                    categoryId = state.categoryId,
                    updatedAt = now,
                )
                repository.updateNote(updated)
                loadedNote = updated
                _uiState.update { it.copy(lastEditedTimestamp = now) }
            }
            generateAnalysisSuggestions()
        }
    }

    private fun generateAnalysisSuggestions() {
        val state = _uiState.value
        val combinedLength = state.title.length + state.content.length
        if (combinedLength < 10) return

        viewModelScope.launch {
            val existingTagNames = state.tags.map { it.name }.toSet()
            val tagDeferred = async {
                tagSuggestionEngine.suggestTags(state.title, state.content, existingTagNames)
                    .filter { it !in dismissedSuggestions }
            }
            val categoryDeferred = async {
                if (!dismissedCategorySuggestion && state.categoryId == null) {
                    categorySuggestionEngine.suggestCategory(
                        noteId = savedNoteId ?: 0L,
                        title = state.title,
                        content = state.content,
                        tags = state.tags,
                    )
                } else null
            }
            val tagSuggestions = tagDeferred.await()
            val catResult = categoryDeferred.await()
            _uiState.update { it.copy(suggestedTags = tagSuggestions, categorySuggestion = catResult) }
        }
    }

    fun acceptSuggestion(tagName: String) {
        val existing = _uiState.value.allTags.find { it.name.equals(tagName, ignoreCase = true) }
        if (existing != null) {
            addTag(existing)
        } else {
            createAndAddTag(tagName)
        }
        _uiState.update { it.copy(suggestedTags = it.suggestedTags.filter { s -> s != tagName }) }
        dismissedSuggestions.add(tagName)
    }

    fun dismissSuggestion(tagName: String) {
        dismissedSuggestions.add(tagName)
        _uiState.update { it.copy(suggestedTags = it.suggestedTags.filter { s -> s != tagName }) }
    }

    fun dismissAllSuggestions() {
        dismissedSuggestions.addAll(_uiState.value.suggestedTags)
        _uiState.update { it.copy(suggestedTags = emptyList()) }
    }

    fun toggleRelatedNotesExpanded() {
        val expanded = !_uiState.value.relatedNotesExpanded
        _uiState.update { it.copy(relatedNotesExpanded = expanded) }
        if (expanded && !relatedNotesLoaded) {
            loadRelatedNotes()
        }
    }

    private fun loadRelatedNotes() {
        relatedNotesLoaded = true
        val state = _uiState.value
        val noteId = savedNoteId ?: return
        _uiState.update { it.copy(isLoadingRelated = true) }
        viewModelScope.launch {
            val related = relatedNotesEngine.findRelatedNotes(
                noteId = noteId,
                title = state.title,
                content = state.content,
                tags = state.tags,
                categoryId = state.categoryId,
            )
            _uiState.update { it.copy(relatedNotes = related, isLoadingRelated = false) }
        }
    }

    fun acceptCategorySuggestion() {
        val suggestion = _uiState.value.categorySuggestion ?: return
        setCategory(suggestion.category)
        _uiState.update { it.copy(categorySuggestion = null) }
        dismissedCategorySuggestion = true
    }

    fun dismissCategorySuggestion() {
        dismissedCategorySuggestion = true
        _uiState.update { it.copy(categorySuggestion = null) }
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
