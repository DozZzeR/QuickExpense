package dev.keslorod.quickexpense.ui.settings

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import dev.keslorod.quickexpense.App
import dev.keslorod.quickexpense.BuildConfig
import dev.keslorod.quickexpense.R
import dev.keslorod.quickexpense.domain.Period
import dev.keslorod.quickexpense.domain.getLabel
import dev.keslorod.quickexpense.ui.widget.hasAnyQuickExpenseWidget
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    app: App,
    onBack: () -> Unit,
    nav: NavHostController
) {
    val scope = rememberCoroutineScope()
    val ctx = LocalContext.current

    var currency by remember { mutableStateOf("RSD") }
    var period by remember { mutableStateOf("day") } // day/week/month/all
    var limitCents by remember { mutableStateOf(0L) }
    var showRemainder by remember { mutableStateOf(false) }
    var languageCode by remember { mutableStateOf("") }
    var previousLanguageCode by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        currency = app.prefs.currencyFlow.first()
        period = app.prefs.widgetPeriodFlow.first()
        limitCents = app.prefs.widgetLimitCentsFlow.first()
        showRemainder = app.prefs.showRemainderInsteadOfExpenseFlow.first()
        languageCode = app.prefs.languageCodeFlow.first()
        previousLanguageCode =languageCode
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ChevronLeft, contentDescription = stringResource(R.string.back))
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
            ) { Text(stringResource(R.string.sources)) }

            Button(
                onClick = { nav.navigate("manage_categories") },
                modifier = Modifier.fillMaxWidth()
            ) { Text(stringResource(R.string.categories)) }

            OutlinedTextField(
                value = currency,
                onValueChange = { currency = it.take(3).uppercase() },
                label = { Text(stringResource(R.string.currency_label)) },
                singleLine = true
            )

            // Период
            Text(stringResource(R.string.widget_period))
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val ctx = LocalContext.current
                val periodOptions = listOf(
                    "day" to Period.DAY.getLabel(ctx),
                    "week" to Period.WEEK.getLabel(ctx),
                    "month" to Period.MONTH.getLabel(ctx),
                    "all" to Period.ALL.getLabel(ctx)
                )
                periodOptions.forEach { (v, label) ->
                    FilterChip(
                        selected = period == v,
                        onClick = { period = v },
                        label = { Text(label) }
                    )
                }
            }

            // Лимит трат
            Text(stringResource(R.string.widget_limit))
            OutlinedTextField(
                value = if (limitCents == 0L) "" else (limitCents / 100).toString(),
                onValueChange = {
                    limitCents = (it.toLongOrNull() ?: 0L) * 100
                },
                label = { Text(stringResource(R.string.limit_label)) },
                singleLine = true
            )

            // Переключатель остатка
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(stringResource(R.string.show_balance_instead))
                Checkbox(
                    checked = showRemainder,
                    onCheckedChange = { showRemainder = it }
                )
            }

            Spacer(Modifier.height(12.dp))

            // Язык
            Text(stringResource(R.string.language))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val languageOptions = listOf(
                    "" to stringResource(R.string.language_system),
                    "en" to stringResource(R.string.language_english),
                    "ru" to stringResource(R.string.language_russian),
                    "sr" to stringResource(R.string.language_serbian)
                )
                languageOptions.forEach { (code, label) ->
                    FilterChip(
                        selected = languageCode == code,
                        onClick = { languageCode = code },
                        label = { Text(label) }
                    )
                }
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

                        val languageChanged = languageCode != previousLanguageCode

                        if (languageChanged) {
                            if (BuildConfig.DEBUG) {
                                Log.d("SettingsScreen", "Language changed: '$previousLanguageCode' -> '$languageCode'")
                            }
                            app.prefs.setLanguageCode(languageCode)

                            // Применяем язык в Main потоке (на UI потоке)
                            kotlinx.coroutines.withContext(Dispatchers.Main) {
                                if (BuildConfig.DEBUG) {
                                    Log.d("SettingsScreen", "Applying locale: '$languageCode'")
                                }
                                app.applyLanguage(languageCode)
                                if (BuildConfig.DEBUG) Log.d("SettingsScreen", "Locale applied")

                                // Перезагружаем Activity чтобы stringResource() перечитал строки на новом языке
                                val activity = ctx as? androidx.activity.ComponentActivity
                                activity?.recreate()
                            }
                        } else {
                            if (BuildConfig.DEBUG) {
                                Log.d("SettingsScreen", "Language not changed, just updating other settings")
                            }
                            app.prefs.setLanguageCode(languageCode)
                        }

                        if (BuildConfig.DEBUG) Log.d("SettingsScreen", "Settings saved")

                        // Попросим обновить виджет коалесированно
                        app.widgetRefresher.schedule()

                        // Закрываем экран только если язык не менялся
                        // (при смене языка Activity пересоздаётся и экран закроется сам)
                        if (!languageChanged) {
                            onBack()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text(stringResource(R.string.save)) }

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
        ) { Text(stringResource(R.string.add_widget)) }
    } else {
        // Если виджет уже есть — вместо «Добавить» покажем полезную кнопку
        OutlinedButton(
            onClick = {
                // коалесированный пересчёт+редроу (твой класс)
                app.widgetRefresher.schedule(300)
                android.widget.Toast.makeText(ctx, "Обновляю виджет…", android.widget.Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text(stringResource(R.string.update_widget)) }
    }
}
