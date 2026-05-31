package com.mindvault.app.domain.analysis

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TextAnalyzerTest {

    // tokenize

    @Test
    fun `tokenize — empty string returns empty list`() {
        assertTrue(TextAnalyzer.tokenize("").isEmpty())
    }

    @Test
    fun `tokenize — strips punctuation and lowercases`() {
        val tokens = TextAnalyzer.tokenize("Hello, World! This is Kotlin.")
        assertTrue("hello" in tokens)
        assertTrue("world" in tokens)
        assertTrue("kotlin" in tokens)
    }

    @Test
    fun `tokenize — filters tokens shorter than 3 characters`() {
        val tokens = TextAnalyzer.tokenize("I am at the top")
        assertTrue(tokens.none { it.length < 3 })
        assertTrue("top" in tokens)
    }

    @Test
    fun `tokenize — handles unicode letters`() {
        val tokens = TextAnalyzer.tokenize("café résumé naïve")
        assertTrue(tokens.any { it.contains("caf") || it == "café" })
    }

    @Test
    fun `tokenize — splits on whitespace and punctuation`() {
        val tokens = TextAnalyzer.tokenize("one,two;three four")
        assertEquals(4, tokens.size)
    }

    // extractKeywords

    @Test
    fun `extractKeywords — removes English stop words`() {
        val keywords = TextAnalyzer.extractKeywords("this is a very important note about the project")
        assertTrue("this" !in keywords)
        assertTrue("very" !in keywords)
        assertTrue("important" in keywords || "project" in keywords)
    }

    @Test
    fun `extractKeywords — empty input returns empty list`() {
        assertTrue(TextAnalyzer.extractKeywords("").isEmpty())
    }

    // wordFrequency

    @Test
    fun `wordFrequency — counts occurrences correctly`() {
        val freq = TextAnalyzer.wordFrequency("apple banana apple cherry apple")
        assertEquals(3, freq["apple"])
        assertEquals(1, freq["banana"])
        assertEquals(1, freq["cherry"])
    }

    @Test
    fun `wordFrequency — empty text returns empty map`() {
        assertTrue(TextAnalyzer.wordFrequency("").isEmpty())
    }

    // topKeywords

    @Test
    fun `topKeywords — returns at most n results`() {
        val text = "alpha beta gamma delta epsilon zeta eta theta iota kappa lambda"
        val top = TextAnalyzer.topKeywords(text, 5)
        assertTrue(top.size <= 5)
    }

    @Test
    fun `topKeywords — sorted by frequency descending`() {
        val text = "android android android kotlin kotlin java"
        val top = TextAnalyzer.topKeywords(text, 3)
        if (top.size >= 2) {
            assertTrue(top[0].second >= top[1].second)
        }
    }

    // termFrequency

    @Test
    fun `termFrequency — values sum to approximately 1 for non-empty text`() {
        val tf = TextAnalyzer.termFrequency("note notes notebook")
        val sum = tf.values.sum()
        assertTrue("TF values should sum to ~1.0", sum > 0.9 && sum <= 1.01)
    }

    @Test
    fun `termFrequency — empty text returns empty map`() {
        assertTrue(TextAnalyzer.termFrequency("").isEmpty())
    }

    // cosineSimilarity

    @Test
    fun `cosineSimilarity — identical documents score 1_0`() {
        val tf = TextAnalyzer.termFrequency("kotlin coroutines android development")
        val sim = TextAnalyzer.cosineSimilarity(tf, tf)
        assertEquals(1.0, sim, 0.001)
    }

    @Test
    fun `cosineSimilarity — disjoint documents score 0_0`() {
        val tf1 = TextAnalyzer.termFrequency("apple banana cherry")
        val tf2 = TextAnalyzer.termFrequency("table chair desk")
        val sim = TextAnalyzer.cosineSimilarity(tf1, tf2)
        assertEquals(0.0, sim, 0.001)
    }

    @Test
    fun `cosineSimilarity — partially overlapping documents score between 0 and 1`() {
        val tf1 = TextAnalyzer.termFrequency("android kotlin mobile")
        val tf2 = TextAnalyzer.termFrequency("android java mobile desktop")
        val sim = TextAnalyzer.cosineSimilarity(tf1, tf2)
        assertTrue(sim > 0.0 && sim < 1.0)
    }

    @Test
    fun `cosineSimilarity — empty maps return 0_0`() {
        val sim = TextAnalyzer.cosineSimilarity(emptyMap(), emptyMap())
        assertEquals(0.0, sim, 0.001)
    }
}
