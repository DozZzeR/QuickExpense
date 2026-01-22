package dev.keslorod.quickexpense.domain

import android.content.Context
import dev.keslorod.quickexpense.R
import java.util.Calendar

enum class Period { NONE, DAY, WEEK, MONTH, ALL }

fun Period.toLabelRes(): Int = when (this) {
    Period.ALL    -> R.string.period_all_time
    Period.MONTH  -> R.string.period_month
    Period.WEEK   -> R.string.period_week
    Period.DAY    -> R.string.period_day
    Period.NONE   -> R.string.period_all_time
}

fun Period.getLabel(context: Context): String = context.getString(toLabelRes())

fun Period.toWidgetSubtitleRes(): Int = when (this) {
    Period.ALL    -> R.string.widget_subtitle_all_time
    Period.MONTH  -> R.string.widget_subtitle_month
    Period.WEEK   -> R.string.widget_subtitle_week
    Period.DAY    -> R.string.widget_subtitle_day
    Period.NONE   -> R.string.widget_subtitle_all_time
}

fun Period.getWidgetSubtitle(context: Context): String = context.getString(toWidgetSubtitleRes())

data class Range(val from: Long, val to: Long)

fun periodRange(period: Period, monthAnchorDay: Int = 1): Range {
    val now = System.currentTimeMillis()
    if (period == Period.ALL) return Range(0L, now)
    if (period == Period.NONE) return Range(now, now)

    val cal = Calendar.getInstance()
    val end = cal.timeInMillis

    when (period) {
        Period.DAY -> {
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
        }
        Period.WEEK -> {
            cal.firstDayOfWeek = Calendar.MONDAY
            cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
        }
        Period.MONTH -> {
            // «с 1-го или выбранного числа»
            val day = monthAnchorDay.coerceIn(1, 28)
            val nowDay = Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
            if (nowDay >= day) {
                cal.set(Calendar.DAY_OF_MONTH, day)
            } else {
                cal.add(Calendar.MONTH, -1)
                cal.set(Calendar.DAY_OF_MONTH, day)
            }
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
        }
        else -> { /* no-op */ }
    }
    return Range(cal.timeInMillis, end)
}
