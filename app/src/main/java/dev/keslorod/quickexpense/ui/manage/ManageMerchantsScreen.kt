package dev.keslorod.quickexpense.ui.manage

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import dev.keslorod.quickexpense.App
import dev.keslorod.quickexpense.R
import dev.keslorod.quickexpense.data.entities.Merchant

@Composable
fun ManageMerchantsScreen(
    app: App,
    onBack: () -> Unit,
    mode: ListScreenMode = ListScreenMode.MANAGE,
    onSelectMerchant: ((Merchant) -> Unit)? = null
) {
    ManageListScreen<Merchant>(
        title = stringResource(R.string.merchants),
        onBack = onBack,
        mode = mode,
        onSelect = onSelectMerchant,
        getName = { it.name },
        isFavorite = { it.isFavorite },
        itemKey = { it.id },
        loadAll = { app.db.merchants().all() },
        addNew = { name -> app.db.merchants().insert(Merchant(name = name, isFavorite = false)) },
        toggleFavorite = { m -> app.db.merchants().update(m.copy(isFavorite = !m.isFavorite)) },
        rename = { m, newName -> app.db.merchants().update(m.copy(name = newName, normalizedName = newName.lowercase().trim())) },
        deleteIfUnused = { m ->
            val cnt = app.db.expenses().countByMerchant(m.id)
            if (cnt == 0L) {
                app.db.merchants().delete(m)
                true
            } else false
        }
    )
}
