# MindVault App Icon — Claude Code Integration Guide

## Overview

This guide tells Claude Code how to integrate the MindVault owl logo into the Android app as an adaptive icon, splash screen element, and in-app branding.

**Pre-requisite:** You should have the final owl logo SVG from Claude Design before running this. If not, the instructions include a fallback to create a simple vector version.

---

## Task 1 — Adaptive Icon Setup

Android adaptive icons require two layers: a background and a foreground, both as vector drawables.

### Files to create/modify:

```
app/src/main/res/
├── drawable/
│   ├── ic_launcher_foreground.xml    # Owl logo (vector drawable)
│   └── ic_launcher_background.xml   # Solid purple background
├── mipmap-anydpi-v26/
│   ├── ic_launcher.xml              # Adaptive icon definition
│   └── ic_launcher_round.xml        # Same for round icons
└── values/
    └── colors.xml                   # Add brand colors if not present
```

### Background layer: `ic_launcher_background.xml`

Simple solid color fill:
```xml
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">
    <path
        android:fillColor="#26215C"
        android:pathData="M0,0h108v108H0z"/>
</vector>
```

The background color should be the darkest purple from the logo palette (`#26215C` or similar). This creates the icon's background on all launcher shapes (circle, squircle, rounded square, etc.).

### Foreground layer: `ic_launcher_foreground.xml`

This is where the owl logo goes. Convert the SVG from Claude Design into an Android `<vector>` drawable.

**Conversion rules:**
- SVG `<path d="...">` → Android `<path android:pathData="...">`
- SVG `fill="#COLOR"` → Android `android:fillColor="#COLOR"`
- SVG `stroke="#COLOR"` → Android `android:strokeColor="#COLOR"` + `android:strokeWidth="N"`
- SVG `stroke-linecap="round"` → Android `android:strokeLineCap="round"`
- SVG `stroke-linejoin="round"` → Android `android:strokeLineJoin="round"`
- SVG `<circle cx="X" cy="Y" r="R">` → Convert to `<path>` using arc commands, or use a helper
- The viewportWidth/Height should be 108 (adaptive icon standard)
- The safe zone is the inner 72dp (centered), so the logo should fit within x=18 to x=90, y=18 to y=90

**Important:** Adaptive icons have a 108dp canvas but only the inner 66dp (some launchers) to 72dp is guaranteed visible. Center the owl in the canvas and ensure it fits within this safe zone with some padding.

**SVG to Vector Drawable conversion approach:**
1. Take the SVG output from Claude Design
2. Scale/translate paths so the owl fits within approximately x=22, y=22, w=64, h=64 of a 108x108 viewport
3. Convert SVG elements to Android vector format
4. Test visibility by checking the icon in different launcher shapes

If the SVG is complex, use Android Studio's "Import Vector Asset" (File → New → Vector Asset → Local file) or convert manually.

### Adaptive icon definitions:

**`mipmap-anydpi-v26/ic_launcher.xml`:**
```xml
<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@drawable/ic_launcher_background"/>
    <foreground android:drawable="@drawable/ic_launcher_foreground"/>
</adaptive-icon>
```

**`mipmap-anydpi-v26/ic_launcher_round.xml`:**
```xml
<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@drawable/ic_launcher_background"/>
    <foreground android:drawable="@drawable/ic_launcher_foreground"/>
</adaptive-icon>
```

### Legacy fallback icons

For devices below API 26, generate PNG raster fallbacks. Place in:
- `mipmap-mdpi/ic_launcher.png` (48x48)
- `mipmap-hdpi/ic_launcher.png` (72x72)
- `mipmap-xhdpi/ic_launcher.png` (96x96)
- `mipmap-xxhdpi/ic_launcher.png` (144x144)
- `mipmap-xxxhdpi/ic_launcher.png` (192x192)

These can be generated from the vector drawable using Android Studio's Image Asset Studio, or by rendering the SVG at each size.

**If raster generation is not possible in Claude Code:** skip the PNGs. The adaptive icon (API 26+) covers minSdk 26, so legacy PNGs are technically unnecessary for this app.

---

## Task 2 — Brand Colors

Add to `res/values/colors.xml` (or create if it doesn't exist):

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <!-- MindVault brand colors -->
    <color name="brand_purple_dark">#26215C</color>
    <color name="brand_purple">#534AB7</color>
    <color name="brand_purple_light">#AFA9EC</color>
    <color name="brand_amber">#EF9F27</color>
    <color name="brand_amber_light">#FAEEDA</color>
    
    <!-- Existing colors should be preserved -->
</resources>
```

---

## Task 3 — In-App Branding (Optional)

### Navigation Drawer Header

If `AppDrawer.kt` has a header section, add the owl logo as a small icon next to the "MindVault" text:

```kotlin
// In AppDrawer composable header:
Row(verticalAlignment = Alignment.CenterVertically) {
    Image(
        painter = painterResource(id = R.drawable.ic_launcher_foreground),
        contentDescription = "MindVault",
        modifier = Modifier.size(40.dp)
    )
    Spacer(modifier = Modifier.width(12.dp))
    Text(
        "MindVault",
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold
    )
}
```

### Splash Screen (Optional, Android 12+)

If targeting a splash screen, configure in `res/values/themes.xml`:

```xml
<style name="Theme.MindVault.Splash" parent="Theme.SplashScreen">
    <item name="windowSplashScreenBackground">#26215C</item>
    <item name="windowSplashScreenAnimatedIcon">@drawable/ic_launcher_foreground</item>
    <item name="postSplashScreenTheme">@style/Theme.MindVault</item>
</style>
```

This shows the owl logo on a dark purple background during app launch.

---

## Task 4 — Update AndroidManifest.xml

Verify the manifest references the adaptive icon:

```xml
<application
    android:icon="@mipmap/ic_launcher"
    android:roundIcon="@mipmap/ic_launcher_round"
    ...>
```

This should already be set from the initial project creation, but verify it points to the adaptive icon, not a legacy PNG.

---

## Fallback: If No Claude Design SVG Is Available

If the user doesn't have a polished SVG from Claude Design, create a simple geometric owl icon programmatically:

- M shape: two thick stroked lines forming an M (stroke-width ~8, purple)
- V shape: two thick stroked lines forming a V inside the M, extending above (stroke-width ~6, amber)
- Two circles for eyes (white fill, purple stroke)
- Two smaller circles for pupils (solid purple)
- Two tiny circles for eye highlights (white)

Use Android `<vector>` path commands directly. The geometric version from the concept exploration in the chat can be used as reference.

---

## Verification

After integration, verify:
1. Icon appears correctly on the home screen (not the default Android robot)
2. Icon works in circle, squircle, and rounded square launcher shapes
3. Icon is readable at all sizes
4. Dark and light launcher backgrounds both look good
5. Navigation drawer shows the owl + "MindVault" header (if implemented)
