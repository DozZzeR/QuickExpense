package dev.keslorod.quickexpense.ui.manage

import androidx.compose.runtime.Composable
import dev.keslorod.quickexpense.App
import dev.keslorod.quickexpense.data.entities.Tag

@Composable
fun ManageTagsScreen(
    app: App,
    onBack: () -> Unit,
    mode: ListScreenMode = ListScreenMode.MANAGE,
    onSelectTag: ((Tag) -> Unit)? = null
) {
    ManageListScreen<Tag>(
        title = "Метки",
        onBack = onBack,
        mode = mode,
        onSelect = onSelectTag,
        getName = { it.name },
        isFavorite = { it.isFavorite },
        itemKey = { it.id },
        loadAll = { app.db.tags().all() },
        addNew = { name -> app.db.tags().insert(Tag(name = name, normalizedName = name.lowercase().trim())) },
        toggleFavorite = { t -> app.db.tags().update(t.copy(isFavorite = !t.isFavorite)) },
        rename = { t, newName -> app.db.tags().update(t.copy(name = newName, normalizedName = newName.lowercase().trim())) },
        deleteIfUnused = { t ->
            // Для меток пока считаем, что удалять можно всегда, 
            // так как связи в split_node_tags имеют ON DELETE CASCADE
            // Но в идеале стоит проверить, используется ли метка где-то.
            // Пока просто удаляем.
            app.db.tags().delete(t)
            true
        }
    )
}
