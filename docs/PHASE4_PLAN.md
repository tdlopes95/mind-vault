# MindVault — Phase 4: Smart Features
## Implementation Plan for Claude Code

---

## Overview

Phase 4 adds the "intelligent" layer — local text analysis algorithms that make MindVault proactively useful. Auto-tag suggestions, related notes, fuzzy search, and auto-categorization, all running on-device with zero network dependency.

**No database schema changes in this phase.** Everything operates on existing data using in-memory analysis.

**Execution approach:** Implement all tasks sequentially in one session. Run the build after each task. Follow the task order — Tasks 1 and 2 share a text analysis foundation that Task 3 and 4 build on.

---

## Pre-requisites

- Phases 1–3 are complete
- Read `IMPLEMENTATION_NOTES.md` for established patterns and existing architecture
- Current DB version is 3 (no changes in this phase)

---

## Key Patterns (established in Phases 1–3)

- **Repository interface pattern:** all repositories have interfaces for testability
- **Entity ↔ Domain mapping:** private extension functions in repository files
- **DI wiring:** DAOs in `DatabaseModule`, repository interfaces in `RepositoryModule`
- **State collection:** `collectAsStateWithLifecycle()` everywhere
- **NoteEntity table name:** `notes` (not `NoteEntity`) — important for any raw queries
- **Tags loaded separately:** `TagRepository` provides tags per note; not eagerly loaded with notes
- **FTS search:** prefix matching via `"$query*"` suffix, 200ms debounce
- **HomeViewModel flow composition:** nested `combine` chains (already complex — be careful adding more)
- **Editor UI pattern:** expandable sections below content field (linked notes, attachments already there)

---

## New Package Structure

All smart analysis logic goes into a new `domain/analysis/` package to keep it cleanly separated from the data and UI layers:

```
domain/
└── analysis/
    ├── TextAnalyzer.kt              # Shared text utilities (tokenize, normalize, frequency)
    ├── StopWords.kt                 # English + Portuguese stop word lists
    ├── TagSuggestionEngine.kt       # Task 1
    ├── RelatedNotesEngine.kt        # Task 2
    ├── FuzzyMatcher.kt              # Task 3
    ├── SynonymMap.kt                # Task 3
    └── CategorySuggestionEngine.kt  # Task 4
```

---

## Task Order

```
Task 1 (Auto-Tag Suggestions) → Task 2 (Related Notes) → Task 3 (Smart Search) → Task 4 (Auto-Categorization)
```

---

## Task 1 — Auto-Tag Suggestions

### Shared Foundation: TextAnalyzer + StopWords

**Create first — used by Tasks 1, 2, and 4.**

**StopWords.kt:**
```kotlin
object StopWords {
    // Common English stop words
    val english = setOf(
        "a", "an", "the", "is", "are", "was", "were", "be", "been", "being",
        "have", "has", "had", "do", "does", "did", "will", "would", "could",
        "should", "may", "might", "can", "shall", "must", "need",
        "i", "me", "my", "we", "our", "you", "your", "he", "she", "it",
        "they", "them", "their", "this", "that", "these", "those",
        "in", "on", "at", "to", "for", "of", "with", "by", "from", "up",
        "about", "into", "through", "during", "before", "after",
        "and", "but", "or", "nor", "not", "so", "yet", "both", "either",
        "if", "then", "else", "when", "where", "how", "what", "which", "who",
        "all", "each", "every", "any", "some", "no", "other", "such",
        "just", "also", "very", "too", "quite", "really", "only",
        "here", "there", "now", "then", "still", "already",
        "more", "most", "less", "least", "much", "many", "few",
        "new", "old", "good", "bad", "great", "little", "big",
        "like", "know", "think", "make", "go", "get", "see", "come",
        "want", "use", "find", "give", "tell", "work", "call", "try",
        "than", "well", "back", "even", "way", "over"
    )

    // Common Portuguese stop words
    val portuguese = setOf(
        "o", "a", "os", "as", "um", "uma", "uns", "umas",
        "de", "do", "da", "dos", "das", "em", "no", "na", "nos", "nas",
        "por", "para", "com", "sem", "sob", "sobre", "entre",
        "e", "ou", "mas", "porém", "contudo", "todavia",
        "que", "se", "como", "quando", "onde", "qual", "quem",
        "eu", "tu", "ele", "ela", "nós", "vós", "eles", "elas",
        "me", "te", "se", "nos", "vos", "lhe", "lhes",
        "meu", "minha", "teu", "tua", "seu", "sua",
        "este", "esta", "esse", "essa", "aquele", "aquela",
        "isto", "isso", "aquilo",
        "ser", "estar", "ter", "haver", "fazer", "poder", "ir",
        "é", "são", "foi", "está", "tem", "há", "vai",
        "não", "sim", "mais", "menos", "muito", "pouco",
        "bem", "mal", "já", "ainda", "também", "só", "apenas"
    )

    val all = english + portuguese

    fun isStopWord(word: String): Boolean = word.lowercase() in all
}
```

**TextAnalyzer.kt:**
```kotlin
object TextAnalyzer {
    /**
     * Tokenize text into lowercase words, removing punctuation.
     * Returns only words with 3+ characters.
     */
    fun tokenize(text: String): List<String> {
        return text.lowercase()
            .replace(Regex("[^\\p{L}\\p{N}\\s]"), " ")  // Keep letters, digits, whitespace
            .split(Regex("\\s+"))
            .filter { it.length >= 3 }
    }

    /**
     * Extract significant words — tokenize then remove stop words.
     */
    fun extractKeywords(text: String): List<String> {
        return tokenize(text).filter { !StopWords.isStopWord(it) }
    }

    /**
     * Get word frequency map from text, excluding stop words.
     */
    fun wordFrequency(text: String): Map<String, Int> {
        return extractKeywords(text)
            .groupingBy { it }
            .eachCount()
    }

    /**
     * Extract top N keywords by frequency.
     */
    fun topKeywords(text: String, n: Int = 10): List<Pair<String, Int>> {
        return wordFrequency(text)
            .entries
            .sortedByDescending { it.value }
            .take(n)
            .map { it.key to it.value }
    }

    /**
     * Compute TF (term frequency) for a document.
     * TF = count of term / total terms in document
     */
    fun termFrequency(text: String): Map<String, Double> {
        val keywords = extractKeywords(text)
        val total = keywords.size.toDouble()
        if (total == 0.0) return emptyMap()
        return keywords.groupingBy { it }.eachCount()
            .mapValues { it.value / total }
    }

    /**
     * Simple cosine similarity between two TF maps.
     */
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
        val denominator = Math.sqrt(norm1) * Math.sqrt(norm2)
        return if (denominator == 0.0) 0.0 else dotProduct / denominator
    }
}
```

### TagSuggestionEngine

**New file:** `domain/analysis/TagSuggestionEngine.kt`

```kotlin
class TagSuggestionEngine @Inject constructor(
    private val tagRepository: TagRepositoryInterface
) {
    /**
     * Suggest tags for a note based on its content.
     * Returns a list of suggested tag names (max 5).
     *
     * Strategy:
     * 1. Match existing tags: if the note content contains text matching existing tag names, suggest those first.
     * 2. Keyword frequency: extract top keywords from title + content. If a keyword appears 2+ times
     *    and isn't already a tag on this note, suggest it.
     * 3. Limit to 5 suggestions.
     */
    suspend fun suggestTags(
        title: String,
        content: String,
        existingTagNames: Set<String>  // Tags already on this note
    ): List<String>
}
```

**Algorithm detail:**

1. **Existing tag matching (highest priority):** Fetch all tags from `tagRepository.getAllTags()`. For each tag, check if the tag name appears in the note's title or content (case-insensitive). If it does and it's not already assigned to this note, suggest it. This leverages the user's own taxonomy.

2. **Keyword extraction (second priority):** Combine title (weighted 2x by duplicating) + content. Run `TextAnalyzer.topKeywords()`. Take keywords that appear 2+ times, aren't already existing tag names, and aren't already suggested from step 1. Format them as potential tag names.

3. **Cap at 5 suggestions total,** prioritizing existing tag matches over new keywords.

### UI — NoteEditorScreen Changes

- After saving a note (in `saveNote()` completion), run `TagSuggestionEngine.suggestTags()` in the ViewModel
- If suggestions are available, show a **suggestion bar** below the title field (or as a collapsible section):
  - Small label: "Suggested tags:"
  - Horizontal row of suggestion chips, each showing the tag name
  - Each chip has two actions: "✓" (accept — adds the tag) and "×" (dismiss — removes from suggestions)
  - The entire bar can be dismissed with a "Dismiss all" action
- Suggestions are **ephemeral** — they're not stored. They regenerate on each save. Dismissed suggestions for the current editing session should not reappear until the next save.
- Don't show suggestions if the note has no content (title + content both empty or very short)

### NoteEditorViewModel Changes

- Add `suggestedTags: List<String>` to `EditorUiState`
- Add `dismissedSuggestions: Set<String>` (session-only, not persisted)
- Inject `TagSuggestionEngine` (or instantiate it — it just needs `TagRepositoryInterface`)
- Method: `generateTagSuggestions()` — called after successful save
- Method: `acceptSuggestion(tagName: String)` — creates the tag if new, adds to note, removes from suggestions
- Method: `dismissSuggestion(tagName: String)` — adds to dismissed set, removes from visible suggestions
- Method: `dismissAllSuggestions()` — clears the suggestions list

### DI Changes

- `TagSuggestionEngine` can be provided via `@Inject constructor` (no module needed if using constructor injection with Hilt)
- If it needs to be a singleton (to cache tag list), bind it in `RepositoryModule`. Otherwise, scoped to the ViewModel is fine.

### Acceptance Criteria

- After saving a note with content, tag suggestions appear within ~200ms
- Suggestions include existing tags whose names appear in the content
- Suggestions include frequent keywords not already tagged
- Accepting a suggestion adds the tag to the note
- Dismissing a suggestion removes it from the current session
- Suggestions don't appear for empty/very short notes
- No suggestions shown if all keywords are already tagged
- Performance: analysis completes in < 200ms for a 5000-word note

---

## Task 2 — Related Notes

### RelatedNotesEngine

**New file:** `domain/analysis/RelatedNotesEngine.kt`

```kotlin
class RelatedNotesEngine @Inject constructor(
    private val noteRepository: NoteRepositoryInterface,
    private val tagRepository: TagRepositoryInterface,
    private val noteLinkRepository: NoteLinkRepositoryInterface
) {
    /**
     * Find notes related to the given note.
     * Returns top 5 related notes with a relevance reason.
     *
     * Scoring:
     * - Tag overlap: +3 points per shared tag
     * - Same category: +2 points
     * - Content similarity: +1-5 points based on TF cosine similarity
     * - Link proximity: +2 points if linked to a common third note
     */
    suspend fun findRelatedNotes(
        noteId: Long,
        title: String,
        content: String,
        tags: List<Tag>,
        categoryId: Long?
    ): List<RelatedNote>
}

data class RelatedNote(
    val note: Note,
    val score: Double,
    val reason: String  // Human-readable: "3 shared tags", "Same category + similar content"
)
```

**Algorithm detail:**

1. **Fetch candidate notes:** Get all active notes from `noteRepository` (exclude the current note and deleted/archived notes). For a large dataset this could be expensive — cap at 100 most recent notes if the total count is > 200.

2. **Tag overlap scoring:** For each candidate note, load its tags via `tagRepository`. Count shared tags with the current note. Score: `sharedTagCount * 3`.

3. **Category scoring:** If the candidate has the same `categoryId` as the current note (and it's not null), add 2 points.

4. **Content similarity scoring:** Compute TF vectors for the current note and each candidate using `TextAnalyzer.termFrequency()`. Compute cosine similarity. Scale to 0–5 points: `cosineSimilarity * 5`.

5. **Link proximity scoring:** Get the current note's linked notes. For each candidate, check if it shares a link target with the current note (they both link to the same third note). Add 2 points per shared link neighbor.

6. **Rank by total score.** Return top 5 with score > 0. Build a human-readable reason string from the contributing factors.

**Performance consideration:** Computing TF-IDF against all notes on every editor open is expensive. Strategies:
- **Lazy computation:** Only compute when the user scrolls to the "Related Notes" section (expand to trigger)
- **Cache in ViewModel:** Don't recompute on recomposition
- **Background coroutine:** Run on `Dispatchers.Default`, show a loading indicator
- **Cap candidates:** Only compare against the 50 most recently edited notes if total > 100

### New UI Component: RelatedNotesSection

**New file:** `ui/components/RelatedNotesSection.kt`

A collapsible section (similar to "Linked Notes") shown at the bottom of the editor, below the linked notes section.

- Header: "💡 Related Notes" with expand/collapse chevron
- When expanded and loading: show a small `CircularProgressIndicator`
- When expanded with results: show a vertical list of compact note cards, each showing:
  - Note title
  - Content preview (1 line)
  - Relevance reason as a small caption (e.g., "3 shared tags · Similar content")
  - Tapping navigates to that note
- When expanded with no results: show "No related notes found"
- Computation triggers on first expand (not on screen load)

### NoteEditorViewModel Changes

- Inject `RelatedNotesEngine`
- Add `relatedNotes: List<RelatedNote>` to `EditorUiState`
- Add `isLoadingRelated: Boolean` to `EditorUiState`
- Add `relatedNotesExpanded: Boolean` to `EditorUiState`
- Method: `loadRelatedNotes()` — called when section is expanded for the first time
- Method: `toggleRelatedNotesExpanded()` — toggles expanded state, triggers load if first time

### Acceptance Criteria

- Related notes section appears in the editor below linked notes
- Expanding the section triggers computation with a loading indicator
- Related notes are ranked by relevance with a visible reason
- Tapping a related note navigates to it
- Notes with shared tags rank highly
- Notes in the same category rank higher
- Content similarity contributes to ranking
- Computation completes within 1 second for 100 notes
- No related notes shown for a brand-new empty note

---

## Task 3 — Smart Search Enhancements

### FuzzyMatcher

**New file:** `domain/analysis/FuzzyMatcher.kt`

```kotlin
object FuzzyMatcher {
    /**
     * Levenshtein distance between two strings.
     */
    fun levenshteinDistance(s1: String, s2: String): Int

    /**
     * Check if two strings are "fuzzy equal" within a tolerance.
     * Tolerance scales with string length:
     * - length 3-4: allow 1 edit
     * - length 5-7: allow 2 edits
     * - length 8+: allow 3 edits
     */
    fun isFuzzyMatch(query: String, target: String): Boolean

    /**
     * Find the best fuzzy matches for a query against a list of candidates.
     * Returns candidates sorted by distance (closest first).
     * Only includes matches within tolerance.
     */
    fun fuzzySearch(query: String, candidates: List<String>): List<Pair<String, Int>>
}
```

**Levenshtein implementation:** Standard dynamic programming algorithm. This is well-documented — a simple 2D matrix approach works fine for strings under 100 characters (which note titles always are).

### SynonymMap

**New file:** `domain/analysis/SynonymMap.kt`

```kotlin
object SynonymMap {
    /**
     * Local synonym groups. Each group is a set of words that should be treated as equivalent in search.
     */
    private val synonymGroups = listOf(
        setOf("image", "photo", "picture", "photograph", "img"),
        setOf("task", "todo", "to-do", "item", "action"),
        setOf("idea", "concept", "thought"),
        setOf("bug", "issue", "problem", "error", "defect"),
        setOf("meeting", "call", "discussion", "standup"),
        setOf("doc", "document", "documentation", "docs"),
        setOf("code", "programming", "coding", "development", "dev"),
        setOf("test", "testing", "qa", "quality"),
        setOf("design", "ui", "ux", "interface", "layout"),
        setOf("deploy", "deployment", "release", "ship"),
        setOf("config", "configuration", "settings", "setup"),
        setOf("db", "database", "storage", "data"),
        setOf("api", "endpoint", "service", "rest"),
        setOf("auth", "authentication", "login", "signin"),
        setOf("note", "notes", "memo", "annotation"),
        setOf("link", "url", "reference", "ref"),
        setOf("file", "attachment", "asset"),
        // Portuguese common synonyms
        setOf("tarefa", "trabalho", "atividade"),
        setOf("ideia", "conceito", "pensamento"),
        setOf("reunião", "encontro", "chamada"),
        setOf("problema", "erro", "defeito"),
        setOf("aula", "classe", "lição"),
        setOf("estudo", "estudar", "aprendizado"),
    )

    // Build a lookup map: word → set of synonyms (excluding itself)
    private val synonymLookup: Map<String, Set<String>> = buildMap {
        for (group in synonymGroups) {
            for (word in group) {
                put(word.lowercase(), group.map { it.lowercase() }.toSet() - word.lowercase())
            }
        }
    }

    /**
     * Get synonyms for a word. Returns empty set if no synonyms known.
     */
    fun getSynonyms(word: String): Set<String> {
        return synonymLookup[word.lowercase()] ?: emptySet()
    }

    /**
     * Expand a search query with synonyms.
     * E.g., "photo gallery" → ["photo", "image", "picture", "photograph", "img", "gallery"]
     */
    fun expandQuery(query: String): List<String> {
        val words = query.lowercase().split(Regex("\\s+"))
        val expanded = mutableSetOf<String>()
        for (word in words) {
            expanded.add(word)
            expanded.addAll(getSynonyms(word))
        }
        return expanded.toList()
    }
}
```

### Search Enhancement Integration

**Changes to HomeViewModel / search flow:**

The current search flow is:
1. User types query → `searchQuery.debounce(200)` → FTS search via `noteRepository.searchNotesFts(query)`

Enhanced flow:
1. User types query → debounce 200ms
2. Run FTS search with original query
3. If FTS returns results → display them
4. If FTS returns empty (or very few results):
   a. **Synonym expansion:** expand the query via `SynonymMap.expandQuery()`, run FTS on each synonym, merge results
   b. **Fuzzy fallback:** if still no results, run `FuzzyMatcher` against all note titles. Show results with a "Did you mean...?" banner.
5. Show a "Did you mean: [corrected query]?" chip if fuzzy search found a close match for the original query

**Search filter chips (new UI):**

Add filter chips below the search bar that appear when a search is active:

- **Date range:** "Last 7 days", "Last 30 days", "Older" (simple presets, not a date picker)
- **Category:** dropdown or chips showing categories that have matching notes
- **Has tags:** filter to notes that have any tags
- **Favorites only:** toggle
- **Has attachments:** filter to notes with attachments

Implementation: these are post-filters applied after the FTS query results. In the ViewModel, add filter state and apply `.filter {}` on the search results flow.

**New data in HomeUiState:**
```kotlin
data class SearchFilters(
    val dateRange: DateRange? = null,        // null = all time
    val categoryId: Long? = null,            // null = all categories
    val favoritesOnly: Boolean = false,
    val hasAttachments: Boolean = false,
    val hasLinks: Boolean = false
)

enum class DateRange { LAST_7_DAYS, LAST_30_DAYS, OLDER }
```

### UI Changes

- **Search results area:** when search is active and has results:
  - Show filter chip row (horizontal scroll) below search bar
  - Each chip is toggleable (outlined when off, filled when active)
  - Active filters narrow results immediately
  - "Clear filters" option when any filter is active
- **"Did you mean...?" banner:** when fuzzy search suggests a correction, show a small banner above results: "Did you mean: [suggestion]?" — tapping applies the suggested query
- **Empty search results:** when no results found even after fuzzy/synonym expansion, show: "No notes found for '[query]'. Try different keywords or check your filters."

### Acceptance Criteria

- Typos in search return results (e.g., "andorid" finds notes about "android")
- Fuzzy tolerance scales with word length
- "Did you mean...?" appears when fuzzy finds a better match
- Synonym expansion works (searching "photo" finds notes mentioning "image")
- Date range filter works
- Category filter works during search
- Favorites filter works during search
- Has-attachments filter works
- Filters combine with AND logic
- Filter state resets when search is cleared
- Performance: fuzzy search on 500 note titles completes in < 100ms

---

## Task 4 — Auto-Categorization Suggestions

### CategorySuggestionEngine

**New file:** `domain/analysis/CategorySuggestionEngine.kt`

```kotlin
class CategorySuggestionEngine @Inject constructor(
    private val categoryRepository: CategoryRepositoryInterface,
    private val tagRepository: TagRepositoryInterface,
    private val noteRepository: NoteRepositoryInterface
) {
    /**
     * Suggest a category for an uncategorized note.
     * Returns null if no confident suggestion.
     *
     * Strategy:
     * 1. Tag-to-category correlation: look at what categories other notes with the same tags are in.
     *    If 60%+ of notes with tag X are in category Y, suggest Y.
     * 2. Content keyword matching: compare the note's keywords against keyword profiles of existing categories
     *    (built from the content of all notes in each category).
     * 3. Only suggest if confidence > threshold (60% correlation or 0.3+ content similarity).
     */
    suspend fun suggestCategory(
        noteId: Long,
        title: String,
        content: String,
        tags: List<Tag>
    ): CategorySuggestion?
}

data class CategorySuggestion(
    val category: Category,
    val confidence: Double,     // 0.0 to 1.0
    val reason: String          // "Most notes tagged #kotlin are in 'Programming'"
)
```

**Algorithm detail:**

1. **Tag-to-category correlation (primary signal):**
  - For each tag on the current note, find all other notes with that tag
  - Check which categories those notes are assigned to
  - Count: for each category, how many notes share at least one tag with the current note?
  - If one category accounts for 60%+ of tag-correlated notes, suggest it
  - Reason: "Most notes tagged #[tag] are in '[category]'"

2. **Content similarity (secondary signal):**
  - For each existing category, build a "category content profile": concatenate title+content of all notes in that category, compute TF map
  - Compute TF for the current note
  - Find the category with the highest cosine similarity
  - If similarity > 0.3, suggest it
  - Reason: "Content is similar to notes in '[category]'"

3. **Combine signals:** If both methods suggest the same category, confidence is high. If they differ, prefer the tag correlation method (user's explicit organization is more reliable than content analysis).

4. **Minimum data requirement:** Don't suggest if there are fewer than 3 categories or fewer than 10 categorized notes (not enough data to learn patterns).

### UI — NoteEditorScreen Changes

- When saving an uncategorized note, run `CategorySuggestionEngine.suggestCategory()` in the ViewModel
- If a suggestion is available, show a **suggestion banner** below the tag suggestions section:
  - "📂 Suggested category: [Category Name]" with the reason as a small caption
  - Two buttons: "Accept" (assigns the category) and "Dismiss" (hides the banner)
- Only show for notes with `categoryId == null`
- Don't show if the user explicitly removed a category (they chose to have none)

**Implementation detail:** To distinguish "never categorized" from "user removed category," you could track this. However, for simplicity in Phase 4, just suggest whenever `categoryId == null`. If the user dismisses, it won't persist — next save will suggest again. This is slightly annoying but simple. A `dismissedCategorySuggestion: Boolean` in the ViewModel (session-scoped) prevents re-suggestion during the current editing session.

### NoteEditorViewModel Changes

- Inject `CategorySuggestionEngine`
- Add `categorySuggestion: CategorySuggestion?` to `EditorUiState`
- Method: `generateCategorySuggestion()` — called after successful save (alongside tag suggestions)
- Method: `acceptCategorySuggestion()` — assigns the suggested category to the note
- Method: `dismissCategorySuggestion()` — sets a session flag to prevent re-suggestion

### Acceptance Criteria

- After saving an uncategorized note, a category suggestion appears (if confident enough)
- Suggestion shows the category name and a reason
- Accepting assigns the category
- Dismissing hides the suggestion for the current session
- No suggestion for notes with very little content
- No suggestion if fewer than 3 categories exist
- Tag-to-category correlation works (notes with #kotlin tags → suggest "Programming" if that's where most #kotlin notes are)
- Content similarity works as a fallback when no tag correlation exists

---

## DI / Architecture Notes

**All analysis engines use `@Inject constructor`** and accept repository interfaces. They don't need `@Singleton` scope — they're stateless utilities that can be created fresh per ViewModel. Hilt will inject them automatically via constructor injection.

**Testing:** Analysis engines are pure logic with repository interfaces as dependencies. They're straightforward to unit test with `FakeNoteRepository`, `FakeTagRepository`, etc. Write tests for:
- `TextAnalyzer.tokenize()` — various inputs, edge cases
- `TextAnalyzer.cosineSimilarity()` — known values
- `FuzzyMatcher.levenshteinDistance()` — known distance pairs
- `SynonymMap.expandQuery()` — expansion works, unknown words pass through
- `TagSuggestionEngine` — with controlled note content and tag lists
- `RelatedNotesEngine` — with notes that share tags vs don't

---

## Performance Budget

All smart features must respect these limits on a mid-range device:

| Operation | Target | Fallback |
|---|---|---|
| Tag suggestion | < 200ms | Cap keyword extraction at 5000 chars |
| Related notes | < 1 second | Cap candidate notes at 50 |
| Fuzzy search | < 100ms on 500 titles | Pre-filter by first character |
| Category suggestion | < 500ms | Cap analysis to 100 notes per category |
| Total post-save analysis | < 700ms | Run tag + category suggestion in parallel |

**Parallelization:** Tag suggestions and category suggestions can run in parallel via `async { }` in the ViewModel since they're independent. Related notes are computed lazily (on expand), so they don't add to save time.

---

## Phase 4 — Definition of Done

- [ ] TextAnalyzer and StopWords work correctly (tokenize, frequency, cosine similarity)
- [ ] Auto-tag suggestions appear after saving with relevant keywords
- [ ] Existing tag matching works (content matches tag names → suggests them)
- [ ] Accepting a tag suggestion adds the tag
- [ ] Dismissing a suggestion removes it for the session
- [ ] Related notes section shows relevant notes with reasons
- [ ] Related notes are ranked by score (tag overlap > category > content)
- [ ] Related notes load lazily on section expand
- [ ] Fuzzy search handles typos in search queries
- [ ] "Did you mean...?" appears for fuzzy corrections
- [ ] Synonym expansion finds related terms
- [ ] Search filter chips work (date range, category, favorites, has attachments)
- [ ] Auto-categorization suggests categories for uncategorized notes
- [ ] Category suggestions based on tag correlation work
- [ ] All smart features run locally with no network
- [ ] Performance: no perceptible lag on save or search
- [ ] All Phase 1–3 functionality still works (regression)