package dev.keslorod.quickexpense.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import dev.keslorod.quickexpense.data.dto.ExpenseWithNames
import dev.keslorod.quickexpense.data.entities.Expense

@Dao
interface ExpenseDao {
    @Insert
    suspend fun insert(item: Expense): Long

    @Update
    suspend fun update(item: Expense)

    @Query("SELECT * FROM expenses WHERE id = :id")
    suspend fun getById(id: String): Expense?

    // Only one currency at a time: amounts in different currencies can't be added together.
    @Query("SELECT COALESCE(SUM(amount),0) FROM expenses WHERE createdAt BETWEEN :from AND :to AND currency = :currency")
    suspend fun sumInRange(from: Long, to: Long, currency: String): Long

    @Query("SELECT * FROM expenses ORDER BY createdAt DESC LIMIT :limit")
    suspend fun lastExpenses(limit: Int = 50): List<Expense>

    @Query("""
         SELECT e.id, e.amount, e.currency, e.createdAt, e.categoryId, e.sourceId, e.merchantId, e.photoPaths,
             c.name AS categoryName, s.name AS sourceName, m.name AS merchantName
        FROM expenses e
        LEFT JOIN categories c ON c.id = e.categoryId
        LEFT JOIN sources    s ON s.id = e.sourceId
         LEFT JOIN merchants  m ON m.id = e.merchantId
        ORDER BY e.createdAt DESC
        LIMIT :limit
    """)
    suspend fun lastExpensesWithNames(limit: Int = 50): List<ExpenseWithNames>

    @Query("""
         SELECT e.id, e.amount, e.currency, e.createdAt, e.categoryId, e.sourceId, e.merchantId, e.photoPaths,
             c.name AS categoryName, s.name AS sourceName, m.name AS merchantName
        FROM expenses e
        LEFT JOIN categories c ON c.id = e.categoryId
        LEFT JOIN sources    s ON s.id = e.sourceId
         LEFT JOIN merchants  m ON m.id = e.merchantId
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

    @Query("""
        SELECT * FROM expenses
        WHERE createdAt BETWEEN :from AND :to AND currency = :currency
        ORDER BY createdAt ASC
    """)
    suspend fun expensesInRangeForCurrency(from: Long, to: Long, currency: String): List<Expense>

    /** Every expense in the range, with no row cap — for export, which must not drop rows. */
    @Query("""
         SELECT e.id, e.amount, e.currency, e.createdAt, e.categoryId, e.sourceId, e.merchantId, e.photoPaths,
             c.name AS categoryName, s.name AS sourceName, m.name AS merchantName
        FROM expenses e
        LEFT JOIN categories c ON c.id = e.categoryId
        LEFT JOIN sources    s ON s.id = e.sourceId
        LEFT JOIN merchants  m ON m.id = e.merchantId
        WHERE e.createdAt BETWEEN :from AND :to
        ORDER BY e.createdAt DESC
    """)
    suspend fun allExpensesInRangeWithNames(from: Long, to: Long): List<ExpenseWithNames>

    @Query("SELECT photoPaths FROM expenses WHERE photoPaths IS NOT NULL")
    suspend fun allPhotoPaths(): List<String>

    @Query("SELECT photoPath FROM expenses WHERE photoPath IS NOT NULL")
    suspend fun allLegacyPhotoPaths(): List<String>

    @Query("SELECT COUNT(*) FROM expenses WHERE sourceId = :sourceId")
    suspend fun countBySource(sourceId: String): Long

    @Query("SELECT COUNT(*) FROM expenses WHERE categoryId = :categoryId")
    suspend fun countByCategory(categoryId: String): Long

    /** Uses of a category anywhere: as an expense's category or as a split item's. */
    @Query("""
        SELECT (SELECT COUNT(*) FROM expenses WHERE categoryId = :categoryId)
             + (SELECT COUNT(*) FROM split_nodes WHERE categoryId = :categoryId)
    """)
    suspend fun countCategoryUsages(categoryId: String): Long

    /** Uses of a tag anywhere: on a whole expense or on a split item. */
    @Query("""
        SELECT (SELECT COUNT(*) FROM expense_tags WHERE tagId = :tagId)
             + (SELECT COUNT(*) FROM split_node_tags WHERE tagId = :tagId)
    """)
    suspend fun countTagUsages(tagId: String): Long

    @Query("SELECT COUNT(*) FROM expenses WHERE merchantId = :merchantId")
    suspend fun countByMerchant(merchantId: String): Long
}
