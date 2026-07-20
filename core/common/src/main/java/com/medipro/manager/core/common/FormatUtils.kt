package com.medipro.manager.core.common

import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object FormatUtils {
    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale("ne", "NP"))
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val dateTimeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

    fun formatCurrency(amount: Double): String = currencyFormat.format(amount)

    fun formatCount(count: Int): String = NumberFormat.getNumberInstance(Locale("ne", "NP")).format(count)

    fun formatDate(timestamp: Long): String = dateFormat.format(Date(timestamp))

    fun formatDateTime(timestamp: Long): String = dateTimeFormat.format(Date(timestamp))

    fun formatRelativeTime(timestamp: Long, now: Long = System.currentTimeMillis()): String {
        val diffMs = (now - timestamp).coerceAtLeast(0)
        val minutes = diffMs / 60_000
        val hours = diffMs / 3_600_000
        val days = diffMs / 86_400_000
        return when {
            diffMs < 60_000 -> "Just now"
            minutes < 60 -> "$minutes minute${if (minutes == 1L) "" else "s"} ago"
            hours < 24 -> "$hours hour${if (hours == 1L) "" else "s"} ago"
            days < 7 -> "$days day${if (days == 1L) "" else "s"} ago"
            else -> formatDateTime(timestamp)
        }
    }
}
