package dev.keslorod.quickexpense.ui.manage

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import dev.keslorod.quickexpense.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

enum class ListScreenMode {
    SELECT,   // Выбор элемента (из добавления траты)
    MANAGE    // Управление (из настроек)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> ManageListScreen(
    title: String,
    onBack: () -> Unit,
    mode: ListScreenMode = ListScreenMode.MANAGE,
    onSelect: ((T) -> Unit)? = null, // вызывается при тапе на элемент в SELECT mode
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
    var selectedItemId by remember { mutableStateOf<Any?>(null) }

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

    // Вычисляем строки ошибок один раз в Composable контексте
    val errorNameEmpty = stringResource(R.string.error_name_empty)
    val errorNameExists = stringResource(R.string.error_name_exists)

    // Валидатор имени: возвращает строку-ошибку или null, если всё ок
    val validateName: (proposed: String, originalName: String?) -> String? = { proposed, originalName ->
        val p = proposed.trim()
        when {
            p.isEmpty() -> errorNameEmpty
            // если редактирование и имя не меняется — ок
            originalName != null && p.equals(originalName.trim(), ignoreCase = true) -> null
            // иначе проверяем, что такого имени нет среди существующих
            existingNames.contains(p.lowercase()) -> errorNameExists
            else -> null
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ChevronLeft, contentDescription = stringResource(R.string.back))
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
            ) { Text(if (mode == ListScreenMode.SELECT) stringResource(R.string.add_item) else stringResource(R.string.add_item_short)) }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { pad ->
        Column(
            Modifier
                .padding(pad)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(items, key = itemKey) { item ->
                    val isSelected = itemKey(item) == selectedItemId
                    val itemColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                    
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .background(
                                color = itemColor,
                                shape = MaterialTheme.shapes.medium
                            )
                            .padding(12.dp)
                            .clickable(enabled = mode == ListScreenMode.SELECT) {
                                if (mode == ListScreenMode.SELECT) {
                                    selectedItemId = itemKey(item)
                                    onSelect?.invoke(item)
                                }
                            },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            getName(item),
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.weight(1f)
                        )
                        Row {
                            // В SELECT режиме кнопка редактирования компактнее
                            if (mode == ListScreenMode.SELECT) {
                                IconButton(
                                    onClick = {
                                        editingItem = item
                                        editingText = getName(item)
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.rename), modifier = Modifier.size(18.dp))
                                }
                            } else {
                                // В MANAGE режиме все кнопки
                                IconButton(onClick = {
                                    editingItem = item
                                    editingText = getName(item)
                                }) {
                                    Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.rename))
                                }
                                IconButton(onClick = {
                                    scope.launch(Dispatchers.IO) {
                                        toggleFavorite(item)
                                        reload()
                                    }
                                }) {
                                    if (isFavorite(item))
                                        Icon(Icons.Filled.Star, contentDescription = stringResource(R.string.remove_from_favorites))
                                    else
                                        Icon(Icons.Outlined.StarBorder, contentDescription = stringResource(R.string.add_to_favorites))
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
                                    Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.delete))
                                }
                            }
                            
                            // В SELECT режиме звезда всегда видна справа
                            if (mode == ListScreenMode.SELECT) {
                                IconButton(
                                    onClick = {
                                        scope.launch(Dispatchers.IO) {
                                            toggleFavorite(item)
                                            reload()
                                        }
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    if (isFavorite(item))
                                        Icon(Icons.Filled.Star, contentDescription = stringResource(R.string.remove_from_favorites), modifier = Modifier.size(18.dp))
                                    else
                                        Icon(Icons.Outlined.StarBorder, contentDescription = stringResource(R.string.add_to_favorites), modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Диалог: Добавить
    if (showAdd) {
        NameDialog(
            title = stringResource(R.string.dialog_add_title),
            initial = "",
            confirmText = stringResource(R.string.dialog_add_confirm),
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
            title = stringResource(R.string.dialog_rename_title),
            initial = editingText,
            confirmText = stringResource(R.string.dialog_rename_confirm),
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
                    placeholder = { Text(stringResource(R.string.error_name_empty)) },
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
            TextButton(onClick = onCancel) { Text(stringResource(R.string.cancel)) }
        }
    )
}

