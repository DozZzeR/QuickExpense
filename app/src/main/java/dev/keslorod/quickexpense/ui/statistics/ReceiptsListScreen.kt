package dev.keslorod.quickexpense.ui.statistics

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import dev.keslorod.quickexpense.App
import dev.keslorod.quickexpense.R
import dev.keslorod.quickexpense.data.entities.Expense
import dev.keslorod.quickexpense.domain.RECEIPT_TAG_ID
import dev.keslorod.quickexpense.domain.formatCents
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Every expense tagged as having a receipt — the "find them all" half of the receipt feature
 * (viewing/exporting one lives in [TransactionDetailsScreen]). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceiptsListScreen(
    app: App,
    onBack: () -> Unit,
    onOpenExpense: (String) -> Unit
) {
    var isLoading by remember { mutableStateOf(true) }
    var expenses by remember { mutableStateOf<List<Expense>>(emptyList()) }
    var merchantNames by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var currency by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        val loaded = withContext(Dispatchers.IO) {
            Triple(
                app.db.expenseTags().getExpensesByTag(RECEIPT_TAG_ID),
                app.db.merchants().all().associate { it.id to it.name },
                app.prefs.currencyFlow.first()
            )
        }
        expenses = loaded.first
        merchantNames = loaded.second
        currency = loaded.third
        isLoading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.receipts)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                }
            )
        }
    ) { pad ->
        when {
            isLoading -> {
                Box(Modifier.padding(pad).fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            expenses.isEmpty() -> {
                Box(Modifier.padding(pad).fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.no_receipts), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            else -> {
                val sdf = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
                val context = LocalContext.current
                LazyColumn(
                    modifier = Modifier.padding(pad).fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(expenses, key = { it.id }) { exp ->
                        val firstPhotoPath = exp.photoPaths?.split("|")?.firstOrNull { it.isNotBlank() }
                        Surface(
                            onClick = { onOpenExpense(exp.id) },
                            shape = MaterialTheme.shapes.medium,
                            tonalElevation = 1.dp
                        ) {
                            Row(
                                Modifier.padding(12.dp).fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val file = firstPhotoPath?.let { File(it) }
                                if (file != null && file.exists()) {
                                    val uri = remember(firstPhotoPath) {
                                        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                                    }
                                    AsyncImage(
                                        model = uri,
                                        contentDescription = null,
                                        modifier = Modifier.size(48.dp).clip(MaterialTheme.shapes.small),
                                        contentScale = ContentScale.Crop
                                    )
                                    Spacer(Modifier.width(12.dp))
                                }
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        merchantNames[exp.merchantId] ?: stringResource(R.string.unknown_merchant),
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        sdf.format(Date(exp.createdAt)),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text("${formatCents(exp.amount)} $currency", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
