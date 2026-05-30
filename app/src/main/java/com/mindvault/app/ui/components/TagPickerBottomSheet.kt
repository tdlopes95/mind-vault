package com.mindvault.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mindvault.app.data.model.Tag

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TagPickerBottomSheet(
    allTags: List<Tag>,
    selectedTagIds: Set<Long>,
    onTagSelected: (Tag) -> Unit,
    onTagDeselected: (Tag) -> Unit,
    onCreateTag: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState()
    var searchQuery by remember { mutableStateOf("") }

    val filteredTags = if (searchQuery.isBlank()) allTags
    else allTags.filter { it.name.contains(searchQuery, ignoreCase = true) }

    val showCreateOption = searchQuery.isNotBlank() &&
        allTags.none { it.name.equals(searchQuery, ignoreCase = true) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            Text(
                text = "Tags",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 12.dp),
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search or create tag…") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                if (showCreateOption) {
                    IconButton(onClick = {
                        onCreateTag(searchQuery.trim())
                        searchQuery = ""
                    }) {
                        Icon(Icons.Default.Add, contentDescription = "Create tag")
                    }
                }
            }

            if (showCreateOption) {
                TextButton(
                    onClick = {
                        onCreateTag(searchQuery.trim())
                        searchQuery = ""
                    },
                    modifier = Modifier.padding(top = 4.dp),
                ) {
                    Text("Create \"#${searchQuery.trim()}\"")
                }
            }

            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp, bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                filteredTags.forEach { tag ->
                    val selected = tag.id in selectedTagIds
                    FilterChip(
                        selected = selected,
                        onClick = {
                            if (selected) onTagDeselected(tag) else onTagSelected(tag)
                        },
                        label = { Text("#${tag.name}") },
                    )
                }
            }
        }
    }
}
