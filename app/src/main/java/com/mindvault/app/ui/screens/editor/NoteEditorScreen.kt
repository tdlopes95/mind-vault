package com.mindvault.app.ui.screens.editor

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import com.mindvault.app.ui.components.AttachmentSection
import com.mindvault.app.ui.components.CategoryPickerDialog
import com.mindvault.app.ui.components.ConfirmDeleteDialog
import com.mindvault.app.ui.components.MarkdownRenderer
import com.mindvault.app.ui.components.NoteLinkChip
import com.mindvault.app.ui.components.NoteLinkPickerDialog
import com.mindvault.app.ui.components.RelatedNotesSection
import com.mindvault.app.ui.components.TagChipRemovable
import com.mindvault.app.ui.components.TagPickerBottomSheet
import com.mindvault.app.util.DateUtils
import com.mindvault.app.util.NoteColors

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun NoteEditorScreen(
    noteId: Long?,
    onNavigateBack: () -> Unit,
    onNavigateToNote: (Long) -> Unit = {},
    viewModel: NoteEditorViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showMenu by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showTagPicker by remember { mutableStateOf(false) }
    var showCategoryPicker by remember { mutableStateOf(false) }
    var showLinkPicker by remember { mutableStateOf(false) }
    var showLinkedNotesExpanded by remember { mutableStateOf(true) }

    val fileLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { viewModel.addAttachment(it) }
    }

    BackHandler {
        viewModel.saveNote()
        onNavigateBack()
    }

    LaunchedEffect(uiState.shouldNavigateBack) {
        if (uiState.shouldNavigateBack) onNavigateBack()
    }

    LaunchedEffect(uiState.navigateToNoteId) {
        uiState.navigateToNoteId?.let { id ->
            viewModel.clearNavigateToNote()
            viewModel.saveNote()
            onNavigateToNote(id)
        }
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
                    // Preview / Edit toggle
                    IconButton(onClick = viewModel::togglePreviewMode) {
                        Icon(
                            imageVector = if (uiState.isPreviewMode) Icons.Outlined.Edit else Icons.Default.Visibility,
                            contentDescription = if (uiState.isPreviewMode) "Edit mode" else "Preview mode",
                        )
                    }
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
                                text = { Text(if (uiState.isPinned) "Unpin" else "Pin") },
                                leadingIcon = {
                                    Icon(
                                        if (uiState.isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                                        contentDescription = null,
                                    )
                                },
                                onClick = { showMenu = false; viewModel.togglePin() },
                            )
                            DropdownMenuItem(
                                text = { Text("Archive") },
                                leadingIcon = { Icon(Icons.Outlined.Archive, contentDescription = null) },
                                onClick = { showMenu = false; viewModel.archiveNote() },
                            )
                            DropdownMenuItem(
                                text = { Text("Delete") },
                                leadingIcon = { Icon(Icons.Outlined.Delete, contentDescription = null) },
                                onClick = { showMenu = false; showDeleteDialog = true },
                            )
                        }
                    }
                },
            )
        },
        bottomBar = {
            if (!uiState.isPreviewMode) {
                EditorBottomBar(
                    selectedColor = uiState.color,
                    lastEditedTimestamp = uiState.lastEditedTimestamp,
                    onColorSelected = viewModel::onColorSelected,
                    onAttachFile = { fileLauncher.launch(arrayOf("*/*")) },
                    onAddLink = {
                        viewModel.loadAvailableForLinking()
                        showLinkPicker = true
                    },
                )
            }
        },
    ) { innerPadding ->
        Crossfade(
            targetState = uiState.isPreviewMode,
            label = "editor_preview_crossfade",
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) { isPreview ->
            if (isPreview) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    if (uiState.title.isNotBlank()) {
                        Text(
                            text = uiState.title,
                            style = MaterialTheme.typography.headlineMedium,
                            modifier = Modifier.padding(bottom = 12.dp),
                        )
                    }
                    if (uiState.content.isNotBlank()) {
                        MarkdownRenderer(
                            content = uiState.content,
                            onContentChange = viewModel::onContentChanged,
                        )
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
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

                    // Tag chips
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

                    // Content field
                    TextField(
                        value = uiState.content,
                        onValueChange = viewModel::onContentChanged,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false),
                        placeholder = { Text("Start writing…", style = MaterialTheme.typography.bodyLarge) },
                        textStyle = MaterialTheme.typography.bodyLarge,
                        colors = transparentFieldColors,
                    )

                    // Attachments section
                    if (uiState.attachments.isNotEmpty()) {
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 12.dp))
                        AttachmentSection(
                            attachments = uiState.attachments,
                            onDelete = { viewModel.deleteAttachment(it) },
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        )
                    }

                    // Tag suggestions bar
                    if (uiState.suggestedTags.isNotEmpty()) {
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 12.dp))
                        TagSuggestionsBar(
                            suggestions = uiState.suggestedTags,
                            onAccept = { viewModel.acceptSuggestion(it) },
                            onDismiss = { viewModel.dismissSuggestion(it) },
                            onDismissAll = { viewModel.dismissAllSuggestions() },
                        )
                    }

                    // Category suggestion banner
                    if (uiState.categorySuggestion != null) {
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 12.dp))
                        CategorySuggestionBanner(
                            suggestion = uiState.categorySuggestion!!,
                            onAccept = { viewModel.acceptCategorySuggestion() },
                            onDismiss = { viewModel.dismissCategorySuggestion() },
                        )
                    }

                    // Linked notes section
                    if (uiState.linkedNotes.isNotEmpty() || uiState.isNewNote.not()) {
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 12.dp))
                        LinkedNotesSection(
                            linkedNotes = uiState.linkedNotes,
                            expanded = showLinkedNotesExpanded,
                            onToggleExpand = { showLinkedNotesExpanded = !showLinkedNotesExpanded },
                            onNavigate = { viewModel.navigateToLinkedNote(it) },
                            onRemove = { viewModel.unlinkNote(it) },
                            onAddLink = {
                                viewModel.loadAvailableForLinking()
                                showLinkPicker = true
                            },
                        )
                    }

                    // Related notes section (shown when note is saved)
                    if (!uiState.isNewNote) {
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 12.dp))
                        RelatedNotesSection(
                            relatedNotes = uiState.relatedNotes,
                            isLoading = uiState.isLoadingRelated,
                            expanded = uiState.relatedNotesExpanded,
                            onToggleExpand = { viewModel.toggleRelatedNotesExpanded() },
                            onNoteClick = { viewModel.navigateToLinkedNote(it) },
                        )
                    }
                }
            }
        }
    }

    if (showDeleteDialog) {
        ConfirmDeleteDialog(
            onConfirm = { showDeleteDialog = false; viewModel.deleteNote() },
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

    if (showLinkPicker) {
        NoteLinkPickerDialog(
            availableNotes = uiState.availableForLinking,
            onNoteSelected = { note ->
                viewModel.linkNote(note.id)
                showLinkPicker = false
            },
            onDismiss = { showLinkPicker = false },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TagSuggestionsBar(
    suggestions: List<String>,
    onAccept: (String) -> Unit,
    onDismiss: (String) -> Unit,
    onDismissAll: () -> Unit,
) {
    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Suggested tags:",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onDismissAll) {
                Text("Dismiss all", style = MaterialTheme.typography.labelSmall)
            }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            suggestions.forEach { tag ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AssistChip(
                        onClick = { onAccept(tag) },
                        label = { Text(tag) },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = "Accept",
                                modifier = Modifier.size(14.dp),
                            )
                        },
                    )
                    IconButton(onClick = { onDismiss(tag) }, modifier = Modifier.size(28.dp)) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Dismiss",
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.outline,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CategorySuggestionBanner(
    suggestion: CategorySuggestion,
    onAccept: () -> Unit,
    onDismiss: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = MaterialTheme.shapes.small,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Suggested category: ${suggestion.category.name}",
                    style = MaterialTheme.typography.bodySmall,
                )
                Text(
                    text = suggestion.reason,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f),
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            TextButton(onClick = onAccept) { Text("Accept") }
            IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Close, contentDescription = "Dismiss", modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
private fun LinkedNotesSection(
    linkedNotes: List<com.mindvault.app.data.model.Note>,
    expanded: Boolean,
    onToggleExpand: () -> Unit,
    onNavigate: (Long) -> Unit,
    onRemove: (Long) -> Unit,
    onAddLink: () -> Unit,
) {
    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggleExpand)
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Default.Link, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.outline)
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Linked Notes (${linkedNotes.size})",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.weight(1f),
            )
        }
        if (expanded) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(vertical = 4.dp),
            ) {
                items(linkedNotes, key = { it.id }) { note ->
                    NoteLinkChip(
                        note = note,
                        onNavigate = { onNavigate(note.id) },
                        onRemove = { onRemove(note.id) },
                    )
                }
                item {
                    AssistChip(
                        onClick = onAddLink,
                        label = { Text("Link") },
                        leadingIcon = { Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    )
                }
            }
        }
    }
}

@Composable
private fun EditorBottomBar(
    selectedColor: Int,
    lastEditedTimestamp: Long,
    onColorSelected: (Int) -> Unit,
    onAttachFile: () -> Unit,
    onAddLink: () -> Unit,
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
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                NoteColors.presets.forEach { color ->
                    ColorDot(
                        color = color,
                        isSelected = color == selectedColor,
                        onClick = { onColorSelected(color) },
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                IconButton(onClick = onAttachFile, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.AttachFile, contentDescription = "Attach file", modifier = Modifier.size(20.dp))
                }
                IconButton(onClick = onAddLink, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Link, contentDescription = "Link note", modifier = Modifier.size(20.dp))
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
