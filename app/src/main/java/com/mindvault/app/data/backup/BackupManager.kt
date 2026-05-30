package com.mindvault.app.data.backup

import android.content.Context
import android.net.Uri
import com.mindvault.app.data.model.Note
import com.mindvault.app.data.repository.AttachmentRepositoryInterface
import com.mindvault.app.data.repository.CategoryRepositoryInterface
import com.mindvault.app.data.repository.NoteLinkRepositoryInterface
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
    private val noteLinkRepository: NoteLinkRepositoryInterface,
    private val attachmentRepository: AttachmentRepositoryInterface,
) {

    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }
    private val isoFormatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME.withZone(ZoneOffset.UTC)

    suspend fun exportToJson(uri: Uri) {
        val notes = noteRepository.getActiveNotes().first() +
            noteRepository.getArchivedNotes().first()
        val allTags = tagRepository.getAllTags().first()
        val allCategories = categoryRepository.getAllCategories().first()
        val categoryById = allCategories.associateBy { it.id }
        val noteByTitle = notes.associateBy { it.title }

        // Collect all links (as title pairs for portability)
        val seenLinkPairs = mutableSetOf<Pair<String, String>>()
        val linkJsonList = mutableListOf<LinkJson>()
        notes.forEach { note ->
            val linked = noteLinkRepository.getLinkedNotes(note.id).first()
            linked.forEach { target ->
                val key = if (note.id < target.id)
                    note.title to target.title
                else
                    target.title to note.title
                if (seenLinkPairs.add(key)) {
                    linkJsonList.add(LinkJson(note.title, target.title))
                }
            }
        }

        val noteJsonList = notes.map { note ->
            val noteTags = tagRepository.getTagsForNote(note.id).first()
            val noteAttachments = attachmentRepository.getAttachmentsForNote(note.id).first()
            NoteJson(
                title = note.title,
                content = note.content,
                color = note.color,
                isFavorite = note.isFavorite,
                isPinned = note.isPinned,
                isArchived = note.isArchived,
                createdAt = note.createdAt,
                updatedAt = note.updatedAt,
                tags = noteTags.map { it.name },
                category = note.categoryId?.let { categoryById[it]?.name },
                attachments = noteAttachments.map { AttachmentJson(it.fileName, it.mimeType, it.fileSize) },
            )
        }

        val exportModel = NoteExportModel(
            exportDate = isoFormatter.format(Instant.now()),
            notes = noteJsonList,
            tags = allTags.map { TagJson(it.name, it.color) },
            categories = allCategories.map { CategoryJson(it.name, it.color, it.icon) },
            links = linkJsonList,
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

        val tagIdByName = existingTags.mapValues { it.value.id }.toMutableMap()
        exportModel.tags.forEach { tagJson ->
            if (tagJson.name !in tagIdByName) {
                val id = tagRepository.insertTag(tagJson.name, tagJson.color)
                tagIdByName[tagJson.name] = id
                importedTags++
            }
        }

        val categoryIdByName = existingCategories.mapValues { it.value.id }.toMutableMap()
        exportModel.categories.forEach { catJson ->
            if (catJson.name !in categoryIdByName) {
                val id = categoryRepository.insertCategory(catJson.name, catJson.color, catJson.icon)
                categoryIdByName[catJson.name] = id
                importedCategories++
            }
        }

        val importedNoteIdByTitle = mutableMapOf<String, Long>()
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
                    isPinned = noteJson.isPinned,
                    isArchived = noteJson.isArchived,
                    createdAt = noteJson.createdAt,
                    updatedAt = noteJson.updatedAt,
                    categoryId = categoryId,
                )
            )
            noteJson.tags.forEach { tagName ->
                tagIdByName[tagName]?.let { tagId -> tagRepository.addTagToNote(noteId, tagId) }
            }
            importedNoteIdByTitle[noteJson.title] = noteId
            importedNotes++
            // Attachments: skip file data, metadata not re-imported since files don't exist
        }

        // Recreate links using imported note IDs (match by title)
        val allNotesById = (noteRepository.getActiveNotes().first() +
            noteRepository.getArchivedNotes().first()).associateBy { it.title }
        exportModel.links.forEach { link ->
            val sourceId = importedNoteIdByTitle[link.sourceTitle]
                ?: allNotesById[link.sourceTitle]?.id
            val targetId = importedNoteIdByTitle[link.targetTitle]
                ?: allNotesById[link.targetTitle]?.id
            if (sourceId != null && targetId != null) {
                noteLinkRepository.linkNotes(sourceId, targetId)
            }
        }

        return ImportResult(importedNotes, importedTags, importedCategories, skippedNotes)
    }
}
