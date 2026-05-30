package com.mindvault.app.ui.navigation

sealed class Screen(val route: String) {

    object Home : Screen("home")

    // noteId = -1L means new note
    object NoteEditor : Screen("note_editor?noteId={noteId}") {
        fun createRoute(noteId: Long? = null): String =
            if (noteId != null) "note_editor?noteId=$noteId" else "note_editor"
    }

    object Archive : Screen("archive")
    object Trash : Screen("trash")
    object Categories : Screen("categories")
    object Settings : Screen("settings")
}
