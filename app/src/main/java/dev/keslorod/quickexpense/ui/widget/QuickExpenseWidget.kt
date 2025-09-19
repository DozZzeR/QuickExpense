package dev.keslorod.quickexpense.ui.widget

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.Preferences
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.currentState
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.Text
import dev.keslorod.quickexpense.ui.quickinput.QuickInputActivity
import kotlin.math.abs

class QuickExpenseWidget : GlanceAppWidget() {

    override val stateDefinition = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        // Ничего тяжёлого — только рендер из уже записанного state
        provideContent { WidgetContent() }
    }

    @Composable
    private fun WidgetContent() {
        val prefs: Preferences = currentState()
        val sum = prefs[WidgetKeys.SUM] ?: 0L
        val currency = prefs[WidgetKeys.CCY] ?: "RSD"
        val subtitle = prefs[WidgetKeys.SUBTITLE].orEmpty()

        Log.d("My debug WidgetContent", prefs.toString())
        val ctx = androidx.glance.LocalContext.current
        val openIntent = Intent(ctx, QuickInputActivity::class.java).apply {
            putExtra("from_widget", true)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }

        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .padding(8.dp)
                .clickable(onClick = actionStartActivity(openIntent))
        ) {
            Text(text = "Расходы: ${format(sum)} $currency")
            if (subtitle.isNotEmpty()) {
                Spacer(modifier = GlanceModifier.height(4.dp))
                Text(text = subtitle)
            }
        }
    }

    private fun format(cents: Long): String {
        val a = abs(cents)
        val major = a / 100
        val minor = a % 100
        val s = "%d,%02d".format(major, minor)
        return if (cents < 0) "-$s" else s
    }
}

class QuickExpenseWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = QuickExpenseWidget()
}
