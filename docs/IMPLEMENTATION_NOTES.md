# MindVault — Implementation Notes

## Build Environment (from Phase 1)

- AGP 9.x: do NOT add `kotlin-android` plugin; use `kotlin-compose` only
- KSP 2.2.10-2.0.2 paired with Kotlin 2.2.10
- Hilt 2.59.2
- Room 2.7.1
- `disallowKotlinSourceSets = false` warning is expected (experimental setting)
- `BuildConfig` requires explicit `buildFeatures { buildConfig = true }` — use hardcoded version string instead to avoid boilerplate
- `@ApplicationContext` annotation produces a warning about future target change (KT-73255); harmless in Kotlin 2.2.10

## Phase 2 Deviations

### Task 1 — Phase 1 Polish

- `lifecycle-runtime-compose` added at version `2.9.0` using the same version ref as `lifecycleViewModelCompose`
- Auto-purge injected into `MindVaultApplication` via `@Inject lateinit var noteRepository`; coroutine launched with a detached `CoroutineScope(SupervisorJob() + Dispatchers.IO)` (no lifecycle scope exists at Application level)
- `BackHandler` placed at the TOP of `NoteEditorScreen` body (before Scaffold) so it intercepts back before any inner handlers

### Task 2 — Tags System

- **Full DB migration (v1→v2) done in Task 2**, including CategoryEntity table and the `categoryId` column on NoteEntity. This avoids a second migration step in Task 3.
- `CategoryEntity.kt`, `CategoryDao.kt`, `CategoryRepository.kt`, `CategoryRepositoryInterface.kt` all created during Task 2 (needed to satisfy `NoteEditorViewModel` DI and the DB `@Database` entities list). Task 3 adds the UI and wires it up.
- `Note` domain model gains `tags: List<Tag> = emptyList()` and `categoryId: Long? = null`
- `NoteRepository` does NOT eagerly load tags — tags are loaded separately via `TagRepository` in each ViewModel that needs them
- `TagPickerBottomSheet`: `rememberModalBottomSheetState()` has no `skipPartialExpansion` parameter in this BOM version (2026.02.01); use default constructor
- Tag filter in `HomeViewModel` uses `tagRepository.getActiveNoteIdsForTag(tagId)` returning `Flow<Set<Long>>` — filtered against the base notes flow. This avoids loading tags on every note in the list

### Task 3 — Categories System

- `CategoryPickerDialog` implemented as an `AlertDialog` (not BottomSheet)
- CategoriesScreen drag-to-reorder deferred — no built-in Compose drag-reorder; noted as future work
- `CategoriesScreen` uses the same `EmptyState` composable but does NOT chain `.fillMaxSize()` in the modifier since `EmptyState` already calls it internally

### Task 4 — FTS Search Upgrade

- `NoteFts` already configured with `contentEntity = NoteEntity::class` — Room creates content-sync triggers automatically; no manual trigger SQL needed
- FTS prefix search: `*` suffix appended in `NoteRepository.searchNotesFts()` — `dao.searchNotesFts("$query*")`
- `HomeViewModel` uses `searchQuery.debounce(200)` before the FTS flatMapLatest to avoid hammering the DB on every keystroke
- Combined search (FTS + tags) is deferred client-side: FTS query returns a set of notes, then `tagNoteIdsFlow` filters by tag ID set. No separate SQL merge needed.
- Recent searches stored as a `Set<String>` in DataStore Preferences (insertion-order not guaranteed by Set; we maintain order by converting to List and using `takeLast(10).reversed()`)
- `HomeViewModel` uses nested `combine` to compose 6 flows (4 → FilterState, then 3) instead of a vararg array combine

### Task 6 — Navigation Drawer

### Task 5 — Backup & Export

- Used `kotlinx-serialization-json 1.8.1` + `kotlin.plugin.serialization` plugin
- `kotlin-serialization` plugin added to `libs.versions.toml` plugins section and `app/build.gradle.kts`
- SAF launchers (`ActivityResultContracts.CreateDocument` / `OpenDocument`) registered in `SettingsScreen` via `rememberLauncherForActivityResult`
- Raw DB backup not implemented (deferred) — only JSON export/import
- `SettingsViewModel` uses `BackupManager` which is `@Singleton` with `@ApplicationContext` — Hilt handles this automatically
- `datastore-preferences 1.1.4` added for recent searches

### Task 6 — Navigation Drawer

- `ModalNavigationDrawer` wraps the entire `NavHost` inside `MindVaultNavHost`
- `HomeViewModel` is created at the `MindVaultNavHost` level using `hiltViewModel()` so the drawer "Favorites" action can call `homeViewModel.setActiveFilter(HomeFilter.Favorites)` and the HomeScreen sees the same ViewModel instance
- All new routes (Archive, Trash, Categories, Settings) share the same enter/exit transitions as NoteEditor
- `AppDrawer` receives `drawerState` but only uses it indirectly (caller passes `onNavigate` callback that closes the drawer)
- "Tags" drawer item from the plan omitted — replaced by the tag filter row on HomeScreen which serves the same purpose

## Phase 3 Deviations

### DB Migration

- `NoteEntity` uses table name `notes` (not `NoteEntity`) — all migration SQL and DAO queries corrected to reference `notes`
- All Phase 3 DB changes (NoteLinkEntity, AttachmentEntity, isPinned) done in a single MIGRATION_2_3
- Coil 2.7.0 added for image thumbnails in attachments

### Task 1 — Note Linking

- `AttachmentSection`, `AttachmentRow`, `AttachmentThumbnail`, and `MarkdownRenderer` components created in the same pass as the editor screen (all four referenced in `NoteEditorScreen`)
- Preview/edit toggle and pin option in editor also implemented during Task 1 editor pass for consistency
- `availableForLinking` in EditorUiState populated on-demand via `loadAvailableForLinking()` (called when picker opens), not eagerly
- Navigation to linked note goes through `navigateToNoteId: Long?` in EditorUiState, cleared after navigation

### Task 2 — Attachments

- `AttachmentRepository` uses `@ApplicationContext` for file I/O — no Context passed to ViewModel
- `TrashViewModel` injects `AttachmentRepositoryInterface` to delete attachment files before permanent note deletion
- `AttachmentRepository` provides `deleteAllAttachmentsForNote()` which deletes both the `attachments/{noteId}/` directory and the DB rows
- FileProvider authority: `com.mindvault.app.fileprovider`

### Task 3 — Markdown Renderer

- Option A (custom Compose parser) implemented — no external Markwon dependency
- Inline parsing handles `**bold**`, `*italic*`, and `` `code` `` via `parseInline()` function using `buildAnnotatedString`
- `MarkdownRenderer` receives `onContentChange` callback for checkbox toggles in preview mode
- Code blocks: tracked with a `while` loop consuming lines until closing ` ``` ` (not Regex-based)
- Bottom bar hidden in preview mode (color selection and link/attach buttons not needed)

### Task 4 — Dashboard

- `HomeFilter` enum gains `Pinned` variant; handled in `baseNotesFlow` flatMapLatest
- `DashboardPreferencesDataStore` uses `dashboardDataStore` extension (different name from `dataStore` used in `RecentSearchesDataStore` to avoid collision)
- `HomeViewModel` uses nested 3-way combine: `filteredNotesFlow` × `(query, FilterState)` × `DashboardState`
- `DashboardState` computed via 5-flow combine (`getPinnedNotes`, `getFavoriteNotes`, `getAllCategories`, `isDashboardMode`, `getActiveNotes`) — category counts derived in-memory from active notes
- `recentNotes` = first 5 items of `getActiveNotes()` (already sorted DESC by updatedAt)
- Dashboard shows in-line LazyRow for Pinned/Favorites sections (horizontal scroll inside vertical LazyColumn — different axes, no conflict)
- "See all" for Pinned/Favorites/Recent switches to grid mode + sets appropriate filter
- Categories section rendered as 2-per-row using `chunked(2)` to avoid nested LazyVerticalGrid inside LazyColumn

### BackupManager

- Export version bumped to 2 — `ignoreUnknownKeys = true` ensures v1 exports still import cleanly
- Links exported as title pairs (not IDs) for portability across devices
- Attachment file data not exported (metadata only) — documented as future work
- On import, links recreated by matching note titles against both newly imported and pre-existing notes

## Phase 4 Deviations

### Package structure

- All analysis engines live in `domain/analysis/` under the app package as planned.
- `CategorySuggestion` data class defined in `NoteEditorViewModel.kt` (same file as `EditorUiState`) rather than in `CategorySuggestionEngine.kt` to avoid circular import — `CategorySuggestionEngine` imports `CategorySuggestion` from the VM package.

### Task 1 — Auto-Tag Suggestions

- `SuggestionChip` composable does not have an `action` parameter in this BOM version; replaced with an `AssistChip` (for accept) + `IconButton` (for dismiss) pair in a `Row` inside a `FlowRow`.
- Tag suggestions appear after `saveNote()` completes — triggered inside `generateAnalysisSuggestions()` called at the end of the save coroutine.
- Both tag suggestion and category suggestion run as `async { }` inside the same `launch` block (parallel per-spec).

### Task 2 — Related Notes

- `RelatedNote` data class defined in `RelatedNotesEngine.kt` and imported into the ViewModel.
- Related notes section uses `navigateToLinkedNote()` for navigation (reuses the existing navigation mechanism from Task 1 note linking).
- Related notes expanded state stored in `EditorUiState` (not local composable state) so it survives recomposition.

### Task 3 — Smart Search

- Synonym expansion and fuzzy fallback implemented as a post-filter in `filteredNotesFlow` (a `combine` of `tagFilteredNotesFlow`, `getActiveNotes()`, query, and filters) rather than as separate FTS queries. This avoids multiple concurrent FTS calls and integrates cleanly with the existing flow composition.
- `baseNotesFlow` performs FTS only; when FTS is empty, `filteredNotesFlow` applies synonym/fuzzy matching against `allActive` in-memory.
- `hasAttachments` and `hasLinks` filter fields defined in `SearchFilters` but UI only exposes date, category, and favorites chips (attachment/link filter omitted from UI — data for those requires joining with attachment/link tables not readily available on the `Note` domain model).
- Search filter row uses `FilterChip` (Material 3) — horizontally scrollable via `LazyRow`.

### Task 4 — Auto-Categorization

- `CategorySuggestionEngine` uses `getActiveNoteIdsForTag()` from `TagRepositoryInterface` (already available) to implement tag-to-category correlation.
- Category profile for content similarity built by concatenating title+content of up to 100 notes per category to avoid OOM on large datasets.

## Phase 5 Deviations

### DB Version

- DB bumped to version 4 for the new indexes on `notes` table (MIGRATION_3_4). Adds: `isDeleted`, `isArchived`, `isFavorite`, `isPinned`, `updatedAt`, and composite `(isDeleted, isArchived)` indexes.

### App Icon

- Adaptive icon XMLs placed in both `mipmap-anydpi/` (pre-existing, now references `@mipmap/`) and new `mipmap-anydpi-v26/` (references `@drawable/`).
- PNG icon layers placed in `drawable/` (for `@drawable/` reference in adaptive icon) and density-specific `mipmap-*dpi/` folders (legacy fallbacks).
- `AppDrawer` header upgraded with `Image(painterResource(R.drawable.ic_launcher_foreground))` + branded `Color(0xFF26215C)` background.

### Animations

- `NoteGrid` entrance animation: `Animatable(0f)` per item with `LaunchedEffect(note.id)`; stagger capped at 400ms max. `rememberSaveable { hasAnimated }` prevents re-animation on revisit.
- FAB scroll visibility: `NestedScrollConnection` at the content `Box` level — threshold ±10px to prevent flickering on tiny scrolls. Applied in addition to TopAppBar's own nestedScroll.
- Swipe-to-delete background: `animateColorAsState` on `dismissState.targetValue != Settled` → `colorScheme.error`.
- Star pulse: `Animatable(1f)` with spring dampingRatio=0.3 → 1.4x → back to 1f.

### Testing

- `NoteEditorViewModelTest` updated to use full 9-arg constructor with all fake repositories (TagSuggestionEngine + RelatedNotesEngine + CategorySuggestionEngine constructed with fake repos, not mocked).
- `FakeNoteRepository` extracted to `test/data/repository/FakeNoteRepository.kt` (shared); original inlined copy removed from test file.
- `rememberSwipeToDismissBoxState(confirmValueChange = ...)` is deprecated in newer Compose but still functional; no replacement available that preserves this swipe-to-delete flow.
