package com.mindvault.app.domain.analysis

import kotlin.math.sqrt

object TextAnalyzer {

    fun tokenize(text: String): List<String> {
        return text.lowercase()
            .replace(Regex("[^\\p{L}\\p{N}\\s]"), " ")
            .split(Regex("\\s+"))
            .filter { it.length >= 3 }
    }

    fun extractKeywords(text: String): List<String> {
        return tokenize(text).filter { !StopWords.isStopWord(it) }
    }

    fun wordFrequency(text: String): Map<String, Int> {
        return extractKeywords(text)
            .groupingBy { it }
            .eachCount()
    }

    fun topKeywords(text: String, n: Int = 10): List<Pair<String, Int>> {
        return wordFrequency(text)
            .entries
            .sortedByDescending { it.value }
            .take(n)
            .map { it.key to it.value }
    }

    fun termFrequency(text: String): Map<String, Double> {
        val keywords = extractKeywords(text)
        val total = keywords.size.toDouble()
        if (total == 0.0) return emptyMap()
        return keywords.groupingBy { it }.eachCount()
            .mapValues { it.value / total }
    }

    fun cosineSimilarity(tf1: Map<String, Double>, tf2: Map<String, Double>): Double {
        val allTerms = tf1.keys + tf2.keys
        var dotProduct = 0.0
        var norm1 = 0.0
        var norm2 = 0.0
        for (term in allTerms) {
            val v1 = tf1[term] ?: 0.0
            val v2 = tf2[term] ?: 0.0
            dotProduct += v1 * v2
            norm1 += v1 * v1
            norm2 += v2 * v2
        }
        val denominator = sqrt(norm1) * sqrt(norm2)
        return if (denominator == 0.0) 0.0 else dotProduct / denominator
    }
}
