package dev.keslorod.quickexpense.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Fixed-order categorical palette for dashboard charts (pie/donut slices, chart lines).
 *
 * Deliberately independent from [MaterialTheme.colorScheme]: this app enables Material You
 * dynamic color (see QuickExpenseTheme), which would otherwise make series colors drift with
 * the user's wallpaper and break color-identity consistency between chart renders. Values are
 * a validated CVD-safe categorical order (adjacent-pair contrast checked for both light and
 * dark) — pick colors by fixed slot index, never re-cycle by rank when the item set changes.
 */
object ChartColors {
    // Slot order: blue, orange, aqua, yellow, magenta, green, violet, red.
    private val lightSeries = listOf(
        Color(0xFF2A78D6),
        Color(0xFFEB6834),
        Color(0xFF1BAF7A),
        Color(0xFFEDA100),
        Color(0xFFE87BA4),
        Color(0xFF008300),
        Color(0xFF4A3AA7),
        Color(0xFFE34948),
    )

    private val darkSeries = listOf(
        Color(0xFF3987E5),
        Color(0xFFD95926),
        Color(0xFF199E70),
        Color(0xFFC98500),
        Color(0xFFD55181),
        Color(0xFF008300),
        Color(0xFF9085E9),
        Color(0xFFE66767),
    )

    /** Neutral color for a folded "Other" bucket — never a real categorical slot. */
    val otherLight = Color(0xFF898781)
    val otherDark = Color(0xFF898781)

    @Composable
    fun series(): List<Color> = if (isSystemInDarkTheme()) darkSeries else lightSeries

    @Composable
    fun other(): Color = if (isSystemInDarkTheme()) otherDark else otherLight

    /** Colors for [count] categorical slots by fixed slot index, wrapping only if exceeded
     *  (callers should fold beyond ~6 items into "Other" instead of relying on wraparound). */
    @Composable
    fun forCount(count: Int): List<Color> {
        val palette = series()
        return List(count) { palette[it % palette.size] }
    }
}
