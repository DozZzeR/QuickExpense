package dev.keslorod.quickexpense.ui.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import dev.keslorod.quickexpense.App
import dev.keslorod.quickexpense.BuildConfig
import dev.keslorod.quickexpense.R
import dev.keslorod.quickexpense.ui.quickinput.QuickInputActivity
import java.util.Calendar
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
        val showRemainder = prefs[WidgetKeys.SHOW_REMAINDER] ?: false

        if (BuildConfig.DEBUG) Log.d("My debug WidgetContent", prefs.toString())
        val ctx = androidx.glance.LocalContext.current
        val openIntent = Intent(ctx, QuickInputActivity::class.java).apply {
            putExtra("from_widget", true)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }

        val labelRes = if (showRemainder) R.string.widget_balance else R.string.widget_expenses
        val label = ctx.getString(labelRes)

        // Mirrors res/layout/widget_expense.xml (the static placeholder Android draws before
        // this Glance content takes over) — background + money-bag watermark — so the widget
        // doesn't visibly lose its icon the moment real data replaces the placeholder.
        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(ImageProvider(R.drawable.widget_bg))
                .clickable(onClick = actionStartActivity(openIntent))
        ) {
            Box(modifier = GlanceModifier.fillMaxSize(), contentAlignment = Alignment.BottomEnd) {
                Image(
                    provider = ImageProvider(R.drawable.widget_bag),
                    contentDescription = null,
                    modifier = GlanceModifier.size(width = 82.dp, height = 80.dp)
                )
            }
            Box(
                modifier = GlanceModifier.fillMaxSize().padding(14.dp),
                contentAlignment = Alignment.TopStart
            ) {
                Column {
                    Text(
                        text = label,
                        style = TextStyle(color = ColorProvider(Color.White.copy(alpha = 0.8f)), fontSize = 12.sp)
                    )
                    Text(
                        text = "${format(sum)} $currency",
                        style = TextStyle(
                            color = ColorProvider(Color.White),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    if (subtitle.isNotEmpty()) {
                        Spacer(modifier = GlanceModifier.height(4.dp))
                        Text(
                            text = subtitle,
                            style = TextStyle(color = ColorProvider(Color.White.copy(alpha = 0.67f)), fontSize = 12.sp)
                        )
                    }
                }
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

    override fun onUpdate(
        context: Context,
        appWidgetManager: android.appwidget.AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        // onUpdate fires the moment a widget is first pinned, before anything in the app
        // has ever written real data to its state — without this it would sit at the
        // zeroed placeholder until an unrelated action (adding an expense, saving
        // Settings) happened to trigger a refresh.
        (context.applicationContext as App).widgetRefresher.schedule(0)
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            // The midnight alarm (see scheduleMidnightWidgetRefresh), and clock/zone changes
            // that move where "today" starts.
            ACTION_MIDNIGHT_REFRESH, Intent.ACTION_TIME_CHANGED, Intent.ACTION_TIMEZONE_CHANGED -> {
                // Keep the process alive until the recompute has actually been written —
                // a plain fire-and-forget launch can be killed as soon as onReceive returns.
                val pending = goAsync()
                (context.applicationContext as App).widgetRefresher.schedule(0)
                    .invokeOnCompletion { pending.finish() }
            }
            else -> super.onReceive(context, intent)
        }
    }

    companion object {
        const val ACTION_MIDNIGHT_REFRESH = "dev.keslorod.quickexpense.action.WIDGET_MIDNIGHT_REFRESH"
    }
}

/**
 * Arms a (re-armed on every refresh) alarm for the next local midnight that wakes
 * [QuickExpenseWidgetReceiver] to recompute the widget. Inexact on purpose: a few minutes'
 * delay is fine for a spending total, and it needs no exact-alarm permission.
 */
fun scheduleMidnightWidgetRefresh(context: Context) {
    val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
    val intent = Intent(context, QuickExpenseWidgetReceiver::class.java)
        .setAction(QuickExpenseWidgetReceiver.ACTION_MIDNIGHT_REFRESH)
    val pendingIntent = PendingIntent.getBroadcast(
        context, 0, intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    val nextMidnight = Calendar.getInstance().apply {
        add(Calendar.DAY_OF_YEAR, 1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 5)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
    alarmManager.set(AlarmManager.RTC, nextMidnight, pendingIntent)
}
