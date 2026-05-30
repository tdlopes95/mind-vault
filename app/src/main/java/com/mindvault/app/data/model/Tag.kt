package com.mindvault.app.data.model

data class Tag(
    val id: Long = 0,
    val name: String,
    val color: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
)
