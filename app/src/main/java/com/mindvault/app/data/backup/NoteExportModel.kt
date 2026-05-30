package com.mindvault.app.data.backup

import kotlinx.serialization.Serializable

@Serializable
data class NoteExportModel(
    val exportVersion: Int = 2,
    val exportDate: String,
    val notes: List<NoteJson>,
    val tags: List<TagJson>,
    val categories: List<CategoryJson>,
    val links: List<LinkJson> = emptyList(),
)

@Serializable
data class NoteJson(
    val title: String,
    val content: String,
    val color: Int = 0,
    val isFavorite: Boolean = false,
    val isPinned: Boolean = false,
    val isArchived: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long,
    val tags: List<String> = emptyList(),
    val category: String? = null,
    // Attachment metadata only — actual files are not exported
    val attachments: List<AttachmentJson> = emptyList(),
)

@Serializable
data class TagJson(
    val name: String,
    val color: Int = 0,
)

@Serializable
data class CategoryJson(
    val name: String,
    val color: Int = 0,
    val icon: String? = null,
)

@Serializable
data class LinkJson(
    val sourceTitle: String,
    val targetTitle: String,
)

@Serializable
data class AttachmentJson(
    val fileName: String,
    val mimeType: String,
    val fileSize: Long,
)

data class ImportResult(
    val importedNotes: Int,
    val importedTags: Int,
    val importedCategories: Int,
    val skippedNotes: Int,
) {
    val summary: String get() =
        "Imported $importedNotes notes, $importedTags tags, $importedCategories categories. Skipped $skippedNotes duplicates."
}
