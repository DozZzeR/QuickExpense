package dev.keslorod.quickexpense.domain

import dev.keslorod.quickexpense.App
import dev.keslorod.quickexpense.R
import dev.keslorod.quickexpense.data.entities.ExpenseTag
import dev.keslorod.quickexpense.data.entities.Tag

/** Well-known id for the built-in "Has receipt" tag — a fixed id (not a random UUID) so it can
 * always be found and reused across app runs, the same way the "unsorted" category works. */
const val RECEIPT_TAG_ID = "has_receipt"

/**
 * Ensures the built-in receipt tag exists (insertIfAbsent is IGNORE, not REPLACE — see its doc
 * comment on why REPLACE here would cascade-delete every expense's existing link to this tag —
 * so this is safe to call every time, no separate "does it exist yet" check needed) and links
 * [expenseId] to it, so expenses with a receipt can always be found later via
 * [dev.keslorod.quickexpense.data.dao.ExpenseTagDao.getExpensesByTag]. The tag's name is
 * captured in whatever language is active right now, same as every other seeded default in
 * this app (e.g. the "unsorted" category) — it won't retroactively relabel itself if the
 * language changes later.
 */
suspend fun App.tagExpenseAsHavingReceipt(expenseId: String) {
    val name = getString(R.string.default_tag_has_receipt)
    db.tags().insertIfAbsent(Tag(id = RECEIPT_TAG_ID, name = name, normalizedName = name.lowercase().trim()))
    db.expenseTags().insert(ExpenseTag(expenseId = expenseId, tagId = RECEIPT_TAG_ID))
}
