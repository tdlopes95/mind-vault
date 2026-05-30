package com.mindvault.app.ui.screens.trash

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mindvault.app.data.model.Note
import com.mindvault.app.ui.components.EmptyState
import com.mindvault.app.ui.components.NoteCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrashScreen(
    onNavigateBack: () -> Unit,
    viewModel: TrashViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showEmptyTrashDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Trash") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (uiState.notes.isNotEmpty()) {
                        TextButton(onClick = { showEmptyTrashDialog = true }) {
                            Text("Empty", color = MaterialTheme.colorScheme.error)
                        }
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            if (uiState.notes.isNotEmpty()) {
                Text(
                    text = "Notes in trash are automatically deleted after 30 days",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
            Box(modifier = Modifier.fillMaxSize()) {
                if (!uiState.isLoading && uiState.notes.isEmpty()) {
                    EmptyState(
                        message = "Trash is empty",
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    TrashGrid(
                        notes = uiState.notes,
                        onRestore = { note -> viewModel.restoreNote(note.id) },
                        onDeletePermanently = { note -> viewModel.permanentlyDeleteNote(note.id) },
                    )
                }
            }
        }
    }

    if (showEmptyTrashDialog) {
        AlertDialog(
            onDismissRequest = { showEmptyTrashDialog = false },
            title = { Text("Empty trash?") },
            text = { Text("All ${uiState.notes.size} notes will be permanently deleted. This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.emptyTrash()
                    showEmptyTrashDialog = false
                }) { Text("Empty trash", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showEmptyTrashDialog = false }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun TrashGrid(
    notes: List<Note>,
    onRestore: (Note) -> Unit,
    onDeletePermanently: (Note) -> Unit,
) {
    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Fixed(2),
        contentPadding = PaddingValues(8.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        items(notes, key = { it.id }) { note ->
            NoteCard(
                note = note,
                onClick = {},
                onLongClick = {},
                onFavoriteToggle = {},
            )
        }
    }
}
