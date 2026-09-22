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
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
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

    // Lay out for the widget's actual size: at the minimum 2x1 cell a long total used to run
    // into the bag and the period line got clipped at the bottom.
    override val sizeMode = SizeMode.Exact

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
        val amountText = "${format(sum)} $currency"

        val size = LocalSize.current
        val w = size.width.value
        val h = size.height.value
        val pad = if (h < 90f) 10f else 14f
        // Bag scales with the height (it's 82x80 at full size) so it never crowds a short widget.
        val bagH = minOf(80f, h * 0.9f)
        val bagW = bagH * 82f / 80f
        // Drop the period line when there's no room for three lines of text.
        val showSubtitle = subtitle.isNotEmpty() && h >= 76f
        // Amount font: as big as fits both vertically and beside the bag (text may overlap the
        // bag's transparent top-left, hence only ~60% of its width is reserved). ~0.6em per char.
        val textLinesHeight = 12f * 1.3f + (if (showSubtitle) 4f + 12f * 1.3f else 0f)
        val byHeight = (h - 2 * pad - textLinesHeight) / 1.3f
        val byWidth = (w - 2 * pad - bagW * 0.6f) / (amountText.length * 0.6f)
        val amountSp = minOf(20f, byHeight, byWidth).coerceAtLeast(11f)

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
                    modifier = GlanceModifier.size(width = bagW.dp, height = bagH.dp)
                )
            }
            Box(
                modifier = GlanceModifier.fillMaxSize().padding(pad.dp),
                contentAlignment = Alignment.TopStart
            ) {
                Column {
                    Text(
                        text = label,
                        style = TextStyle(color = ColorProvider(Color.White.copy(alpha = 0.8f)), fontSize = 12.sp)
                    )
                    Text(
                        text = amountText,
                        maxLines = 1,
                        style = TextStyle(
                            color = ColorProvider(Color.White),
                            fontSize = amountSp.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    if (showSubtitle) {
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
