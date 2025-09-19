package dev.keslorod.quickexpense.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import dev.keslorod.quickexpense.data.entities.Category

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY isFavorite DESC, sort ASC, name ASC")
    suspend fun all(): List<Category>

    @Query("SELECT * FROM categories WHERE isFavorite = 1 ORDER BY sort ASC LIMIT 8")
    suspend fun favorites(): List<Category>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<Category>)
}
