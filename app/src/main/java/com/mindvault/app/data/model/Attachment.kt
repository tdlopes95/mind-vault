package com.mindvault.app.data.model

data class Attachment(
    val id: Long = 0,
    val noteId: Long,
    val fileName: String,
    val filePath: String,
    val mimeType: String,
    val fileSize: Long,
    val createdAt: Long = System.currentTimeMillis(),
) {
    val isImage: Boolean get() = mimeType.startsWith("image/")
    val isPdf: Boolean get() = mimeType == "application/pdf"
    val formattedSize: String get() = when {
        fileSize < 1024 -> "$fileSize B"
        fileSize < 1024 * 1024 -> "${fileSize / 1024} KB"
        else -> "${"%.1f".format(fileSize / (1024.0 * 1024.0))} MB"
    }
}
