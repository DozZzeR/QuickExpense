package dev.keslorod.quickexpense

import android.app.Application
import android.app.LocaleManager
import android.os.Build
import android.os.LocaleList
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import dev.keslorod.quickexpense.data.db.AppDatabase
import dev.keslorod.quickexpense.data.prefs.AppPrefs
import dev.keslorod.quickexpense.ui.widget.WidgetRecomputeAndRedraw
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
            dev.keslorod.quickexpense.data.entities.Source(name = getString(R.string.default_source_cash)),
            dev.keslorod.quickexpense.data.entities.Source(name = getString(R.string.default_source_card)),
        )
        db.sources().upsertAll(sources)

        // категории
        val cats = listOf(
            dev.keslorod.quickexpense.data.entities.Category(name = getString(R.string.default_category_groceries)),
            dev.keslorod.quickexpense.data.entities.Category(name = getString(R.string.default_category_home)),
            dev.keslorod.quickexpense.data.entities.Category(name = getString(R.string.default_category_car)),
        )
        db.categories().upsertAll(cats)
    }

    override fun onCreate() {
        super.onCreate()
        
        if (BuildConfig.DEBUG) Log.d("App.onCreate", "Starting...")
        
        // Применяем сохранённый язык при запуске (без блокировки главного потока)
        appScope.launch {
            try {
                if (BuildConfig.DEBUG) Log.d("App.onCreate", "Reading language from DataStore...")
                val languageCode = prefs.languageCodeFlow.first()
                if (BuildConfig.DEBUG) Log.d(
                    "App.onCreate",
                    "Loaded language code: '$languageCode' (length: ${languageCode.length}, blank: ${languageCode.isBlank()})"
                )

                withContext(Dispatchers.Main) {
                    applyLanguage(languageCode)
                }
                if (BuildConfig.DEBUG) Log.d("App.onCreate", "Locales applied successfully")
            } catch (e: Exception) {
                Log.e("App.onCreate", "Error applying locales: ${e.message}", e)
            }
        }
        
        // Инициализируем БД если нужно
        appScope.launch(Dispatchers.IO) {
            val already = prefs.seededFlow.first()
            if (!already) {
                seedDefaults()
                prefs.setSeeded(true)
            }
        }
    }

    fun applyLanguage(languageCode: String) {
        if (Build.VERSION.SDK_INT >= 33) {
            val localeManager = getSystemService(LocaleManager::class.java)
            val localeList = if (languageCode.isBlank()) {
                LocaleList.getEmptyLocaleList()
            } else {
                LocaleList.forLanguageTags(languageCode)
            }
            localeManager.applicationLocales = localeList
            
            if (BuildConfig.DEBUG) {
                Log.d(
                    "language",
                    "LocaleManager.applicationLocales = ${localeManager.applicationLocales.toLanguageTags()}"
                )
                Log.d(
                    "language",
                    "Resources.configuration.locales = ${resources.configuration.locales[0].toLanguageTag()}"
                )
            }
        } else {
            // fallback для старых API (minSdk 26)
            AppCompatDelegate.setApplicationLocales(
                if (languageCode.isBlank())
                    LocaleListCompat.getEmptyLocaleList()
                else
                    LocaleListCompat.forLanguageTags(languageCode)
            )
        }
    }
}
