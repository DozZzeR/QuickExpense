package dev.keslorod.quickexpense.ui.quickinput

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import dev.keslorod.quickexpense.App
import dev.keslorod.quickexpense.R
import dev.keslorod.quickexpense.receipt.LocalReceiptScanner
import dev.keslorod.quickexpense.receipt.ReceiptScanResult
import dev.keslorod.quickexpense.data.entities.SplitNode
import dev.keslorod.quickexpense.data.entities.Tag
import dev.keslorod.quickexpense.ui.split.SplitEditorScreen
import java.util.Calendar

enum class OperationMode { EXPENSE, INCOME, TRANSFER }
enum class QuickAddType { SOURCE, MERCHANT, CATEGORY, TEMPLATES }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickAddScreen(
    app: App,
    currency: String,
    sourceOptions: List<Option>,
    categoryOptions: List<Option>,
    merchantOptions: List<Option>,
    onConfirm: (cents: Long, sourceId: String, merchantId: String?, categoryId: String?, date: Long, receiptPaths: List<String>, splitNodes: List<SplitNode>, nodeTags: Map<String, List<Tag>>) -> Unit,
    onCancel: () -> Unit
) {
    var mode by remember { mutableStateOf(OperationMode.EXPENSE) }
    var amountText by remember { mutableStateOf("") }
    var selectedDate by remember { mutableStateOf(Calendar.getInstance()) }
    var showDatePicker by remember { mutableStateOf(false) }
    var source by remember { mutableStateOf<Option?>(sourceOptions.firstOrNull()) }
    var merchant by remember { mutableStateOf<Option?>(null) }
    var category by remember { mutableStateOf<Option?>(null) }
    
    var activePanel by remember { mutableStateOf<QuickAddType?>(null) }
    var showManageType by remember { mutableStateOf<QuickAddType?>(null) }
    
    // Split state
    var showSplitEditor by remember { mutableStateOf(false) }
    var draftSplitNodes by remember { mutableStateOf<List<SplitNode>>(emptyList()) }
    var draftNodeTags by remember { mutableStateOf<Map<String, List<Tag>>>(emptyMap()) }
    
    val anyChildHasCategory = remember(draftSplitNodes) {
        draftSplitNodes.any { it.parentId == null && it.categoryId != null }
    }

    LaunchedEffect(anyChildHasCategory) {
        if (anyChildHasCategory) {
            category = null
        }
    }

    // Receipts state
    var lastScan by remember { mutableStateOf<ReceiptScanResult?>(null) }
    var showGallery by remember { mutableStateOf(false) }
    val scanner = LocalReceiptScanner.current
    val scannerHandle = scanner.rememberLauncher { lastScan = it }

    fun toCents(txt: String): Long {
        if (txt.isBlank()) return 0
        val parts = txt.replace(',', '.').split('.', limit = 2)
        val major = parts[0].ifBlank { "0" }
        val minor = (parts.getOrNull(1) ?: "").padEnd(2, '0').take(2)
        return (major.toLongOrNull() ?: 0L) * 100 + (minor.toLongOrNull() ?: 0L)
    }

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top section: Operation mode selector
            OperationModeSelector(
                selectedMode = mode,
                onModeChange = { mode = it },
                onOpenTemplates = { activePanel = QuickAddType.TEMPLATES }
            )

            Spacer(Modifier.height(32.dp))

            // Amount display
            AmountDisplay(amountText, currency)

            Spacer(Modifier.height(32.dp))

            // Picker rows
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                PickerRow(
                    label = stringResource(R.string.source),
                    value = source?.label ?: stringResource(R.string.choose_source),
                    onClick = { activePanel = QuickAddType.SOURCE }
                )
                PickerRow(
                    label = stringResource(R.string.to_where),
                    value = merchant?.label ?: stringResource(R.string.choose_or_add),
                    onClick = { activePanel = QuickAddType.MERCHANT }
                )
                if (!anyChildHasCategory) {
                    PickerRow(
                        label = stringResource(R.string.category),
                        value = category?.label ?: stringResource(R.string.optional),
                        onClick = { activePanel = QuickAddType.CATEGORY }
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            // Custom numpad
            CustomNumPad(
                onDigit = { digit -> if (amountText.length < 10) amountText += digit },
                onDot = { 
                    if (!amountText.contains(".")) {
                        amountText = if (amountText.isEmpty()) "0." else "$amountText."
                    }
                },
                onDelete = { if (amountText.isNotEmpty()) amountText = amountText.dropLast(1) }
            )

            Spacer(Modifier.height(24.dp))
            
            // Split and Scan buttons
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val cents = toCents(amountText)
                OutlinedButton(
                    onClick = { showSplitEditor = true },
                    modifier = Modifier.weight(1f),
                    enabled = cents > 0,
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Icon(Icons.Default.CallSplit, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (draftSplitNodes.isEmpty()) "Разбить" else "Сплит (${draftSplitNodes.size})")
                }

                OutlinedButton(
                    onClick = { scannerHandle.start() },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Icon(Icons.Default.Receipt, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (lastScan == null) stringResource(R.string.scan_receipt) else stringResource(R.string.scan_receipt_pages, lastScan!!.files.size))
                }
            }
            
            if (lastScan != null) {
                TextButton(onClick = { showGallery = true }) {
                    Text(stringResource(R.string.open_receipt_gallery))
                }
            }

    Spacer(Modifier.height(24.dp))

    // Save/date buttons
    val isFormValid = source != null && amountText.isNotEmpty() && toCents(amountText) > 0
    
    SaveDateButtons(
        enabled = isFormValid,
        onSave = { daysOffset -> 
            val cal = (selectedDate.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, daysOffset) }
            // TODO: handle split saving in onConfirm if needed, 
            // but for now we just pass data back to activity
            onConfirm(
                toCents(amountText),
                source!!.id,
                merchant?.id,
                category?.id,
                cal.timeInMillis,
                lastScan?.files?.map { it.absolutePath }.orEmpty(),
                draftSplitNodes,
                draftNodeTags
            )
            // Note: Split nodes saving should be handled in QuickInputActivity
        },
        onOpenCalendar = { showDatePicker = true }
    )

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDate.timeInMillis,
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    return utcTimeMillis <= System.currentTimeMillis()
                }
            }
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        selectedDate = Calendar.getInstance().apply { timeInMillis = it }
                    }
                    showDatePicker = false
                }) {
                    Text(stringResource(R.string.confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
            
            Spacer(Modifier.height(16.dp))
            TextButton(onClick = onCancel) {
                Text(stringResource(R.string.cancel))
            }
        }

        // Overlay panel: QuickGridPanel
        if (activePanel != null) {
            QuickGridPanel(
                type = activePanel!!,
                sourceOptions = sourceOptions,
                merchantOptions = merchantOptions,
                categoryOptions = categoryOptions,
                onDismiss = { activePanel = null },
                onSelect = { type, option ->
                    when (type) {
                        QuickAddType.SOURCE -> source = option
                        QuickAddType.MERCHANT -> merchant = option
                        QuickAddType.CATEGORY -> category = option
                        QuickAddType.TEMPLATES -> {
                            // Apply template logic could go here
                        }
                    }
                    activePanel = null
                },
                onManage = { type ->
                    showManageType = type
                    activePanel = null
                }
            )
        }

        if (showManageType != null) {
            val type = showManageType!!
            val title = when (type) {
                QuickAddType.SOURCE -> stringResource(R.string.select_source)
                QuickAddType.MERCHANT -> stringResource(R.string.select_merchant)
                QuickAddType.CATEGORY -> stringResource(R.string.select_category)
                else -> ""
            }
            
            Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                when (type) {
                    QuickAddType.SOURCE -> {
                        dev.keslorod.quickexpense.ui.manage.ManageListScreen(
                            title = title,
                            onBack = { showManageType = null },
                            mode = dev.keslorod.quickexpense.ui.manage.ListScreenMode.SELECT,
                            onSelect = { source = Option(it.id, it.name); showManageType = null },
                            getName = { it.name },
                            isFavorite = { it.isFavorite },
                            itemKey = { it.id },
                            loadAll = { app.db.sources().all() },
                            addNew = { app.db.sources().insert(dev.keslorod.quickexpense.data.entities.Source(name = it, isFavorite = false)) },
                            toggleFavorite = { app.db.sources().update(it.copy(isFavorite = !it.isFavorite)) },
                            rename = { item, newName -> app.db.sources().update(item.copy(name = newName)) },
                            deleteIfUnused = { item ->
                                if (app.db.expenses().countBySource(item.id) == 0L) {
                                    app.db.sources().delete(item)
                                    true
                                } else false
                            }
                        )
                    }
                    QuickAddType.MERCHANT -> {
                        dev.keslorod.quickexpense.ui.manage.ManageListScreen(
                            title = title,
                            onBack = { showManageType = null },
                            mode = dev.keslorod.quickexpense.ui.manage.ListScreenMode.SELECT,
                            onSelect = { merchant = Option(it.id, it.name); showManageType = null },
                            getName = { it.name },
                            isFavorite = { it.isFavorite },
                            itemKey = { it.id },
                            loadAll = { app.db.merchants().all() },
                            addNew = { app.db.merchants().insert(dev.keslorod.quickexpense.data.entities.Merchant(name = it, isFavorite = false)) },
                            toggleFavorite = { app.db.merchants().update(it.copy(isFavorite = !it.isFavorite)) },
                            rename = { item, newName -> app.db.merchants().update(item.copy(name = newName)) },
                            deleteIfUnused = { item ->
                                if (app.db.expenses().countByMerchant(item.id) == 0L) {
                                    app.db.merchants().delete(item)
                                    true
                                } else false
                            }
                        )
                    }
                    QuickAddType.CATEGORY -> {
                        dev.keslorod.quickexpense.ui.manage.ManageListScreen(
                            title = title,
                            onBack = { showManageType = null },
                            mode = dev.keslorod.quickexpense.ui.manage.ListScreenMode.SELECT,
                            onSelect = { category = Option(it.id, it.name); showManageType = null },
                            getName = { it.name },
                            isFavorite = { it.isFavorite },
                            itemKey = { it.id },
                            loadAll = { app.db.categories().all() },
                            addNew = { app.db.categories().insert(dev.keslorod.quickexpense.data.entities.Category(name = it, isFavorite = false)) },
                            toggleFavorite = { app.db.categories().update(it.copy(isFavorite = !it.isFavorite)) },
                            rename = { item, newName -> app.db.categories().update(item.copy(name = newName)) },
                            deleteIfUnused = { item ->
                                if (app.db.expenses().countByCategory(item.id) == 0L) {
                                    app.db.categories().delete(item)
                                    true
                                } else false
                            }
                        )
                    }
                    else -> {}
                }
            }
        }
        
        if (showGallery && lastScan != null) {
            // ... (existing dialog)
        }

        if (showSplitEditor) {
            Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                SplitEditorScreen(
                    app = app,
                    totalAmount = toCents(amountText),
                    currency = currency,
                    initialNodes = draftSplitNodes,
                    initialTags = draftNodeTags,
                    initialLabel = merchant?.label ?: "Транзакция",
                    onBack = { showSplitEditor = false },
                    onDone = { nodes, tags ->
                        draftSplitNodes = nodes
                        draftNodeTags = tags
                        showSplitEditor = false
                    }
                )
            }
        }
    }
}

@Composable
fun OperationModeSelector(
    selectedMode: OperationMode,
    onModeChange: (OperationMode) -> Unit,
    onOpenTemplates: () -> Unit
) {
    val modes = listOf(
        OperationMode.EXPENSE to stringResource(R.string.expense),
        OperationMode.INCOME to stringResource(R.string.income),
        OperationMode.TRANSFER to stringResource(R.string.transfer)
    )

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier.height(48.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            modes.forEach { (mode, label) ->
                val isSelected = selectedMode == mode
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(24.dp))
                        .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                        .clickable { 
                            if (isSelected) onOpenTemplates() else onModeChange(mode)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = label,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AmountDisplay(amount: String, currency: String) {
    val displayAmount = if (amount.isEmpty()) "0" else amount
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "$displayAmount $currency",
            style = MaterialTheme.typography.displayLarge.copy(
                fontSize = 56.sp,
                fontWeight = FontWeight.Light,
                textAlign = TextAlign.Center
            ),
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Composable
fun PickerRow(label: String, value: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            Modifier
                .padding(horizontal = 20.dp, vertical = 14.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun CustomNumPad(
    onDigit: (String) -> Unit,
    onDot: () -> Unit,
    onDelete: () -> Unit
) {
    val buttons = listOf(
        "1", "2", "3",
        "4", "5", "6",
        "7", "8", "9",
        ".", "0", "⌫"
    )

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        for (i in 0 until 4) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for (j in 0 until 3) {
                    val text = buttons[i * 3 + j]
                    NumButton(
                        text = text,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            when (text) {
                                "." -> onDot()
                                "⌫" -> onDelete()
                                else -> onDigit(text)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun NumButton(text: String, modifier: Modifier, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = Color.Transparent,
        modifier = modifier.height(56.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = text,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
    }
}

@Composable
fun SaveDateButtons(enabled: Boolean, onSave: (Int) -> Unit, onOpenCalendar: () -> Unit) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SecondaryDateButton(
            text = stringResource(R.string.yesterday),
            enabled = enabled,
            onClick = { onSave(-1) },
            modifier = Modifier.weight(1f)
        )
        PrimaryDateButton(
            text = stringResource(R.string.today),
            enabled = enabled,
            onClick = { onSave(0) },
            modifier = Modifier.weight(1.2f)
        )
        SecondaryDateButton(
            icon = Icons.Default.CalendarToday,
            text = stringResource(R.string.calendar),
            enabled = enabled,
            onClick = onOpenCalendar,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun PrimaryDateButton(text: String, enabled: Boolean, onClick: () -> Unit, modifier: Modifier) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ),
        modifier = modifier.height(56.dp)
    ) {
        Text(text, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun SecondaryDateButton(text: String = "", icon: ImageVector? = null, enabled: Boolean, onClick: () -> Unit, modifier: Modifier) {
    FilledTonalButton(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(16.dp),
        modifier = modifier.height(56.dp)
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
            if (text.isNotEmpty()) Spacer(Modifier.width(4.dp))
        }
        if (text.isNotEmpty()) {
            Text(text)
        }
    }
}

@Composable
fun QuickGridPanel(
    type: QuickAddType,
    sourceOptions: List<Option>,
    merchantOptions: List<Option>,
    categoryOptions: List<Option>,
    onDismiss: () -> Unit,
    onSelect: (QuickAddType, Option) -> Unit,
    onManage: (QuickAddType) -> Unit
) {
    val title = when (type) {
        QuickAddType.SOURCE -> stringResource(R.string.select_source)
        QuickAddType.MERCHANT -> stringResource(R.string.quick_choice_to_where)
        QuickAddType.CATEGORY -> stringResource(R.string.select_category)
        QuickAddType.TEMPLATES -> stringResource(R.string.templates_expense)
    }

    val items = when (type) {
        QuickAddType.SOURCE -> sourceOptions
        QuickAddType.MERCHANT -> merchantOptions
        QuickAddType.CATEGORY -> categoryOptions
        QuickAddType.TEMPLATES -> listOf(
            Option("t_lidl", "Lidl"),
            Option("t_maxi", "Maxi"),
            Option("t_coffee", "Coffee"),
            Option("t_taxi", "Taxi"),
            Option("t_netflix", "Netflix"),
            Option("t_salary", "Salary")
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.4f))
            .clickable { onDismiss() }
    ) {
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .clickable(enabled = false) {},
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 4.dp
        ) {
            Column(
                Modifier
                    .padding(24.dp)
                    .navigationBarsPadding()
            ) {
                Box(
                    Modifier
                        .width(32.dp)
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                        .align(Alignment.CenterHorizontally)
                )
                Spacer(Modifier.height(24.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(24.dp))
                
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.heightIn(max = 300.dp)
                ) {
                    items(items) { item ->
                        GridCard(item.label, onClick = { onSelect(type, item) })
                    }
                    if (type != QuickAddType.TEMPLATES) {
                        item {
                            ActionCard(
                                title = if (items.isEmpty()) "+" else "…",
                                onClick = { onManage(type) }
                            )
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun GridCard(title: String, subtitle: String? = null, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier.height(72.dp)
    ) {
        Column(
            Modifier.padding(12.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
fun ActionCard(title: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = Color.Transparent,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.height(72.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
