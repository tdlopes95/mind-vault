package com.mindvault.app.domain.analysis

import com.mindvault.app.data.model.Category
import com.mindvault.app.data.model.Tag
import com.mindvault.app.data.repository.CategoryRepositoryInterface
import com.mindvault.app.data.repository.NoteRepositoryInterface
import com.mindvault.app.data.repository.TagRepositoryInterface
import com.mindvault.app.ui.screens.editor.CategorySuggestion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import javax.inject.Inject

class CategorySuggestionEngine @Inject constructor(
    private val categoryRepository: CategoryRepositoryInterface,
    private val tagRepository: TagRepositoryInterface,
    private val noteRepository: NoteRepositoryInterface,
) {
    suspend fun suggestCategory(
        noteId: Long,
        title: String,
        content: String,
        tags: List<Tag>,
    ): CategorySuggestion? = withContext(Dispatchers.Default) {
        val categories = categoryRepository.getAllCategories().first()
        if (categories.size < 3) return@withContext null

        val allNotes = noteRepository.getActiveNotes().first()
            .filter { it.id != noteId && it.categoryId != null }
        if (allNotes.size < 10) return@withContext null

        val categoryMap = categories.associateBy { it.id }

        // Signal 1: tag-to-category correlation
        var tagCorrelationResult: Pair<Category, String>? = null
        if (tags.isNotEmpty()) {
            val categoryVotes = mutableMapOf<Long, Int>()
            for (tag in tags) {
                val tagNoteIds = tagRepository.getActiveNoteIdsForTag(tag.id).first()
                for (note in allNotes) {
                    if (note.id in tagNoteIds && note.categoryId != null) {
                        categoryVotes[note.categoryId] = (categoryVotes[note.categoryId] ?: 0) + 1
                    }
                }
            }
            val totalVotes = categoryVotes.values.sum()
            if (totalVotes > 0) {
                val best = categoryVotes.maxByOrNull { it.value }
                if (best != null) {
                    val confidence = best.value.toDouble() / totalVotes
                    if (confidence >= 0.6) {
                        categoryMap[best.key]?.let { cat ->
                            val topTag = tags.firstOrNull()?.name ?: ""
                            tagCorrelationResult = cat to "Most notes tagged #$topTag are in '${cat.name}'"
                        }
                    }
                }
            }
        }

        // Signal 2: content similarity
        var contentSimilarityResult: Pair<Category, Double>? = null
        val noteTf = TextAnalyzer.termFrequency("$title $content")
        if (noteTf.isNotEmpty()) {
            val categorySimilarities = mutableMapOf<Long, Double>()
            for (cat in categories) {
                val catNotes = allNotes.filter { it.categoryId == cat.id }.take(100)
                if (catNotes.isEmpty()) continue
                val profileText = catNotes.joinToString(" ") { "${it.title} ${it.content}" }
                val profileTf = TextAnalyzer.termFrequency(profileText)
                val sim = TextAnalyzer.cosineSimilarity(noteTf, profileTf)
                categorySimilarities[cat.id] = sim
            }
            val bestSim = categorySimilarities.maxByOrNull { it.value }
            if (bestSim != null && bestSim.value >= 0.3) {
                categoryMap[bestSim.key]?.let { cat ->
                    contentSimilarityResult = cat to bestSim.value
                }
            }
        }

        // Combine signals
        return@withContext when {
            tagCorrelationResult != null -> {
                val (cat, reason) = tagCorrelationResult!!
                CategorySuggestion(category = cat, confidence = 0.8, reason = reason)
            }
            contentSimilarityResult != null -> {
                val (cat, sim) = contentSimilarityResult!!
                CategorySuggestion(
                    category = cat,
                    confidence = sim,
                    reason = "Content is similar to notes in '${cat.name}'",
                )
            }
            else -> null
        }
    }
}
