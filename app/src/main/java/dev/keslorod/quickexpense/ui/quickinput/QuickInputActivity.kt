package dev.keslorod.quickexpense.ui.quickinput

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.lifecycleScope
import dev.keslorod.quickexpense.App
import dev.keslorod.quickexpense.data.entities.Expense
import dev.keslorod.quickexpense.data.entities.SplitNodeTag
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
                        QuickAddScreen(
                            app = app,
                            currency = currency,
                            sourceOptions = sourceOptions,
                            categoryOptions = categoryOptions,
                            merchantOptions = merchantOptions,
                            onConfirm = { cents, sourceId, merchantId, categoryId, date, receiptPaths, splitNodes, nodeTags ->
                                lifecycleScope.launch(Dispatchers.IO) {
                                    try {
                                        val expense = Expense(
                                            amount = cents,
                                            currency = currency,
                                            sourceId = sourceId,
                                            categoryId = categoryId ?: "unsorted",
                                            merchantId = merchantId,
                                            photoPaths = if (receiptPaths.isEmpty()) null else receiptPaths.joinToString("|"),
                                            createdAt = date
                                        )
                                        app.db.expenses().insert(expense)

                                        // Сохраняем сплиты
                                        splitNodes.forEach { node ->
                                            val nodeToSave = node.copy(expenseId = expense.id)
                                            app.db.splitNodes().insert(nodeToSave)
                                            
                                            // Сохраняем метки для этого узла
                                            nodeTags[node.id]?.forEach { tag ->
                                                app.db.splitNodeTags().insert(SplitNodeTag(nodeToSave.id, tag.id))
                                            }
                                        }

                                        app.widgetRefresher.schedule()

                                    } finally {
                                        withContext(Dispatchers.Main) {
                                            if (launchedFromWidget)
                                                finishAffinity()
                                            else
                                                finish()
                                        }
                                    }
                                }
                            },
                            onCancel = {
                                if (launchedFromWidget)
                                    finishAffinity()
                                else
                                    finish()
                            }
                        )
                    }
                }
            }
        }
    }
}

