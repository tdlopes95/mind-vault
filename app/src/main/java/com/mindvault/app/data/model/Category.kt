package com.mindvault.app.data.model

data class Category(
    val id: Long = 0,
    val name: String,
    val color: Int = 0,
    val icon: String? = null,
    val sortOrder: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
)
