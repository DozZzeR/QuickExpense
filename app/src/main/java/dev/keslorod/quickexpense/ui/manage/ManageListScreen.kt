package dev.keslorod.quickexpense.ui.manage

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> ManageListScreen(
    title: String,
    onBack: () -> Unit,
    // извлечение полей
    getName: (T) -> String,
    isFavorite: (T) -> Boolean,
    itemKey: (T) -> Any,
    // операции
    loadAll: suspend () -> List<T>,
    addNew: suspend (name: String) -> Unit,
    toggleFavorite: suspend (item: T) -> Unit,
    rename: suspend (item: T, newName: String) -> Unit,
    deleteIfUnused: suspend (item: T) -> Boolean, // true — удалили; false — есть ссылки
) {
    val scope = rememberCoroutineScope()
    var items by remember { mutableStateOf<List<T>>(emptyList()) }

    // Диалоги
    var showAdd by remember { mutableStateOf(false) }
    var editingItem by remember { mutableStateOf<T?>(null) }
    var editingText by remember { mutableStateOf("") }

    val snackbarHostState = remember { SnackbarHostState() }

    fun reload() = scope.launch(Dispatchers.IO) { items = loadAll() }
    LaunchedEffect(Unit) { reload() }

    // Набор имён для валидации дублей (trim+lowercase)
    val existingNames = remember(items) {
        items.map { getName(it).trim().lowercase() }.toSet()
    }

    // Валидатор имени: возвращает строку-ошибку или null, если всё ок
    val validateName: (proposed: String, originalName: String?) -> String? = { proposed, originalName ->
        val p = proposed.trim()
        when {
            p.isEmpty() -> "Введите название"
            // если редактирование и имя не меняется — ок
            originalName != null && p.equals(originalName.trim(), ignoreCase = true) -> null
            // иначе проверяем, что такого имени нет среди существующих
            existingNames.contains(p.lowercase()) -> "Такое имя уже есть"
            else -> null
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ChevronLeft, contentDescription = "Назад")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    editingItem = null
                    editingText = ""
                    showAdd = true
                }
            ) { Text("Добавить") }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { pad ->
        Column(
            Modifier
                .padding(pad)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            LazyColumn {
                items(items, key = itemKey) { item ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            getName(item),
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.weight(1f)
                        )
                        Row {
                            IconButton(onClick = {
                                editingItem = item
                                editingText = getName(item)
                            }) {
                                Icon(Icons.Filled.Edit, contentDescription = "Переименовать")
                            }
                            IconButton(onClick = {
                                scope.launch(Dispatchers.IO) {
                                    toggleFavorite(item)
                                    reload()
                                }
                            }) {
                                if (isFavorite(item))
                                    Icon(Icons.Filled.Star, contentDescription = "Убрать из избранного")
                                else
                                    Icon(Icons.Outlined.StarBorder, contentDescription = "В избранное")
                            }
                            IconButton(onClick = {
                                scope.launch(Dispatchers.IO) {
                                    val deleted = deleteIfUnused(item)
                                    if (deleted) {
                                        reload()
                                    } else {
                                        scope.launch {
                                            snackbarHostState.showSnackbar(
                                                "Нельзя удалить: есть операции с этой записью",
                                                withDismissAction = true
                                            )
                                        }
                                    }
                                }
                            }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Удалить")
                            }
                        }
                    }
                    HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)
                }
            }
        }
    }

    // Диалог: Добавить
    if (showAdd) {
        NameDialog(
            title = "Добавить",
            initial = "",
            confirmText = "Создать",
            validator = { name -> validateName(name, null) },
            onCancel = { showAdd = false },
            onConfirm = { name ->
                scope.launch(Dispatchers.IO) {
                    addNew(name.trim())
                    showAdd = false
                    reload()
                }
            }
        )
    }


    // Диалог: Переименовать
    editingItem?.let { item ->
        val originalName = getName(item)
        NameDialog(
            title = "Переименовать",
            initial = editingText,
            confirmText = "Сохранить",
            validator = { name -> validateName(name, originalName) },
            onCancel = { editingItem = null },
            onConfirm = { newName ->
                scope.launch(Dispatchers.IO) {
                    rename(item, newName.trim())
                    editingItem = null
                    reload()
                }
            }
        )
    }
}

/** Универсальный диалог ввода имени с inline-валидацией (пусто/дубликат). */
@Composable
private fun NameDialog(
    title: String,
    initial: String,
    confirmText: String,
    validator: (String) -> String?, // null = валидно; иначе текст ошибки
    onCancel: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var text by remember { mutableStateOf(initial) }
    val error: String? = validator(text)
    val canSave = error == null

    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    singleLine = true,
                    placeholder = { Text("Название") },
                    isError = error != null,
                    supportingText = { if (error != null) Text(error) },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                        onDone = { if (canSave) onConfirm(text) }
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(text) },
                enabled = canSave
            ) { Text(confirmText) }
        },
        dismissButton = {
            TextButton(onClick = onCancel) { Text("Отмена") }
        }
    )
}

