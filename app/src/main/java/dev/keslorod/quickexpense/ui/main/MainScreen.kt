package dev.keslorod.quickexpense.ui.main

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.keslorod.quickexpense.domain.formatCents
import dev.keslorod.quickexpense.export.enqueueExport
import dev.keslorod.quickexpense.ui.quickinput.QuickInputActivity
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onOpenSettings: () -> Unit
) {
    val vm: MainViewModel = viewModel()
    val state by vm.state.collectAsState()
    val items by vm.items.collectAsState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) { vm.load() }

    val ctx = LocalContext.current
    var menuOpen by remember { mutableStateOf(false) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                vm.load()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("QuickExpense") },
                actions = {
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Меню")
                    }
                    DropdownMenu(
                        expanded = menuOpen,
                        onDismissRequest = { menuOpen = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Экспорт CSV/ZIP") },
                            leadingIcon = { Icon(Icons.Default.Download, contentDescription = null) },
                            onClick = {
                                menuOpen = false
                                scope.launch {
                                    val r = vm.currentRange()   // suspend
                                    enqueueExport(ctx, r.from, r.to)
                                }
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Настройки") },
                            leadingIcon = { Icon(Icons.Default.Settings, contentDescription = null) },
                            onClick = {
                                menuOpen = false
                                onOpenSettings()
                            }
                        )
                    }
                }

            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { ctx.startActivity(Intent(ctx, QuickInputActivity::class.java)) }
            ) { Icon(Icons.Default.Add, contentDescription = "Добавить") }
        }
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize().padding(16.dp)) {
            Text("Сводка", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(6.dp))
            Text(
                "${formatCents(state.totalCents)} ${state.currency}",
                style = MaterialTheme.typography.headlineLarge
            )
            if (state.subtitle.isNotEmpty()) {
                Text(state.subtitle, style = MaterialTheme.typography.bodyMedium)
            }

            Spacer(Modifier.height(16.dp))
            Text("Последние операции", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(6.dp))
            if (items.isEmpty()) {
                Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    Text("Пока нет")
                }
            } else {
                LazyColumn(Modifier.fillMaxWidth().weight(1f)) {
                    items(items) { item ->
                        ExpenseRow(item, state.currency)
                    }
                }
            }
        }
    }
}

@Composable
private fun ExpenseRow(item: ExpenseItemUi, currency: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(item.title, style = MaterialTheme.typography.titleMedium, maxLines = 1)
            Text(item.subtitle, style = MaterialTheme.typography.bodySmall, maxLines = 1)
        }
        // сумма справа: одна строка, без переносов
        Text(
            text = "${formatCents(item.amount)} $currency",
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
            softWrap = false,
            modifier = Modifier
                .padding(start = 12.dp)
                .widthIn(min = 96.dp), // фиксируем минимальную ширину
        )
    }
    HorizontalDivider()
}
