package com.medipro.manager.core.common

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object DateRangeUtils {
    fun dayRange(dayOffset: Int = 0): Pair<Long, Long> {
        val cal = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, dayOffset)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val start = cal.timeInMillis
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        return start to cal.timeInMillis
    }

    fun rangeForDays(startOffset: Int, endOffset: Int): Pair<Long, Long> {
        val (start, _) = dayRange(startOffset)
        val (_, end) = dayRange(endOffset)
        return start to end
    }

    fun thisMonthRange(): Pair<Long, Long> {
        val cal = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val start = cal.timeInMillis
        val end = System.currentTimeMillis()
        return start to end
    }

    fun customRange(startMillis: Long, endMillis: Long): Pair<Long, Long> {
        val startCal = Calendar.getInstance().apply {
            timeInMillis = startMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val endCal = Calendar.getInstance().apply {
            timeInMillis = endMillis
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }
        return startCal.timeInMillis to endCal.timeInMillis
    }

    fun formatShortDate(epochMs: Long): String =
        SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(epochMs))
}

enum class ReportPeriod(val label: String) {
    TODAY("Today"),
    YESTERDAY("Yesterday"),
    LAST_7_DAYS("Last 7 Days"),
    THIS_MONTH("This Month"),
    CUSTOM("Custom"),
    ;

    fun toRange(customStart: Long? = null, customEnd: Long? = null): Pair<Long, Long> = when (this) {
        TODAY -> DateRangeUtils.dayRange(0)
        YESTERDAY -> DateRangeUtils.dayRange(-1)
        LAST_7_DAYS -> DateRangeUtils.rangeForDays(-6, 0)
        THIS_MONTH -> DateRangeUtils.thisMonthRange()
        CUSTOM -> {
            require(customStart != null && customEnd != null) { "Custom range requires start and end dates" }
            DateRangeUtils.customRange(customStart, customEnd)
        }
    }
}
