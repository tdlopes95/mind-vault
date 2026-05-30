package com.mindvault.app.domain.analysis

import com.mindvault.app.data.repository.TagRepositoryInterface
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class TagSuggestionEngine @Inject constructor(
    private val tagRepository: TagRepositoryInterface,
) {
    suspend fun suggestTags(
        title: String,
        content: String,
        existingTagNames: Set<String>,
    ): List<String> {
        val combinedText = "$title $content".lowercase()
        if (combinedText.isBlank() || combinedText.length < 10) return emptyList()

        val allTags = tagRepository.getAllTags().first()
        val suggestions = mutableListOf<String>()
        val dismissedLower = existingTagNames.map { it.lowercase() }.toSet()

        // Step 1: match existing tag names found in the text
        for (tag in allTags) {
            val tagLower = tag.name.lowercase()
            if (tagLower !in dismissedLower && combinedText.contains(tagLower)) {
                suggestions.add(tag.name)
            }
        }

        // Step 2: top keywords by frequency (2+ occurrences, not already a tag)
        val existingTagNamesLower = allTags.map { it.name.lowercase() }.toSet()
        val weightedText = "$title $title $content"
        val capped = weightedText.take(5000)
        for ((word, count) in TextAnalyzer.topKeywords(capped, 20)) {
            if (count >= 2 && word !in dismissedLower && word !in existingTagNamesLower) {
                suggestions.add(word)
            }
            if (suggestions.size >= 5) break
        }

        return suggestions.distinct().take(5)
    }
}
