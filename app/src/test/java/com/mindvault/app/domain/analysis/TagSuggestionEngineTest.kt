package com.mindvault.app.domain.analysis

import com.mindvault.app.data.model.Tag
import com.mindvault.app.data.repository.TagRepositoryInterface
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TagSuggestionEngineTest {

    private fun engine(tags: List<Tag> = emptyList()) =
        TagSuggestionEngine(FakeTagRepo(tags))

    @Test
    fun `suggests tag whose name appears in content`() = runTest {
        val tags = listOf(Tag(id = 1, name = "kotlin"))
        val suggestions = engine(tags).suggestTags("My kotlin notes", "kotlin is great", emptySet())
        assertTrue("kotlin" in suggestions)
    }

    @Test
    fun `does not suggest tag already assigned`() = runTest {
        val tags = listOf(Tag(id = 1, name = "kotlin"))
        val suggestions = engine(tags).suggestTags("kotlin notes", "about kotlin", setOf("kotlin"))
        assertTrue("kotlin" !in suggestions)
    }

    @Test
    fun `suggests high-frequency keyword as new tag`() = runTest {
        // "android" appears 3 times — should be suggested even with no existing tag
        val text = "android android android development"
        val suggestions = engine().suggestTags("android topic", text, emptySet())
        assertTrue("android" in suggestions)
    }

    @Test
    fun `caps suggestions at 5`() = runTest {
        val manyTags = (1..10).map { Tag(id = it.toLong(), name = "tag$it") }
        val content = manyTags.joinToString(" ") { it.name }
        val suggestions = engine(manyTags).suggestTags("title", content, emptySet())
        assertTrue(suggestions.size <= 5)
    }

    @Test
    fun `empty content returns no suggestions`() = runTest {
        val suggestions = engine().suggestTags("", "", emptySet())
        assertTrue(suggestions.isEmpty())
    }

    @Test
    fun `very short content returns no suggestions`() = runTest {
        val suggestions = engine().suggestTags("hi", "ok", emptySet())
        assertTrue(suggestions.isEmpty())
    }

    @Test
    fun `single-occurrence keyword not suggested`() = runTest {
        // "dragon" and "fruit" only appear in content (not in title) → appear once total → not suggested
        val suggestions = engine().suggestTags("hiking trip", "dragon fruit", emptySet())
        assertTrue("dragon" !in suggestions)
        assertTrue("fruit" !in suggestions)
    }

    // Minimal fake implementation
    private class FakeTagRepo(private val tags: List<Tag>) : TagRepositoryInterface {
        override fun getAllTags(): Flow<List<Tag>> = flow { emit(tags) }
        override fun getTagsForNote(noteId: Long): Flow<List<Tag>> = flow { emit(emptyList()) }
        override fun searchTags(query: String): Flow<List<Tag>> = flow { emit(tags.filter { it.name.contains(query) }) }
        override suspend fun insertTag(name: String, color: Int): Long = 0L
        override suspend fun deleteTag(tag: Tag) {}
        override suspend fun addTagToNote(noteId: Long, tagId: Long) {}
        override suspend fun removeTagFromNote(noteId: Long, tagId: Long) {}
        override suspend fun removeAllTagsFromNote(noteId: Long) {}
        override suspend fun setTagsForNote(noteId: Long, tagIds: List<Long>) {}
        override fun getActiveNoteIdsForTag(tagId: Long): Flow<Set<Long>> = flow { emit(emptySet()) }
    }
}
