package com.mindvault.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.mindvault.app.data.model.Category

@Composable
fun CategoryPickerDialog(
    categories: List<Category>,
    selectedCategoryId: Long?,
    onCategorySelected: (Category?) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Category") },
        text = {
            LazyColumn {
                item {
                    CategoryRow(
                        name = "None",
                        color = 0,
                        selected = selectedCategoryId == null,
                        onClick = { onCategorySelected(null) },
                    )
                }
                items(categories) { category ->
                    CategoryRow(
                        name = category.name,
                        color = category.color,
                        selected = category.id == selectedCategoryId,
                        onClick = { onCategorySelected(category) },
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Done") }
        },
    )
}

@Composable
private fun CategoryRow(
    name: String,
    color: Int,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = onClick)
        if (color != 0) {
            Surface(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape),
                color = Color(color),
            ) {}
            Spacer(modifier = Modifier.width(6.dp))
        }
        Text(
            text = name,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
    }
}
