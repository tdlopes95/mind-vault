package com.mindvault.app.data.model

data class Note(
    val id: Long = 0,
    val title: String = "",
    val content: String = "",
    val color: Int = 0,
    val isFavorite: Boolean = false,
    val isArchived: Boolean = false,
    val isDeleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val deletedAt: Long? = null,
)
