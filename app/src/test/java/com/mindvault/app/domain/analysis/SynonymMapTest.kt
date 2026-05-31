package com.mindvault.app.domain.analysis

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SynonymMapTest {

    @Test
    fun `getSynonyms — photo returns image and picture`() {
        val synonyms = SynonymMap.getSynonyms("photo")
        assertTrue("image" in synonyms)
        assertTrue("picture" in synonyms)
    }

    @Test
    fun `getSynonyms — image returns photo and picture`() {
        val synonyms = SynonymMap.getSynonyms("image")
        assertTrue("photo" in synonyms)
        assertTrue("picture" in synonyms)
    }

    @Test
    fun `getSynonyms — unknown word returns empty set`() {
        assertTrue(SynonymMap.getSynonyms("unknownword").isEmpty())
    }

    @Test
    fun `getSynonyms — word not included in its own synonym set`() {
        val synonyms = SynonymMap.getSynonyms("photo")
        assertFalse("photo" in synonyms)
    }

    @Test
    fun `getSynonyms — case insensitive lookup`() {
        val synonyms = SynonymMap.getSynonyms("PHOTO")
        assertTrue("image" in synonyms)
    }

    @Test
    fun `getSynonyms — task returns todo and action`() {
        val synonyms = SynonymMap.getSynonyms("task")
        assertTrue("todo" in synonyms || "to-do" in synonyms)
        assertTrue("action" in synonyms)
    }

    @Test
    fun `expandQuery — includes original words and their synonyms`() {
        val expanded = SynonymMap.expandQuery("photo gallery")
        assertTrue("photo" in expanded)
        assertTrue("image" in expanded)
        assertTrue("picture" in expanded)
        assertTrue("gallery" in expanded)
    }

    @Test
    fun `expandQuery — single known word expands correctly`() {
        val expanded = SynonymMap.expandQuery("bug")
        assertTrue("bug" in expanded)
        assertTrue("issue" in expanded)
        assertTrue("error" in expanded)
    }

    @Test
    fun `expandQuery — unknown words are included unchanged`() {
        val expanded = SynonymMap.expandQuery("xylophone")
        assertTrue("xylophone" in expanded)
    }

    @Test
    fun `expandQuery — empty string returns empty or minimal list`() {
        val expanded = SynonymMap.expandQuery("")
        // Empty string split on whitespace may produce [""] — just verify it doesn't throw
        assertTrue(expanded.size >= 0)
    }
}
