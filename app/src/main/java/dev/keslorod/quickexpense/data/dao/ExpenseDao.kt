package dev.keslorod.quickexpense.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import dev.keslorod.quickexpense.data.dto.ExpenseWithNames
import dev.keslorod.quickexpense.data.entities.Expense

@Dao
interface ExpenseDao {
    @Insert
    suspend fun insert(item: Expense)

    @Query("SELECT COALESCE(SUM(amount),0) FROM expenses WHERE createdAt BETWEEN :from AND :to")
    suspend fun sumInRange(from: Long, to: Long): Long

    @Query("SELECT * FROM expenses ORDER BY createdAt DESC LIMIT :limit")
    suspend fun lastExpenses(limit: Int = 50): List<Expense>

    @Query("""
        SELECT e.id, e.amount, e.currency, e.createdAt, e.categoryId, e.sourceId,
               c.name AS categoryName, s.name AS sourceName
        FROM expenses e
        LEFT JOIN categories c ON c.id = e.categoryId
        LEFT JOIN sources    s ON s.id = e.sourceId
        ORDER BY e.createdAt DESC
        LIMIT :limit
    """)
    suspend fun lastExpensesWithNames(limit: Int = 50): List<ExpenseWithNames>

    @Query("""
        SELECT e.id, e.amount, e.currency, e.createdAt, e.categoryId, e.sourceId,
               c.name AS categoryName, s.name AS sourceName
        FROM expenses e
        LEFT JOIN categories c ON c.id = e.categoryId
        LEFT JOIN sources    s ON s.id = e.sourceId
        WHERE e.createdAt BETWEEN :from AND :to
        ORDER BY e.createdAt DESC
        LIMIT :limit
    """)
    suspend fun expensesInRangeWithNames(
        from: Long,
        to: Long,
        limit: Int = 200
    ): List<ExpenseWithNames>

    @Query("""
        SELECT * FROM expenses 
        WHERE createdAt BETWEEN :from AND :to 
        ORDER BY createdAt ASC
    """)
    suspend fun expensesInRange(from: Long, to: Long): List<Expense>
}
