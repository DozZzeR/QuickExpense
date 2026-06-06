package dev.keslorod.quickexpense.ui.quickinput

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import coil.compose.AsyncImage
import dev.keslorod.quickexpense.App
import dev.keslorod.quickexpense.R
import dev.keslorod.quickexpense.data.entities.Category
import dev.keslorod.quickexpense.data.entities.Merchant
import dev.keslorod.quickexpense.data.entities.Source
import dev.keslorod.quickexpense.receipt.LocalReceiptScanner
import dev.keslorod.quickexpense.receipt.ReceiptScanResult
import dev.keslorod.quickexpense.ui.manage.ListScreenMode
import dev.keslorod.quickexpense.ui.manage.ManageListScreen

private const val MAX_RECENTS = 5
private const val MAX_FAVORITES = 16

data class Option(val id: String, val label: String)

private enum class QuickPickType { SOURCE, CATEGORY, MERCHANT }

@Composable
fun QuickInputScreen(
    app: App,
    currency: String,
    sourceOptions: List<Option>,
    categoryQuickOptions: List<Option>,
    merchantOptions: List<Option>,
    onConfirm: (cents: Long, sourceId: String, categoryId: String, merchantId: String?, receiptPaths: List<String>) -> Unit,
    onCancel: () -> Unit
) {
    var amountText by remember { mutableStateOf("") }
    var source by remember { mutableStateOf<Option?>(null) }
    var category by remember { mutableStateOf<Option?>(null) }
    var merchant by remember { mutableStateOf<Option?>(null) }
    var activeType by remember { mutableStateOf(QuickPickType.SOURCE) }
    var showAllType by remember { mutableStateOf<QuickPickType?>(null) }
    var recentCategories by remember { mutableStateOf<List<Option>>(emptyList()) }
    var recentSources by remember { mutableStateOf<List<Option>>(emptyList()) }
    var recentMerchants by remember { mutableStateOf<List<Option>>(emptyList()) }
    var currentFavoritesCategories by remember { mutableStateOf(categoryQuickOptions) }
    var currentFavoritesSources by remember { mutableStateOf(sourceOptions) }
    var currentFavoritesMerchants by remember { mutableStateOf(merchantOptions) }

    var lastScan by remember { mutableStateOf<ReceiptScanResult?>(null) }
    var showGallery by remember { mutableStateOf(false) }
    val scanner = LocalReceiptScanner.current
    val scannerHandle = scanner.rememberLauncher { lastScan = it }

    // Перезапрашиваем избранные после закрытия "Все"
    LaunchedEffect(showAllType) {
        if (showAllType == null) {
            val freshSources = app.db.sources().favorites()
            val freshCategories = app.db.categories().favorites()
            val freshMerchants = app.db.merchants().favorites()
            currentFavoritesSources = freshSources.map { Option(it.id, it.name) }
            currentFavoritesCategories = freshCategories.map { Option(it.id, it.name) }
            currentFavoritesMerchants = freshMerchants.map { Option(it.id, it.name) }
        }
    }

    fun toCents(txt: String): Long {
        if (txt.isBlank()) return 0
        val parts = txt.split(',', limit = 2)
        val major = parts[0].ifBlank { "0" }
        val minor = (parts.getOrNull(1) ?: "").padEnd(2, '0').take(2)
        return (major.toLongOrNull() ?: 0L) * 100 + (minor.toLongOrNull() ?: 0L)
    }

    fun updateRecents(list: List<Option>, item: Option): List<Option> {
        val without = list.filterNot { it.id == item.id }
        return (listOf(item) + without).take(MAX_RECENTS)
    }

    fun selectedFor(type: QuickPickType): Option? = when (type) {
        QuickPickType.SOURCE -> source
        QuickPickType.CATEGORY -> category
        QuickPickType.MERCHANT -> merchant
    }

    fun setSelected(type: QuickPickType, item: Option) {
        when (type) {
            QuickPickType.SOURCE -> source = item
            QuickPickType.CATEGORY -> category = item
            QuickPickType.MERCHANT -> merchant = item
        }
    }

    fun autoAdvance(type: QuickPickType, wasEmpty: Boolean) {
        if (!wasEmpty) return
        val order = listOf(QuickPickType.SOURCE, QuickPickType.CATEGORY, QuickPickType.MERCHANT)
        val next = order.dropWhile { it != type }.drop(1).firstOrNull { selectedFor(it) == null }
        if (next != null) activeType = next
    }

    if (showAllType != null) {
        when (showAllType) {
            QuickPickType.SOURCE -> {
                ManageListScreen<Source>(
                    title = stringResource(R.string.select_source),
                    mode = ListScreenMode.SELECT,
                    onSelect = { selectedSource ->
                        val option = Option(selectedSource.id, selectedSource.name)
                        val wasEmpty = source == null
                        source = option
                        recentSources = updateRecents(recentSources, option)
                        showAllType = null
                        autoAdvance(QuickPickType.SOURCE, wasEmpty)
                    },
                    onBack = { showAllType = null },
                    getName = { it.name },
                    isFavorite = { it.isFavorite },
                    itemKey = { it.id },
                    loadAll = { app.db.sources().all() },
                    addNew = { name -> app.db.sources().insert(Source(name = name, isFavorite = false)) },
                    toggleFavorite = { s -> app.db.sources().update(s.copy(isFavorite = !s.isFavorite)) },
                    rename = { s, newName -> app.db.sources().update(s.copy(name = newName)) },
                    deleteIfUnused = { s ->
                        val cnt = app.db.expenses().countBySource(s.id)
                        if (cnt == 0L) {
                            app.db.sources().delete(s)
                            true
                        } else false
                    }
                )
            }
            QuickPickType.CATEGORY -> {
                ManageListScreen<Category>(
                    title = stringResource(R.string.select_category),
                    mode = ListScreenMode.SELECT,
                    onSelect = { selectedCategory ->
                        val option = Option(selectedCategory.id, selectedCategory.name)
                        val wasEmpty = category == null
                        category = option
                        recentCategories = updateRecents(recentCategories, option)
                        showAllType = null
                        autoAdvance(QuickPickType.CATEGORY, wasEmpty)
                    },
                    onBack = { showAllType = null },
                    getName = { it.name },
                    isFavorite = { it.isFavorite },
                    itemKey = { it.id },
                    loadAll = { app.db.categories().all() },
                    addNew = { name -> app.db.categories().insert(Category(name = name, isFavorite = false)) },
                    toggleFavorite = { c -> app.db.categories().update(c.copy(isFavorite = !c.isFavorite)) },
                    rename = { c, newName -> app.db.categories().update(c.copy(name = newName)) },
                    deleteIfUnused = { c ->
                        val cnt = app.db.expenses().countByCategory(c.id)
                        if (cnt == 0L) {
                            app.db.categories().delete(c)
                            true
                        } else false
                    }
                )
            }
            QuickPickType.MERCHANT -> {
                ManageListScreen<Merchant>(
                    title = stringResource(R.string.select_merchant),
                    mode = ListScreenMode.SELECT,
                    onSelect = { selectedMerchant ->
                        val option = Option(selectedMerchant.id, selectedMerchant.name)
                        val wasEmpty = merchant == null
                        merchant = option
                        recentMerchants = updateRecents(recentMerchants, option)
                        showAllType = null
                        autoAdvance(QuickPickType.MERCHANT, wasEmpty)
                    },
                    onBack = { showAllType = null },
                    getName = { it.name },
                    isFavorite = { it.isFavorite },
                    itemKey = { it.id },
                    loadAll = { app.db.merchants().all() },
                    addNew = { name -> app.db.merchants().insert(Merchant(name = name, isFavorite = false)) },
                    toggleFavorite = { m -> app.db.merchants().update(m.copy(isFavorite = !m.isFavorite)) },
                    rename = { m, newName -> app.db.merchants().update(m.copy(name = newName)) },
                    deleteIfUnused = { m ->
                        val cnt = app.db.expenses().countByMerchant(m.id)
                        if (cnt == 0L) {
                            app.db.merchants().delete(m)
                            true
                        } else false
                    }
                )
            }
            null -> {}
        }
    } else {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            QuickPickControl(
                activeType = activeType,
                selected = mapOf(
                    QuickPickType.SOURCE to source,
                    QuickPickType.CATEGORY to category,
                    QuickPickType.MERCHANT to merchant
                ),
                recentsByType = mapOf(
                    QuickPickType.SOURCE to recentSources,
                    QuickPickType.CATEGORY to recentCategories,
                    QuickPickType.MERCHANT to recentMerchants
                ),
                favoritesByType = mapOf(
                    QuickPickType.SOURCE to currentFavoritesSources,
                    QuickPickType.CATEGORY to currentFavoritesCategories,
                    QuickPickType.MERCHANT to currentFavoritesMerchants
                ),
                onTypeChange = { activeType = it },
                onSelect = { type, item ->
                    val wasEmpty = selectedFor(type) == null
                    setSelected(type, item)
                    when (type) {
                        QuickPickType.SOURCE -> recentSources = updateRecents(recentSources, item)
                        QuickPickType.CATEGORY -> recentCategories = updateRecents(recentCategories, item)
                        QuickPickType.MERCHANT -> recentMerchants = updateRecents(recentMerchants, item)
                    }
                    autoAdvance(type, wasEmpty)
                },
                onOpenAll = { showAllType = it }
            )

            Spacer(Modifier.height(16.dp))
            val amountPretty = if (amountText.isBlank()) "0" else amountText
            Text("$amountPretty $currency", style = MaterialTheme.typography.displayMedium)

            Spacer(Modifier.height(16.dp))
            NumberPad(
                onDigit = { d -> amountText = (amountText + d).take(12) },
                onSep = {
                    if (!amountText.contains(',')) {
                        amountText = if (amountText.isBlank()) "0," else "$amountText,"
                    }
                },
                onBack = { if (amountText.isNotEmpty()) amountText = amountText.dropLast(1) }
            )

            Spacer(Modifier.height(16.dp))

            Spacer(Modifier.height(24.dp))
            OutlinedButton(
                onClick = { scannerHandle.start() },
                modifier = Modifier.fillMaxWidth()
            ) { Text(stringResource(R.string.scan_receipt)) }

            if (lastScan != null) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.scan_receipt_pages, lastScan!!.files.size),
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { showGallery = true },
                    modifier = Modifier.fillMaxWidth()
                ) { Text(stringResource(R.string.open_receipt_gallery)) }
            }

            Spacer(Modifier.height(12.dp))
            val cents = toCents(amountText)
            val isFormValid = source != null && category != null && cents > 0
            Button(
                onClick = {
                    val sId = source?.id ?: return@Button
                    val cId = category?.id ?: return@Button
                    val mId = merchant?.id
                    onConfirm(cents, sId, cId, mId, lastScan?.files?.map { it.absolutePath }.orEmpty())
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = isFormValid
            ) { Text(stringResource(R.string.ok_save)) }
            OutlinedButton(
                onClick = { onCancel() },
                modifier = Modifier.fillMaxWidth()
            ) { Text(stringResource(R.string.cancel)) }
        }

        if (showGallery && lastScan != null) {
            AlertDialog(
                onDismissRequest = { showGallery = false },
                confirmButton = {
                    TextButton(onClick = { showGallery = false }) {
                        Text(stringResource(R.string.close))
                    }
                },
                title = { Text(stringResource(R.string.receipt_gallery_title)) },
                text = {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(lastScan!!.files) { file ->
                            AsyncImage(
                                model = file,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(180.dp)
                            )
                        }
                    }
                }
            )
        }
    }
}

@Composable
private fun QuickPickControl(
    activeType: QuickPickType,
    selected: Map<QuickPickType, Option?>,
    recentsByType: Map<QuickPickType, List<Option>>,
    favoritesByType: Map<QuickPickType, List<Option>>,
    onTypeChange: (QuickPickType) -> Unit,
    onSelect: (QuickPickType, Option) -> Unit,
    onOpenAll: (QuickPickType) -> Unit,
    fieldOrder: List<QuickPickType> = listOf(QuickPickType.SOURCE, QuickPickType.CATEGORY, QuickPickType.MERCHANT),
    maxRecents: Int = MAX_RECENTS
) {
    @Composable
    fun fieldLabel(type: QuickPickType): String = when (type) {
        QuickPickType.SOURCE -> stringResource(R.string.source)
        QuickPickType.CATEGORY -> stringResource(R.string.category)
        QuickPickType.MERCHANT -> stringResource(R.string.merchant)
    }

    fun fieldIcon(type: QuickPickType) = when (type) {
        QuickPickType.SOURCE -> Icons.Filled.AccountBalanceWallet
        QuickPickType.CATEGORY -> Icons.Filled.Category
        QuickPickType.MERCHANT -> Icons.Filled.Store
    }

    Column(
        Modifier.fillMaxWidth()
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            fieldOrder.forEach { type ->
                val value = selected[type]?.label ?: stringResource(R.string.select_item)
                val isActive = activeType == type
                AssistChip(
                    onClick = { onTypeChange(type) },
                    label = {
                        Text(
                            text = value,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    leadingIcon = {
                        Icon(fieldIcon(type), contentDescription = fieldLabel(type))
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = if (isActive) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant
                    ),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        val recents = recentsByType[activeType].orEmpty().take(maxRecents)
        val favorites = favoritesByType[activeType].orEmpty().filter { fav ->
            recents.none { it.id == fav.id }
        }.take(MAX_FAVORITES)
        val combined = recents + favorites

        Box(
            modifier = Modifier.fillMaxWidth()
        ) {
            if (combined.isEmpty()) {
                Text(
                    text = stringResource(R.string.quick_pick_empty),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier
                        .padding(end = 72.dp)
                        .fillMaxWidth()
                )
            } else {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(end = 72.dp)
                ) {
                    items(recents) { opt ->
                        QuickPickChip(
                            label = opt.label,
                            selected = selected[activeType]?.id == opt.id,
                            onClick = { onSelect(activeType, opt) }
                        )
                    }
                    if (recents.isNotEmpty() && favorites.isNotEmpty()) {
                        item { Spacer(Modifier.width(8.dp)) }
                    }
                    items(favorites) { opt ->
                        QuickPickChip(
                            label = opt.label,
                            selected = selected[activeType]?.id == opt.id,
                            onClick = { onSelect(activeType, opt) }
                        )
                    }
                }
            }

            Button(
                onClick = { onOpenAll(activeType) },
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .height(40.dp)
            ) {
                Text(stringResource(R.string.all))
            }
        }
    }
}

@Composable
private fun QuickPickChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis)
        },
        modifier = Modifier.heightIn(min = 44.dp)
    )
}


@Composable
private fun NumberPad(
    onDigit: (String) -> Unit,
    onSep: () -> Unit,
    onBack: () -> Unit
) {
    @Composable
    fun RowScope.key(label: String, onClick: () -> Unit) {
        Button(onClick = onClick, modifier = Modifier.weight(1f)) { Text(label) }
    }

    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.fillMaxWidth()) {
            key("1") { onDigit("1") }; key("2") { onDigit("2") }; key("3") { onDigit("3") }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.fillMaxWidth()) {
            key("4") { onDigit("4") }; key("5") { onDigit("5") }; key("6") { onDigit("6") }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.fillMaxWidth()) {
            key("7") { onDigit("7") }; key("8") { onDigit("8") }; key("9") { onDigit("9") }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.fillMaxWidth()) {
            key(",") { onSep() }; key("0") { onDigit("0") }; key("⌫") { onBack() }
        }
    }
}

