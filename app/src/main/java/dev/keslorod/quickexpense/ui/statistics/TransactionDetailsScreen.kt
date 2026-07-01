package dev.keslorod.quickexpense.ui.statistics

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CallSplit
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.keslorod.quickexpense.R
import dev.keslorod.quickexpense.domain.formatCents
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionDetailsScreen(
    expenseId: String,
    onBack: () -> Unit,
    onEditTransaction: (String) -> Unit,
    onEditSplit: (String) -> Unit
) {
    val viewModel: TransactionDetailsViewModel = viewModel(factory = TransactionDetailsViewModelFactory(
        application = androidx.compose.ui.platform.LocalContext.current.applicationContext as android.app.Application,
        expenseId = expenseId
    ))

    val uiState by viewModel.uiState.collectAsState()
    val currency by viewModel.currency.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.open_purchase)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    IconButton(onClick = { onEditTransaction(expenseId) }) {
                        Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.edit_purchase))
                    }
                }
            )
        }
    ) { pad ->
        when (val state = uiState) {
            is TransactionDetailsUiState.Loading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is TransactionDetailsUiState.Error -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(state.message, color = MaterialTheme.colorScheme.error)
                }
            }
            is TransactionDetailsUiState.Success -> {
                val data = state.data
                val exp = data.expense
                val sdf = remember { SimpleDateFormat("dd MMMM yyyy, HH:mm", Locale.getDefault()) }

                Column(
                    modifier = Modifier
                        .padding(pad)
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Merchant & Amount
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = data.merchantName ?: stringResource(R.string.unknown_merchant),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "${formatCents(exp.amount)} $currency",
                            style = MaterialTheme.typography.displayMedium,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = sdf.format(Date(exp.createdAt)),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    // Basic Info
                    InfoRow(stringResource(R.string.source), data.sourceName ?: "—")
                    InfoRow(stringResource(R.string.category), data.categoryName ?: stringResource(R.string.uncategorized))
                    if (!exp.note.isNullOrBlank()) {
                        InfoRow(stringResource(R.string.note_label), exp.note)
                    }

                    if (data.splitNodes.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        Text(stringResource(R.string.breakdown), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        
                        // Simple list of split nodes
                        data.splitNodes.filter { it.parentId == null }.forEach { rootNode ->
                            SplitNodeItem(rootNode, data.splitNodes, data.nodeTags, currency)
                        }
                    }

                    Spacer(Modifier.height(24.dp))
                    
                    OutlinedButton(
                        onClick = { onEditSplit(expenseId) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Icon(Icons.Default.CallSplit, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.edit_split))
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun SplitNodeItem(
    node: dev.keslorod.quickexpense.data.entities.SplitNode,
    allNodes: List<dev.keslorod.quickexpense.data.entities.SplitNode>,
    nodeTags: Map<String, List<dev.keslorod.quickexpense.data.entities.Tag>>,
    currency: String
) {
    val children = allNodes.filter { it.parentId == node.id }
    
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(node.label ?: "Позиция", style = MaterialTheme.typography.bodyLarge)
                val tags = nodeTags[node.id].orEmpty()
                if (tags.isNotEmpty()) {
                    Text(
                        tags.joinToString(" ") { "#${it.name}" },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Text("${formatCents(node.amount)} $currency", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
        }
        
        if (children.isNotEmpty()) {
            Column(modifier = Modifier.padding(start = 16.dp)) {
                children.forEach { child ->
                    SplitNodeItem(child, allNodes, nodeTags, currency)
                }
            }
        }
    }
}
