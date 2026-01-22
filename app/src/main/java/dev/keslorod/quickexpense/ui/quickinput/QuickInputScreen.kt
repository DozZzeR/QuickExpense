package dev.keslorod.quickexpense.ui.quickinput

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.keslorod.quickexpense.App
import dev.keslorod.quickexpense.data.entities.Category
import dev.keslorod.quickexpense.data.entities.Source
import dev.keslorod.quickexpense.ui.manage.ListScreenMode
import dev.keslorod.quickexpense.ui.manage.ManageListScreen
import kotlinx.coroutines.launch

private const val MAX_RECENT_CATEGORIES = 3
private const val MAX_RECENT_SOURCES = 3

data class Option(val id: String, val label: String)

@Composable
fun QuickInputScreen(
    app: App,
    currency: String,
    sourceOptions: List<Option>,
    categoryQuickOptions: List<Option>,
    onConfirm: (cents: Long, sourceId: String, categoryId: String) -> Unit,
    onCancel: () -> Unit
) {
    var amountText by remember { mutableStateOf("") }
    var source by remember { mutableStateOf<Option?>(null) }
    var category by remember { mutableStateOf<Option?>(null) }
    var showCategorySelector by remember { mutableStateOf(false) }
    var showSourceSelector by remember { mutableStateOf(false) }
    var recentCategories by remember { mutableStateOf<List<Option>>(emptyList()) }  // недавние выборы категорий
    var currentFavoritesCategories by remember { mutableStateOf(categoryQuickOptions) }  // текущий список избранных категорий
    var recentSources by remember { mutableStateOf<List<Option>>(emptyList()) }  // недавние выборы источников
    var currentFavoritesSources by remember { mutableStateOf(sourceOptions) }  // текущий список избранных источников
    val categoryListState = rememberLazyListState()  // для скролла списка категорий
    val sourceListState = rememberLazyListState()  // для скролла списка источников
    val scope = rememberCoroutineScope()  // для выполнения скролла

    // Перезапрашиваем избранные категории при закрытии selector
    LaunchedEffect(showCategorySelector) {
        if (!showCategorySelector) {
            val freshFavorites = app.db.categories().favorites()
            currentFavoritesCategories = freshFavorites.map { Option(it.id, it.name) }
        }
    }
    
    // Перезапрашиваем избранные источники при закрытии selector
    LaunchedEffect(showSourceSelector) {
        if (!showSourceSelector) {
            val freshFavorites = app.db.sources().favorites()
            currentFavoritesSources = freshFavorites.map { Option(it.id, it.name) }
        }
    }
    
    // Скроллим в начало при выборе категории
    LaunchedEffect(category) {
        if (category != null) {
            scope.launch {
                categoryListState.animateScrollToItem(0)
            }
        }
    }
    
    // Скроллим в начало при выборе источника
    LaunchedEffect(source) {
        if (source != null) {
            scope.launch {
                sourceListState.animateScrollToItem(0)
            }
        }
    }

    fun toCents(txt: String): Long {
        if (txt.isBlank()) return 0
        val parts = txt.split(',', limit = 2)
        val major = parts[0].ifBlank { "0" }
        val minor = (parts.getOrNull(1) ?: "").padEnd(2, '0').take(2)
        return (major.toLongOrNull() ?: 0L) * 100 + (minor.toLongOrNull() ?: 0L)
    }

    if (showCategorySelector) {
        ManageListScreen<Category>(
            title = "Выбрать категорию",
            mode = ListScreenMode.SELECT,
            onSelect = { selectedCategory ->
                val option = Option(selectedCategory.id, selectedCategory.name)
                category = option
                
                // Добавляем в recent
                recentCategories = (listOf(option) + recentCategories).take(MAX_RECENT_CATEGORIES)
                showCategorySelector = false
            },
            onBack = { showCategorySelector = false },
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
    } else if (showSourceSelector) {
        ManageListScreen<Source>(
            title = "Выбрать источник",
            mode = ListScreenMode.SELECT,
            onSelect = { selectedSource ->
                val option = Option(selectedSource.id, selectedSource.name)
                source = option
                
                // Добавляем в recent
                recentSources = (listOf(option) + recentSources).take(MAX_RECENT_SOURCES)
                showSourceSelector = false
            },
            onBack = { showSourceSelector = false },
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
    } else {
        Column(
            Modifier.fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Источник", fontWeight = FontWeight.Medium)
                IconButton(onClick = { showSourceSelector = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Все источники")
                }
            }
            Spacer(Modifier.height(8.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                state = sourceListState
            ) {
                // Недавние выборы
                items(recentSources) { opt ->
                    val isSelected = source?.id == opt.id
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            source = opt
                        },
                        label = { Text(opt.label) },
                        modifier = Modifier.graphicsLayer {
                            alpha = if (isSelected || source == null) 1f else 0.65f
                        }
                    )
                }

                // Большой спейс между recent и favorites (если есть оба)
                if (recentSources.isNotEmpty()) {
                    item {
                        Spacer(Modifier.width(16.dp))  // х3 от стандартного 8.dp
                    }
                }

                // Избранные (кроме тех что уже в recent)
                val favoritesSourcesToShow = currentFavoritesSources.filter { fav ->
                    recentSources.none { it.id == fav.id }
                }
                items(favoritesSourcesToShow) { opt ->
                    val isSelected = source?.id == opt.id
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            source = opt
                            recentSources = (listOf(opt) + recentSources).take(MAX_RECENT_SOURCES)
                        },
                        label = { Text(opt.label) },
                        modifier = Modifier.graphicsLayer {
                            alpha = if (isSelected || source == null) 1f else 0.65f
                        }
                    )
                }
            }

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
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Категория", fontWeight = FontWeight.Medium)
                IconButton(onClick = { showCategorySelector = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Все категории")
                }
            }
            Spacer(Modifier.height(8.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                state = categoryListState
            ) {
                // Недавние выборы
                items(recentCategories) { opt ->
                    val isSelected = category?.id == opt.id
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            category = opt
                        },
                        label = { Text(opt.label) },
                        modifier = Modifier.graphicsLayer {
                            alpha = if (isSelected || category == null) 1f else 0.65f
                        }
                    )
                }

                // Большой спейс между recent и favorites (если есть оба)
                if (recentCategories.isNotEmpty()) {
                    item {
                        Spacer(Modifier.width(16.dp))  // х3 от стандартного 8.dp
                    }
                }

                // Избранные (кроме тех что уже в recent)
                val favoritesToShow = currentFavoritesCategories.filter { fav ->
                    recentCategories.none { it.id == fav.id }
                }
                items(favoritesToShow) { opt ->
                    val isSelected = category?.id == opt.id
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            category = opt
                            recentCategories = (listOf(opt) + recentCategories).take(MAX_RECENT_CATEGORIES)
                        },
                        label = { Text(opt.label) },
                        modifier = Modifier.graphicsLayer {
                            alpha = if (isSelected || category == null) 1f else 0.65f
                        }
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
            val cents = toCents(amountText)
            val isFormValid = source != null && category != null && cents > 0
            Button(
                onClick = {
                    val sId = source?.id ?: return@Button
                    val cId = category?.id ?: return@Button
                    onConfirm(cents, sId, cId)
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = isFormValid
            ) { Text("OK — сохранить") }
            OutlinedButton(
                onClick = { onCancel() },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Отмена") }
        }
    }
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

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            key("1") { onDigit("1") }; key("2") { onDigit("2") }; key("3") { onDigit("3") }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            key("4") { onDigit("4") }; key("5") { onDigit("5") }; key("6") { onDigit("6") }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            key("7") { onDigit("7") }; key("8") { onDigit("8") }; key("9") { onDigit("9") }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            key(",") { onSep() }; key("0") { onDigit("0") }; key("⌫") { onBack() }
        }
    }
}

