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
