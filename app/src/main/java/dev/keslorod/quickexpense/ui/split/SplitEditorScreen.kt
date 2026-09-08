package dev.keslorod.quickexpense.ui.split

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import dev.keslorod.quickexpense.App
import dev.keslorod.quickexpense.R
import dev.keslorod.quickexpense.data.entities.SplitNode
import dev.keslorod.quickexpense.data.entities.Tag
import dev.keslorod.quickexpense.domain.formatCents
import dev.keslorod.quickexpense.ui.manage.ListScreenMode
import dev.keslorod.quickexpense.ui.manage.ManageCategoriesScreen
import dev.keslorod.quickexpense.ui.manage.ManageTagsScreen
import dev.keslorod.quickexpense.ui.quickinput.Option
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SplitEditorScreen(
    app: App,
    expenseId: String? = null,
    totalAmount: Long,
    currency: String,
    initialNodes: List<SplitNode> = emptyList(),
    initialTags: Map<String, List<Tag>> = emptyMap(),
    initialLabel: String = "Transaction",
    // The category picked on the main screen before splitting, offered as the shared
    // default for every top-level row until one row's category is changed away from it.
    defaultCategoryId: String? = null,
    initialDiverged: Boolean = false,
    onDivergedChange: (Boolean) -> Unit = {},
    onDone: (List<SplitNode>, Map<String, List<Tag>>) -> Unit,
    onBack: () -> Unit
) {
    // Все узлы в плоском списке (храним в памяти во время редактирования)
    var allNodes by remember { mutableStateOf(initialNodes) }

    // Метки для каждого узла
    var nodeTags by remember { mutableStateOf<Map<String, List<Tag>>>(initialTags) }

    // Текущий путь (для drill-down)
    var currentParentId by remember { mutableStateOf<String?>(null) }

    var isLoading by remember { mutableStateOf(expenseId != null && initialNodes.isEmpty()) }

    // True once a top-level row's category has diverged from defaultCategoryId — see the
    // param doc above and the onSave handler below.
    var diverged by remember { mutableStateOf(initialDiverged) }

    // Leaving with unsaved edits (rows added/changed since this screen opened) should be
    // a deliberate choice, not a silent discard.
    val hasUnsavedChanges = allNodes != initialNodes || nodeTags != initialTags
    var showDiscardConfirm by remember { mutableStateOf(false) }
    val requestExit: () -> Unit = {
        if (hasUnsavedChanges) showDiscardConfirm = true else onBack()
    }
    BackHandler(enabled = currentParentId == null) { requestExit() }

    LaunchedEffect(expenseId) {
        if (expenseId != null && initialNodes.isEmpty()) {
            val nodes = app.db.splitNodes().getByExpenseId(expenseId)
            val tagsMap = mutableMapOf<String, List<Tag>>()
            nodes.forEach { node ->
                tagsMap[node.id] = app.db.splitNodeTags().getTagsForSplitNode(node.id)
            }
            allNodes = nodes
            nodeTags = tagsMap
            isLoading = false
        }
    }

    if (isLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }
    
    // Вычисляем текущий узел и его данные
    val currentParent = allNodes.find { it.id == currentParentId }
    val currentTotal = currentParent?.amount ?: totalAmount
    val currentLabel = currentParent?.label ?: initialLabel
    
    val children = allNodes.filter { it.parentId == currentParentId }
    val allocatedAmount = children.sumOf { it.amount }
    val remainingAmount = currentTotal - allocatedAmount
    
    // Диалог редактирования элемента
    var editingNode by remember { mutableStateOf<SplitNode?>(null) }
    var showEditor by remember { mutableStateOf(false) }

    if (showDiscardConfirm) {
        AlertDialog(
            onDismissRequest = { showDiscardConfirm = false },
            title = { Text(stringResource(R.string.discard_changes_title)) },
            text = { Text(stringResource(R.string.discard_changes_message)) },
            confirmButton = {
                TextButton(onClick = { showDiscardConfirm = false; onBack() }) {
                    Text(stringResource(R.string.discard))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardConfirm = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(currentLabel, style = MaterialTheme.typography.titleMedium)
                        Text(
                            stringResource(R.string.split_remaining_fmt, formatCents(remainingAmount), currency),
                            style = MaterialTheme.typography.bodySmall,
                            color = if (remainingAmount < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (currentParentId == null) requestExit()
                        else currentParentId = currentParent?.parentId
                    }) {
                        Icon(if (currentParentId == null) Icons.Default.Close else Icons.Default.ChevronLeft, contentDescription = null)
                    }
                }
            )
        },
        bottomBar = {
            // All primary actions for this screen live here, at the bottom — matching
            // where the rest of the app anchors its save/confirm controls.
            Surface(tonalElevation = 2.dp) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .navigationBarsPadding()
                ) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = {
                                editingNode = null
                                showEditor = true
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.split_add))
                        }

                        if (remainingAmount > 0) {
                            OutlinedButton(
                                onClick = {
                                    val newNode = SplitNode(
                                        expenseId = "",
                                        parentId = currentParentId,
                                        amount = remainingAmount,
                                        categoryId = if (currentParentId == null && !diverged) defaultCategoryId else null,
                                        depth = (currentParent?.depth ?: 0) + 1
                                    )
                                    allNodes = allNodes + newNode
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(stringResource(R.string.split_fill_rest))
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = { onDone(allNodes, nodeTags) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.done))
                    }
                }
            }
        }
    ) { pad ->
        LazyColumn(
            Modifier
                .padding(pad)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(children) { node ->
                SplitNodeItem(
                    node = node,
                    currency = currency,
                    tags = nodeTags[node.id].orEmpty(),
                    onClick = {
                        editingNode = node
                        showEditor = true
                    },
                    onDrillDown = {
                        if (node.depth < 3) {
                            currentParentId = node.id
                        }
                    }
                )
            }
            
            if (remainingAmount > 0) {
                item {
                    UnallocatedItem(amount = remainingAmount, currency = currency)
                }
            }
        }
    }
    
    if (showEditor) {
        SplitItemEditorSheet(
            app = app,
            initialNode = editingNode,
            parentId = currentParentId,
            maxAmount = remainingAmount + (editingNode?.amount ?: 0L),
            currency = currency,
            initialTags = editingNode?.let { nodeTags[it.id] }.orEmpty(),
            defaultCategoryIdForNew = if (currentParentId == null && !diverged) defaultCategoryId else null,
            onDismiss = { showEditor = false },
            onSave = { node, tags ->
                if (editingNode != null) {
                    allNodes = allNodes.map { if (it.id == node.id) node else it }
                } else {
                    allNodes = allNodes + node.copy(depth = (currentParent?.depth ?: 0) + 1)
                }
                nodeTags = nodeTags + (node.id to tags)
                // A top-level row diverging from the shared default breaks the sharing
                // for the rest of this split — see the defaultCategoryId param doc.
                if (currentParentId == null && !diverged && node.categoryId != defaultCategoryId) {
                    diverged = true
                    onDivergedChange(true)
                }
                showEditor = false
            },
            onDelete = { node ->
                // Delete the node together with its whole subtree, otherwise
                // grandchildren stay behind pointing at a parentId that no longer exists.
                val toRemove = collectSubtreeIds(node.id, allNodes)
                allNodes = allNodes.filter { it.id !in toRemove }
                nodeTags = nodeTags - toRemove
                showEditor = false
            }
        )
    }
}

@Composable
fun SplitNodeItem(
    node: SplitNode,
    currency: String,
    tags: List<Tag>,
    onClick: () -> Unit,
    onDrillDown: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 1.dp
    ) {
        Row(
            Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(node.label ?: stringResource(R.string.split_untitled), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                if (tags.isNotEmpty()) {
                    Text(
                        tags.joinToString(" ") { "#${it.name}" },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            Column(horizontalAlignment = Alignment.End) {
                Text("${formatCents(node.amount)} $currency", style = MaterialTheme.typography.bodyLarge)
                if (node.depth < 3) {
                    IconButton(onClick = onDrillDown, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.ChevronRight, contentDescription = null)
                    }
                }
            }
        }
    }
}

@Composable
fun UnallocatedItem(amount: Long, currency: String) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(stringResource(R.string.split_unallocated), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("${formatCents(amount)} $currency", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SplitItemEditorSheet(
    app: App,
    initialNode: SplitNode?,
    parentId: String?,
    maxAmount: Long,
    currency: String,
    initialTags: List<Tag>,
    defaultCategoryIdForNew: String? = null,
    onDismiss: () -> Unit,
    onSave: (SplitNode, List<Tag>) -> Unit,
    onDelete: (SplitNode) -> Unit
) {
    var amountText by remember { mutableStateOf(initialNode?.let { formatCents(it.amount, '.') } ?: "") }
    var label by remember { mutableStateOf(initialNode?.label ?: "") }
    var selectedCategoryId by remember { mutableStateOf(initialNode?.categoryId ?: defaultCategoryIdForNew) }
    var categoryName by remember { mutableStateOf<String?>(null) }
    var tags by remember { mutableStateOf(initialTags) }
    
    var showCategoryPicker by remember { mutableStateOf(false) }
    var showTagPicker by remember { mutableStateOf(false) }

    LaunchedEffect(selectedCategoryId) {
        selectedCategoryId?.let { id ->
            categoryName = app.db.categories().all().find { it.id == id }?.name
        }
    }

    // Tapping the scrim / swiping down / pressing back all resolve to the same
    // "dismiss" request — treat that uniformly as a cancel, and if anything was typed,
    // confirm before throwing it away instead of closing silently (see conversation).
    val initialAmountText = remember { initialNode?.let { formatCents(it.amount, '.') } ?: "" }
    val initialLabelText = remember { initialNode?.label ?: "" }
    val initialCategoryId = remember { initialNode?.categoryId ?: defaultCategoryIdForNew }
    val hasChanges = amountText != initialAmountText || label != initialLabelText ||
        selectedCategoryId != initialCategoryId || tags != initialTags
    var showSheetDiscardConfirm by remember { mutableStateOf(false) }
    val requestDismiss: () -> Unit = {
        if (hasChanges) showSheetDiscardConfirm = true else onDismiss()
    }

    if (showSheetDiscardConfirm) {
        AlertDialog(
            onDismissRequest = { showSheetDiscardConfirm = false },
            title = { Text(stringResource(R.string.discard_changes_title)) },
            text = { Text(stringResource(R.string.discard_changes_message)) },
            confirmButton = {
                TextButton(onClick = { showSheetDiscardConfirm = false; onDismiss() }) {
                    Text(stringResource(R.string.discard))
                }
            },
            dismissButton = {
                TextButton(onClick = { showSheetDiscardConfirm = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }


    if (showCategoryPicker) {
        ManageCategoriesScreen(
            app = app,
            onBack = { showCategoryPicker = false },
            mode = ListScreenMode.SELECT,
            onSelectCategory = { 
                selectedCategoryId = it.id
                categoryName = it.name
                showCategoryPicker = false
            }
        )
        return
    }

    if (showTagPicker) {
        ManageTagsScreen(
            app = app,
            onBack = { showTagPicker = false },
            mode = ListScreenMode.SELECT,
            onSelectTag = { tag ->
                if (tags.none { it.id == tag.id }) {
                    tags = tags + tag
                }
                showTagPicker = false
            }
        )
        return
    }

    ModalBottomSheet(onDismissRequest = requestDismiss) {
        Column(
            Modifier
                .padding(16.dp)
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
        ) {
            Text(stringResource(if (initialNode == null) R.string.split_add_item else R.string.split_edit_item), style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = amountText,
                onValueChange = { amountText = it },
                label = { Text(stringResource(R.string.split_amount)) },
                modifier = Modifier.fillMaxWidth(),
                suffix = { Text(currency) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
            )

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = label,
                onValueChange = { label = it },
                label = { Text(stringResource(R.string.split_name)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(Modifier.height(12.dp))

            OutlinedCard(
                onClick = { showCategoryPicker = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(stringResource(R.string.category), style = MaterialTheme.typography.bodyMedium)
                    Text(categoryName ?: stringResource(R.string.uncategorized), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(Modifier.height(12.dp))
            
            Text(stringResource(R.string.tags), style = MaterialTheme.typography.labelLarge)
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                tags.forEach { tag ->
                    InputChip(
                        selected = true,
                        onClick = { tags = tags.filter { it.id != tag.id } },
                        label = { Text(tag.name) },
                        trailingIcon = { Icon(Icons.Default.Close, contentDescription = null, Modifier.size(16.dp)) }
                    )
                }
                AssistChip(
                    onClick = { showTagPicker = true },
                    label = { Text(stringResource(R.string.split_add_tag)) }
                )
            }
            
            Spacer(Modifier.height(24.dp))
            
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (initialNode != null) {
                    OutlinedButton(
                        onClick = { onDelete(initialNode) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.delete))
                    }
                }
                
                val parsed = parseAmount(amountText)
                val canSave = parsed > 0 && parsed <= maxAmount

                Button(
                    onClick = {
                        val node = (initialNode ?: SplitNode(expenseId = "", parentId = parentId, amount = parsed)).copy(
                            amount = parsed,
                            label = label,
                            categoryId = selectedCategoryId
                        )
                        onSave(node, tags)
                    },
                    modifier = Modifier.weight(1f),
                    enabled = canSave
                ) {
                    Text(stringResource(R.string.save))
                }
            }
            if (parseAmount(amountText) > maxAmount) {
                Text(
                    stringResource(R.string.split_exceeds_fmt, formatCents(parseAmount(amountText) - maxAmount), currency),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

/**
 * Returns the id of [rootId] plus the ids of all its descendants (transitively),
 * so a whole split subtree can be removed at once.
 */
fun collectSubtreeIds(rootId: String, nodes: List<SplitNode>): Set<String> {
    val childrenByParent = nodes.groupBy { it.parentId }
    val result = mutableSetOf<String>()
    val stack = ArrayDeque<String>().apply { add(rootId) }
    while (stack.isNotEmpty()) {
        val id = stack.removeLast()
        if (!result.add(id)) continue
        childrenByParent[id]?.forEach { stack.add(it.id) }
    }
    return result
}

fun parseAmount(txt: String): Long {
    val clean = txt.replace(',', '.')
    val parts = clean.split('.', limit = 2)
    val major = parts[0].toLongOrNull() ?: 0L
    val minor = (parts.getOrNull(1) ?: "").padEnd(2, '0').take(2).toLongOrNull() ?: 0L
    return major * 100 + minor
}
