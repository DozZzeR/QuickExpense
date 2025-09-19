package dev.keslorod.quickexpense

import android.app.Application
import dev.keslorod.quickexpense.data.db.AppDatabase
import dev.keslorod.quickexpense.data.prefs.AppPrefs
import dev.keslorod.quickexpense.ui.widget.WidgetRecomputeAndRedraw
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class App : Application() {
    // Ленивая инициализация — доступ из любой точки через (application as App)
    val db by lazy { AppDatabase.get(this) }
    val prefs by lazy { AppPrefs(this) }
    val appScope by lazy { CoroutineScope(SupervisorJob() + Dispatchers.Default) }
    val widgetRefresher by lazy { WidgetRecomputeAndRedraw(appScope, this) }

    private suspend fun seedDefaults() {
        val db = db
        // источники
        val sources = listOf(
            dev.keslorod.quickexpense.data.entities.Source(name = "Наличка"),
            dev.keslorod.quickexpense.data.entities.Source(name = "Карта"),
        )
        db.sources().upsertAll(sources)

        // категории
        val cats = listOf(
            dev.keslorod.quickexpense.data.entities.Category(name = "Продукты"),
            dev.keslorod.quickexpense.data.entities.Category(name = "Дом"),
            dev.keslorod.quickexpense.data.entities.Category(name = "Машина"),
        )
        db.categories().upsertAll(cats)
    }

    override fun onCreate() {
        super.onCreate()
        kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) {
            val already = prefs.seededFlow.first()
            if (!already) {
                seedDefaults()
                prefs.setSeeded(true)
            }
        }
    }
}
