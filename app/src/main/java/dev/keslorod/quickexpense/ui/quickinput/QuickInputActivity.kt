package dev.keslorod.quickexpense.ui.quickinput

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.lifecycleScope
import androidx.room.withTransaction
import dev.keslorod.quickexpense.App
import dev.keslorod.quickexpense.BuildConfig
import dev.keslorod.quickexpense.R
import dev.keslorod.quickexpense.data.entities.Expense
import dev.keslorod.quickexpense.data.entities.SplitNode
import dev.keslorod.quickexpense.data.entities.SplitNodeTag
import dev.keslorod.quickexpense.data.entities.Tag
import dev.keslorod.quickexpense.domain.RECEIPT_TAG_ID
import dev.keslorod.quickexpense.domain.UNSORTED_CATEGORY_ID
import dev.keslorod.quickexpense.domain.tagExpenseAsHavingReceipt
import dev.keslorod.quickexpense.ui.theme.QuickExpenseTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class QuickInputActivity : AppCompatActivity() {
    private val launchedFromWidget by lazy { intent.getBooleanExtra("from_widget", false) }
    // Present when this screen was opened to edit an already-saved expense (from
    // TransactionDetailsScreen's edit button) rather than to create a new one.
    private val editingExpenseId by lazy { intent.getStringExtra(EXTRA_EDIT_EXPENSE_ID) }
    private val app by lazy { application as App }

    // Only touched on the main thread. Guards against a second save (a double tap on "Today",
    // or the voice auto-save firing alongside a manual tap) inserting the same expense twice
    // while the first save is still in flight — finish() only happens once it completes.
    private var saveInProgress = false

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
            if (editingExpenseId != null && editingExpense == null) {
                // Asked to edit an expense that no longer exists — don't fall through to the
                // create-new form, whose Save would silently mint a brand-new expense instead.
                withContext(Dispatchers.Main) { close() }
                return@launch
            }
            val currency = editingExpense?.currency ?: defaultCurrency
            val initialSource = editingExpense?.let { exp -> allSourceOptions.find { it.id == exp.sourceId } }
            val initialMerchant = editingExpense?.merchantId?.let { mid -> allMerchantOptions.find { it.id == mid } }
            val initialCategory = editingExpense?.let { exp -> allCategoryOptions.find { it.id == exp.categoryId } }
            val initialReceiptPaths = editingExpense?.photoPaths?.split("|")?.filter { it.isNotBlank() }.orEmpty()
            // Its persisted split becomes QuickAddScreen's in-memory draft, written back only
            // on Save (see onConfirm) — same as a new expense's split.
            val initialSplitNodes = editingExpense?.let { app.db.splitNodes().getByExpenseId(it.id) }.orEmpty()
            val initialNodeTags = initialSplitNodes.associate { it.id to app.db.splitNodeTags().getTagsForSplitNode(it.id) }
            // A split row with no category of its own was saved alongside an expense whose
            // missing category became "unsorted" — the two mean the same thing, not a divergence.
            val initialSplitDiverged = editingExpense != null &&
                initialSplitNodes.any { it.parentId == null && (it.categoryId ?: UNSORTED_CATEGORY_ID) != editingExpense.categoryId }

            withContext(Dispatchers.Main) {
                setContent {
                    QuickExpenseTheme {
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
                            editingExpenseId = editingExpense?.id,
                            initialAmountCents = editingExpense?.amount,
                            initialSource = initialSource,
                            initialMerchant = initialMerchant,
                            initialCategory = initialCategory,
                            initialDateMillis = editingExpense?.createdAt,
                            initialReceiptPaths = initialReceiptPaths,
                            initialSplitNodes = initialSplitNodes,
                            initialNodeTags = initialNodeTags,
                            initialSplitDiverged = initialSplitDiverged,
                            onConfirm = { cents, sourceId, merchantId, categoryId, date, receiptPaths, splitNodes, nodeTags ->
                                if (BuildConfig.DEBUG) {
                                    android.util.Log.d("ReceiptDebug", "onConfirm received receiptPaths=$receiptPaths")
                                }
                                if (saveInProgress) return@QuickAddScreen
                                saveInProgress = true
                                lifecycleScope.launch {
                                    val saved = try {
                                        withContext(Dispatchers.IO) {
                                            save(editingExpense, currency, cents, sourceId, merchantId, categoryId, date, receiptPaths, splitNodes, nodeTags)
                                        }
                                        true
                                    } catch (e: Exception) {
                                        android.util.Log.e("QuickInputActivity", "Failed to save expense", e)
                                        false
                                    }
                                    if (saved) {
                                        app.widgetRefresher.schedule()
                                        close()
                                    } else {
                                        // Stay on the form with everything still filled in, so the
                                        // user knows nothing was written and can simply retry —
                                        // closing here made a failed save look like a normal one.
                                        saveInProgress = false
                                        Toast.makeText(this@QuickInputActivity, R.string.save_failed, Toast.LENGTH_LONG).show()
                                    }
                                }
                            },
                            onCancel = { close() }
                        )
                    }
                }
            }
        }
    }

    private fun close() {
        if (launchedFromWidget) finishAffinity() else finish()
    }

    /**
     * Writes the expense together with its receipt tag and split tree in one transaction, so a
     * failure part-way through (a constraint violation, whatever) can't leave e.g. an expense
     * saved with only half of its split. Receipt files that this edit replaced are deleted only
     * after the transaction has committed.
     */
    private suspend fun save(
        editingExpense: Expense?,
        currency: String,
        cents: Long,
        sourceId: String,
        merchantId: String?,
        categoryId: String?,
        date: Long,
        receiptPaths: List<String>,
        splitNodes: List<SplitNode>,
        nodeTags: Map<String, List<Tag>>
    ) {
        val photoPaths = if (receiptPaths.isEmpty()) null else receiptPaths.joinToString("|")
        val oldPaths = editingExpense?.photoPaths
            ?.split("|")?.filter { it.isNotBlank() }.orEmpty().toSet()

        app.db.withTransaction {
            val expenseId = if (editingExpense != null) {
                app.db.expenses().update(
                    editingExpense.copy(
                        amount = cents,
                        sourceId = sourceId,
                        categoryId = categoryId ?: UNSORTED_CATEGORY_ID,
                        merchantId = merchantId,
                        photoPaths = photoPaths,
                        createdAt = date
                    )
                )
                app.db.splitNodes().deleteByExpenseId(editingExpense.id)
                editingExpense.id
            } else {
                val expense = Expense(
                    amount = cents,
                    currency = currency,
                    sourceId = sourceId,
                    categoryId = categoryId ?: UNSORTED_CATEGORY_ID,
                    merchantId = merchantId,
                    photoPaths = photoPaths,
                    createdAt = date
                )
                app.db.expenses().insert(expense)
                expense.id
            }

            if (receiptPaths.isNotEmpty()) {
                app.tagExpenseAsHavingReceipt(expenseId)
            } else if (oldPaths.isNotEmpty()) {
                app.db.expenseTags().delete(expenseId, RECEIPT_TAG_ID)
            }

            // Parents before children: split_nodes.parentId is a self-referencing foreign key,
            // and SQLite checks it at each insert, not at commit.
            splitNodes.sortedBy { it.depth }.forEach { node ->
                val nodeToSave = node.copy(expenseId = expenseId)
                app.db.splitNodes().insert(nodeToSave)
                nodeTags[node.id]?.forEach { tag ->
                    app.db.splitNodeTags().insert(SplitNodeTag(nodeToSave.id, tag.id))
                }
            }
        }

        // A receipt replaced during this edit leaves its old file with nothing pointing at it
        // once the new path list is saved — clean it up rather than leak it forever.
        (oldPaths - receiptPaths.toSet()).forEach { File(it).delete() }
    }

    companion object {
        const val EXTRA_EDIT_EXPENSE_ID = "edit_expense_id"
    }
}
