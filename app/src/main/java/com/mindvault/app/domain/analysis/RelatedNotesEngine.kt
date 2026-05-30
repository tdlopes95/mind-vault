package com.mindvault.app.domain.analysis

import com.mindvault.app.data.model.Note
import com.mindvault.app.data.model.Tag
import com.mindvault.app.data.repository.NoteLinkRepositoryInterface
import com.mindvault.app.data.repository.NoteRepositoryInterface
import com.mindvault.app.data.repository.TagRepositoryInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class RelatedNote(
    val note: Note,
    val score: Double,
    val reason: String,
)

class RelatedNotesEngine @Inject constructor(
    private val noteRepository: NoteRepositoryInterface,
    private val tagRepository: TagRepositoryInterface,
    private val noteLinkRepository: NoteLinkRepositoryInterface,
) {
    suspend fun findRelatedNotes(
        noteId: Long,
        title: String,
        content: String,
        tags: List<Tag>,
        categoryId: Long?,
    ): List<RelatedNote> = withContext(Dispatchers.Default) {
        val allNotes = noteRepository.getActiveNotes().first()
            .filter { it.id != noteId }
            .let { if (it.size > 100) it.sortedByDescending { n -> n.updatedAt }.take(50) else it }

        if (allNotes.isEmpty()) return@withContext emptyList()

        val currentTf = TextAnalyzer.termFrequency("$title $content")
        val currentTagIds = tags.map { it.id }.toSet()
        val currentLinks = noteLinkRepository.getLinkedNotes(noteId).first()
            .map { it.id }.toSet()

        val results = mutableListOf<RelatedNote>()

        for (candidate in allNotes) {
            var score = 0.0
            val reasons = mutableListOf<String>()

            // Tag overlap
            val candidateTags = tagRepository.getTagsForNote(candidate.id).first()
            val sharedTags = candidateTags.count { it.id in currentTagIds }
            if (sharedTags > 0) {
                score += sharedTags * 3.0
                reasons.add("$sharedTags shared tag${if (sharedTags > 1) "s" else ""}")
            }

            // Category match
            if (categoryId != null && candidate.categoryId == categoryId) {
                score += 2.0
                reasons.add("Same category")
            }

            // Content similarity
            val candidateTf = TextAnalyzer.termFrequency("${candidate.title} ${candidate.content}")
            val similarity = TextAnalyzer.cosineSimilarity(currentTf, candidateTf)
            if (similarity > 0.05) {
                val contentScore = similarity * 5.0
                score += contentScore
                if (similarity > 0.2) reasons.add("Similar content")
            }

            // Link proximity
            val candidateLinks = noteLinkRepository.getLinkedNotes(candidate.id).first()
                .map { it.id }.toSet()
            val sharedLinks = currentLinks.intersect(candidateLinks).size
            if (sharedLinks > 0) {
                score += sharedLinks * 2.0
                reasons.add("$sharedLinks shared link${if (sharedLinks > 1) "s" else ""}")
            }

            if (score > 0) {
                results.add(RelatedNote(candidate, score, reasons.joinToString(" · ")))
            }
        }

        results.sortedByDescending { it.score }.take(5)
    }
}
