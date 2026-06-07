package dev.keslorod.quickexpense.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import dev.keslorod.quickexpense.data.entities.SplitNodeTag
import dev.keslorod.quickexpense.data.entities.Tag

@Dao
interface SplitNodeTagDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(crossRef: SplitNodeTag)

    @Query("DELETE FROM split_node_tags WHERE splitNodeId = :splitNodeId AND tagId = :tagId")
    suspend fun delete(splitNodeId: String, tagId: String)

    @Query("DELETE FROM split_node_tags WHERE splitNodeId = :splitNodeId")
    suspend fun deleteBySplitNodeId(splitNodeId: String)

    @Query("""
        SELECT tags.* FROM tags
        INNER JOIN split_node_tags ON tags.id = split_node_tags.tagId
        WHERE split_node_tags.splitNodeId = :splitNodeId
    """)
    suspend fun getTagsForSplitNode(splitNodeId: String): List<Tag>
}
