package com.mindvault.app.ui.screens.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mindvault.app.data.model.Note
import com.mindvault.app.ui.components.CollapsibleSearchBar
import com.mindvault.app.ui.components.ConfirmDeleteDialog
import com.mindvault.app.ui.components.EmptyState
import com.mindvault.app.ui.components.NoteCard
import com.mindvault.app.ui.components.NoteOptionsBottomSheet
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNoteClick: (Long) -> Unit,
    onNewNote: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    var searchVisible by rememberSaveable { mutableStateOf(false) }
    var noteForOptions by remember { mutableStateOf<Note?>(null) }
    var noteToDelete by remember { mutableStateOf<Note?>(null) }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = {
                    AnimatedVisibility(visible = !searchVisible, enter = fadeIn(), exit = fadeOut()) {
                        Text("MindVault")
                    }
                    CollapsibleSearchBar(
                        visible = searchVisible,
                        query = uiState.searchQuery,
                        onQueryChange = viewModel::onSearchQueryChanged,
                        onOpen = { searchVisible = true },
                        onClose = {
                            searchVisible = false
                            viewModel.onSearchQueryChanged("")
                        },
                    )
                },
                scrollBehavior = scrollBehavior,
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onNewNote) {
                Icon(Icons.Default.Add, contentDescription = "New note")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding(),
        ) {
            if (!uiState.isLoading && uiState.notes.isEmpty()) {
                EmptyState(
                    message = if (uiState.searchQuery.isBlank()) "Your vault is empty" else "No notes match your search",
                )
            } else {
                NoteGrid(
                    notes = uiState.notes,
                    onNoteClick = onNoteClick,
                    onLongPress = { note -> noteForOptions = note },
                    onFavoriteToggle = viewModel::toggleFavorite,
                    onSwipeDelete = { note ->
                        viewModel.deleteNote(note.id)
                        scope.launch {
                            val result = snackbarHostState.showSnackbar(
                                message = "Note deleted",
                                actionLabel = "Undo",
                                duration = SnackbarDuration.Short,
                            )
                            if (result == SnackbarResult.ActionPerformed) {
                                viewModel.restoreNote(note.id)
                            }
                        }
                    },
                )
            }
        }
    }

    noteForOptions?.let { note ->
        NoteOptionsBottomSheet(
            isFavorite = note.isFavorite,
            selectedColor = note.color,
            onFavoriteToggle = { viewModel.toggleFavorite(note) },
            onArchive = { viewModel.archiveNote(note.id) },
            onDelete = { noteToDelete = note },
            onColorSelected = { color -> viewModel.changeNoteColor(note, color) },
            onDismiss = { noteForOptions = null },
        )
    }

    noteToDelete?.let { note ->
        ConfirmDeleteDialog(
            onConfirm = {
                viewModel.deleteNote(note.id)
                noteToDelete = null
                noteForOptions = null
            },
            onDismiss = { noteToDelete = null },
        )
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
    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Fixed(2),
        contentPadding = PaddingValues(8.dp),
    ) {
        items(notes, key = { it.id }) { note ->
            val dismissState = rememberSwipeToDismissBoxState(
                confirmValueChange = { value ->
                    if (value == SwipeToDismissBoxValue.StartToEnd || value == SwipeToDismissBoxValue.EndToStart) {
                        onSwipeDelete(note)
                        true
                    } else false
                },
            )

            LaunchedEffect(note.id) {
                dismissState.reset()
            }

            SwipeToDismissBox(
                state = dismissState,
                backgroundContent = {},
                modifier = Modifier.padding(4.dp),
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
