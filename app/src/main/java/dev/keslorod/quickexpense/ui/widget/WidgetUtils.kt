package dev.keslorod.quickexpense.ui.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetManager

suspend fun hasAnyQuickExpenseWidget(ctx: Context): Boolean {
    val mgr = GlanceAppWidgetManager(ctx.applicationContext)
    val ids = mgr.getGlanceIds(QuickExpenseWidget::class.java)
    return ids.isNotEmpty()
}
