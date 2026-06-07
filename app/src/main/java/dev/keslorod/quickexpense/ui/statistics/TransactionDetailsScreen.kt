package dev.keslorod.quickexpense.ui.statistics

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.keslorod.quickexpense.R
import dev.keslorod.quickexpense.domain.formatCents
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionDetailsScreen(
    expenseId: String,
    onBack: () -> Unit,
    onEditTransaction: (String) -> Unit,
    onEditSplit: (String) -> Unit
) {
    // Placeholder implementation
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
        Column(
            modifier = Modifier
                .padding(pad)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Детали транзакции $expenseId (в разработке)")
            
            Button(onClick = { onEditSplit(expenseId) }) {
                Text(stringResource(R.string.edit_split))
            }
        }
    }
}
