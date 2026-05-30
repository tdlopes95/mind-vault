package com.mindvault.app.ui.screens.editor

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.AssistChip
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mindvault.app.ui.components.CategoryPickerDialog
import com.mindvault.app.ui.components.ConfirmDeleteDialog
import com.mindvault.app.ui.components.TagChipRemovable
import com.mindvault.app.ui.components.TagPickerBottomSheet
import com.mindvault.app.util.DateUtils
import com.mindvault.app.util.NoteColors

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun NoteEditorScreen(
    noteId: Long?,
    onNavigateBack: () -> Unit,
    viewModel: NoteEditorViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showMenu by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showTagPicker by remember { mutableStateOf(false) }
    var showCategoryPicker by remember { mutableStateOf(false) }

    BackHandler {
        viewModel.saveNote()
        onNavigateBack()
    }

    LaunchedEffect(uiState.shouldNavigateBack) {
        if (uiState.shouldNavigateBack) onNavigateBack()
    }

    val noteBackground = if (uiState.color != 0) Color(uiState.color)
    else MaterialTheme.colorScheme.background

    val transparentFieldColors = TextFieldDefaults.colors(
        focusedContainerColor = Color.Transparent,
        unfocusedContainerColor = Color.Transparent,
        focusedIndicatorColor = Color.Transparent,
        unfocusedIndicatorColor = Color.Transparent,
        disabledIndicatorColor = Color.Transparent,
    )

    Scaffold(
        containerColor = noteBackground,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = noteBackground),
                navigationIcon = {
                    IconButton(onClick = {
                        viewModel.saveNote()
                        onNavigateBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                title = {},
                actions = {
                    IconButton(onClick = viewModel::toggleFavorite) {
                        Icon(
                            imageVector = if (uiState.isFavorite) Icons.Filled.Star else Icons.Outlined.StarOutline,
                            contentDescription = if (uiState.isFavorite) "Remove from favorites" else "Add to favorites",
                            tint = if (uiState.isFavorite) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More options")
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                        ) {
                            DropdownMenuItem(
                                text = { Text("Archive") },
                                leadingIcon = { Icon(Icons.Outlined.Archive, contentDescription = null) },
                                onClick = {
                                    showMenu = false
                                    viewModel.archiveNote()
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("Delete") },
                                leadingIcon = { Icon(Icons.Outlined.Delete, contentDescription = null) },
                                onClick = {
                                    showMenu = false
                                    showDeleteDialog = true
                                },
                            )
                        }
                    }
                },
            )
        },
        bottomBar = {
            EditorBottomBar(
                selectedColor = uiState.color,
                lastEditedTimestamp = uiState.lastEditedTimestamp,
                onColorSelected = viewModel::onColorSelected,
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .imePadding(),
        ) {
            TextField(
                value = uiState.title,
                onValueChange = viewModel::onTitleChanged,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Title", style = MaterialTheme.typography.headlineMedium) },
                textStyle = MaterialTheme.typography.headlineMedium,
                colors = transparentFieldColors,
                singleLine = false,
            )

            // Tag chips row
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                uiState.tags.forEach { tag ->
                    TagChipRemovable(tag = tag, onRemove = { viewModel.removeTag(tag) })
                }
                AssistChip(
                    onClick = { showTagPicker = true },
                    label = { Text("+ Tag") },
                    leadingIcon = { Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp)) },
                )
            }

            // Category row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = { showCategoryPicker = true }) {
                    Text(
                        text = uiState.category?.name ?: "No category",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.outline,
                    )
                }
            }

            TextField(
                value = uiState.content,
                onValueChange = viewModel::onContentChanged,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false),
                placeholder = { Text("Start writing...", style = MaterialTheme.typography.bodyLarge) },
                textStyle = MaterialTheme.typography.bodyLarge,
                colors = transparentFieldColors,
            )
        }
    }

    if (showDeleteDialog) {
        ConfirmDeleteDialog(
            onConfirm = {
                showDeleteDialog = false
                viewModel.deleteNote()
            },
            onDismiss = { showDeleteDialog = false },
        )
    }

    if (showTagPicker) {
        TagPickerBottomSheet(
            allTags = uiState.allTags,
            selectedTagIds = uiState.tags.map { it.id }.toSet(),
            onTagSelected = { tag -> viewModel.addTag(tag) },
            onTagDeselected = { tag -> viewModel.removeTag(tag) },
            onCreateTag = { name -> viewModel.createAndAddTag(name) },
            onDismiss = { showTagPicker = false },
        )
    }

    if (showCategoryPicker) {
        CategoryPickerDialog(
            categories = uiState.allCategories,
            selectedCategoryId = uiState.categoryId,
            onCategorySelected = { category ->
                viewModel.setCategory(category)
                showCategoryPicker = false
            },
            onDismiss = { showCategoryPicker = false },
        )
    }
}

@Composable
private fun EditorBottomBar(
    selectedColor: Int,
    lastEditedTimestamp: Long,
    onColorSelected: (Int) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
    ) {
        HorizontalDivider()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                NoteColors.presets.forEach { color ->
                    ColorDot(
                        color = color,
                        isSelected = color == selectedColor,
                        onClick = { onColorSelected(color) },
                    )
                }
            }
            Text(
                text = "Edited ${DateUtils.formatRelativeTime(lastEditedTimestamp)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
            )
        }
    }
}

@Composable
private fun ColorDot(
    color: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val bgColor = if (color == 0) Color.Transparent else Color(color)
    Box(
        modifier = Modifier
            .size(24.dp)
            .clip(CircleShape)
            .background(bgColor)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.outline,
                shape = CircleShape,
            )
            .clickable(onClick = onClick),
    )
}
