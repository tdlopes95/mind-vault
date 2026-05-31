# MindVault — Complete Project Roadmap
## Personal Knowledge Vault & Second Brain

---

## Project Summary

MindVault is an offline-first Android app for personal knowledge management. It combines the simplicity of Google Keep with the organizational depth of Obsidian — notes, tags, categories, note linking, markdown, attachments, and smart features, all running locally on-device with no cloud dependency.

---

## Architecture & Tech Stack

| Layer | Technology | Notes |
|---|---|---|
| Language | Kotlin | |
| UI | Jetpack Compose + Material Design 3 | Dynamic color + dark mode |
| Architecture | MVVM | Model–View–ViewModel |
| Database | Room + FTS4 | FTS4 set up from Phase 1 |
| DI | Hilt (2.57+) | Required for AGP 9.x compat |
| Navigation | Navigation Compose (2.9+) | Requires compileSdk ≥ 35 |
| State | StateFlow + State hoisting | |
| Async | Kotlin Coroutines + Flow | |
| Min SDK | 26 (Android 8.0) | |
| Target/Compile SDK | 35 | Bumped from 34 per Nav Compose requirement |
| Build | Gradle (Kotlin DSL) + KSP | KSP 2.x versioning scheme |

### Known Build Environment Constraints (from Phase 1)

These are documented in `IMPLEMENTATION_NOTES.md` and must be preserved across all phases:

- `kotlin-android` plugin must NOT be applied (AGP 9.x conflict with `kotlin-compose`)
- `android.disallowKotlinSourceSets=false` required in `gradle.properties` (KSP + AGP 9.x)
- `org.gradle.java.home` must point to the system JDK (not VS Code's bundled JRE)
- Hilt 2.57+ required (older versions fail with AGP 9.x)

---

## Current Project Structure (post-Phase 3)

```
com.mindvault.app/
├── MindVaultApplication.kt              # @HiltAndroidApp + auto-purge on startup
├── MainActivity.kt                      # Single Activity, hosts NavHost
│
├── data/
│   ├── local/
│   │   ├── MindVaultDatabase.kt         # Room DB (version 3, migrations v1→v2→v3)
│   │   ├── dao/
│   │   │   ├── NoteDao.kt              # Includes FTS4 search, pin queries
│   │   │   ├── TagDao.kt
│   │   │   ├── CategoryDao.kt
│   │   │   ├── NoteLinkDao.kt          # Bidirectional link queries
│   │   │   └── AttachmentDao.kt
│   │   ├── entity/
│   │   │   ├── NoteEntity.kt           # categoryId, isPinned
│   │   │   ├── NoteFts.kt
│   │   │   ├── TagEntity.kt
│   │   │   ├── NoteTagCrossRef.kt
│   │   │   ├── CategoryEntity.kt
│   │   │   ├── NoteLinkEntity.kt
│   │   │   └── AttachmentEntity.kt
│   │   └── converter/
│   │       └── DateConverter.kt
│   │
│   ├── repository/
│   │   ├── NoteRepository.kt
│   │   ├── NoteRepositoryInterface.kt
│   │   ├── TagRepository.kt
│   │   ├── TagRepositoryInterface.kt
│   │   ├── CategoryRepository.kt
│   │   ├── CategoryRepositoryInterface.kt
│   │   ├── NoteLinkRepository.kt
│   │   ├── NoteLinkRepositoryInterface.kt
│   │   ├── AttachmentRepository.kt
│   │   └── AttachmentRepositoryInterface.kt
│   │
│   ├── model/
│   │   ├── Note.kt                     # tags, categoryId, isPinned
│   │   ├── Tag.kt
│   │   ├── Category.kt
│   │   ├── NoteLink.kt
│   │   └── Attachment.kt               # isImage, isPdf, formattedSize
│   │
│   ├── local/
│   │   ├── RecentSearchesDataStore.kt
│   │   └── DashboardPreferencesDataStore.kt
│   │
│   └── backup/
│       └── BackupManager.kt            # v2: links, isPinned, attachment metadata
│
├── di/
│   ├── DatabaseModule.kt
│   └── RepositoryModule.kt
│
├── domain/
│   └── analysis/
│       ├── TextAnalyzer.kt              # Tokenize, keyword extraction, TF, cosine similarity
│       ├── StopWords.kt                 # English + Portuguese stop words
│       ├── TagSuggestionEngine.kt       # Existing tag matching + keyword frequency
│       ├── RelatedNotesEngine.kt        # Tag overlap, category, content similarity, link proximity
│       ├── FuzzyMatcher.kt              # Levenshtein distance, tolerance scaling
│       ├── SynonymMap.kt                # EN + PT synonym groups
│       └── CategorySuggestionEngine.kt  # Tag-to-category correlation + content similarity
│
├── ui/
│   ├── theme/ ...
│   ├── navigation/
│   │   ├── MindVaultNavHost.kt
│   │   └── Screen.kt
│   ├── screens/
│   │   ├── home/
│   │   │   ├── HomeScreen.kt           # Dashboard + grid modes
│   │   │   └── HomeViewModel.kt        # Pinned, recent, favorites, categories
│   │   ├── editor/
│   │   │   ├── NoteEditorScreen.kt     # Tags, links, attachments, markdown preview
│   │   │   └── NoteEditorViewModel.kt
│   │   ├── categories/ ...
│   │   ├── archive/ ...
│   │   ├── trash/ ...
│   │   └── settings/ ...
│   │
│   └── components/
│       ├── NoteCard.kt                 # Link count badge, pin icon
│       ├── EmptyState.kt
│       ├── SearchBar.kt
│       ├── ConfirmDeleteDialog.kt
│       ├── NoteOptionsBottomSheet.kt   # Pin/unpin option added
│       ├── TagPickerBottomSheet.kt
│       ├── TagChip.kt
│       ├── CategoryPickerDialog.kt
│       ├── AppDrawer.kt
│       ├── NoteLinkChip.kt
│       ├── NoteLinkPickerDialog.kt
│       ├── AttachmentRow.kt
│       ├── AttachmentThumbnail.kt      # Coil AsyncImage for images
│       └── MarkdownRenderer.kt         # Custom parser, 9 elements, interactive checklists
│
└── util/
    ├── DateUtils.kt
    └── Constants.kt
```

---

## Database Schema Evolution

### Phase 1 (current — DB version 1)

**NoteEntity:** id, title, content, color, isFavorite, isArchived, isDeleted, createdAt, updatedAt, deletedAt
**NoteFts:** title, content (FTS4, mirrors NoteEntity)

### Phase 2 additions (DB version 2)

**TagEntity:** id (Long PK), name (String, unique), color (Int, default 0), createdAt (Long)
**NoteTagCrossRef:** noteId (Long FK), tagId (Long FK) — composite PK
**CategoryEntity:** id (Long PK), name (String, unique), color (Int), icon (String?), sortOrder (Int), createdAt (Long)
- Add `categoryId: Long?` field to NoteEntity (FK to CategoryEntity, nullable)

### Phase 3 additions (DB version 3)

**NoteLinkEntity:** id (Long PK), sourceNoteId (Long FK), targetNoteId (Long FK), createdAt (Long) — unique constraint on (source, target)
**AttachmentEntity:** id (Long PK), noteId (Long FK), fileName (String), filePath (String), mimeType (String), fileSize (Long), createdAt (Long)
- Add `isPinned: Boolean` field to NoteEntity (for dashboard pinning)

### Phase 4 — no schema changes

Smart features operate on existing data using in-memory analysis.

### Phase 5 — DB version 4

Performance indexes on the `notes` table (isDeleted, isArchived, isFavorite, isPinned, updatedAt, composite isDeleted+isArchived). Added via MIGRATION_3_4.

---

# Phase 1 — Foundation ✅ COMPLETE

**Status:** All tasks done. App launches, notes CRUD works, search works, dark mode works.

**Open items — all resolved in Phase 2 Task 1:**
- ~~System back gesture does not trigger auto-save~~ → BackHandler added
- ~~`collectAsState()` used instead of `collectAsStateWithLifecycle()`~~ → upgraded everywhere
- ~~Auto-purge of old deleted notes~~ → added to MindVaultApplication startup

**See:** `PHASE1_PLAN.md` and `IMPLEMENTATION_NOTES.md` for full details.

---

# Phase 2 — Organization & Search ✅ COMPLETE

**Status:** All 6 tasks done. Tags, categories, FTS search, JSON backup/export, navigation drawer, archive/trash screens all functional.

**Deferred items carried to Phase 3 (low priority):**
- Raw database backup/restore (JSON backup works; raw `.db` copy not implemented)
- CategoriesScreen drag-to-reorder (`sortOrder` field exists in DB but no drag UI)

**Completed:**
- BackHandler for system back gesture save
- collectAsStateWithLifecycle() everywhere
- Auto-purge of 30-day-old trash on app startup
- DB migration v1 → v2 (tags, categories, cross-refs, categoryId on notes)
- FTS4 search with prefix matching and 200ms debounce
- RecentSearchesDataStore (DataStore Preferences)
- BackupManager with JSON export/import via SAF
- ModalNavigationDrawer with All Notes, Favorites, Archive, Trash, Categories, Settings
- ArchiveScreen and TrashScreen with restore/delete/empty functionality

## Overview

Phase 2 adds the organizational layer: tags, categories, upgraded FTS search, and backup/export. This turns MindVault from "a notes app" into "a knowledge tool."

## Task 1 — Phase 1 Polish & Fixes

**What to do:**
- Add `BackHandler` in `NoteEditorScreen` to trigger save on system back gesture
- Add `lifecycle-runtime-compose` dependency and switch `collectAsState()` → `collectAsStateWithLifecycle()` across all screens
- Verify notes persist across app restart (force-stop + reopen)
- Purge old deleted notes on app startup (30-day cutoff) — add to `MindVaultApplication` or `MainActivity`

**Acceptance criteria:**
- System back gesture saves the note
- Old soft-deleted notes are auto-purged on launch

---

## Task 2 — Tags System

**New files:**
```
data/local/entity/TagEntity.kt
data/local/entity/NoteTagCrossRef.kt
data/local/dao/TagDao.kt
data/model/Tag.kt
data/repository/TagRepository.kt
data/repository/TagRepositoryInterface.kt
```

**TagDao queries:**
```
suspend fun insertTag(tag: TagEntity): Long
suspend fun deleteTag(tag: TagEntity)
fun getAllTags(): Flow<List<TagEntity>>
fun getTagsForNote(noteId: Long): Flow<List<TagEntity>>
fun getNotesForTag(tagId: Long): Flow<List<NoteEntity>>
suspend fun addTagToNote(crossRef: NoteTagCrossRef)
suspend fun removeTagFromNote(noteId: Long, tagId: Long)
fun searchTags(query: String): Flow<List<TagEntity>>
```

**UI changes:**
- `NoteEditorScreen`: add tag chips below the title field. Tapping "+" opens a tag picker bottom sheet. User can select existing tags or create new ones inline.
- `HomeScreen`: add a horizontal tag filter row below the search bar (scrollable chips). Tapping a tag filters notes. Tapping again deselects.
- `TagPickerBottomSheet.kt` — new component: search/select/create tags

**Acceptance criteria:**
- User can create tags
- User can add/remove tags on a note
- User can filter notes by tag on home screen
- Tags persist and survive app restart

---

## Task 3 — Categories System

**New files:**
```
data/local/entity/CategoryEntity.kt
data/local/dao/CategoryDao.kt
data/model/Category.kt
data/repository/CategoryRepository.kt
data/repository/CategoryRepositoryInterface.kt
ui/screens/categories/CategoriesScreen.kt
ui/screens/categories/CategoriesViewModel.kt
ui/components/CategoryPickerDialog.kt
```

**CategoryDao queries:**
```
suspend fun insertCategory(category: CategoryEntity): Long
suspend fun updateCategory(category: CategoryEntity)
suspend fun deleteCategory(category: CategoryEntity)
fun getAllCategories(): Flow<List<CategoryEntity>>
fun getCategoryById(id: Long): Flow<CategoryEntity?>
fun getNotesInCategory(categoryId: Long): Flow<List<NoteEntity>>
fun getUncategorizedNotes(): Flow<List<NoteEntity>>
```

**Schema change:** add `categoryId: Long?` to `NoteEntity` (migration v1 → v2)

**UI changes:**
- `NoteEditorScreen`: add category selector (small chip or label under tags, tap to change)
- `HomeScreen`: add category filter (tab row or dropdown alongside tag filter)
- `CategoriesScreen`: manage categories (create, rename, reorder, delete, set color/icon). Accessible from a navigation drawer or bottom nav.
- Add navigation route: `Screen.Categories`

**Acceptance criteria:**
- User can create/edit/delete categories
- User can assign a note to a category
- User can filter notes by category
- Deleting a category does NOT delete its notes (sets categoryId to null)

---

## Task 4 — FTS Search Upgrade

**What to do:**
- Replace the Phase 1 `LIKE`-based `searchNotes()` in `NoteDao` with FTS4 queries against `NoteFts`
- Implement search ranking (FTS4 `matchinfo()` or `rank` function)
- Add search results screen or enhance home screen search to show results with highlighted matching terms
- Add search by tag name and category name (combine FTS results with tag/category matches)
- Add recent searches (store last 10 queries in DataStore or a simple Room table)

**FTS4 query pattern:**
```sql
SELECT NoteEntity.* FROM NoteEntity
JOIN NoteFts ON NoteEntity.rowid = NoteFts.rowid
WHERE NoteFts MATCH :query
ORDER BY rank
```

**UI changes:**
- Search results show which field matched (title vs content) with bold highlights
- "No results" state with suggestion to broaden search
- Recent searches shown when search bar is focused but empty

**Acceptance criteria:**
- FTS search returns results faster than LIKE for 100+ notes
- Search matches partial words (prefix matching via FTS4 `*` operator)
- Tag and category names are searchable
- Recent searches persist

---

## Task 5 — Backup & Export

**New files:**
```
data/backup/BackupManager.kt
ui/screens/settings/SettingsScreen.kt
ui/screens/settings/SettingsViewModel.kt
```

**Backup functionality:**
- **JSON export:** export all notes (with tags, categories) as a structured JSON file to Downloads folder
- **JSON import:** import from a previously exported JSON file, with duplicate detection (by title + createdAt)
- **Database backup:** copy the Room `.db` file to external storage (raw backup, fastest restore)
- Use Android's `MediaStore` API or SAF (Storage Access Framework) for file picker

**Settings screen:**
- Add `Screen.Settings` route
- Navigation: accessible from HomeScreen top bar (gear icon)
- Options: Export notes (JSON), Import notes (JSON), Backup database, Restore database, About section

**Acceptance criteria:**
- User can export all notes to JSON
- User can import notes from JSON (with duplicate handling)
- User can backup/restore the raw database
- Export file is human-readable JSON

---

## Task 6 — Navigation Drawer or Bottom Navigation

**What to do:**
- Add a navigation structure beyond just Home. Options:
  - **Option A — Navigation Drawer** (recommended for this app): hamburger menu with sections: All Notes, Favorites, Archive, Trash, Categories, Tags, Settings
  - **Option B — Bottom Navigation**: Home, Search, Categories, Settings (simpler but less room for growth)
- Implement Archive screen (list of archived notes, option to restore or permanently delete)
- Implement Trash screen (list of soft-deleted notes, option to restore or permanently delete, "Empty Trash" button)

**New screens:**
```
ui/screens/archive/ArchiveScreen.kt
ui/screens/archive/ArchiveViewModel.kt
ui/screens/trash/TrashScreen.kt
ui/screens/trash/TrashViewModel.kt
```

**Acceptance criteria:**
- User can navigate to Archive, Trash, Categories, Settings
- Archive shows archived notes with restore option
- Trash shows deleted notes with restore and permanent delete options
- "Empty Trash" permanently deletes all trashed notes

---

## Phase 2 — Definition of Done

- [x] Tags: create, assign to notes, filter by tag, remove
- [x] Categories: create, assign, filter, manage
- [x] FTS search works with ranking and highlights
- [x] Backup/export to JSON works
- [x] Import from JSON works
- [x] Archive and Trash screens functional
- [x] Navigation drawer (or bottom nav) provides access to all sections
- [x] System back gesture saves notes
- [x] DB migration v1 → v2 tested
- [x] All existing Phase 1 functionality still works (regression)

---

# Phase 3 — Advanced Content ✅ COMPLETE

## Overview

Phase 3 adds content depth: note linking (the "second brain" core), attachments, basic markdown rendering, and the dashboard. This is where MindVault starts feeling like Obsidian-lite.

## Task 1 — Note Linking

**New files:**
```
data/local/entity/NoteLinkEntity.kt
data/local/dao/NoteLinkDao.kt
data/model/NoteLink.kt
data/repository/NoteLinkRepository.kt
ui/components/NoteLinkChip.kt
ui/components/NoteLinkPickerDialog.kt
```

**NoteLinkDao queries:**
```
suspend fun insertLink(link: NoteLinkEntity): Long
suspend fun deleteLink(sourceNoteId: Long, targetNoteId: Long)
fun getLinkedNotes(noteId: Long): Flow<List<NoteEntity>>  // both directions
fun getBacklinks(noteId: Long): Flow<List<NoteEntity>>     // notes that link TO this note
fun getLinkCount(noteId: Long): Flow<Int>
```

**How linking works:**
- In the editor, user taps a "Link" button (chain icon) → opens a note picker dialog
- Dialog shows searchable list of all other notes
- Selecting a note creates a bidirectional link (stored as a single row, queried both ways)
- Linked notes appear as chips below the note content
- Tapping a linked note chip navigates to that note

**UI changes:**
- `NoteEditorScreen`: "Linked Notes" section at the bottom, expandable. Shows linked note chips + "Add link" button.
- `NoteCard` (home screen): small link count badge if note has links (e.g., "🔗 3")

**Acceptance criteria:**
- User can link two notes together
- User can remove a link
- Linked notes are navigable (tap chip → go to that note)
- Backlinks work (note B shows note A in its links if A linked to B)

---

## Task 2 — Attachments

**New files:**
```
data/local/entity/AttachmentEntity.kt
data/local/dao/AttachmentDao.kt
data/model/Attachment.kt
data/repository/AttachmentRepository.kt
ui/components/AttachmentRow.kt
ui/components/AttachmentPicker.kt
```

**File storage strategy:**
- Attachments stored in app's internal storage: `context.filesDir/attachments/{noteId}/{filename}`
- Thumbnails generated for images and stored alongside
- AttachmentEntity stores the relative path, not absolute

**Supported types (Phase 3):**
- Images (JPEG, PNG, WebP) — show inline thumbnail
- PDFs — show icon + filename
- Generic files — show icon + filename + size

**UI changes:**
- `NoteEditorScreen`: attachment button in the toolbar (paperclip icon). Opens system file picker or camera.
- Attachments shown as a horizontal scrollable row below the content field (thumbnails for images, icons for others)
- Tap attachment → open with system intent (ACTION_VIEW)
- Long press → delete attachment (with confirmation)

**Acceptance criteria:**
- User can attach images and files to a note
- Attachments persist and display correctly
- Deleting a note deletes its attachments from storage
- Attachments are included in JSON export (as base64 or file references)

---

## Task 3 — Basic Markdown Rendering

**Scope — explicitly limited to:**
- Headings: `# H1`, `## H2`, `### H3`
- Bold: `**text**`
- Italic: `*text*`
- Bullet lists: `- item`
- Numbered lists: `1. item`
- Checklists: `- [ ] todo` / `- [x] done`
- Code: `` `inline` `` and ``` ```block``` ```
- Horizontal rules: `---`

**NOT in scope:** tables, images in markdown, LaTeX, wiki-links, embeds.

**Implementation approach:**
- Use a library for rendering. Recommended: `Markwon` (mature, supports Compose via `MarkwonView` in AndroidView) or build a simple custom parser for the limited subset above.
- The editor remains plain text — no WYSIWYG. Markdown renders in a "Preview" toggle.
- Add a preview/edit toggle button to the editor toolbar (eye icon)

**New files:**
```
ui/components/MarkdownRenderer.kt    # Compose wrapper for markdown rendering
```

**UI changes:**
- `NoteEditorScreen`: toggle button (edit ↔ preview). In preview mode, content renders as formatted markdown. In edit mode, raw text.
- Checklists are interactive in preview mode (tap to toggle)

**Acceptance criteria:**
- All listed markdown elements render correctly
- Preview toggle works smoothly
- Checklists are tappable in preview and update the note content
- Raw markdown is stored (not HTML) — keeps export clean

---

## Task 4 — Dashboard / Home Screen Upgrade

**What to do:**
- Transform the home screen from a flat note grid into a sectioned dashboard:
  - **Pinned notes** (top section, horizontal scroll or small grid)
  - **Recent notes** (last 5-10 edited, vertical list or grid)
  - **Favorites** (horizontal scroll)
  - **Categories** (grid of category cards with note count)
  - **Quick actions** row: New Note, New from Template (future), Search
- Add `isPinned` field to NoteEntity (migration v2 → v3)
- Pin/unpin from note card long-press menu and editor

**UI approach:**
- Use a `LazyColumn` with different item types for each section
- Each section has a header ("Pinned", "Recent", "Favorites") with a "See all" link
- Keep the existing flat grid view accessible via a toggle (grid icon in top bar) for users who prefer it

**Acceptance criteria:**
- Dashboard shows pinned, recent, and favorite sections
- User can pin/unpin notes
- "See all" navigates to filtered views
- Grid view toggle still works

---

## Phase 3 — Definition of Done

- [x] Note linking works bidirectionally
- [x] Backlinks display correctly
- [x] Attachments can be added, viewed, and deleted
- [x] Image attachments show thumbnails
- [x] Markdown preview renders all supported elements
- [x] Checklists are interactive in preview
- [x] Dashboard shows sectioned layout
- [x] Pin/unpin notes works
- [x] DB migration v2 → v3 tested
- [x] Export includes tags, categories, and links (attachments optional)

---

# Phase 4 — Smart Features ✅ COMPLETE

## Overview

Phase 4 adds the "intelligent" layer — all running locally with no AI/ML models, using text analysis algorithms. These features make MindVault proactively useful rather than passively storing data.

## Task 1 — Auto-Tag Suggestions

**How it works:**
- When the user finishes editing a note (on save), analyze the title + content
- Extract candidate tags using:
  - **Keyword frequency:** words that appear 3+ times (excluding stop words)
  - **Existing tag matching:** if the note contains text matching existing tags, suggest those
  - **Simple NLP patterns:** capitalize proper nouns, detect common topics (programming languages, subjects, etc.)
- Show suggestions as a non-intrusive chip row: "Suggested tags: #kotlin #database #android" with accept/dismiss per tag

**New files:**
```
domain/analysis/TagSuggestionEngine.kt
domain/analysis/StopWords.kt           # Portuguese + English stop word lists
domain/analysis/TextAnalyzer.kt        # Shared text analysis utilities
```

**UI changes:**
- `NoteEditorScreen`: after saving, if suggestions are available, show a collapsible suggestion bar at the top of the screen (brief, dismissible)

**Acceptance criteria:**
- Suggestions are relevant (not just common words)
- User can accept or dismiss each suggestion
- Accepted suggestions become real tags on the note
- Performance: analysis completes in < 200ms for a 5000-word note

---

## Task 2 — Related Notes

**How it works:**
- For each note, compute a similarity score against all other notes using:
  - **Tag overlap:** notes sharing 2+ tags are related
  - **Category match:** same category = related
  - **Content similarity:** TF-IDF-like scoring on FTS4 terms. Simple approach: extract top 10 keywords from current note → run FTS search → rank results by match count.
  - **Link proximity:** notes linked to the same note are related
- Show top 3-5 related notes at the bottom of the editor/viewer

**New files:**
```
domain/analysis/RelatedNotesEngine.kt
ui/components/RelatedNotesSection.kt
```

**UI changes:**
- `NoteEditorScreen` (or a new read-view if added): "Related Notes" section at the bottom, below linked notes. Shows cards for related notes with a brief reason ("3 shared tags", "Same category").

**Acceptance criteria:**
- Related notes are non-trivially relevant
- Computation happens in background (doesn't block UI)
- Results update when note content changes
- Empty state when no related notes found

---

## Task 3 — Smart Search Enhancements

**What to do:**
- **Fuzzy search:** implement Levenshtein distance for typo tolerance. When FTS returns no results, fall back to fuzzy matching on note titles.
- **Search filters:** add filter chips to search results: by date range, by category, by tag, favorites only, has attachments
- **Search scoring:** weight title matches higher than content matches
- **Synonyms (simple):** maintain a small local synonym map (e.g., "image" ↔ "photo" ↔ "picture", "task" ↔ "todo"). Expand search queries with synonyms automatically.

**New files:**
```
domain/analysis/FuzzyMatcher.kt
domain/analysis/SynonymMap.kt
```

**UI changes:**
- Search results show filter chips below the search bar
- Results show match context (snippet with highlighted terms)
- "Did you mean...?" suggestion when fuzzy match differs from query

**Acceptance criteria:**
- Typos in search still return results (e.g., "andorid" finds "android" notes)
- Filter chips narrow results correctly
- Synonym expansion works transparently

---

## Task 4 — Auto-Categorization Suggestions

**How it works:**
- When a note has no category and the user saves it, analyze content and suggest a category:
  - Match against existing category names and their typical note content
  - Use tag-to-category correlation (if most notes with tag #kotlin are in category "Programming", suggest that)
  - Simple keyword rules as fallback
- Non-intrusive: show as a suggestion toast/chip, never auto-assign

**New files:**
```
domain/analysis/CategorySuggestionEngine.kt
```

**Acceptance criteria:**
- Suggestions are shown only for uncategorized notes
- User can accept, dismiss, or ignore
- Suggestions improve as more notes are categorized (learns from existing data)

---

## Phase 4 — Definition of Done

- [x] Auto-tag suggestions appear and are relevant
- [x] Related notes section shows meaningful results
- [x] Fuzzy search handles typos
- [x] Search filters work (date, category, tag, favorites)
- [x] Category suggestions work for uncategorized notes
- [x] All smart features run locally, no network
- [x] Performance: no perceptible lag on 500+ notes
- [x] All existing features still work (regression)

---

# Phase 5 — Polish & Finalization

## Overview

Phase 5 is about quality, performance, and delight. No new features — just making everything work better.

## Task 1 — Performance Optimization

**What to do:**
- Profile the app with Android Studio Profiler (CPU, memory, DB)
- Optimize Room queries: ensure indexes exist on frequently filtered columns (isDeleted, isArchived, isFavorite, categoryId, updatedAt)
- Add `@Index` annotations where needed
- Lazy load attachment thumbnails
- Optimize recomposition: verify no unnecessary recompositions in note grid (use `key()` correctly in LazyColumn/LazyStaggeredGrid)
- Pagination: if note count > 100, implement paging with Paging 3 library for the home screen

**Acceptance criteria:**
- App cold start < 2 seconds on mid-range device
- Scrolling through 200+ notes is smooth (60fps)
- Memory usage stays under 150MB

---

## Task 2 — Animations & Micro-interactions

**What to do:**
- Refine screen transitions (shared element transitions for note card → editor if Compose supports it)
- Add subtle animations:
  - FAB scale animation on scroll (hide on scroll down, show on scroll up)
  - Note card entrance animation (stagger fade-in on first load)
  - Favorite star animation (scale pulse on toggle)
  - Delete swipe animation (card slides + fades, red background reveals)
  - Search bar expand/collapse (already exists, refine timing)
- Haptic feedback on key actions (delete, favorite, archive)

**Acceptance criteria:**
- Animations are smooth and not distracting
- Haptic feedback works on supported devices
- No animation jank even with many notes

---

## Task 3 — Testing Suite

**What to do:**
- **Unit tests** (JVM): all ViewModels, all Repository methods, all analysis engines (tag suggestion, related notes, fuzzy search). Use `FakeNoteRepository` pattern established in Phase 1.
- **Instrumented tests** (Android): all DAO methods (already done for NoteDao in Phase 1, extend to TagDao, CategoryDao, NoteLinkDao, AttachmentDao), all database migrations
- **UI tests** (Compose): key user flows — create note, edit note, delete note, search, add tag, navigate between screens. Use Compose Testing APIs.
- Target: 80%+ code coverage on data layer, 60%+ on ViewModels

**Acceptance criteria:**
- All tests pass
- CI-runnable test suite (can run with `./gradlew test` and `./gradlew connectedAndroidTest`)
- No flaky tests

---

## Task 4 — Accessibility & Edge Cases

**What to do:**
- Content descriptions on all icons and interactive elements
- Keyboard navigation support
- Screen reader testing (TalkBack)
- Handle edge cases:
  - Very long note titles (truncation)
  - Empty notes (prevent saving empty title + empty content)
  - Rapid tapping (debounce FAB, save button)
  - Orientation changes (state preserved)
  - Low storage (graceful handling when attachments can't be saved)
  - Database corruption recovery (Room's `.journal` handling)

**Acceptance criteria:**
- App is usable with TalkBack
- No crashes on edge cases
- State survives configuration changes

---

## Task 5 — Final UI Polish

**What to do:**
- Review all screens for visual consistency
- Ensure Material 3 dynamic color works across all components
- Dark mode review: check all custom colors have dark variants
- Typography pass: ensure hierarchy is clear (title > subtitle > body > caption)
- Empty states for all list screens (archive, trash, categories, search results)
- Loading states for all async operations
- Error states with retry options
- App icon design

**Acceptance criteria:**
- Consistent visual language across all screens
- Dark mode looks polished (not just "inverted colors")
- All states handled: loading, empty, error, content

---

## Phase 5 — Definition of Done (= Project Done) ✅

- [x] Database indexes added (isDeleted, isArchived, isFavorite, isPinned, updatedAt, composite) — DB v4
- [x] No unnecessary recompositions — `remember(note.updatedAt)` for time formatting
- [x] FAB hides on scroll (NestedScrollConnection, ±10px threshold)
- [x] Note cards animate in on first load (staggered fade, 40ms/item, max 400ms, rememberSaveable guard)
- [x] Favorite star has pulse animation (spring dampingRatio=0.3, 1.4x scale)
- [x] Swipe-to-delete shows red background (animateColorAsState on error color)
- [x] Haptic feedback on delete, archive, favorite, pin
- [x] FAB click debounced (500ms)
- [x] All ViewModel unit tests pass (64 tests)
- [x] All analysis engine tests pass (TextAnalyzer, FuzzyMatcher, SynonymMap, TagSuggestionEngine)
- [x] NoteEditorViewModelTest updated with full constructor + all fake repositories
- [x] DAO instrumented tests created (Tag, Category, NoteLink, Attachment)
- [x] All icons have content descriptions
- [x] Empty notes are not saved (existing check in saveNote())
- [x] Attachment storage errors handled gracefully (try-catch with file cleanup)
- [x] App has custom owl icon (adaptive icon in mipmap-anydpi-v26, PNG layers)
- [x] Brand colors added (brand_purple_dark, brand_purple, brand_amber, etc.)
- [x] Navigation drawer branded with owl logo + deep purple header
- [x] Ready for personal daily use

---

# Feature Matrix — What Ships When

| Feature | Phase 1 ✅ | Phase 2 ✅ | Phase 3 ✅ | Phase 4 ✅ | Phase 5 |
|---|---|---|---|---|---|
| Notes CRUD | ✅ | | | | |
| Soft delete + undo | ✅ | | | | |
| Favorites | ✅ | | | | |
| Archive | ✅ | | | | |
| Note colors | ✅ | | | | |
| Dark mode | ✅ | | | | |
| Basic search (LIKE) | ✅ | | | | |
| Staggered grid | ✅ | | | | |
| Tags | | ✅ | | | |
| Categories | | ✅ | | | |
| FTS search | | ✅ | | | |
| Backup/Export | | ✅ | | | |
| Archive/Trash screens | | ✅ | | | |
| Navigation drawer | | ✅ | | | |
| Note linking | | | ✅ | | |
| Attachments | | | ✅ | | |
| Markdown preview | | | ✅ | | |
| Dashboard home | | | ✅ | | |
| Pin/unpin notes | | | ✅ | | |
| Auto-tag suggestions | | | | ✅ | |
| Related notes | | | | ✅ | |
| Fuzzy search | | | | ✅ | |
| Auto-categorization | | | | ✅ | |
| Note linking | | | ✅ | | |
| Attachments | | | ✅ | | |
| Markdown preview | | | ✅ | | |
| Dashboard | | | ✅ | | |
| Auto-tag suggestions | | | | ✅ | |
| Related notes | | | | ✅ | |
| Fuzzy search | | | | ✅ | |
| Auto-categorization | | | | ✅ | |
| Performance optimization | | | | | ✅ |
| Animations polish | | | | | ✅ |
| Testing suite | | | | | ✅ |
| Accessibility | | | | | ✅ |

---

# Claude Code Workflow (Updated)

## Per-phase approach (recommended from Phase 2 onward)

1. Add the phase plan file to your project docs folder (e.g., `docs/PHASE2_PLAN.md`)
2. Open Claude Code in the project root
3. Prompt: *"Read docs/PHASE2_PLAN.md and IMPLEMENTATION_NOTES.md. Implement the full phase, following the task order. After each task, confirm what was done before moving to the next. Run the build after each task to catch errors early."*
4. Let Claude Code work through all tasks sequentially
5. Test on emulator when complete
6. Update `IMPLEMENTATION_NOTES.md` with any deviations
7. Git commit the entire phase

## If something breaks mid-phase

- Stop Claude Code
- Describe the issue in a new conversation, referencing the plan and implementation notes
- Fix the specific issue
- Resume from the next task

## Context management

- Keep `IMPLEMENTATION_NOTES.md` updated — it's your institutional memory
- The plan files + implementation notes are enough context for any new Claude conversation
- No architecture map needed at this project scale