package dev.keslorod.quickexpense.ui.widget

import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.booleanPreferencesKey

object WidgetKeys {
    val SUM = longPreferencesKey("sum_cents")
    val CCY = stringPreferencesKey("currency")
    val SUBTITLE = stringPreferencesKey("subtitle") // "за день" / "за неделю" / ...
    val SHOW_REMAINDER = booleanPreferencesKey("show_remainder") // показывать остаток вместо расхода
}
