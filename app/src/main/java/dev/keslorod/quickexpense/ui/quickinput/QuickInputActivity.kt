package dev.keslorod.quickexpense.ui.quickinput

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import androidx.room.withTransaction
import dev.keslorod.quickexpense.App
import dev.keslorod.quickexpense.BuildConfig
import dev.keslorod.quickexpense.R
import dev.keslorod.quickexpense.data.entities.Expense
import dev.keslorod.quickexpense.data.entities.SplitNodeTag
import dev.keslorod.quickexpense.domain.RECEIPT_TAG_ID
import dev.keslorod.quickexpense.domain.tagExpenseAsHavingReceipt
import dev.keslorod.quickexpense.ui.split.SplitEditorScreen
import dev.keslorod.quickexpense.ui.theme.QuickExpenseTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class QuickInputActivity : ComponentActivity() {
    private val launchedFromWidget by lazy { intent.getBooleanExtra("from_widget", false) }
    // Present when this screen was opened to edit an already-saved expense (from
    // TransactionDetailsScreen's edit button) rather than to create a new one.
    private val editingExpenseId by lazy { intent.getStringExtra(EXTRA_EDIT_EXPENSE_ID) }
    private val app by lazy { application as App }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Грузим фаворты/валюту в IO, затем рисуем UI
        lifecycleScope.launch(Dispatchers.IO) {
            val sources = app.db.sources().favorites()
            val allSources = app.db.sources().all()
            val allCategories = app.db.categories().all()
            val merchants = app.db.merchants().favorites()
            val allMerchants = app.db.merchants().all()
            val defaultCurrency = app.prefs.currencyFlow.first()
            val languageCode = app.prefs.languageCodeFlow.first()

            // Готовим быстрый выбор: только избранные категории и источники
            val quickCategories = allCategories.filter { it.isFavorite }
            val sourceOptions = sources.map { Option(it.id, it.name) }
            val categoryOptions = quickCategories.map { Option(it.id, it.name) }
            val merchantOptions = merchants.map { Option(it.id, it.name) }
            // Full lists (not just favorites) so voice input can match a name it hasn't
            // been used often enough to be pinned as a favorite.
            val allCategoryOptions = allCategories.map { Option(it.id, it.name) }
            val allMerchantOptions = allMerchants.map { Option(it.id, it.name) }
            val allSourceOptions = allSources.map { Option(it.id, it.name) }

            // Load the expense being edited (if any) and derive QuickAddScreen's prefill —
            // its own currency, not the app-wide default, so an old expense in a currency the
            // user has since switched away from still displays and saves correctly.
            val editingExpense = editingExpenseId?.let { app.db.expenses().getById(it) }
            val currency = editingExpense?.currency ?: defaultCurrency
            val initialSource = editingExpense?.let { exp -> allSourceOptions.find { it.id == exp.sourceId } }
            val initialMerchant = editingExpense?.merchantId?.let { mid -> allMerchantOptions.find { it.id == mid } }
            val initialCategory = editingExpense?.let { exp -> allCategoryOptions.find { it.id == exp.categoryId } }
            val initialReceiptPaths = editingExpense?.photoPaths?.split("|")?.filter { it.isNotBlank() }.orEmpty()

            withContext(Dispatchers.Main) {
                setContent {
                    QuickExpenseTheme {
                        // Non-null while the user is fixing an existing expense's split from
                        // within the edit form (see QuickAddScreen's onOpenSplitEditor) — splits
                        // on an already-saved expense live directly in the DB, so this hosts the
                        // same real split editor MainActivity's own split_editor route uses,
                        // just inline in this Activity instead of as a separate nav destination.
                        var splitEditorTarget by remember { mutableStateOf<Pair<String, Long>?>(null) }
                        val target = splitEditorTarget

                        if (target != null) {
                            val (splitExpenseId, splitAmount) = target
                            SplitEditorScreen(
                                app = app,
                                expenseId = splitExpenseId,
                                totalAmount = splitAmount,
                                currency = currency,
                                initialLabel = getString(R.string.transaction_default),
                                onBack = { splitEditorTarget = null },
                                onDone = { nodes, tags ->
                                    lifecycleScope.launch(Dispatchers.IO) {
                                        app.db.withTransaction {
                                            app.db.splitNodes().deleteByExpenseId(splitExpenseId)
                                            nodes.sortedBy { it.depth }.forEach { node ->
                                                val toSave = node.copy(expenseId = splitExpenseId)
                                                app.db.splitNodes().insert(toSave)
                                                tags[node.id]?.forEach { tag ->
                                                    app.db.splitNodeTags().insert(SplitNodeTag(toSave.id, tag.id))
                                                }
                                            }
                                        }
                                        withContext(Dispatchers.Main) { splitEditorTarget = null }
                                    }
                                }
                            )
                        } else {
                            QuickAddScreen(
                                app = app,
                                currency = currency,
                                sourceOptions = sourceOptions,
                                categoryOptions = categoryOptions,
                                merchantOptions = merchantOptions,
                                allCategoryOptions = allCategoryOptions,
                                allMerchantOptions = allMerchantOptions,
                                allSourceOptions = allSourceOptions,
                                languageCode = languageCode,
                                editingExpenseId = editingExpenseId,
                                initialAmountCents = editingExpense?.amount,
                                initialSource = initialSource,
                                initialMerchant = initialMerchant,
                                initialCategory = initialCategory,
                                initialDateMillis = editingExpense?.createdAt,
                                initialReceiptPaths = initialReceiptPaths,
                                onOpenSplitEditor = { id, amount -> splitEditorTarget = id to amount },
                                onConfirm = { cents, sourceId, merchantId, categoryId, date, receiptPaths, splitNodes, nodeTags ->
                                    if (BuildConfig.DEBUG) {
                                        android.util.Log.d("ReceiptDebug", "onConfirm received receiptPaths=$receiptPaths")
                                    }
                                    lifecycleScope.launch(Dispatchers.IO) {
                                        try {
                                            val editId = editingExpenseId
                                            if (editId != null && editingExpense != null) {
                                                val updated = editingExpense.copy(
                                                    amount = cents,
                                                    sourceId = sourceId,
                                                    categoryId = categoryId ?: "unsorted",
                                                    merchantId = merchantId,
                                                    photoPaths = if (receiptPaths.isEmpty()) null else receiptPaths.joinToString("|"),
                                                    createdAt = date
                                                )
                                                app.db.expenses().update(updated)

                                                // A receipt replaced during this edit leaves its old file
                                                // with nothing pointing at it once the new path list is
                                                // saved — clean it up rather than leak it forever.
                                                val oldPaths = editingExpense.photoPaths
                                                    ?.split("|")?.filter { it.isNotBlank() }.orEmpty().toSet()
                                                (oldPaths - receiptPaths.toSet()).forEach { File(it).delete() }

                                                if (receiptPaths.isNotEmpty()) {
                                                    app.tagExpenseAsHavingReceipt(editId)
                                                } else if (oldPaths.isNotEmpty()) {
                                                    app.db.expenseTags().delete(editId, RECEIPT_TAG_ID)
                                                }
                                                // Splits aren't touched here — editing an existing
                                                // expense's split goes through the real split editor
                                                // above (onOpenSplitEditor), which persists on its own.
                                            } else {
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

                                                if (receiptPaths.isNotEmpty()) {
                                                    app.tagExpenseAsHavingReceipt(expense.id)
                                                }

                                                // Сохраняем сплиты
                                                splitNodes.forEach { node ->
                                                    val nodeToSave = node.copy(expenseId = expense.id)
                                                    app.db.splitNodes().insert(nodeToSave)

                                                    // Сохраняем метки для этого узла
                                                    nodeTags[node.id]?.forEach { tag ->
                                                        app.db.splitNodeTags().insert(SplitNodeTag(nodeToSave.id, tag.id))
                                                    }
                                                }
                                            }

                                            app.widgetRefresher.schedule()

                                        } catch (e: Exception) {
                                            // The old bare try/finally let any failure here (DB
                                            // constraint, whatever) vanish silently — finish() still
                                            // ran, so the screen closed looking like a normal save
                                            // while nothing was actually written.
                                            android.util.Log.e("QuickInputActivity", "Failed to save expense", e)
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

    companion object {
        const val EXTRA_EDIT_EXPENSE_ID = "edit_expense_id"
    }
}
