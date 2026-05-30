package com.mindvault.app.ui.screens.archive

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mindvault.app.data.model.Note
import com.mindvault.app.ui.components.EmptyState
import com.mindvault.app.ui.components.NoteCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArchiveScreen(
    onNoteClick: (Long) -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: ArchiveViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Archive") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            if (!uiState.isLoading && uiState.notes.isEmpty()) {
                EmptyState(
                    message = "No archived notes",
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                ArchiveGrid(
                    notes = uiState.notes,
                    onNoteClick = onNoteClick,
                    onUnarchive = { note -> viewModel.unarchiveNote(note.id) },
                    onDelete = { note -> viewModel.deleteNote(note.id) },
                )
            }
        }
    }
}

@Composable
private fun ArchiveGrid(
    notes: List<Note>,
    onNoteClick: (Long) -> Unit,
    onUnarchive: (Note) -> Unit,
    onDelete: (Note) -> Unit,
) {
    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Fixed(2),
        contentPadding = PaddingValues(8.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        items(notes, key = { it.id }) { note ->
            NoteCard(
                note = note,
                onClick = { onNoteClick(note.id) },
                onLongClick = {},
                onFavoriteToggle = {},
            )
        }
    }
}
