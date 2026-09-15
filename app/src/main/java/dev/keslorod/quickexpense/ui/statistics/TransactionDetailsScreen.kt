package dev.keslorod.quickexpense.ui.statistics

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import dev.keslorod.quickexpense.R
import dev.keslorod.quickexpense.domain.exportReceiptToGallery
import dev.keslorod.quickexpense.domain.formatCents
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionDetailsScreen(
    expenseId: String,
    onBack: () -> Unit,
    onEditTransaction: (String) -> Unit
) {
    val viewModel: TransactionDetailsViewModel = viewModel(factory = TransactionDetailsViewModelFactory(
        application = androidx.compose.ui.platform.LocalContext.current.applicationContext as android.app.Application,
        expenseId = expenseId
    ))

    val uiState by viewModel.uiState.collectAsState()
    val currency by viewModel.currency.collectAsState()

    // Editing (via onEditTransaction) is a separate Activity, not a nav destination on this
    // screen's own back stack — reload on resume so returning from a save shows the change,
    // same pattern as MainScreen's own history list.
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                viewModel.loadDetails()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

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

                    val receiptPaths = exp.photoPaths?.split("|")?.filter { it.isNotBlank() }.orEmpty()
                    if (receiptPaths.isNotEmpty()) {
                        ReceiptsSection(receiptPaths)
                    }

                    if (data.splitNodes.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        Text(stringResource(R.string.breakdown), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        
                        // Simple list of split nodes
                        data.splitNodes.filter { it.parentId == null }.forEach { rootNode ->
                            SplitNodeItem(rootNode, data.splitNodes, data.nodeTags, currency)
                        }
                    }
                    // Splitting is reached through Edit now (its own "Разбить" button, same as
                    // creating a new expense) rather than a direct shortcut from here.
                }
            }
        }
    }
}

@Composable
private fun ReceiptsSection(paths: List<String>) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val exportedMessage = stringResource(R.string.receipt_exported)
    val exportFailedMessage = stringResource(R.string.receipt_export_failed)
    var zoomedPath by remember { mutableStateOf<String?>(null) }

    Column {
        Text(stringResource(R.string.receipts), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(paths) { path ->
                val file = remember(path) { File(path) }
                if (file.exists()) {
                    val uri = remember(path) {
                        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                    }
                    Box(modifier = Modifier.size(120.dp)) {
                        AsyncImage(
                            model = uri,
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(MaterialTheme.shapes.medium)
                                .clickable { zoomedPath = path },
                            contentScale = ContentScale.Crop
                        )
                        IconButton(
                            onClick = {
                                coroutineScope.launch(Dispatchers.IO) {
                                    val ok = exportReceiptToGallery(context, file)
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(
                                            context,
                                            if (ok) exportedMessage else exportFailedMessage,
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            },
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(4.dp)
                                .background(Color.Black.copy(alpha = 0.45f), CircleShape)
                                .size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.Download,
                                contentDescription = stringResource(R.string.save_to_gallery),
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    val pathToZoom = zoomedPath
    if (pathToZoom != null) {
        ZoomableReceiptDialog(path = pathToZoom, onDismiss = { zoomedPath = null })
    }
}

/** Full-screen pinch-to-zoom/pan viewer for one receipt photo — opened by tapping its
 * thumbnail above. Scale/offset reset on their own each time since this whole dialog is torn
 * down when dismissed, rather than kept around and just hidden. */
@Composable
private fun ZoomableReceiptDialog(path: String, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val file = remember(path) { File(path) }
    val uri = remember(path) {
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var containerSize by remember { mutableStateOf(IntSize.Zero) }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black)
                .onSizeChanged { containerSize = it }
        ) {
            AsyncImage(
                model = uri,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        translationX = offset.x,
                        translationY = offset.y
                    )
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            val newScale = (scale * zoom).coerceIn(1f, 6f)
                            // Zoomed-out (or back to 1x) has nothing to pan to — clamp panning
                            // room to how far the scaled image actually extends past the
                            // viewport, otherwise it could be dragged arbitrarily far off-screen
                            // with no way back short of closing and reopening this dialog.
                            val maxX = (containerSize.width * (newScale - 1) / 2f).coerceAtLeast(0f)
                            val maxY = (containerSize.height * (newScale - 1) / 2f).coerceAtLeast(0f)
                            scale = newScale
                            offset = Offset(
                                (offset.x + pan.x).coerceIn(-maxX, maxX),
                                (offset.y + pan.y).coerceIn(-maxY, maxY)
                            )
                        }
                    }
            )
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .background(Color.Black.copy(alpha = 0.45f), CircleShape)
            ) {
                Icon(Icons.Default.Close, contentDescription = stringResource(R.string.close), tint = Color.White)
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
                // Same reasoning as SplitEditorScreen's SplitNodeItem: a null label is a real
                // (if unnamed) item, not the "Unallocated" bucket, which is a distinct concept.
                Text(node.label ?: stringResource(R.string.split_default_name), style = MaterialTheme.typography.bodyLarge)
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
