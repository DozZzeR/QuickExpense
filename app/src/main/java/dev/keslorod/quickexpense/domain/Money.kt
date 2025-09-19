package dev.keslorod.quickexpense.domain

fun formatCents(cents: Long, sep: Char = ','): String {
    val sign = if (cents < 0) "-" else ""
    val abs = kotlin.math.abs(cents)
    val major = abs / 100
    val minor = abs % 100
    return "$sign$major$sep${minor.toString().padStart(2, '0')}"
}
