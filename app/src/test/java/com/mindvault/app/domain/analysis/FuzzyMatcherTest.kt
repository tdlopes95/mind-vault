package com.mindvault.app.domain.analysis

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FuzzyMatcherTest {

    // levenshteinDistance

    @Test
    fun `levenshteinDistance — kitten to sitting is 3`() {
        assertEquals(3, FuzzyMatcher.levenshteinDistance("kitten", "sitting"))
    }

    @Test
    fun `levenshteinDistance — empty to abc is 3`() {
        assertEquals(3, FuzzyMatcher.levenshteinDistance("", "abc"))
    }

    @Test
    fun `levenshteinDistance — identical strings is 0`() {
        assertEquals(0, FuzzyMatcher.levenshteinDistance("abc", "abc"))
    }

    @Test
    fun `levenshteinDistance — single insertion`() {
        assertEquals(1, FuzzyMatcher.levenshteinDistance("cat", "cats"))
    }

    @Test
    fun `levenshteinDistance — single deletion`() {
        assertEquals(1, FuzzyMatcher.levenshteinDistance("cats", "cat"))
    }

    @Test
    fun `levenshteinDistance — single substitution`() {
        assertEquals(1, FuzzyMatcher.levenshteinDistance("cat", "bat"))
    }

    @Test
    fun `levenshteinDistance — both empty is 0`() {
        assertEquals(0, FuzzyMatcher.levenshteinDistance("", ""))
    }

    // isFuzzyMatch tolerance rules

    @Test
    fun `isFuzzyMatch — 3-char query tolerates 1 edit`() {
        assertTrue(FuzzyMatcher.isFuzzyMatch("cat", "bat"))   // dist=1, len=3 → tol=1
        assertFalse(FuzzyMatcher.isFuzzyMatch("cat", "dog"))  // dist=3 > tol=1
    }

    @Test
    fun `isFuzzyMatch — 5-char query tolerates 2 edits`() {
        assertTrue(FuzzyMatcher.isFuzzyMatch("hello", "hells"))   // dist=1
        assertTrue(FuzzyMatcher.isFuzzyMatch("hello", "jello"))   // dist=1
    }

    @Test
    fun `isFuzzyMatch — 8-char query tolerates 3 edits`() {
        // "android" is 7 chars → tol=2; "androzds" has dist=2
        assertTrue(FuzzyMatcher.isFuzzyMatch("android", "androzd"))
    }

    @Test
    fun `isFuzzyMatch — case insensitive comparison`() {
        assertTrue(FuzzyMatcher.isFuzzyMatch("Hello", "hello"))
        assertTrue(FuzzyMatcher.isFuzzyMatch("KOTLIN", "kotlin"))
    }

    // fuzzySearch

    @Test
    fun `fuzzySearch — returns candidates within tolerance sorted by distance`() {
        val candidates = listOf("cat", "bat", "hat", "dog", "elephant")
        val results = FuzzyMatcher.fuzzySearch("cat", candidates)

        assertTrue(results.isNotEmpty())
        assertTrue(results.none { (_, dist) -> dist > 1 }) // 3-char → tol=1
        // "cat" itself should be first (dist=0)
        assertEquals("cat", results.first().first)
    }

    @Test
    fun `fuzzySearch — excludes out-of-tolerance candidates`() {
        val candidates = listOf("completely", "different", "words")
        val results = FuzzyMatcher.fuzzySearch("cat", candidates)
        assertTrue(results.isEmpty())
    }

    @Test
    fun `fuzzySearch — results sorted ascending by distance`() {
        val candidates = listOf("kitten", "sitten", "sitting", "cat")
        val results = FuzzyMatcher.fuzzySearch("kitten", candidates)
        for (i in 0 until results.size - 1) {
            assertTrue(results[i].second <= results[i + 1].second)
        }
    }
}
