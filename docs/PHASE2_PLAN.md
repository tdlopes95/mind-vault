# MindVault — Phase 2: Organization & Search
## Implementation Plan for Claude Code

---

## Overview

Phase 2 adds the organizational layer on top of Phase 1's notes CRUD: tags, categories, FTS search upgrade, backup/export, and proper navigation to Archive/Trash/Settings screens. This turns MindVault from "a notes app" into "a knowledge tool."

**Execution approach:** Implement all tasks sequentially in one session. Run the build after each task to catch errors early. Follow the task order strictly — later tasks depend on earlier ones.

---

## Pre-requisites

- Phase 1 is complete and all acceptance criteria met
- Read `IMPLEMENTATION_NOTES.md` for build environment constraints (AGP 9.x workarounds, Hilt 2.57+, KSP 2.x versioning, etc.)
- Current DB version is 1. This phase creates migration to version 2.

---

## Key Architecture Decisions (carried from Phase 1)

- `NoteRepositoryInterface` pattern: all new repositories must have an interface for testability
- `RepositoryModule.kt` binds interfaces to implementations
- Entity ↔ Domain mapping via private extension functions in the repository
- Navigation uses `-1L` as sentinel for "no ID" (NavType.LongType doesn't support null)
- `collectAsState()` is currently used — this phase upgrades to `collectAsStateWithLifecycle()`

---

## Database Migration: Version 1 → 2

### New tables

**TagEntity**
| Field | Type | Notes |
|---|---|---|
| id | Long (PK, auto-generate) | |
| name | String (unique) | Tag display name |
| color | Int | Default 0 (no color) |
| createdAt | Long | Epoch millis |

**NoteTagCrossRef**
| Field | Type | Notes |
|---|---|---|
| noteId | Long (FK → NoteEntity) | Composite PK |
| tagId | Long (FK → TagEntity) | Composite PK |

**CategoryEntity**
| Field | Type | Notes |
|---|---|---|
| id | Long (PK, auto-generate) | |
| name | String (unique) | Category name |
| color | Int | Default 0 |
| icon | String? | Material icon name, nullable |
| sortOrder | Int | For manual ordering |
| createdAt | Long | Epoch millis |

### Schema change to existing table

**NoteEntity** — add column:
- `categoryId: Long?` (nullable FK → CategoryEntity, default null)

### Migration SQL

```sql
CREATE TABLE IF NOT EXISTS `TagEntity` (
    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    `name` TEXT NOT NULL,
    `color` INTEGER NOT NULL DEFAULT 0,
    `createdAt` INTEGER NOT NULL
);
CREATE UNIQUE INDEX IF NOT EXISTS `index_TagEntity_name` ON `TagEntity` (`name`);

CREATE TABLE IF NOT EXISTS `NoteTagCrossRef` (
    `noteId` INTEGER NOT NULL,
    `tagId` INTEGER NOT NULL,
    PRIMARY KEY (`noteId`, `tagId`),
    FOREIGN KEY (`noteId`) REFERENCES `NoteEntity`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`tagId`) REFERENCES `TagEntity`(`id`) ON DELETE CASCADE
);
CREATE INDEX IF NOT EXISTS `index_NoteTagCrossRef_tagId` ON `NoteTagCrossRef` (`tagId`);

CREATE TABLE IF NOT EXISTS `CategoryEntity` (
    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    `name` TEXT NOT NULL,
    `color` INTEGER NOT NULL DEFAULT 0,
    `icon` TEXT,
    `sortOrder` INTEGER NOT NULL DEFAULT 0,
    `createdAt` INTEGER NOT NULL
);
CREATE UNIQUE INDEX IF NOT EXISTS `index_CategoryEntity_name` ON `CategoryEntity` (`name`);

ALTER TABLE `NoteEntity` ADD COLUMN `categoryId` INTEGER DEFAULT NULL
    REFERENCES `CategoryEntity`(`id`) ON DELETE SET NULL;
```

---

## Task Order

```
Task 1 (Phase 1 fixes) → Task 2 (Tags) → Task 3 (Categories) → Task 4 (FTS Search) → Task 5 (Backup/Export) → Task 6 (Navigation + Archive/Trash)
```

---

## Task 1 — Phase 1 Polish & Fixes

**What to do:**
- Add `BackHandler` composable in `NoteEditorScreen.kt` to trigger `viewModel.saveNote()` on system back gesture (predictive back). This ensures auto-save works regardless of whether the user taps the in-app back arrow or swipes back.
- Add `lifecycle-runtime-compose` to dependencies (`libs.versions.toml` + `app/build.gradle.kts`)
- Replace all `collectAsState()` calls with `collectAsStateWithLifecycle()` across all screens (HomeScreen, NoteEditorScreen)
- Add auto-purge of old deleted notes on app startup: in `MainActivity` or `MindVaultApplication`, launch a coroutine that calls `noteRepository.purgeOldDeletedNotes(cutoff)` where cutoff = `System.currentTimeMillis() - 30 * 24 * 60 * 60 * 1000L`
- Verify notes persist across app force-stop + reopen

**Acceptance criteria:**
- System back gesture saves the current note
- Lifecycle-aware state collection is used everywhere
- Notes older than 30 days in trash are purged on app launch

---

## Task 2 — Tags System

**New files to create:**
```
data/local/entity/TagEntity.kt
data/local/entity/NoteTagCrossRef.kt
data/local/dao/TagDao.kt
data/model/Tag.kt
data/repository/TagRepository.kt
data/repository/TagRepositoryInterface.kt
ui/components/TagPickerBottomSheet.kt
ui/components/TagChip.kt
```

**TagDao queries:**
```kotlin
@Insert(onConflict = OnConflictStrategy.IGNORE)
suspend fun insertTag(tag: TagEntity): Long

@Delete
suspend fun deleteTag(tag: TagEntity)

@Query("SELECT * FROM TagEntity ORDER BY name ASC")
fun getAllTags(): Flow<List<TagEntity>>

@Query("SELECT TagEntity.* FROM TagEntity INNER JOIN NoteTagCrossRef ON TagEntity.id = NoteTagCrossRef.tagId WHERE NoteTagCrossRef.noteId = :noteId")
fun getTagsForNote(noteId: Long): Flow<List<TagEntity>>

@Query("SELECT NoteEntity.* FROM NoteEntity INNER JOIN NoteTagCrossRef ON NoteEntity.id = NoteTagCrossRef.noteId WHERE NoteTagCrossRef.tagId = :tagId AND NoteEntity.isDeleted = 0")
fun getNotesForTag(tagId: Long): Flow<List<NoteEntity>>

@Insert(onConflict = OnConflictStrategy.IGNORE)
suspend fun addTagToNote(crossRef: NoteTagCrossRef)

@Query("DELETE FROM NoteTagCrossRef WHERE noteId = :noteId AND tagId = :tagId")
suspend fun removeTagFromNote(noteId: Long, tagId: Long)

@Query("SELECT * FROM TagEntity WHERE name LIKE '%' || :query || '%' ORDER BY name ASC")
fun searchTags(query: String): Flow<List<TagEntity>>
```

**Tag domain model:**
```kotlin
data class Tag(
    val id: Long = 0,
    val name: String,
    val color: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)
```

**UI changes:**

1. **NoteEditorScreen:** Add a horizontal row of tag chips below the title field. Include a "+" chip that opens `TagPickerBottomSheet`. Each tag chip has an "x" to remove it.

2. **TagPickerBottomSheet:** Shows all existing tags as selectable chips. Has a search/create field at the top — typing a name that doesn't exist shows a "Create #newname" option. Tags already on the current note are pre-selected (filled chips vs outlined).

3. **HomeScreen:** Add a horizontal scrollable chip row below the search bar showing all tags that have at least one note. Tapping a tag filters the note list. Tapping the same tag again deselects it. Selected tag chip is filled, others are outlined.

4. **Update Note domain model** to include `tags: List<Tag>` field. Update repository to fetch tags alongside notes where needed.

**DI changes:**
- Add `TagDao` provider to `DatabaseModule`
- Add `TagRepositoryInterface` binding to `RepositoryModule`

**Acceptance criteria:**
- User can create new tags
- User can add/remove tags on a note from the editor
- Tags display as chips on the note editor
- Home screen tag filter works
- Tags persist across app restart

---

## Task 3 — Categories System

**New files to create:**
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
```kotlin
@Insert(onConflict = OnConflictStrategy.IGNORE)
suspend fun insertCategory(category: CategoryEntity): Long

@Update
suspend fun updateCategory(category: CategoryEntity)

@Delete
suspend fun deleteCategory(category: CategoryEntity)

@Query("SELECT * FROM CategoryEntity ORDER BY sortOrder ASC, name ASC")
fun getAllCategories(): Flow<List<CategoryEntity>>

@Query("SELECT * FROM CategoryEntity WHERE id = :id")
fun getCategoryById(id: Long): Flow<CategoryEntity?>

@Query("SELECT * FROM NoteEntity WHERE categoryId = :categoryId AND isDeleted = 0 AND isArchived = 0")
fun getNotesInCategory(categoryId: Long): Flow<List<NoteEntity>>

@Query("SELECT * FROM NoteEntity WHERE categoryId IS NULL AND isDeleted = 0 AND isArchived = 0")
fun getUncategorizedNotes(): Flow<List<NoteEntity>>
```

**Category domain model:**
```kotlin
data class Category(
    val id: Long = 0,
    val name: String,
    val color: Int = 0,
    val icon: String? = null,
    val sortOrder: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)
```

**Schema change:** Add `categoryId: Long?` to `NoteEntity` and `Note` domain model. Update mappings in `NoteRepository`.

**UI changes:**

1. **NoteEditorScreen:** Add a category indicator below the tag chips. Shows current category name (or "No category"). Tapping opens `CategoryPickerDialog`.

2. **CategoryPickerDialog:** AlertDialog or BottomSheet showing all categories as a list. Each entry shows color dot + name. "None" option at top. "Create new category" at bottom.

3. **CategoriesScreen:** Management screen showing all categories as a reorderable list. Each item shows: color dot, icon, name, note count. Options: edit (name, color, icon), delete (with confirmation — does NOT delete notes, just sets their categoryId to null). FAB to create new category.

4. **HomeScreen:** Add category filter — either as a second chip row below tags, or a dropdown. When a category is selected, only notes in that category are shown. Combine with tag filter (AND logic: tag AND category).

5. **Navigation:** Add `Screen.Categories` route. Accessible from the navigation structure (will be wired into drawer in Task 6).

**DI changes:**
- Add `CategoryDao` provider to `DatabaseModule`
- Add `CategoryRepositoryInterface` binding to `RepositoryModule`

**Acceptance criteria:**
- User can create/edit/delete categories
- User can assign a note to a category from the editor
- Category filter works on home screen (combined with tag filter)
- Deleting a category sets its notes' categoryId to null (does NOT delete notes)
- Categories display with color and optional icon

---

## Task 4 — FTS Search Upgrade

**What to do:**

1. **Replace LIKE search with FTS4:** Update `NoteDao.searchNotes()` to query the `NoteFts` virtual table instead of using `LIKE`. The FTS table was created in Phase 1 and mirrors NoteEntity's title + content.

   **Important:** FTS4 content sync must be maintained. Ensure that when notes are inserted/updated/deleted, the FTS table is updated accordingly. Room should handle this via `contentSyncTriggers = true` if the FTS entity has `contentEntity = NoteEntity::class` — verify this is configured in `NoteFts.kt`.

2. **Combined search:** Search should also match tag names and category names. Approach: run FTS query on notes + separate LIKE queries on TagEntity.name and CategoryEntity.name, then merge results (notes matching FTS + notes that have matching tags + notes in matching categories). Deduplicate by note ID.

3. **Search results enhancement:** When displaying search results, show a snippet of the matching content with the search term context (e.g., "...keyword appears here in the note...").

4. **Recent searches:** Store last 10 search queries using DataStore Preferences (simpler than a Room table for this). Show them as chips when the search bar is focused but the query is empty. Tap to re-run. "Clear recent" option.

**New dependencies:**
- `androidx.datastore:datastore-preferences` — for recent searches storage

**New files:**
```
data/local/RecentSearchesDataStore.kt
```

**UI changes:**
- Search bar: when focused with empty query, show recent search chips below
- Search results: content snippet with match context
- "No results" state with suggestion text

**Acceptance criteria:**
- FTS search is noticeably faster than LIKE for 100+ notes
- Partial word matching works (prefix search via FTS4 `*` operator, e.g., "and*" matches "android")
- Tag and category names are searchable (searching "work" finds notes tagged #work)
- Recent searches persist and display correctly
- Clear recent searches works

---

## Task 5 — Backup & Export

**New files to create:**
```
data/backup/BackupManager.kt
data/backup/NoteExportModel.kt          # JSON serialization model
ui/screens/settings/SettingsScreen.kt
ui/screens/settings/SettingsViewModel.kt
```

**Dependencies:**
- `kotlinx-serialization-json` — for JSON export (add plugin + dependency)
- OR use `Gson` if already available / simpler to add

**Export model (JSON structure):**
```json
{
  "exportVersion": 1,
  "exportDate": "2026-05-30T14:30:00Z",
  "notes": [
    {
      "title": "My Note",
      "content": "Note content...",
      "color": 0,
      "isFavorite": false,
      "isArchived": false,
      "createdAt": 1717000000000,
      "updatedAt": 1717000000000,
      "tags": ["kotlin", "android"],
      "category": "Programming"
    }
  ],
  "tags": [
    { "name": "kotlin", "color": 0 },
    { "name": "android", "color": 0 }
  ],
  "categories": [
    { "name": "Programming", "color": 0, "icon": null }
  ]
}
```

**BackupManager methods:**
```kotlin
suspend fun exportToJson(): Uri      // Returns URI of exported file
suspend fun importFromJson(uri: Uri): ImportResult  // Returns success/failure + stats
suspend fun backupDatabase(): Uri    // Raw DB copy
suspend fun restoreDatabase(uri: Uri): Boolean
```

**Import logic:**
- Duplicate detection: match by title + createdAt timestamp. If a note with the same title AND createdAt exists, skip it.
- Tags and categories: create if they don't exist, reuse if they do (match by name)
- Return stats: "Imported X notes, Y tags, Z categories. Skipped N duplicates."

**File handling:**
- Use `Storage Access Framework` (SAF) with `ActivityResultContracts.CreateDocument` for export and `ActivityResultContracts.OpenDocument` for import
- Export location: user picks via system file picker
- File extension: `.json` for JSON export, `.db` for database backup

**Settings screen:**
- Add `Screen.Settings` route to navigation
- Screen layout:
  - Section "Data": Export notes (JSON), Import notes (JSON)
  - Section "Backup": Backup database, Restore database (with warning: "This will replace all current data")
  - Section "About": App version, "MindVault" branding

**UI changes:**
- HomeScreen top bar: add settings gear icon → navigates to Settings
- Settings: show progress indicator during export/import, result snackbar on completion

**Acceptance criteria:**
- JSON export produces a valid, human-readable file
- JSON import correctly imports notes with tags and categories
- Duplicate notes are detected and skipped
- Database backup/restore works (with user confirmation for restore)
- Settings screen is accessible and functional

---

## Task 6 — Navigation Drawer + Archive/Trash Screens

**New files to create:**
```
ui/screens/archive/ArchiveScreen.kt
ui/screens/archive/ArchiveViewModel.kt
ui/screens/trash/TrashScreen.kt
ui/screens/trash/TrashViewModel.kt
ui/components/AppDrawer.kt
```

**Navigation structure — Modal Navigation Drawer:**
- Drawer items:
  - 📝 All Notes (→ Home)
  - ⭐ Favorites (→ Home with favorites filter)
  - 📦 Archive (→ ArchiveScreen)
  - 🗑️ Trash (→ TrashScreen)
  - 📂 Categories (→ CategoriesScreen)
  - 🏷️ Tags (→ future, for now just shows tag count)
  - ⚙️ Settings (→ SettingsScreen)
- Drawer opens via hamburger icon in HomeScreen top bar (replace or add alongside app title)
- Current section highlighted in drawer

**New routes:**
- `Screen.Archive`
- `Screen.Trash`
- `Screen.Settings` (if not already added in Task 5)
- `Screen.Categories` (if not already added in Task 3)

**ArchiveScreen:**
- Shows all archived notes in a list/grid (reuse `NoteCard`)
- Each card has: "Unarchive" action (long press or swipe), "Delete permanently" action
- Top bar: "Archive" title + back arrow
- Empty state: "No archived notes"

**TrashScreen:**
- Shows all soft-deleted notes in a list/grid
- Each card shows: title, deletion date, "Restore" and "Delete permanently" actions
- Top bar: "Trash" title + "Empty Trash" button (with confirmation dialog)
- Empty state: "Trash is empty"
- Info text: "Notes in trash are automatically deleted after 30 days"

**HomeScreen changes:**
- Replace or augment the top bar to include a hamburger/menu icon for the drawer
- "Favorites" filter: when selected from drawer, home screen shows only favorite notes

**Acceptance criteria:**
- Navigation drawer opens and closes smoothly
- All drawer items navigate to correct screens
- Archive screen shows archived notes with restore/delete options
- Trash screen shows deleted notes with restore/delete/empty options
- "Empty Trash" permanently deletes all trashed notes (with confirmation)
- Drawer highlights current section
- Back navigation from drawer screens works correctly

---

## Phase 2 — Definition of Done

- [ ] System back gesture saves notes (BackHandler)
- [ ] collectAsStateWithLifecycle used everywhere
- [ ] Auto-purge of 30-day-old trash on app launch
- [ ] Tags: create, assign, remove, filter by tag
- [ ] Categories: create, edit, delete, assign, filter
- [ ] FTS4 search working with prefix matching
- [ ] Tag and category names searchable
- [ ] Recent searches persist
- [ ] JSON export works
- [ ] JSON import works with duplicate detection
- [ ] Database backup/restore works
- [ ] Navigation drawer functional
- [ ] Archive screen functional
- [ ] Trash screen functional with "Empty Trash"
- [ ] Settings screen accessible
- [ ] DB migration v1 → v2 works without data loss
- [ ] All Phase 1 functionality still works (regression check)
