package dev.keslorod.quickexpense.ui.manage

import androidx.compose.runtime.Composable
import dev.keslorod.quickexpense.App
import dev.keslorod.quickexpense.data.entities.Source

@Composable
fun ManageSourcesScreen(
    app: App,
    onBack: () -> Unit,
    mode: ListScreenMode = ListScreenMode.MANAGE,
    onSelectSource: ((Source) -> Unit)? = null
) {
    ManageListScreen<Source>(
        title = "Источники",
        onBack = onBack,
        mode = mode,
        onSelect = onSelectSource,
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
}
