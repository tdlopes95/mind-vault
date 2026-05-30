package com.mindvault.app.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DateUtils {

    fun formatRelativeTime(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp
        return when {
            diff < 60_000 -> "Just now"
            diff < 3_600_000 -> "${diff / 60_000} min ago"
            diff < 7_200_000 -> "1 hour ago"
            diff < 86_400_000 -> "${diff / 3_600_000} hours ago"
            diff < 172_800_000 -> "Yesterday"
            diff < 604_800_000 -> "${diff / 86_400_000} days ago"
            else -> SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(timestamp))
        }
    }

    fun formatFullDate(timestamp: Long): String =
        SimpleDateFormat("MMM d, yyyy 'at' HH:mm", Locale.getDefault()).format(Date(timestamp))
}
