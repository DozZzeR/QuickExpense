package dev.keslorod.quickexpense.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.keslorod.quickexpense.App
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    app: App,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()

    var currency by remember { mutableStateOf("RSD") }
    var period by remember { mutableStateOf("day") } // day/week/month/all
    var anchor by remember { mutableStateOf(1) }

    LaunchedEffect(Unit) {
        currency = app.prefs.currencyFlow.first()
        period = app.prefs.widgetPeriodFlow.first()
        anchor = app.prefs.monthAnchorDayFlow.first()
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Настройки") }) }
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {

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

            // Якорный день
            Text("Якорный день месяца: $anchor")
            Slider(
                value = anchor.toFloat(),
                onValueChange = { anchor = it.toInt().coerceIn(1, 28) },
                valueRange = 1f..28f
            )

            Spacer(Modifier.height(12.dp))
            Button(
                onClick = {
                    scope.launch {
                        app.prefs.setCurrency(currency)
                        app.prefs.setWidgetPeriod(period)
                        app.prefs.setMonthAnchorDay(anchor)

                        // Попросим обновить виджет коалесированно
                        app.widgetRefresher.schedule()

                        onBack()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Сохранить") }
        }
    }
}
