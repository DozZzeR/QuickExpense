package dev.keslorod.quickexpense.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import dev.keslorod.quickexpense.data.entities.Tag

@Dao
interface TagDao {
    @Query("SELECT * FROM tags ORDER BY name ASC")
    suspend fun all(): List<Tag>

    @Query("SELECT * FROM tags WHERE normalizedName = :normalizedName LIMIT 1")
    suspend fun findByNormalizedName(normalizedName: String): Tag?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(tag: Tag)

    @Update suspend fun update(tag: Tag)

    @Delete suspend fun delete(tag: Tag)
}
