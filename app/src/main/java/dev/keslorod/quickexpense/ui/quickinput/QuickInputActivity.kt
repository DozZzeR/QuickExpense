package dev.keslorod.quickexpense.ui.quickinput

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.lifecycleScope
import dev.keslorod.quickexpense.App
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

        // Грузим фавориты/валюту в IO, затем рисуем UI
        lifecycleScope.launch(Dispatchers.IO) {
            val sources = app.db.sources().favorites()
            val categories = app.db.categories().favorites()
            val currency = app.prefs.currencyFlow.first()

            withContext(Dispatchers.Main) {
                setContent {
                    QuickExpenseTheme {
                        QuickInputScreen(
                            currency = currency,
                            sourceOptions = sources.map { Option(it.id, it.name) },
                            categoryOptions = categories.map { Option(it.id, it.name) },
                            onConfirm = { cents, sourceId, categoryId ->
                                lifecycleScope.launch(Dispatchers.IO) {
                                    try {
                                        app.db.expenses().insert(
                                            Expense(
                                                amount = cents,
                                                currency = currency,
                                                sourceId = sourceId,
                                                categoryId = categoryId
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
                            }
                        )
                    }
                }
            }
        }
    }
}

