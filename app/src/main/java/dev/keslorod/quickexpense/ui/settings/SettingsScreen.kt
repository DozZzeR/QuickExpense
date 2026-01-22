package dev.keslorod.quickexpense.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import dev.keslorod.quickexpense.App
import dev.keslorod.quickexpense.ui.widget.hasAnyQuickExpenseWidget
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    app: App,
    onBack: () -> Unit,
    nav: NavHostController
) {
    val scope = rememberCoroutineScope()

    var currency by remember { mutableStateOf("RSD") }
    var period by remember { mutableStateOf("day") } // day/week/month/all
    var limitCents by remember { mutableStateOf(0L) }
    var showRemainder by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        currency = app.prefs.currencyFlow.first()
        period = app.prefs.widgetPeriodFlow.first()
        limitCents = app.prefs.widgetLimitCentsFlow.first()
        showRemainder = app.prefs.showRemainderInsteadOfExpenseFlow.first()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Настройки") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ChevronLeft, contentDescription = "Назад")
                    }
                })
            }
    ) { pad ->
        Column(Modifier
            .padding(pad)
            .fillMaxSize()
            .padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = { nav.navigate("manage_sources") },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Источники") }

            Button(
                onClick = { nav.navigate("manage_categories") },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Категории") }

            OutlinedTextField(
                value = currency,
                onValueChange = { currency = it.take(3).uppercase() },
                label = { Text("Валюта (код)") },
                singleLine = true
            )

            // Период
            Text("Период виджета")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("day" to "День", "week" to "Неделя", "month" to "Месяц", "all" to "Всё").forEach { (v, label) ->
                    FilterChip(
                        selected = period == v,
                        onClick = { period = v },
                        label = { Text(label) }
                    )
                }
            }

            // Лимит трат
            Text("Лимит трат (в центах, 0 = без лимита)")
            OutlinedTextField(
                value = if (limitCents == 0L) "" else (limitCents / 100).toString(),
                onValueChange = { 
                    limitCents = (it.toLongOrNull() ?: 0L) * 100
                },
                label = { Text("Лимит") },
                singleLine = true
            )

            // Переключатель остатка
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Показывать остаток вместо расхода")
                Checkbox(
                    checked = showRemainder,
                    onCheckedChange = { showRemainder = it }
                )
            }

            Spacer(Modifier.height(12.dp))
            Button(
                onClick = {
                    scope.launch {
                        app.prefs.setCurrency(currency)
                        app.prefs.setWidgetPeriod(period)
                        app.prefs.setWidgetLimitCents(limitCents)
                        app.prefs.setShowRemainderInsteadOfExpense(showRemainder)

                        // Попросим обновить виджет коалесированно
                        app.widgetRefresher.schedule()

                        onBack()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Сохранить") }

            WidgetControls(app)
        }
    }
}


@Composable
fun WidgetControls(app: App) {
    val ctx = LocalContext.current
    var hasWidget by remember { mutableStateOf(false) }

    // при входе на экран проверяем наличие
    LaunchedEffect(Unit) {
        hasWidget = hasAnyQuickExpenseWidget(ctx)
    }

    if (!hasWidget) {
        // Кнопка "Добавить", видна только если ни одного виджета нет
        Button(
            onClick = {
                val mgr = android.appwidget.AppWidgetManager.getInstance(ctx)
                val cn = android.content.ComponentName(
                    ctx,
                    dev.keslorod.quickexpense.ui.widget.QuickExpenseWidgetReceiver::class.java
                )
                if (mgr.isRequestPinAppWidgetSupported) {
                    val cb = android.app.PendingIntent.getActivity(
                        ctx, 0, android.content.Intent(),
                        android.app.PendingIntent.FLAG_IMMUTABLE
                    )
                    mgr.requestPinAppWidget(cn, null, cb)
                    // Сразу переключим состояние; окончательно подтвердится при следующем открытии
                    hasWidget = true
                } else {
                    android.widget.Toast.makeText(
                        ctx,
                        "Зажмите рабочий стол → Виджеты → QuickExpense",
                        android.widget.Toast.LENGTH_LONG
                    ).show()
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Добавить виджет на рабочий стол") }
    } else {
        // Если виджет уже есть — вместо «Добавить» покажем полезную кнопку
        OutlinedButton(
            onClick = {
                // коалесированный пересчёт+редроу (твой класс)
                app.widgetRefresher.schedule(300)
                android.widget.Toast.makeText(ctx, "Обновляю виджет…", android.widget.Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Обновить виджет сейчас") }
    }
}
