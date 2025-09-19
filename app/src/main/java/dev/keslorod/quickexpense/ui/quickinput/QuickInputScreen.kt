package dev.keslorod.quickexpense.ui.quickinput

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

data class Option(val id: String, val label: String)

@Composable
fun QuickInputScreen(
    currency: String,
    sourceOptions: List<Option>,
    categoryOptions: List<Option>,
    onConfirm: (cents: Long, sourceId: String, categoryId: String) -> Unit,
) {
    var amountText by remember { mutableStateOf("") }
    var source by remember { mutableStateOf(sourceOptions.firstOrNull()) }
    var category by remember { mutableStateOf(categoryOptions.firstOrNull()) }

    fun toCents(txt: String): Long {
        if (txt.isBlank()) return 0
        val parts = txt.split(',', limit = 2)
        val major = parts[0].ifBlank { "0" }
        val minor = (parts.getOrNull(1) ?: "").padEnd(2, '0').take(2)
        return (major.toLongOrNull() ?: 0L) * 100 + (minor.toLongOrNull() ?: 0L)
    }

    Column(
        Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Источник", fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(8.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(sourceOptions.size) { i ->
                val opt = sourceOptions[i]
                FilterChip(
                    selected = source?.id == opt.id,
                    onClick = { source = opt },
                    label = { Text(opt.label) }
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
        Text("Категория", fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(8.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(categoryOptions.size) { i ->
                val opt = categoryOptions[i]
                FilterChip(
                    selected = category?.id == opt.id,
                    onClick = { category = opt },
                    label = { Text(opt.label) }
                )
            }
        }

        Spacer(Modifier.height(24.dp))
        Button(
            onClick = {
                val cents = toCents(amountText)
                val sId = source?.id ?: return@Button
                val cId = category?.id ?: return@Button
                if (cents > 0) onConfirm(cents, sId, cId)
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("OK — сохранить") }
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
