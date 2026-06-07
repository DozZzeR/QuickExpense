package dev.keslorod.quickexpense.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import dev.keslorod.quickexpense.data.entities.SplitNode

@Dao
interface SplitNodeDao {
    @Query("SELECT * FROM split_nodes WHERE expenseId = :expenseId ORDER BY sortOrder ASC")
    suspend fun getByExpenseId(expenseId: String): List<SplitNode>

    @Query("SELECT * FROM split_nodes WHERE parentId = :parentId ORDER BY sortOrder ASC")
    suspend fun getByParentId(parentId: String): List<SplitNode>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(node: SplitNode)

    @Update suspend fun update(node: SplitNode)

    @Delete suspend fun delete(node: SplitNode)

    @Transaction
    @Query("DELETE FROM split_nodes WHERE expenseId = :expenseId")
    suspend fun deleteByExpenseId(expenseId: String)
}
