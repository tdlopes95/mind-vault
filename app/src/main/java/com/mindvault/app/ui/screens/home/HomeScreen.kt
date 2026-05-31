package com.mindvault.app.ui.screens.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mindvault.app.data.model.Category
import com.mindvault.app.data.model.Note
import com.mindvault.app.ui.components.CollapsibleSearchBar
import com.mindvault.app.ui.components.ConfirmDeleteDialog
import com.mindvault.app.ui.components.EmptyState
import com.mindvault.app.ui.components.NoteCard
import com.mindvault.app.ui.components.NoteOptionsBottomSheet
import com.mindvault.app.ui.components.TagFilterChip
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNoteClick: (Long) -> Unit,
    onNewNote: () -> Unit,
    onOpenSettings: () -> Unit = {},
    onOpenDrawer: (() -> Unit)? = null,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val haptic = LocalHapticFeedback.current

    var searchVisible by rememberSaveable { mutableStateOf(false) }
    var searchFocused by remember { mutableStateOf(false) }
    var noteForOptions by remember { mutableStateOf<Note?>(null) }
    var noteToDelete by remember { mutableStateOf<Note?>(null) }
    var isFabVisible by rememberSaveable { mutableStateOf(true) }

    val fabScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (available.y < -10) isFabVisible = false
                else if (available.y > 10) isFabVisible = true
                return Offset.Zero
            }
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        AnimatedVisibility(visible = !searchVisible, enter = fadeIn(), exit = fadeOut()) {
                            Text("MindVault")
                        }
                        CollapsibleSearchBar(
                            visible = searchVisible,
                            query = uiState.searchQuery,
                            onQueryChange = viewModel::onSearchQueryChanged,
                            onOpen = { searchVisible = true; searchFocused = true },
                            onClose = {
                                searchVisible = false
                                searchFocused = false
                                viewModel.onSearchQueryChanged("")
                            },
                        )
                    },
                    navigationIcon = {
                        if (onOpenDrawer != null) {
                            IconButton(onClick = onOpenDrawer) {
                                Icon(Icons.Default.Menu, contentDescription = "Open menu")
                            }
                        }
                    },
                    actions = {
                        IconButton(onClick = viewModel::toggleDashboardMode) {
                            Icon(
                                imageVector = if (uiState.isDashboardMode) Icons.Default.GridView else Icons.Default.Dashboard,
                                contentDescription = if (uiState.isDashboardMode) "Grid view" else "Dashboard view",
                            )
                        }
                        IconButton(onClick = onOpenSettings) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings")
                        }
                    },
                    scrollBehavior = scrollBehavior,
                )

                if (searchVisible && uiState.searchQuery.isBlank() && uiState.recentSearches.isNotEmpty()) {
                    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("Recent", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                            TextButton(onClick = viewModel::clearRecentSearches) {
                                Text("Clear", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            items(uiState.recentSearches) { query ->
                                AssistChip(onClick = { viewModel.onSearchQueryChanged(query) }, label = { Text(query) })
                            }
                        }
                    }
                }

                if (searchVisible && uiState.searchQuery.isNotBlank()) {
                    SearchFilterRow(
                        filters = uiState.searchFilters,
                        categories = uiState.categories,
                        onDateRange = viewModel::setSearchDateRange,
                        onCategoryFilter = viewModel::setSearchCategory,
                        onFavoritesToggle = viewModel::toggleSearchFavorites,
                        onClearAll = viewModel::clearSearchFilters,
                    )
                }

                if (uiState.allTags.isNotEmpty() && !searchVisible && !uiState.isDashboardMode) {
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        items(uiState.allTags, key = { it.id }) { tag ->
                            TagFilterChip(
                                tag = tag,
                                selected = tag.id == uiState.selectedTagId,
                                onClick = { viewModel.toggleTagFilter(tag.id) },
                            )
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            AnimatedVisibility(
                visible = isFabVisible,
                enter = scaleIn() + fadeIn(),
                exit = scaleOut() + fadeOut(),
            ) {
                var lastFabClick by remember { mutableLongStateOf(0L) }
                FloatingActionButton(
                    onClick = {
                        val now = System.currentTimeMillis()
                        if (now - lastFabClick > 500L) {
                            lastFabClick = now
                            onNewNote()
                        }
                    }
                ) {
                    Icon(Icons.Default.Add, contentDescription = "New note")
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
                .nestedScroll(fabScrollConnection),
        ) {
            if (uiState.isDashboardMode && uiState.searchQuery.isBlank()) {
                DashboardView(
                    uiState = uiState,
                    onNoteClick = onNoteClick,
                    onLongPress = { note -> noteForOptions = note },
                    onFavoriteToggle = viewModel::toggleFavorite,
                    onSeeAllPinned = { viewModel.setActiveFilter(HomeFilter.Pinned); viewModel.toggleDashboardMode() },
                    onSeeAllFavorites = { viewModel.setActiveFilter(HomeFilter.Favorites); viewModel.toggleDashboardMode() },
                    onSeeAllRecent = { viewModel.setActiveFilter(HomeFilter.All); viewModel.toggleDashboardMode() },
                    onNewNote = onNewNote,
                )
            } else {
                Column {
                    if (uiState.fuzzyCorrection != null && uiState.searchQuery.isNotBlank()) {
                        FuzzyBanner(
                            correction = uiState.fuzzyCorrection!!,
                            onApply = { viewModel.applyFuzzyCorrection(it) },
                        )
                    }

                    if (!uiState.isLoading && uiState.notes.isEmpty()) {
                        EmptyState(
                            message = if (uiState.searchQuery.isBlank()) "Your vault is empty"
                            else "No notes found for '${uiState.searchQuery}'. Try different keywords or check your filters.",
                        )
                    } else {
                        NoteGrid(
                            notes = uiState.notes,
                            onNoteClick = { id ->
                                if (uiState.searchQuery.isNotBlank()) viewModel.onSearchSubmitted(uiState.searchQuery)
                                onNoteClick(id)
                            },
                            onLongPress = { note -> noteForOptions = note },
                            onFavoriteToggle = viewModel::toggleFavorite,
                            onSwipeDelete = { note ->
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.deleteNote(note.id)
                                scope.launch {
                                    val result = snackbarHostState.showSnackbar(
                                        message = "Note deleted",
                                        actionLabel = "Undo",
                                        duration = SnackbarDuration.Short,
                                    )
                                    if (result == SnackbarResult.ActionPerformed) viewModel.restoreNote(note.id)
                                }
                            },
                        )
                    }
                }
            }
        }
    }

    noteForOptions?.let { note ->
        NoteOptionsBottomSheet(
            isFavorite = note.isFavorite,
            isPinned = note.isPinned,
            selectedColor = note.color,
            onFavoriteToggle = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                viewModel.toggleFavorite(note)
            },
            onPinToggle = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                viewModel.togglePin(note)
            },
            onArchive = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                viewModel.archiveNote(note.id)
            },
            onDelete = { noteToDelete = note },
            onColorSelected = { color -> viewModel.changeNoteColor(note, color) },
            onDismiss = { noteForOptions = null },
        )
    }

    noteToDelete?.let { note ->
        ConfirmDeleteDialog(
            onConfirm = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                viewModel.deleteNote(note.id)
                noteToDelete = null
                noteForOptions = null
            },
            onDismiss = { noteToDelete = null },
        )
    }
}

@Composable
private fun DashboardView(
    uiState: HomeUiState,
    onNoteClick: (Long) -> Unit,
    onLongPress: (Note) -> Unit,
    onFavoriteToggle: (Note) -> Unit,
    onSeeAllPinned: () -> Unit,
    onSeeAllFavorites: () -> Unit,
    onSeeAllRecent: () -> Unit,
    onNewNote: () -> Unit,
) {
    LazyColumn(
        contentPadding = PaddingValues(bottom = 88.dp),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                AssistChip(
                    onClick = onNewNote,
                    label = { Text("New Note") },
                    leadingIcon = { Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp)) },
                )
            }
        }

        if (uiState.pinnedNotes.isNotEmpty()) {
            item {
                SectionHeader(title = "Pinned", onSeeAll = onSeeAllPinned.takeIf { uiState.pinnedNotes.size > 4 })
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(uiState.pinnedNotes.take(4), key = { it.id }) { note ->
                        NoteCard(
                            note = note,
                            onClick = { onNoteClick(note.id) },
                            onLongClick = { onLongPress(note) },
                            onFavoriteToggle = { onFavoriteToggle(note) },
                            modifier = Modifier.width(160.dp),
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        if (uiState.recentNotes.isNotEmpty()) {
            item {
                SectionHeader(title = "Recent", onSeeAll = onSeeAllRecent)
            }
            items(uiState.recentNotes, key = { "recent_${it.id}" }) { note ->
                NoteCard(
                    note = note,
                    onClick = { onNoteClick(note.id) },
                    onLongClick = { onLongPress(note) },
                    onFavoriteToggle = { onFavoriteToggle(note) },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                )
            }
        }

        if (uiState.favoriteNotes.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                SectionHeader(title = "Favorites", onSeeAll = onSeeAllFavorites.takeIf { uiState.favoriteNotes.size > 4 })
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(uiState.favoriteNotes.take(4), key = { "fav_${it.id}" }) { note ->
                        NoteCard(
                            note = note,
                            onClick = { onNoteClick(note.id) },
                            onLongClick = { onLongPress(note) },
                            onFavoriteToggle = { onFavoriteToggle(note) },
                            modifier = Modifier.width(160.dp),
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        if (uiState.categories.isNotEmpty()) {
            item {
                SectionHeader(title = "Categories", onSeeAll = null)
            }
            items(
                uiState.categories.chunked(2),
                key = { chunk -> "cat_${chunk.first().id}" },
            ) { chunk ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    chunk.forEach { category ->
                        CategoryCard(
                            category = category,
                            noteCount = uiState.categoryNoteCounts[category.id] ?: 0,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    if (chunk.size == 1) Spacer(modifier = Modifier.weight(1f))
                }
            }
            item { Spacer(modifier = Modifier.height(8.dp)) }
        }

        if (uiState.pinnedNotes.isEmpty() && uiState.recentNotes.isEmpty() && uiState.favoriteNotes.isEmpty() && uiState.categories.isEmpty()) {
            item {
                EmptyState(message = "Your vault is empty")
            }
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    onSeeAll: (() -> Unit)?,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )
        if (onSeeAll != null) {
            TextButton(onClick = onSeeAll) {
                Text("See all", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
private fun CategoryCard(
    category: Category,
    noteCount: Int,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(12.dp),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(if (category.color != 0) Color(category.color) else MaterialTheme.colorScheme.primary),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = category.name,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                )
                Text(
                    text = "$noteCount notes",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
        }
    }
}

@Composable
private fun FuzzyBanner(
    correction: String,
    onApply: (String) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.tertiaryContainer,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Did you mean: ",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = { onApply(correction) }) {
                Text(
                    text = correction,
                    style = MaterialTheme.typography.bodySmall,
                    fontStyle = FontStyle.Italic,
                )
            }
        }
    }
}

@Composable
private fun SearchFilterRow(
    filters: SearchFilters,
    categories: List<Category>,
    onDateRange: (DateRange?) -> Unit,
    onCategoryFilter: (Long?) -> Unit,
    onFavoritesToggle: () -> Unit,
    onClearAll: () -> Unit,
) {
    val hasActiveFilters = filters.dateRange != null || filters.categoryId != null || filters.favoritesOnly

    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (hasActiveFilters) {
            item {
                AssistChip(onClick = onClearAll, label = { Text("Clear filters") })
            }
        }
        item {
            FilterChip(
                selected = filters.dateRange == DateRange.LAST_7_DAYS,
                onClick = {
                    onDateRange(if (filters.dateRange == DateRange.LAST_7_DAYS) null else DateRange.LAST_7_DAYS)
                },
                label = { Text("Last 7 days") },
            )
        }
        item {
            FilterChip(
                selected = filters.dateRange == DateRange.LAST_30_DAYS,
                onClick = {
                    onDateRange(if (filters.dateRange == DateRange.LAST_30_DAYS) null else DateRange.LAST_30_DAYS)
                },
                label = { Text("Last 30 days") },
            )
        }
        item {
            FilterChip(
                selected = filters.favoritesOnly,
                onClick = onFavoritesToggle,
                label = { Text("Favorites") },
            )
        }
        if (categories.isNotEmpty()) {
            items(categories, key = { "cat_filter_${it.id}" }) { cat ->
                FilterChip(
                    selected = filters.categoryId == cat.id,
                    onClick = {
                        onCategoryFilter(if (filters.categoryId == cat.id) null else cat.id)
                    },
                    label = { Text(cat.name) },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NoteGrid(
    notes: List<Note>,
    onNoteClick: (Long) -> Unit,
    onLongPress: (Note) -> Unit,
    onFavoriteToggle: (Note) -> Unit,
    onSwipeDelete: (Note) -> Unit,
) {
    var hasAnimated by rememberSaveable { mutableStateOf(false) }

    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Fixed(2),
        contentPadding = PaddingValues(8.dp),
    ) {
        itemsIndexed(notes, key = { _, note -> note.id }) { index, note ->
            val animAlpha = remember(note.id) { Animatable(if (hasAnimated) 1f else 0f) }

            LaunchedEffect(note.id) {
                if (animAlpha.value < 1f) {
                    kotlinx.coroutines.delay((index * 40L).coerceAtMost(400L))
                    animAlpha.animateTo(1f, tween(200))
                }
            }

            LaunchedEffect(notes.isNotEmpty()) {
                if (notes.isNotEmpty()) hasAnimated = true
            }

            val dismissState = rememberSwipeToDismissBoxState(
                confirmValueChange = { value ->
                    if (value == SwipeToDismissBoxValue.StartToEnd || value == SwipeToDismissBoxValue.EndToStart) {
                        onSwipeDelete(note)
                        true
                    } else false
                },
            )

            LaunchedEffect(note.id) { dismissState.reset() }

            val swipeBgColor by animateColorAsState(
                targetValue = if (dismissState.targetValue != SwipeToDismissBoxValue.Settled)
                    MaterialTheme.colorScheme.error
                else Color.Transparent,
                label = "swipe_bg",
            )

            SwipeToDismissBox(
                state = dismissState,
                backgroundContent = {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(4.dp)
                            .background(swipeBgColor, RoundedCornerShape(12.dp))
                            .padding(horizontal = 20.dp),
                        contentAlignment = Alignment.CenterEnd,
                    ) {
                        if (dismissState.targetValue != SwipeToDismissBoxValue.Settled) {
                            Icon(Icons.Default.Delete, contentDescription = null, tint = Color.White)
                        }
                    }
                },
                modifier = Modifier.padding(4.dp).alpha(animAlpha.value),
            ) {
                NoteCard(
                    note = note,
                    onClick = { onNoteClick(note.id) },
                    onLongClick = { onLongPress(note) },
                    onFavoriteToggle = { onFavoriteToggle(note) },
                )
            }
        }
    }
}
