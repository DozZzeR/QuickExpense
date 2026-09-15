package dev.keslorod.quickexpense.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import dev.keslorod.quickexpense.data.entities.Expense
import dev.keslorod.quickexpense.data.entities.ExpenseTag
import dev.keslorod.quickexpense.data.entities.Tag

@Dao
interface ExpenseTagDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(crossRef: ExpenseTag)

    @Query("DELETE FROM expense_tags WHERE expenseId = :expenseId AND tagId = :tagId")
    suspend fun delete(expenseId: String, tagId: String)

    @Query("DELETE FROM expense_tags WHERE expenseId = :expenseId")
    suspend fun deleteByExpenseId(expenseId: String)

    @Query("""
        SELECT tags.* FROM tags
        INNER JOIN expense_tags ON tags.id = expense_tags.tagId
        WHERE expense_tags.expenseId = :expenseId
    """)
    suspend fun getTagsForExpense(expenseId: String): List<Tag>

    @Query("""
        SELECT expenses.* FROM expenses
        INNER JOIN expense_tags ON expenses.id = expense_tags.expenseId
        WHERE expense_tags.tagId = :tagId
        ORDER BY expenses.createdAt DESC
    """)
    suspend fun getExpensesByTag(tagId: String): List<Expense>

    @Query("""
        SELECT tags.*, expense_tags.expenseId FROM tags
        INNER JOIN expense_tags ON tags.id = expense_tags.tagId
        WHERE expense_tags.expenseId IN (:expenseIds)
    """)
    suspend fun getTagsForExpenses(expenseIds: List<String>): List<TagWithExpenseId>
}

data class TagWithExpenseId(
    @androidx.room.Embedded val tag: Tag,
    val expenseId: String
)
