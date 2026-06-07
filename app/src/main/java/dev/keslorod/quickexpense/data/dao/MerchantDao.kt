package dev.keslorod.quickexpense.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import dev.keslorod.quickexpense.data.entities.Merchant

@Dao
interface MerchantDao {
    @Query("SELECT * FROM merchants ORDER BY isFavorite DESC, sort ASC, name ASC")
    suspend fun all(): List<Merchant>

    @Query("SELECT * FROM merchants WHERE isFavorite = 1 ORDER BY sort ASC LIMIT $MAX_QUICK_OPTIONS")
    suspend fun favorites(): List<Merchant>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<Merchant>)

    @Insert suspend fun insert(merchant: Merchant)

    @Update suspend fun update(merchant: Merchant)

    @Delete suspend fun delete(merchant: Merchant)
}
