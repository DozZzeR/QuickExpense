package dev.keslorod.quickexpense.domain

import dev.keslorod.quickexpense.App
import dev.keslorod.quickexpense.data.entities.Category
import dev.keslorod.quickexpense.data.entities.Tag

/** Fixed id of the seeded fallback category every uncategorized expense is saved under. */
const val UNSORTED_CATEGORY_ID = "unsorted"

/** Built-ins the app itself relies on by id — never deletable, used or not. */
fun Category.isBuiltIn(): Boolean = id == UNSORTED_CATEGORY_ID
fun Tag.isBuiltIn(): Boolean = id == RECEIPT_TAG_ID

/**
 * Deletes [category] only if nothing refers to it — neither an expense nor a split item (the
 * latter has no foreign key, so deleting it would silently leave splits pointing nowhere).
 * Returns true if it was deleted.
 */
suspend fun App.deleteCategoryIfUnused(category: Category): Boolean {
    if (category.isBuiltIn()) return false
    if (db.expenses().countCategoryUsages(category.id) > 0L) return false
    db.categories().delete(category)
    return true
}

/**
 * Deletes [tag] only if no expense or split item carries it — its links are ON DELETE CASCADE,
 * so deleting a used tag would silently strip it from every expense that has it.
 * Returns true if it was deleted.
 */
suspend fun App.deleteTagIfUnused(tag: Tag): Boolean {
    if (tag.isBuiltIn()) return false
    if (db.expenses().countTagUsages(tag.id) > 0L) return false
    db.tags().delete(tag)
    return true
}
