package dev.keslorod.quickexpense.ui.quickinput

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.lifecycleScope
import dev.keslorod.quickexpense.App
import dev.keslorod.quickexpense.data.dao.MAX_QUICK_OPTIONS
import dev.keslorod.quickexpense.data.entities.Expense
import dev.keslorod.quickexpense.ui.theme.QuickExpenseTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class QuickInputActivity : ComponentActivity() {
    private val launchedFromWidget by lazy { intent.getBooleanExtra("from_widget", false) }
    private val app by lazy { application as App }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Грузим фаворты/валюту в IO, затем рисуем UI
        lifecycleScope.launch(Dispatchers.IO) {
            val sources = app.db.sources().favorites()
            val allCategories = app.db.categories().all()
            val merchants = app.db.merchants().favorites()
            val currency = app.prefs.currencyFlow.first()

            // Готовим быстрый выбор: только избранные категории и источники
            val quickCategories = allCategories.filter { it.isFavorite }
            val sourceOptions = sources.map { Option(it.id, it.name) }
            val categoryOptions = quickCategories.map { Option(it.id, it.name) }
            val merchantOptions = merchants.map { Option(it.id, it.name) }

            withContext(Dispatchers.Main) {
                setContent {
                    QuickExpenseTheme {
                        QuickInputScreen(
                            app = app,
                            currency = currency,
                            sourceOptions = sourceOptions,
                            categoryQuickOptions = categoryOptions,
                            merchantOptions = merchantOptions,
                            onConfirm = { cents, sourceId, categoryId, merchantId, receiptPaths ->
                                lifecycleScope.launch(Dispatchers.IO) {
                                    try {
                                        app.db.expenses().insert(
                                            Expense(
                                                amount = cents,
                                                currency = currency,
                                                sourceId = sourceId,
                                                categoryId = categoryId,
                                                merchantId = merchantId,
                                                photoPaths = if (receiptPaths.isEmpty()) null else receiptPaths.joinToString("|")
                                            )
                                        )

                                        app.widgetRefresher.schedule()

                                    } finally {
                                        withContext(Dispatchers.Main) {
                                            if (launchedFromWidget)
                                                // finishAndRemoveTask()
                                                finishAffinity()
                                            else
                                                finish()
                                        }
                                    }
                                }
                            },
                            onCancel = {
                                lifecycleScope.launch(Dispatchers.IO) {
                                    withContext(Dispatchers.Main) {
                                        if (launchedFromWidget)
                                            finishAffinity()
                                        else
                                            finish()
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

