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

    // IGNORE, not REPLACE: REPLACE on a fixed-id row (e.g. the built-in "has receipt" tag)
    // is a DELETE+INSERT under the hood, and expense_tags' FK to tags is ON DELETE CASCADE —
    // so re-"seeding" an already-existing tag this way would silently wipe every expense's
    // existing link to it. Use this for any tag keyed by a well-known fixed id.
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfAbsent(tag: Tag)

    @Update suspend fun update(tag: Tag)

    @Delete suspend fun delete(tag: Tag)
}
