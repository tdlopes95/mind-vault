# MindVault — Phase 1: Foundation
## Concrete Implementation Plan for Claude Code

---

## Overview

Phase 1 establishes the project skeleton: a working Android app where the user can create, view, edit, delete, and list notes. Everything else builds on top of this. By the end of Phase 1 you should have a clean, running app with solid architecture — even if it only does "notes CRUD," it should feel right.

---

## Tech Stack (locked in)

| Layer | Technology |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose + Material Design 3 |
| Architecture | MVVM (Model–View–ViewModel) |
| Database | Room (with FTS4 from the start) |
| DI | Hilt |
| Navigation | Navigation Compose |
| State | StateFlow + State hoisting |
| Async | Kotlin Coroutines + Flow |
| Min SDK | 26 (Android 8.0) |
| Target SDK | 34 |
| Build | Gradle (Kotlin DSL) |

---

## Project Structure

```
com.mindvault.app/
├── MindVaultApplication.kt          # @HiltAndroidApp
├── MainActivity.kt                  # Single Activity, hosts NavHost
│
├── data/
│   ├── local/
│   │   ├── MindVaultDatabase.kt     # Room Database class
│   │   ├── dao/
│   │   │   └── NoteDao.kt
│   │   ├── entity/
│   │   │   ├── NoteEntity.kt
│   │   │   └── NoteFts.kt           # FTS4 virtual table
│   │   └── converter/
│   │       └── DateConverter.kt
│   │
│   ├── repository/
│   │   └── NoteRepository.kt
│   │
│   └── model/
│       └── Note.kt                  # Domain model (clean, no Room annotations)
│
├── di/
│   ├── DatabaseModule.kt            # Provides Room DB + DAOs
│   └── RepositoryModule.kt          # Provides repositories
│
├── ui/
│   ├── theme/
│   │   ├── Theme.kt                 # Light/Dark + dynamic color
│   │   ├── Color.kt
│   │   └── Type.kt
│   │
│   ├── navigation/
│   │   ├── MindVaultNavHost.kt
│   │   └── Screen.kt                # Sealed class for routes
│   │
│   ├── screens/
│   │   ├── home/
│   │   │   ├── HomeScreen.kt
│   │   │   └── HomeViewModel.kt
│   │   │
│   │   ├── editor/
│   │   │   ├── NoteEditorScreen.kt
│   │   │   └── NoteEditorViewModel.kt
│   │   │
│   │   └── notedetail/
│   │       ├── NoteDetailScreen.kt
│   │       └── NoteDetailViewModel.kt
│   │
│   └── components/
│       ├── NoteCard.kt              # Reusable note card composable
│       ├── EmptyState.kt            # "No notes yet" placeholder
│       ├── SearchBar.kt             # Top search bar component
│       └── ConfirmDeleteDialog.kt   # Reusable delete confirmation
│
└── util/
    ├── DateUtils.kt                 # Formatting helpers
    └── Constants.kt
```

---

## Database Schema (Phase 1 only)

### NoteEntity

| Field | Type | Notes |
|---|---|---|
| id | Long (PK, auto-generate) | |
| title | String | Can be empty |
| content | String | Main note body |
| color | Int | Note card color (default 0 = no color) |
| isFavorite | Boolean | Default false |
| isArchived | Boolean | Default false |
| isDeleted | Boolean | Soft delete flag, default false |
| createdAt | Long | Epoch millis |
| updatedAt | Long | Epoch millis |
| deletedAt | Long? | Nullable — set when soft-deleted |

### NoteFts (FTS4 Virtual Table)

| Field | Type | Notes |
|---|---|---|
| title | String | Mirrors NoteEntity.title |
| content | String | Mirrors NoteEntity.content |

> FTS table is set up now so we never have to retrofit it. Phase 2 search will use it.

---

## Task Breakdown

### Task 1 — Project Setup & Configuration

**What to do:**
- Create a new Android project (Empty Compose Activity)
- Package name: `com.mindvault.app`
- Configure `build.gradle.kts` (app-level) with all dependencies:
  - Compose BOM (latest stable)
  - Material 3
  - Room (runtime, compiler, KTX)
  - Hilt (android, compiler, navigation-compose)
  - Navigation Compose
  - Coroutines
  - Lifecycle ViewModel Compose
- Configure `build.gradle.kts` (project-level) with plugins
- Set `minSdk = 26`, `targetSdk = 34`
- Create `MindVaultApplication.kt` with `@HiltAndroidApp`
- Add `@AndroidEntryPoint` to `MainActivity`
- Set up the theme (Theme.kt, Color.kt, Type.kt) with Material 3 dynamic color and dark mode support

**Acceptance criteria:**
- Project compiles and runs on emulator
- Dark/light theme toggles with system setting
- Hilt is initialized without errors

---

### Task 2 — Room Database & Data Layer

**What to do:**
- Create `NoteEntity.kt` with all fields from the schema above
- Create `DateConverter.kt` for Long ↔ Date conversion
- Create `NoteFts.kt` — FTS4 entity linked to NoteEntity
- Create `NoteDao.kt` with these queries:
  ```
  // Insert
  suspend fun insertNote(note: NoteEntity): Long
  
  // Update
  suspend fun updateNote(note: NoteEntity)
  
  // Soft delete (set isDeleted = true, deletedAt = now)
  suspend fun softDeleteNote(id: Long)
  
  // Permanent delete
  suspend fun permanentlyDeleteNote(id: Long)
  
  // Restore from trash
  suspend fun restoreNote(id: Long)
  
  // Get all active notes (not deleted, not archived), ordered by updatedAt DESC
  fun getActiveNotes(): Flow<List<NoteEntity>>
  
  // Get archived notes
  fun getArchivedNotes(): Flow<List<NoteEntity>>
  
  // Get deleted notes (trash)
  fun getDeletedNotes(): Flow<List<NoteEntity>>
  
  // Get favorite notes
  fun getFavoriteNotes(): Flow<List<NoteEntity>>
  
  // Get single note by id
  fun getNoteById(id: Long): Flow<NoteEntity?>
  
  // Toggle favorite
  suspend fun toggleFavorite(id: Long, isFavorite: Boolean)
  
  // Toggle archive
  suspend fun toggleArchive(id: Long, isArchived: Boolean)
  
  // Basic search (Phase 1 — simple LIKE, will upgrade to FTS in Phase 2)
  fun searchNotes(query: String): Flow<List<NoteEntity>>
  
  // Purge old deleted notes (deletedAt older than 30 days)
  suspend fun purgeOldDeletedNotes(cutoffTimestamp: Long)
  ```
- Create `MindVaultDatabase.kt` — Room database class (version 1, entities: NoteEntity, NoteFts)
- Create `Note.kt` — clean domain model (no Room annotations)
- Create `NoteRepository.kt` — wraps DAO, maps Entity ↔ Domain model
- Create `DatabaseModule.kt` and `RepositoryModule.kt` for Hilt

**Acceptance criteria:**
- Database creates successfully on first run
- All DAO queries compile
- Repository exposes Flow-based methods

---

### Task 3 — Navigation Structure

**What to do:**
- Create `Screen.kt` — sealed class/interface:
  ```
  Screen.Home
  Screen.NoteEditor(noteId: Long? = null)  // null = new note
  Screen.NoteDetail(noteId: Long)
  ```
- Create `MindVaultNavHost.kt` with NavHost and routes for all three screens
- Wire it into `MainActivity`

**Acceptance criteria:**
- Navigation between screens works
- Back button works correctly
- Note ID passes correctly through nav arguments

---

### Task 4 — Home Screen

**What to do:**
- Create `HomeViewModel.kt`:
  - Exposes `StateFlow<HomeUiState>` with:
    - `notes: List<Note>`
    - `isLoading: Boolean`
    - `searchQuery: String`
  - Methods: `onSearchQueryChanged()`, `deleteNote()`, `toggleFavorite()`
- Create `HomeScreen.kt`:
  - Top bar with app name ("MindVault") and search icon
  - Search bar (expandable from icon, or always visible — your call)
  - Notes displayed in a **staggered grid** (2 columns, like Google Keep)
  - Each note shown as a `NoteCard` component showing:
    - Title (bold, max 1 line)
    - Content preview (max 3-4 lines, faded overflow)
    - Relative date ("2 min ago", "Yesterday", etc.)
    - Favorite star icon
    - Card background color if set
  - Empty state when no notes exist (illustration + "Your vault is empty" message)
  - FAB (Floating Action Button) → navigates to NoteEditor (new note)
  - Swipe-to-delete on note cards (with undo snackbar)
  - Long press → shows options (delete, archive, favorite, change color)

**Acceptance criteria:**
- Notes load from Room and display in grid
- FAB opens editor for new note
- Tapping a card opens NoteDetail
- Swipe to delete works with undo
- Search filters notes in real time
- Empty state shows when no notes

---

### Task 5 — Note Editor Screen

**What to do:**
- Create `NoteEditorViewModel.kt`:
  - Loads existing note if `noteId != null`
  - Exposes `StateFlow<EditorUiState>` with title, content, color, isFavorite
  - Auto-saves on back navigation (if title or content is non-empty)
  - Methods: `onTitleChanged()`, `onContentChanged()`, `onColorSelected()`, `toggleFavorite()`, `saveNote()`
- Create `NoteEditorScreen.kt`:
  - Clean, distraction-free editor
  - Title field (large font, no border, placeholder "Title")
  - Content field (body text, expands to fill screen, placeholder "Start writing...")
  - Top bar with:
    - Back arrow (auto-saves)
    - Favorite toggle (star icon)
    - More menu (color picker, delete, archive)
  - Bottom bar or bottom sheet with:
    - Color palette selector (6-8 preset colors + "no color")
    - "Last edited: [relative time]" text
  - Auto-save behavior: saves when navigating away (onPause equivalent)

**Acceptance criteria:**
- Can create a new note with title + content
- Can edit an existing note
- Auto-saves on back navigation
- Color selection works and persists
- Favorite toggle works

---

### Task 6 — Note Detail Screen

**What to do:**
- Create `NoteDetailViewModel.kt`:
  - Loads note by ID
  - Methods: `deleteNote()`, `toggleFavorite()`, `toggleArchive()`
- Create `NoteDetailScreen.kt`:
  - Displays full note content (read-only view)
  - Title at top (large)
  - Content below
  - Metadata: "Created [date] · Edited [date]"
  - Top bar with: back, edit (pencil icon → navigates to NoteEditor), favorite, more menu (archive, delete, change color)
  - Note background color applied

**Decision point:** You might find that a separate detail screen is unnecessary and prefer to go directly to the editor on tap (like Google Keep does). This is a valid simplification for Phase 1 — the detail screen can be added later if you want a read-only view. If you skip it, Task 4's "tap card" action goes straight to NoteEditor.

**Acceptance criteria:**
- Note displays correctly with all fields
- Edit button navigates to editor
- Delete/archive/favorite work

---

### Task 7 — Reusable Components & Polish

**What to do:**
- `NoteCard.kt` — used in HomeScreen grid
- `EmptyState.kt` — reusable empty placeholder (icon + message + optional action button)
- `ConfirmDeleteDialog.kt` — "Are you sure?" dialog
- `SearchBar.kt` — animated expand/collapse search
- Color palette composable (row of colored circles, checkmark on selected)
- Date utility functions (`DateUtils.kt`):
  - `formatRelativeTime(timestamp: Long): String` → "Just now", "5 min ago", "Yesterday", "May 15"
  - `formatFullDate(timestamp: Long): String` → "May 29, 2026 at 14:30"
- Smooth transitions/animations between screens (shared element transitions if feasible, otherwise crossfade)

**Acceptance criteria:**
- Components render correctly in isolation
- Consistent styling across the app
- Animations feel smooth (no jank)

---

## Color Palette (suggested presets for note colors)

```
No color (default/transparent)
#FFAB91  — Soft coral
#FFCC80  — Warm peach
#FFF59D  — Light yellow
#C5E1A5  — Soft green
#80DEEA  — Light cyan
#CF93D8  — Soft purple
#F48FB1  — Soft pink
#90A4AE  — Cool gray
```

---

## What Phase 1 Does NOT Include

These are explicitly out of scope for Phase 1 (covered in later phases):
- Tags system
- Categories
- Note linking / relations
- Markdown rendering
- Attachments
- Dashboard with sections
- Knowledge graph
- Smart features (auto-tags, related notes)
- Backup/export (moved to Phase 2)
- Quick capture widget

---

## Recommended Execution Order

```
Task 1 → Task 2 → Task 3 → Task 7 (components) → Task 4 → Task 5 → Task 6
```

Reason: Build the foundation (setup, data, navigation) first, then create reusable components before the screens that use them.

---

## Claude Code Instructions

When handing each task to Claude Code, include:
1. This document as context
2. The specific task number
3. Any design decisions you've made
4. The current state of the codebase (if tasks are sequential)

Example prompt for Claude Code:
> "I'm building MindVault, an Android note-taking app. Here's the Phase 1 plan [attach this file]. Please implement Task 2 — Room Database & Data Layer. Follow the project structure and schema defined in the plan. Use Kotlin, Room, Hilt, and Coroutines."

---

## Definition of Done — Phase 1

Phase 1 is complete when:
- [ ] App launches without crashes
- [ ] User can create a new note via FAB
- [ ] User can edit an existing note
- [ ] User can delete a note (soft delete + undo)
- [ ] User can favorite/unfavorite a note
- [ ] User can archive a note
- [ ] Notes persist across app restarts
- [ ] Search filters notes by title/content
- [ ] Note colors work
- [ ] Dark mode works
- [ ] Empty state displays when no notes exist
- [ ] Smooth navigation between screens
- [ ] Code follows MVVM architecture
- [ ] Hilt DI is wired correctly throughout
