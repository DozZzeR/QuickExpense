package dev.keslorod.quickexpense.ui.manage

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import dev.keslorod.quickexpense.App
import dev.keslorod.quickexpense.R
import dev.keslorod.quickexpense.data.entities.Category

@Composable
fun ManageCategoriesScreen(
    app: App,
    onBack: () -> Unit,
    mode: ListScreenMode = ListScreenMode.MANAGE,
    onSelectCategory: ((Category) -> Unit)? = null
) {
    ManageListScreen<Category>(
        title = stringResource(R.string.categories),
        onBack = onBack,
        mode = mode,
        onSelect = onSelectCategory,
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
}