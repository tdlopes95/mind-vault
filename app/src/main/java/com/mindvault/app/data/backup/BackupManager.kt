package com.mindvault.app.data.backup

import android.content.Context
import android.net.Uri
import com.mindvault.app.data.model.Note
import com.mindvault.app.data.repository.CategoryRepositoryInterface
import com.mindvault.app.data.repository.NoteRepositoryInterface
import com.mindvault.app.data.repository.TagRepositoryInterface
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackupManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val noteRepository: NoteRepositoryInterface,
    private val tagRepository: TagRepositoryInterface,
    private val categoryRepository: CategoryRepositoryInterface,
) {

    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }
    private val isoFormatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME.withZone(ZoneOffset.UTC)

    suspend fun exportToJson(uri: Uri) {
        val notes = noteRepository.getActiveNotes().first() +
            noteRepository.getArchivedNotes().first()
        val allTags = tagRepository.getAllTags().first()
        val allCategories = categoryRepository.getAllCategories().first()

        val categoryById = allCategories.associateBy { it.id }

        val noteJsonList = notes.map { note ->
            val noteTags = tagRepository.getTagsForNote(note.id).first()
            NoteJson(
                title = note.title,
                content = note.content,
                color = note.color,
                isFavorite = note.isFavorite,
                isArchived = note.isArchived,
                createdAt = note.createdAt,
                updatedAt = note.updatedAt,
                tags = noteTags.map { it.name },
                category = note.categoryId?.let { categoryById[it]?.name },
            )
        }

        val exportModel = NoteExportModel(
            exportDate = isoFormatter.format(Instant.now()),
            notes = noteJsonList,
            tags = allTags.map { TagJson(it.name, it.color) },
            categories = allCategories.map { CategoryJson(it.name, it.color, it.icon) },
        )

        context.contentResolver.openOutputStream(uri)?.use { stream ->
            stream.write(json.encodeToString(exportModel).toByteArray(Charsets.UTF_8))
        } ?: error("Could not open output stream for URI: $uri")
    }

    suspend fun importFromJson(uri: Uri): ImportResult {
        val jsonString = context.contentResolver.openInputStream(uri)?.use { stream ->
            stream.readBytes().toString(Charsets.UTF_8)
        } ?: error("Could not open input stream for URI: $uri")

        val exportModel = json.decodeFromString<NoteExportModel>(jsonString)

        val existingNotes = noteRepository.getActiveNotes().first() +
            noteRepository.getArchivedNotes().first() +
            noteRepository.getDeletedNotes().first()
        val existingNoteKeys = existingNotes.map { Pair(it.title, it.createdAt) }.toSet()

        val existingCategories = categoryRepository.getAllCategories().first().associateBy { it.name }
        val existingTags = tagRepository.getAllTags().first().associateBy { it.name }

        var importedNotes = 0
        var importedTags = 0
        var importedCategories = 0
        var skippedNotes = 0

        // Create missing tags
        val tagIdByName = existingTags.mapValues { it.value.id }.toMutableMap()
        exportModel.tags.forEach { tagJson ->
            if (tagJson.name !in tagIdByName) {
                val id = tagRepository.insertTag(tagJson.name, tagJson.color)
                tagIdByName[tagJson.name] = id
                importedTags++
            }
        }

        // Create missing categories
        val categoryIdByName = existingCategories.mapValues { it.value.id }.toMutableMap()
        exportModel.categories.forEach { catJson ->
            if (catJson.name !in categoryIdByName) {
                val id = categoryRepository.insertCategory(catJson.name, catJson.color, catJson.icon)
                categoryIdByName[catJson.name] = id
                importedCategories++
            }
        }

        // Import notes
        exportModel.notes.forEach { noteJson ->
            val key = Pair(noteJson.title, noteJson.createdAt)
            if (key in existingNoteKeys) {
                skippedNotes++
                return@forEach
            }
            val categoryId = noteJson.category?.let { categoryIdByName[it] }
            val noteId = noteRepository.insertNote(
                Note(
                    title = noteJson.title,
                    content = noteJson.content,
                    color = noteJson.color,
                    isFavorite = noteJson.isFavorite,
                    isArchived = noteJson.isArchived,
                    createdAt = noteJson.createdAt,
                    updatedAt = noteJson.updatedAt,
                    categoryId = categoryId,
                )
            )
            noteJson.tags.forEach { tagName ->
                tagIdByName[tagName]?.let { tagId ->
                    tagRepository.addTagToNote(noteId, tagId)
                }
            }
            importedNotes++
        }

        return ImportResult(importedNotes, importedTags, importedCategories, skippedNotes)
    }
}
