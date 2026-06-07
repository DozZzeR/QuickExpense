package dev.keslorod.quickexpense.data.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.*

@Entity(
    tableName = "split_nodes",
    foreignKeys = [
        ForeignKey(
            entity = Expense::class,
            parentColumns = ["id"],
            childColumns = ["expenseId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = SplitNode::class,
            parentColumns = ["id"],
            childColumns = ["parentId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("expenseId"),
        Index("parentId"),
        Index("categoryId")
    ]
)
data class SplitNode(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val expenseId: String,
    val parentId: String? = null, // null for root nodes
    val amount: Long,
    val label: String? = null,
    val categoryId: String? = null,
    val depth: Int = 0,
    val sortOrder: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
