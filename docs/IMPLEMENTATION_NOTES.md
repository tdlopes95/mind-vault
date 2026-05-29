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

### NoteDetail screen skipped

**Plan (Task 6):** separate read-only NoteDetailScreen
**Actual:** tapping a note card goes directly to NoteEditorScreen

The plan explicitly calls this out as a valid simplification ("like Google Keep does"). Task 6 is skipped; Task 4's card tap navigates straight to the editor.
