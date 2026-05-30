# MindVault — Implementation Notes

Deviations from the plan, AGP/library compatibility issues, and decisions made during implementation.

---

## Task 1 — Project Setup & Configuration

### compileSdk / targetSdk bumped to 35

**Plan said:** targetSdk = 34
**Actual:** compileSdk = 35, targetSdk = 35

Navigation Compose 2.9.0 requires `compileSdk >= 35`. Since the plan's intent was "use a modern SDK," bumping both to 35 is the right call.

---

### KSP version scheme changed

**Plan assumed:** KSP follows the old `{kotlin-version}-1.0.x` pattern (e.g. `2.2.10-1.0.25`)
**Actual version used:** `2.2.10-2.0.2`

As of Kotlin 2.x, KSP adopted a new versioning scheme: `{kotlin-version}-{ksp-release}` where the KSP release is now `2.0.x` instead of `1.0.x`. Always verify the correct KSP version at https://repo1.maven.org/maven2/com/google/devtools/ksp/com.google.devtools.ksp.gradle.plugin/maven-metadata.xml.

---

### Hilt minimum version for AGP 9.x

**Plan said:** Hilt (any recent version)
**Actual version used:** 2.59.2

Hilt versions below ~2.57 fail with `"Android BaseExtension not found"` on AGP 9.x because the old Hilt Gradle plugin used a deprecated AGP extension API. Use 2.57+ (2.59.2 confirmed working).

---

### `kotlin-android` plugin must NOT be applied

**Plan assumed:** standard plugin stack (android-application + kotlin-android + kotlin-compose)
**Actual:** `kotlin-android` is omitted

In AGP 9.x, applying both `org.jetbrains.kotlin.android` and `org.jetbrains.kotlin.plugin.compose` causes:
```
Cannot add extension with name 'kotlin', as there is an extension already registered with that name.
```
AGP 9.x has built-in Kotlin support. The `kotlin-compose` plugin is sufficient alongside `com.android.application`. Hilt and Room via KSP work without `kotlin-android`.

---

### `android.disallowKotlinSourceSets=false` required in gradle.properties

AGP 9.x introduces a new "built-in Kotlin" mode that blocks KSP from registering generated sources via the `kotlin.sourceSets` DSL. Without this flag, the build fails with:
```
Using kotlin.sourceSets DSL to add Kotlin sources is not allowed with built-in Kotlin.
```
**Fix:** added to `gradle.properties`:
```properties
android.disallowKotlinSourceSets=false
```
This is marked experimental by Gradle but is the correct workaround until KSP fully supports AGP 9.x built-in Kotlin.

---

---

## Task 2 — Room Database & Data Layer

### RepositoryModule is technically redundant with @Singleton on NoteRepository

**Plan said:** create both `DatabaseModule.kt` and `RepositoryModule.kt`
**Actual:** `NoteRepository` uses `@Inject constructor` and `@Singleton`, so Hilt could wire it automatically. `RepositoryModule.kt` is kept to match the plan's explicit structure and to make the binding visible — useful once more repositories are added in later phases.

---

### softDeleteNote default parameter not usable from @Query

`softDeleteNote` takes a `now: Long = System.currentTimeMillis()` default, but Room generates the implementation — the default value is a Kotlin-side convenience for callers within the codebase, not a SQL default. Callers that don't pass `now` will get the correct timestamp at call time.

---

### Entity ↔ Domain mapping kept as private file-level extension functions

Mapping functions (`toDomain()`, `toEntity()`) live as private extensions at the bottom of `NoteRepository.kt` rather than as methods on the classes themselves. This keeps Room annotations out of the domain model and keeps the domain model free of any data-layer awareness — consistent with clean architecture intent.

---

---

## Task 2 — Testing (NoteDao instrumented tests)

### Test dependencies added

- `kotlinx-coroutines-test` (same version as `coroutines`) — for `runTest`
- `room-testing` (same version as `room`) — for `Room.inMemoryDatabaseBuilder`
- No Turbine; `Flow.first()` is sufficient since every test only needs the current emission

### DAO methods covered

All 13 DAO methods exercised across 13 tests: `insertNote`, `updateNote`, `softDeleteNote`, `permanentlyDeleteNote` (indirectly via purge), `restoreNote`, `getActiveNotes`, `getArchivedNotes`, `getDeletedNotes`, `getFavoriteNotes`, `getNoteById`, `toggleFavorite`, `toggleArchive`, `searchNotes`, `purgeOldDeletedNotes`.

### No bugs found in DAO queries

All tests passed on first run. No corrections to `NoteDao.kt` were needed.

### Gradle JDK environment fix required to compile androidTest

VS Code's bundled JRE (`redhat.java` extension) is missing `jlink`, which AGP 9.x needs for the `androidJdkImage` transform. Compilation of androidTest sources fails silently on Kotlin but hard-fails on the Java task. Fix: added `org.gradle.java.home=/usr/lib/jvm/java-25` to `gradle.properties` to force the system JDK.

---

---

## Task 4 — Home Screen

### Task 7 components built inline during Task 4

`NoteCard`, `EmptyState`, `ConfirmDeleteDialog`, `NoteOptionsBottomSheet`, `DateUtils`, and `Constants` (note color presets) were created as part of Task 4 rather than waiting for a separate Task 7 pass, since they are all directly required by `HomeScreen`. Task 7 (animations, `SearchBar` standalone component, polish) remains partially pending.

### Long-press → bottom sheet, not a context menu

The plan mentioned "long press → shows options" without specifying the UI pattern. A `ModalBottomSheet` was chosen: it's a first-class Material 3 pattern, gives space for the color palette row, and avoids the accessibility pitfalls of a floating context menu.

### Swipe-to-delete uses `SwipeToDismissBox` (Material 3 API)

`SwipeToDismissBox` is the current M3 API (the older `SwipeToDismiss` from M2 is deprecated). Background content is empty — the card simply slides away. The undo snackbar calls `repository.restoreNote`, so no data is lost before the snackbar times out.

### `material-icons-extended` added to dependencies

Star, Archive, Delete, NoteAlt, Close icons are from the extended icon set, which is not included in the base `material3` artifact. Added `androidx.compose.material:material-icons-extended` (BOM-versioned) to `libs.versions.toml` and `app/build.gradle.kts`.

### `collectAsState()` used instead of `collectAsStateWithLifecycle()`

`collectAsStateWithLifecycle` requires `lifecycle-runtime-compose`, which wasn't in the catalog. `collectAsState()` is sufficient here and avoids adding another dependency.

---

## Task 7 — Reusable Components & Polish

### All components delivered

| Component | File | Notes |
|---|---|---|
| `NoteCard` | `ui/components/NoteCard.kt` | Created in Task 4 |
| `EmptyState` | `ui/components/EmptyState.kt` | Created in Task 4 |
| `ConfirmDeleteDialog` | `ui/components/ConfirmDeleteDialog.kt` | Created in Task 4 |
| `CollapsibleSearchBar` | `ui/components/SearchBar.kt` | Extracted from HomeScreen in Task 7 |
| Color palette | `ui/components/NoteOptionsBottomSheet.kt` | Inline `ColorCircle` rows; color presets in `util/Constants.kt` |
| `DateUtils` | `util/DateUtils.kt` | `formatRelativeTime` + `formatFullDate` |

### Screen transition animations added to NavHost

`MindVaultNavHost` now uses `slideInHorizontally + fadeIn` / `slideOutHorizontally + fadeOut` for forward navigation, and the mirrored variants for back navigation (`popEnterTransition` / `popExitTransition`). Transitions are defined as file-level `val`s to avoid reallocation on recomposition. The exit slide uses `it / 3` offset (parallax feel) rather than full-width slide-out.

### Search bar extracted to `CollapsibleSearchBar.kt` during Task 7

The initial Task 4 implementation had search inline in `HomeScreen`. During Task 7 it was extracted to `ui/components/SearchBar.kt` as `CollapsibleSearchBar`, which `HomeScreen` now imports. The component handles open/close animation with `expandHorizontally` + `fadeIn`.

---

## Task 3 — Navigation Structure

### NoteDetail route omitted — NoteDetail screen skipped

`Screen.kt` only defines `Home` and `NoteEditor`. `NoteDetail` is not included because the NoteDetail screen is skipped (card tap goes straight to editor, per the plan's explicit simplification note).

### noteId sentinel: -1L means new note

`NavType.LongType` does not support true null as a nav argument. `-1L` is used as the sentinel for "new note." `MindVaultNavHost` maps `-1L` back to `null` before passing to `NoteEditorScreen`, keeping the screen API clean (`noteId: Long?`).

### Placeholder screens created for Task 3 acceptance criteria

`HomeScreen.kt` and `NoteEditorScreen.kt` contain minimal `Box + Text` placeholders so navigation between routes is testable before Tasks 4 and 5 are implemented.

---

### NoteDetail screen skipped

**Plan (Task 6):** separate read-only NoteDetailScreen
**Actual:** tapping a note card goes directly to NoteEditorScreen

The plan explicitly calls this out as a valid simplification ("like Google Keep does"). Task 6 is skipped; Task 4's card tap navigates straight to the editor.

---

## Task 5 — Note Editor Screen

### NoteRepositoryInterface introduced for ViewModel testability

**Plan assumed:** ViewModel could be tested by injecting the concrete `NoteRepository`
**Actual:** `NoteRepository` requires a Room `NoteDao` in its constructor, making it unusable in JVM unit tests without Android instrumentation. A `NoteRepositoryInterface` was extracted so that `NoteEditorViewModel` (and `HomeViewModel`, for consistency) accept the interface instead of the concrete class. `RepositoryModule` now provides `NoteRepositoryInterface`, with `NoteRepository` as its implementation. `FakeNoteRepository` in the test source set implements the same interface.

---

### auto-save is call-site triggered, not lifecycle-aware

**Plan said:** "auto-save on back navigation (onPause equivalent)"
**Actual:** There is no `onPause` hook in Compose NavHost composables. Auto-save is implemented by having the back arrow's `onClick` call `viewModel.saveNote()` before `onNavigateBack()`. The Android system back gesture (predictive back) is NOT wired to save — only the in-app back arrow triggers it. A lifecycle `DisposableEffect` or `BackHandler` could be added to cover the gesture, but that is deferred.

---

### Delete/archive from editor use `shouldNavigateBack` event in UiState

Rather than a `SharedFlow` or `Channel` for navigation events, `EditorUiState` includes a `shouldNavigateBack: Boolean` field. When `deleteNote()` or `archiveNote()` completes, the flag is set to `true` and the screen reacts via `LaunchedEffect`. This avoids the boilerplate of a separate event channel while remaining testable.

---

### Color palette re-implemented inline in NoteEditorScreen

`ColorCircle` in `NoteOptionsBottomSheet.kt` is a `private` composable. Rather than making it public or moving it to a shared file, a similar `ColorDot` composable (24 dp instead of 32 dp, to fit the bottom bar) is implemented privately in `NoteEditorScreen.kt`. The two are deliberately kept separate since their sizes and contexts differ.

---

### `coroutines-test` added to `testImplementation`

It was already in `androidTestImplementation` (for DAO tests). Added to `testImplementation` as well so the JVM ViewModel tests can use `runTest`, `StandardTestDispatcher`, and `advanceUntilIdle`.
