package dev.keslorod.quickexpense.ui.widget

import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

object WidgetKeys {
    val SUM = longPreferencesKey("sum_cents")
    val CCY = stringPreferencesKey("currency")
    val SUBTITLE = stringPreferencesKey("subtitle") // "за день" / "за неделю" / ...
}
