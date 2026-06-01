package com.mindvault.app.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mindvault.app.data.local.DashboardPreferencesDataStore
import com.mindvault.app.data.local.RecentSearchesDataStore
import com.mindvault.app.data.model.Category
import com.mindvault.app.data.model.Note
import com.mindvault.app.data.model.Tag
import com.mindvault.app.data.repository.CategoryRepositoryInterface
import com.mindvault.app.data.repository.NoteRepositoryInterface
import com.mindvault.app.data.repository.TagRepositoryInterface
import com.mindvault.app.domain.analysis.FuzzyMatcher
import com.mindvault.app.domain.analysis.SynonymMap
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

enum class HomeFilter { All, Favorites, Pinned }

enum class DateRange { LAST_7_DAYS, LAST_30_DAYS, OLDER }

data class SearchFilters(
    val dateRange: DateRange? = null,
    val categoryId: Long? = null,
    val favoritesOnly: Boolean = false,
    val hasAttachments: Boolean = false,
    val hasLinks: Boolean = false,
)

data class HomeUiState(
    val notes: List<Note> = emptyList(),
    val pinnedNotes: List<Note> = emptyList(),
    val recentNotes: List<Note> = emptyList(),
    val favoriteNotes: List<Note> = emptyList(),
    val categories: List<Category> = emptyList(),
    val categoryNoteCounts: Map<Long, Int> = emptyMap(),
    val isLoading: Boolean = true,
    val searchQuery: String = "",
    val allTags: List<Tag> = emptyList(),
    val selectedTagId: Long? = null,
    val activeFilter: HomeFilter = HomeFilter.All,
    val recentSearches: List<String> = emptyList(),
    val isDashboardMode: Boolean = true,
    val searchFilters: SearchFilters = SearchFilters(),
    val fuzzyCorrection: String? = null,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: NoteRepositoryInterface,
    private val tagRepository: TagRepositoryInterface,
    private val categoryRepository: CategoryRepositoryInterface,
    private val recentSearches: RecentSearchesDataStore,
    private val dashboardPrefs: DashboardPreferencesDataStore,
) : ViewModel() {

    private val searchQuery = MutableStateFlow("")
    private val selectedTagId = MutableStateFlow<Long?>(null)
    private val activeFilter = MutableStateFlow(HomeFilter.All)
    private val searchFilters = MutableStateFlow(SearchFilters())
    private val fuzzyCorrection = MutableStateFlow<String?>(null)

    private val _searchExpandedRequest = MutableStateFlow(false)
    val searchExpandedRequest: StateFlow<Boolean> = _searchExpandedRequest.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    private val baseNotesFlow = combine(searchQuery.debounce(200), activeFilter) { q, f -> Pair(q, f) }
        .flatMapLatest { (query, filter) ->
            when {
                query.isNotBlank() -> {
                    // FTS first; synonym/fuzzy fallback handled in filteredNotesFlow
                    repository.searchNotesFts(query).map { ftsResults ->
                        if (ftsResults.isNotEmpty()) fuzzyCorrection.value = null
                        ftsResults
                    }
                }
                filter == HomeFilter.Favorites -> repository.getFavoriteNotes().map { it.also { fuzzyCorrection.value = null } }
                filter == HomeFilter.Pinned -> repository.getPinnedNotes().map { it.also { fuzzyCorrection.value = null } }
                else -> repository.getActiveNotes().map { it.also { fuzzyCorrection.value = null } }
            }
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    private val tagNoteIdsFlow = selectedTagId.flatMapLatest { tagId ->
        if (tagId == null) flowOf(null)
        else tagRepository.getActiveNoteIdsForTag(tagId)
    }

    // Notes after tag filter but before search filters
    private val tagFilteredNotesFlow = combine(baseNotesFlow, tagNoteIdsFlow) { notes, tagNoteIds ->
        if (tagNoteIds == null) notes else notes.filter { it.id in tagNoteIds }
    }

    // Apply search filters post-FTS
    private val filteredNotesFlow = combine(
        tagFilteredNotesFlow,
        repository.getActiveNotes(),
        searchQuery.debounce(200),
        searchFilters,
    ) { tagFiltered, allActive, query, filters ->
        val now = System.currentTimeMillis()
        val cal = Calendar.getInstance()

        // When FTS returns empty and query is active, try synonym + fuzzy fallback on allActive
        var base = tagFiltered
        if (query.isNotBlank() && tagFiltered.isEmpty()) {
            // Synonym expansion: filter allActive by synonym terms
            val synonymTerms = SynonymMap.expandQuery(query)
            val synonymResults = allActive.filter { note ->
                val text = "${note.title} ${note.content}".lowercase()
                synonymTerms.any { term -> text.contains(term) }
            }
            if (synonymResults.isNotEmpty()) {
                base = synonymResults
            } else {
                // Fuzzy fallback on titles
                val titles = allActive.map { it.title }
                val queryWords = query.lowercase().split(Regex("\\s+"))
                val fuzzyMatches = allActive.filter { note ->
                    queryWords.any { word ->
                        note.title.split(Regex("\\s+")).any { titleWord ->
                            FuzzyMatcher.isFuzzyMatch(word, titleWord)
                        }
                    }
                }
                if (fuzzyMatches.isNotEmpty()) {
                    // Set a correction hint using the closest title word
                    val bestTitle = fuzzyMatches.firstOrNull()?.title ?: ""
                    fuzzyCorrection.value = bestTitle.take(30)
                    base = fuzzyMatches
                }
            }
        }

        // Apply search filters
        base.filter { note ->
            val passDate = when (filters.dateRange) {
                DateRange.LAST_7_DAYS -> {
                    cal.apply { timeInMillis = now; add(Calendar.DAY_OF_YEAR, -7) }
                    note.updatedAt >= cal.timeInMillis
                }
                DateRange.LAST_30_DAYS -> {
                    cal.apply { timeInMillis = now; add(Calendar.DAY_OF_YEAR, -30) }
                    note.updatedAt >= cal.timeInMillis
                }
                DateRange.OLDER -> {
                    cal.apply { timeInMillis = now; add(Calendar.DAY_OF_YEAR, -30) }
                    note.updatedAt < cal.timeInMillis
                }
                null -> true
            }
            val passCategory = filters.categoryId == null || note.categoryId == filters.categoryId
            val passFavorites = !filters.favoritesOnly || note.isFavorite
            passDate && passCategory && passFavorites
        }
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

    private data class DashboardState(
        val pinnedNotes: List<Note>,
        val recentNotes: List<Note>,
        val favoriteNotes: List<Note>,
        val categories: List<Category>,
        val categoryNoteCounts: Map<Long, Int>,
        val isDashboardMode: Boolean,
    )

    private val dashboardStateFlow = combine(
        repository.getPinnedNotes(),
        repository.getFavoriteNotes(),
        categoryRepository.getAllCategories(),
        dashboardPrefs.isDashboardMode,
        repository.getActiveNotes(),
    ) { pinned, favs, cats, dashMode, active ->
        val counts = active
            .filter { it.categoryId != null }
            .groupBy { it.categoryId!! }
            .mapValues { it.value.size }
        DashboardState(pinned, active.take(5), favs, cats, counts, dashMode)
    }

    val uiState: StateFlow<HomeUiState> = combine(
        filteredNotesFlow,
        combine(searchQuery, filterStateFlow) { q, fs -> Pair(q, fs) },
        combine(dashboardStateFlow, searchFilters, fuzzyCorrection) { d, f, fc -> Triple(d, f, fc) },
    ) { filteredNotes, queryAndFilter, dashAndFilters ->
        val (query, filterState) = queryAndFilter
        val (dash, filters, correction) = dashAndFilters
        HomeUiState(
            notes = filteredNotes,
            pinnedNotes = dash.pinnedNotes,
            recentNotes = dash.recentNotes,
            favoriteNotes = dash.favoriteNotes,
            categories = dash.categories,
            categoryNoteCounts = dash.categoryNoteCounts,
            isLoading = false,
            searchQuery = query,
            allTags = filterState.allTags,
            selectedTagId = filterState.selectedTagId,
            activeFilter = filterState.activeFilter,
            recentSearches = filterState.recentSearches,
            isDashboardMode = dash.isDashboardMode,
            searchFilters = filters,
            fuzzyCorrection = correction,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState(),
    )

    fun onSearchQueryChanged(query: String) {
        searchQuery.value = query
        if (query.isBlank()) {
            searchFilters.value = SearchFilters()
            fuzzyCorrection.value = null
        }
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

    fun toggleDashboardMode() {
        viewModelScope.launch {
            dashboardPrefs.setDashboardMode(!uiState.value.isDashboardMode)
        }
    }

    fun deleteNote(id: Long) {
        viewModelScope.launch { repository.softDeleteNote(id) }
    }

    fun toggleFavorite(note: Note) {
        viewModelScope.launch { repository.toggleFavorite(note.id, !note.isFavorite) }
    }

    fun togglePin(note: Note) {
        viewModelScope.launch { repository.togglePin(note.id, !note.isPinned) }
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

    fun setSearchDateRange(range: DateRange?) {
        searchFilters.value = searchFilters.value.copy(dateRange = range)
    }

    fun setSearchCategory(categoryId: Long?) {
        searchFilters.value = searchFilters.value.copy(categoryId = categoryId)
    }

    fun toggleSearchFavorites() {
        searchFilters.value = searchFilters.value.copy(favoritesOnly = !searchFilters.value.favoritesOnly)
    }

    fun clearSearchFilters() {
        searchFilters.value = SearchFilters()
    }

    fun applyFuzzyCorrection(correctedQuery: String) {
        searchQuery.value = correctedQuery
        fuzzyCorrection.value = null
    }

    fun openSearch() {
        _searchExpandedRequest.value = true
    }

    fun clearSearchExpandedRequest() {
        _searchExpandedRequest.value = false
    }
}
