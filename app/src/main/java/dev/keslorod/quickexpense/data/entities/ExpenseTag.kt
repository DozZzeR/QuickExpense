package dev.keslorod.quickexpense.data.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/** Tags a whole expense — as opposed to [SplitNodeTag], which tags one part of a split. */
@Entity(
    tableName = "expense_tags",
    primaryKeys = ["expenseId", "tagId"],
    foreignKeys = [
        ForeignKey(
            entity = Expense::class,
            parentColumns = ["id"],
            childColumns = ["expenseId"],
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
        Index("expenseId"),
        Index("tagId")
    ]
)
data class ExpenseTag(
    val expenseId: String,
    val tagId: String
)
