# MindVault — Final Polish & Quick Capture Widget
## Bug Fixes, Visual Refresh, and Differentiating Feature

---

## Overview

This plan addresses four areas:
1. **Bug fixes** — attachment visibility, dashboard layout overlap
2. **Tags UX overhaul** — replace the bottom sheet picker with inline tag input
3. **Visual refresh** — modernize the overall look with better shadows, colors, and card styling
4. **Quick Capture Widget** — Android home screen widget for instant note creation

**Execution approach:** Fix bugs first (Task 1), then visual refresh (Task 2), then tags UX (Task 3), then widget (Task 4). Run the build after each task.

---

## Pre-requisites

- Phase 5 is complete and the build is green
- Read `IMPLEMENTATION_NOTES.md` for full history
- Current DB version is 4 (no schema changes in this plan)

---

## Task 1 — Bug Fixes

### 1A — Attachment Visibility After Editing

**Symptom:** After attaching a file to a note, the attachment doesn't appear in the UI until a second attachment is added.

**Root cause (likely):** The attachments Flow in `NoteEditorViewModel` is either:
- Loaded once on init (not collected reactively), OR
- The `noteId` isn't set when attachments are first loaded (new note hasn't been saved to DB yet, so `noteId` is still -1/null), OR
- After `addAttachment()` completes, the Flow doesn't re-emit because the DAO query is bound to the original noteId

**Fix approach:**
1. In `NoteEditorViewModel`, verify that `attachments` is collected via `flatMapLatest` on the noteId state, not loaded once:
   ```kotlin
   // WRONG: loads once
   init { loadAttachments() }
   
   // RIGHT: reacts to noteId changes
   private val attachmentsFlow = _noteId
       .flatMapLatest { id ->
           if (id > 0) attachmentRepository.getAttachmentsForNote(id)
           else flowOf(emptyList())
       }
       .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
   ```
2. After `addAttachment()`, if the note was just created (first save), ensure the noteId is updated in the state so the Flow picks it up
3. Verify that `AttachmentDao.getAttachmentsForNote()` returns a `Flow` (reactive), not a `suspend` function (one-shot)
4. Test: attach an image → it should appear immediately in the attachment row without adding a second one

### 1B — Dashboard Title/Search Overlap

**Symptom:** On the dashboard, "MindVault" title and the search icon overlap or are visually cramped.

**Fix approach:**
In `HomeScreen.kt`, locate the `TopAppBar` or `CenterAlignedTopAppBar` definition. The issue is likely that the title and action icons are fighting for space. Options:

**Option A — Center the title, move search next to grid toggle:**
```kotlin
CenterAlignedTopAppBar(
    title = { Text("MindVault", style = MaterialTheme.typography.titleLarge) },
    navigationIcon = {
        IconButton(onClick = { /* open drawer */ }) {
            Icon(Icons.Default.Menu, contentDescription = "Menu")
        }
    },
    actions = {
        IconButton(onClick = { /* toggle search */ }) {
            Icon(Icons.Default.Search, contentDescription = "Search")
        }
        IconButton(onClick = { /* toggle grid/dashboard */ }) {
            Icon(/* grid or dashboard icon */, contentDescription = "Toggle view")
        }
    }
)
```

**Option B — Move search to a dedicated search bar below the top bar:**
The search bar expands from the top bar into a full search field. When collapsed, only the icon shows in the actions area. This is already partially implemented — verify it works correctly and doesn't overlap.

**Recommendation:** Option A with `CenterAlignedTopAppBar` is cleanest. It gives the title breathing room and groups the action icons on the right.

**Acceptance criteria:**
- Title is centered and clearly readable
- Search and grid toggle icons are properly spaced on the right
- No overlap at any screen width
- Menu/hamburger icon on the left opens the drawer

---

## Task 2 — Visual Refresh

This task modernizes the app's visual feel without changing functionality. Focus on cards, colors, spacing, and depth.

### 2A — Card Styling Overhaul

**Current problem:** Cards look flat and generic (basic Material surface with minimal elevation).

**Modern card styling in `NoteCard.kt`:**

```kotlin
Card(
    modifier = modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(16.dp)),  // More rounded corners (was probably 8-12dp)
    colors = CardDefaults.cardColors(
        containerColor = if (note.color != 0) {
            Color(note.color).copy(alpha = 0.15f)  // Subtle tint, not full saturation
        } else {
            MaterialTheme.colorScheme.surfaceContainerLow
        }
    ),
    elevation = CardDefaults.cardElevation(
        defaultElevation = 0.dp  // Flat cards with border look more modern than elevated
    ),
    border = BorderStroke(
        width = 1.dp,
        color = if (note.color != 0) {
            Color(note.color).copy(alpha = 0.3f)
        } else {
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        }
    )
) {
    // Card content
}
```

**Key changes:**
- **Rounder corners** (16dp instead of 8-12dp) — modern Material You feel
- **Subtle color tints** instead of fully saturated backgrounds — colored notes use `alpha = 0.15f` of the note color as a light wash, with a matching low-alpha border. This looks sophisticated rather than "painted."
- **Flat + bordered** instead of elevated — drop the default elevation shadow. Use a thin `outlineVariant` border for depth. This is the modern card trend (see Google Keep 2025+, Apple Notes, Notion).
- **In dark mode**, the same approach works: the tint is subtle against the dark surface, and the border provides definition.

### 2B — Note Color Palette Refresh

**Current problem:** The preset note colors feel generic (basic pastel Material colors).

Replace the color presets in `Constants.kt` with a more refined palette that works well as subtle tints:

```kotlin
val noteColors = listOf(
    0,                  // No color (default)
    0xFFE8B4B8.toInt(), // Dusty rose
    0xFFE6CBA8.toInt(), // Warm sand
    0xFFD4E09B.toInt(), // Soft sage
    0xFF9BC4CB.toInt(), // Muted teal
    0xFFA8C5E2.toInt(), // Soft sky
    0xFFB8A9C9.toInt(), // Lavender
    0xFFCBA0AA.toInt(), // Mauve
    0xFFD4C4A8.toInt(), // Parchment
)
```

**These colors are designed to:**
- Look sophisticated, not childish (muted tones, not neon)
- Work as subtle card tints at 15-20% alpha
- Look great in both light and dark modes
- Be clearly distinguishable from each other

**Update the color picker in `NoteOptionsBottomSheet`:**
- Larger color circles (32dp → 40dp)
- Selected state: checkmark overlay + slightly thicker border
- "No color" option: circle with a diagonal slash through it

### 2C — Typography & Spacing Polish

**Typography adjustments in NoteCard:**
```kotlin
// Title
Text(
    text = note.title,
    style = MaterialTheme.typography.titleSmall,  // Slightly smaller, cleaner
    fontWeight = FontWeight.SemiBold,             // Not Bold — softer
    maxLines = 2,                                  // Allow 2 lines for titles
    overflow = TextOverflow.Ellipsis
)

// Content preview
Text(
    text = note.content,
    style = MaterialTheme.typography.bodySmall,
    color = MaterialTheme.colorScheme.onSurfaceVariant,  // Slightly muted
    maxLines = 4,
    overflow = TextOverflow.Ellipsis,
    lineHeight = 18.sp  // Tighter line height for previews
)

// Metadata (date, tag count, link count)
Text(
    text = formattedDate,
    style = MaterialTheme.typography.labelSmall,
    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
)
```

**Spacing improvements:**
- Card internal padding: 14dp (consistent all sides)
- Between title and content: 6dp
- Between content and metadata: 10dp
- Grid spacing (between cards): 10dp horizontal, 10dp vertical (was probably 8dp)
- Dashboard section spacing: 24dp between sections (header to header)
- Dashboard section header to content: 12dp

### 2D — Dashboard Section Headers

**Modernize section headers (Pinned, Recent, Favorites, Categories):**

```kotlin
@Composable
fun SectionHeader(
    title: String,
    icon: ImageVector? = null,
    onSeeAll: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.width(8.dp))
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.weight(1f))
        if (onSeeAll != null) {
            TextButton(onClick = onSeeAll) {
                Text("See all", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}
```

**Key:** Clean, understated headers. No emoji in the headers (use Material icons instead). "See all" as a `TextButton`, not bold text.

### 2E — Bottom Bar / Editor Polish

**NoteEditorScreen bottom bar:**
- Add a subtle top border: `Modifier.drawBehind { drawLine(...) }` or `Divider`
- Space action icons evenly
- Use `FilledTonalIconButton` for the active mode (edit/preview) toggle instead of a plain `IconButton` — this provides visual feedback for the current mode

### 2F — Dark Mode Color Refinement

Review dark mode to ensure:
- Note card tints are dimmed appropriately (15% alpha works in dark mode but verify readability)
- The brand purple (`#26215C`) works as a drawer header background in dark mode
- Tags chips and category indicators have enough contrast
- The search bar background doesn't blend into the app bar
- Empty state illustrations/icons use `onSurfaceVariant` tint, not hardcoded colors

### Acceptance Criteria — Visual Refresh

- Cards have 16dp rounded corners and subtle borders (no hard elevation shadows)
- Note colors appear as soft tints, not full-saturation backgrounds
- Color palette feels cohesive and sophisticated
- Typography hierarchy is clear: titles are SemiBold, content is muted, metadata is subtle
- Dashboard sections have clean headers with Material icons
- Consistent spacing throughout (no cramped areas)
- Dark mode looks polished and intentional
- The overall feel is "modern minimal" not "Material 2 default"

---

## Task 3 — Tags UX Overhaul

### Current Problem

Adding tags requires: tap "+" → bottom sheet opens → search through list or type to create → select → close sheet. Too many steps for a frequent action.

### New Design: Inline Tag Input

Replace the `TagPickerBottomSheet` workflow with an inline tag input field directly in the editor:

```
[Tag1] [Tag2] [Tag3] [+ Add tag___________]
```

**How it works:**
1. Below the title field, show existing tags as chips in a `FlowRow`
2. At the end of the chips, show a compact `TextField` with placeholder "Add tag..."
3. As the user types, show autocomplete suggestions in a dropdown (matching existing tags)
4. Press Enter or tap a suggestion → tag is added (created if new)
5. Each tag chip has an "×" to remove
6. The text field stays inline — no bottom sheet, no dialog

**Implementation:**

```kotlin
@Composable
fun InlineTagInput(
    tags: List<Tag>,
    allTags: List<Tag>,
    onAddTag: (String) -> Unit,
    onRemoveTag: (Tag) -> Unit,
    modifier: Modifier = Modifier
) {
    var inputText by remember { mutableStateOf("") }
    var showSuggestions by remember { mutableStateOf(false) }
    
    val suggestions = remember(inputText, allTags, tags) {
        if (inputText.length >= 1) {
            allTags.filter { tag ->
                tag.name.contains(inputText, ignoreCase = true) &&
                tag !in tags  // Don't suggest already-added tags
            }.take(5)
        } else emptyList()
    }
    
    Column(modifier = modifier) {
        FlowRow(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Existing tag chips
            tags.forEach { tag ->
                InputChip(
                    selected = false,
                    onClick = { },
                    label = { Text("#${tag.name}", style = MaterialTheme.typography.labelMedium) },
                    trailingIcon = {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Remove ${tag.name}",
                            modifier = Modifier.size(14.dp).clickable { onRemoveTag(tag) }
                        )
                    },
                    modifier = Modifier.height(28.dp)
                )
            }
            
            // Inline text field for adding tags
            BasicTextField(
                value = inputText,
                onValueChange = { 
                    inputText = it
                    showSuggestions = it.isNotEmpty()
                },
                modifier = Modifier
                    .widthIn(min = 80.dp, max = 160.dp)
                    .height(28.dp),
                textStyle = MaterialTheme.typography.labelMedium.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = {
                        if (inputText.isNotBlank()) {
                            onAddTag(inputText.trim())
                            inputText = ""
                        }
                    }
                ),
                decorationBox = { inner ->
                    Box(contentAlignment = Alignment.CenterStart) {
                        if (inputText.isEmpty()) {
                            Text(
                                "Add tag...",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                        }
                        inner()
                    }
                }
            )
        }
        
        // Autocomplete dropdown
        if (showSuggestions && suggestions.isNotEmpty()) {
            Surface(
                modifier = Modifier.padding(horizontal = 16.dp),
                shape = RoundedCornerShape(8.dp),
                tonalElevation = 2.dp,
                shadowElevation = 4.dp
            ) {
                Column {
                    suggestions.forEach { tag ->
                        Text(
                            "#${tag.name}",
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onAddTag(tag.name)
                                    inputText = ""
                                    showSuggestions = false
                                }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}
```

**ViewModel changes:**
- Add `allTags: List<Tag>` to `EditorUiState` (fetch all tags on editor open for autocomplete)
- Modify `addTag(name: String)` to create the tag if it doesn't exist, then add to note
- Remove or deprecate the `TagPickerBottomSheet` (can keep it as a "browse all tags" option accessible via a long-press or overflow menu, but it shouldn't be the primary flow)

**Acceptance criteria:**
- User can type a tag name inline and press Enter to add it
- Autocomplete shows matching existing tags as user types
- Selecting a suggestion adds the tag immediately
- Creating a new tag (not in autocomplete) works
- Removing a tag via × works
- The flow feels fast — no dialogs, no bottom sheets for the common case
- Tags still persist correctly

---

## Task 4 — Quick Capture Widget

### Overview

An Android home screen widget that lets the user create a note in one tap, without opening the app. This is the single most impactful convenience feature — it removes the "open app → find FAB → tap → start typing" friction.

### Widget Design

**Size:** 2×1 cells (minimum), resizable to 4×1

**Layout (2×1):**
```
┌──────────────────────────────────┐
│  🦉  MindVault    [+ New Note]  │
└──────────────────────────────────┘
```

**Layout (4×1 — expanded):**
```
┌──────────────────────────────────────────────────────────────┐
│  🦉  MindVault   │  [✏️ New Note]  [📌 Quick Idea]  [🔍]   │
└──────────────────────────────────────────────────────────────┘
```

**Actions:**
- **New Note:** Launches the app directly into `NoteEditorScreen` with a new empty note
- **Quick Idea (expanded only):** Launches the app into the editor with the title pre-filled as "Quick Idea" and the note auto-pinned
- **Search (expanded only):** Launches the app into the home screen with search bar expanded

### Implementation

**Use Jetpack Glance** (Compose-based widget framework). It's the modern way to build widgets and aligns with the app's Compose-first approach.

**Dependencies to add:**
```kotlin
// libs.versions.toml
glance = "1.1.1"  // or latest stable

// app/build.gradle.kts
implementation("androidx.glance:glance-appwidget:$glance")
implementation("androidx.glance:glance-material3:$glance")
```

**New files to create:**
```
ui/widget/
├── MindVaultWidget.kt           # GlanceAppWidget implementation
├── MindVaultWidgetReceiver.kt   # GlanceAppWidgetReceiver
└── MindVaultWidgetContent.kt    # Widget UI composable
```

**MindVaultWidget.kt:**
```kotlin
class MindVaultWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            MindVaultWidgetContent()
        }
    }
    
    override val sizeMode = SizeMode.Responsive(
        setOf(
            DpSize(120.dp, 48.dp),   // 2×1 compact
            DpSize(250.dp, 48.dp),   // 4×1 expanded
        )
    )
}
```

**MindVaultWidgetContent.kt:**
```kotlin
@Composable
fun MindVaultWidgetContent() {
    val size = LocalSize.current
    val isExpanded = size.width > 200.dp
    
    GlanceTheme {
        Row(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(GlanceTheme.colors.surface)
                .cornerRadius(16.dp)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // App icon (small)
            Image(
                provider = ImageProvider(R.drawable.ic_launcher_foreground),
                contentDescription = "MindVault",
                modifier = GlanceModifier.size(28.dp)
            )
            
            Spacer(GlanceModifier.width(8.dp))
            
            Text(
                text = "MindVault",
                style = TextStyle(
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    color = GlanceTheme.colors.onSurface
                ),
                modifier = GlanceModifier.defaultWeight()
            )
            
            // New Note button (always visible)
            Button(
                text = "New Note",
                onClick = actionStartActivity<MainActivity>(
                    actionParametersOf(
                        ActionParameters.Key<String>("action") to "new_note"
                    )
                ),
                modifier = GlanceModifier.cornerRadius(12.dp),
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = GlanceTheme.colors.primary,
                    contentColor = GlanceTheme.colors.onPrimary
                )
            )
            
            if (isExpanded) {
                Spacer(GlanceModifier.width(8.dp))
                
                // Quick Idea button
                Button(
                    text = "Quick Idea",
                    onClick = actionStartActivity<MainActivity>(
                        actionParametersOf(
                            ActionParameters.Key<String>("action") to "quick_idea"
                        )
                    ),
                    modifier = GlanceModifier.cornerRadius(12.dp)
                )
                
                Spacer(GlanceModifier.width(8.dp))
                
                // Search button
                CircleIconButton(
                    imageProvider = ImageProvider(R.drawable.ic_search),
                    contentDescription = "Search",
                    onClick = actionStartActivity<MainActivity>(
                        actionParametersOf(
                            ActionParameters.Key<String>("action") to "search"
                        )
                    )
                )
            }
        }
    }
}
```

**MindVaultWidgetReceiver.kt:**
```kotlin
class MindVaultWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = MindVaultWidget()
}
```

### Intent Handling in MainActivity

Modify `MainActivity.kt` to handle widget actions:

```kotlin
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val widgetAction = intent?.getStringExtra("action")
        
        setContent {
            MindVaultTheme {
                MindVaultNavHost(
                    initialAction = widgetAction  // Pass to NavHost
                )
            }
        }
    }
}
```

In `MindVaultNavHost.kt`, handle the initial action:
```kotlin
@Composable
fun MindVaultNavHost(
    initialAction: String? = null,
    // ... existing params
) {
    LaunchedEffect(initialAction) {
        when (initialAction) {
            "new_note" -> navController.navigate(Screen.NoteEditor.route)
            "quick_idea" -> navController.navigate(
                Screen.NoteEditor.createRoute(noteId = -1L) + "?quickIdea=true"
            )
            "search" -> { /* Set search expanded state in HomeViewModel */ }
        }
    }
    
    // ... existing NavHost
}
```

For the "quick_idea" action, the `NoteEditorViewModel` should check for the flag and pre-fill:
- Title: "Quick Idea" (or timestamp-based: "Idea — May 31")
- isPinned: true
- Focus on the content field immediately

### AndroidManifest.xml — Widget Declaration

```xml
<receiver
    android:name=".ui.widget.MindVaultWidgetReceiver"
    android:exported="true">
    <intent-filter>
        <action android:name="android.appwidget.action.APPWIDGET_UPDATE" />
    </intent-filter>
    <meta-data
        android:name="android.appwidget.provider"
        android:resource="@xml/mindvault_widget_info" />
</receiver>
```

### Widget Info XML

**`res/xml/mindvault_widget_info.xml`:**
```xml
<?xml version="1.0" encoding="utf-8"?>
<appwidget-provider xmlns:android="http://schemas.android.com/apk/res/android"
    android:minWidth="120dp"
    android:minHeight="48dp"
    android:minResizeWidth="120dp"
    android:minResizeHeight="48dp"
    android:maxResizeWidth="320dp"
    android:maxResizeHeight="48dp"
    android:resizeMode="horizontal"
    android:targetCellWidth="2"
    android:targetCellHeight="1"
    android:widgetCategory="home_screen"
    android:initialLayout="@layout/widget_loading"
    android:previewLayout="@layout/widget_preview"
    android:description="@string/widget_description"
    android:updatePeriodMillis="0" />
```

**`res/layout/widget_loading.xml`:** (simple loading placeholder)
```xml
<?xml version="1.0" encoding="utf-8"?>
<FrameLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="@android:color/transparent">
    <TextView
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_gravity="center"
        android:text="MindVault"
        android:textSize="14sp"/>
</FrameLayout>
```

**String resources to add:**
```xml
<string name="widget_description">Quick access to create notes and ideas</string>
<string name="widget_name">MindVault Quick Capture</string>
```

### Acceptance Criteria — Quick Capture Widget

- Widget appears in the widget picker with a preview
- 2×1 size shows app name + "New Note" button
- 4×1 size also shows "Quick Idea" and search
- "New Note" opens the app directly in the editor with a blank note
- "Quick Idea" opens the editor with pre-filled title and auto-pinned
- "Search" opens the app with search expanded
- Widget respects system dark/light theme
- Widget has rounded corners and looks modern
- Widget works after phone restart (receiver handles boot)

---

## Definition of Done — Final Polish

- [ ] Attachment visibility bug fixed (attachments appear immediately after adding)
- [ ] Dashboard layout fixed (no title/search overlap)
- [ ] Cards have modern styling (16dp corners, subtle tints, thin borders)
- [ ] Note color palette refreshed (muted, sophisticated tones)
- [ ] Typography and spacing are consistent and polished
- [ ] Dark mode looks intentional and refined
- [ ] Tags input is inline with autocomplete (no bottom sheet for common flow)
- [ ] Quick Capture Widget works on home screen
- [ ] Widget supports 2×1 and 4×1 sizes
- [ ] All existing features still work (regression)