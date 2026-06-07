package dev.keslorod.quickexpense.data.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "split_node_tags",
    primaryKeys = ["splitNodeId", "tagId"],
    foreignKeys = [
        ForeignKey(
            entity = SplitNode::class,
            parentColumns = ["id"],
            childColumns = ["splitNodeId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Tag::class,
            parentColumns = ["id"],
            childColumns = ["tagId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("splitNodeId"),
        Index("tagId")
    ]
)
data class SplitNodeTag(
    val splitNodeId: String,
    val tagId: String
)
