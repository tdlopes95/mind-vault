package com.mindvault.app.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mindvault.app.data.local.RecentSearchesDataStore
import com.mindvault.app.data.model.Note
import com.mindvault.app.data.model.Tag
import com.mindvault.app.data.repository.NoteRepositoryInterface
import com.mindvault.app.data.repository.TagRepositoryInterface
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class HomeFilter { All, Favorites }

data class HomeUiState(
    val notes: List<Note> = emptyList(),
    val isLoading: Boolean = true,
    val searchQuery: String = "",
    val allTags: List<Tag> = emptyList(),
    val selectedTagId: Long? = null,
    val activeFilter: HomeFilter = HomeFilter.All,
    val recentSearches: List<String> = emptyList(),
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: NoteRepositoryInterface,
    private val tagRepository: TagRepositoryInterface,
    private val recentSearches: RecentSearchesDataStore,
) : ViewModel() {

    private val searchQuery = MutableStateFlow("")
    private val selectedTagId = MutableStateFlow<Long?>(null)
    private val activeFilter = MutableStateFlow(HomeFilter.All)

    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    private val baseNotesFlow = combine(searchQuery.debounce(200), activeFilter) { q, f -> Pair(q, f) }
        .flatMapLatest { (query, filter) ->
            when {
                query.isNotBlank() -> repository.searchNotesFts(query)
                filter == HomeFilter.Favorites -> repository.getFavoriteNotes()
                else -> repository.getActiveNotes()
            }
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    private val tagNoteIdsFlow = selectedTagId.flatMapLatest { tagId ->
        if (tagId == null) flowOf(null)
        else tagRepository.getActiveNoteIdsForTag(tagId)
    }

    private val filteredNotesFlow = combine(baseNotesFlow, tagNoteIdsFlow) { notes, tagNoteIds ->
        if (tagNoteIds == null) notes
        else notes.filter { it.id in tagNoteIds }
    }

    private data class FilterState(
        val allTags: List<Tag>,
        val selectedTagId: Long?,
        val activeFilter: HomeFilter,
        val recentSearches: List<String>,
    )

    private val filterStateFlow = combine(
        tagRepository.getAllTags(),
        selectedTagId,
        activeFilter,
        recentSearches.recentSearches,
    ) { tags, tagId, filter, recent ->
        FilterState(tags, tagId, filter, recent)
    }

    val uiState: StateFlow<HomeUiState> = combine(
        filteredNotesFlow,
        searchQuery,
        filterStateFlow,
    ) { notes, query, filterState ->
        HomeUiState(
            notes = notes,
            isLoading = false,
            searchQuery = query,
            allTags = filterState.allTags,
            selectedTagId = filterState.selectedTagId,
            activeFilter = filterState.activeFilter,
            recentSearches = filterState.recentSearches,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState(),
    )

    fun onSearchQueryChanged(query: String) {
        searchQuery.value = query
    }

    fun onSearchSubmitted(query: String) {
        if (query.isNotBlank()) {
            viewModelScope.launch { recentSearches.addSearch(query) }
        }
    }

    fun clearRecentSearches() {
        viewModelScope.launch { recentSearches.clearAll() }
    }

    fun toggleTagFilter(tagId: Long) {
        selectedTagId.value = if (selectedTagId.value == tagId) null else tagId
    }

    fun setActiveFilter(filter: HomeFilter) {
        activeFilter.value = filter
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
