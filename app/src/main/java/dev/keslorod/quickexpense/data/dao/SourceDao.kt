package dev.keslorod.quickexpense.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import dev.keslorod.quickexpense.data.entities.Source

@Dao
interface SourceDao {
    @Query("SELECT * FROM sources ORDER BY isFavorite DESC, sort ASC, name ASC")
    suspend fun all(): List<Source>

    @Query("SELECT * FROM sources WHERE isFavorite = 1 ORDER BY sort ASC LIMIT 8")
    suspend fun favorites(): List<Source>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<Source>)
}
