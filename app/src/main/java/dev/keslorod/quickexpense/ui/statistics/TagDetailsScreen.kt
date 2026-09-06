package dev.keslorod.quickexpense.ui.statistics

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import dev.keslorod.quickexpense.R
import dev.keslorod.quickexpense.ui.statistics.components.DateRangeFilterBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TagDetailsScreen(
    tagId: String,
    filterViewModel: StatisticsFilterViewModel,
    onBack: () -> Unit,
    onTransactionClick: (String) -> Unit
) {
    val filterState by filterViewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.tag_details_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                }
            )
        }
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            DateRangeFilterBar(
                state = filterState,
                onPresetSelected = { filterViewModel.setPreset(it) }
            )
            Box(Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                Text(stringResource(R.string.tag_details_placeholder_fmt, tagId))
            }
        }
    }
}
