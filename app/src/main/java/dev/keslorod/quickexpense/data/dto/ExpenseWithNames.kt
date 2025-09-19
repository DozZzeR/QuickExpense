package dev.keslorod.quickexpense.data.dto

import androidx.room.ColumnInfo

data class ExpenseWithNames(
    val id: String,
    val amount: Long,
    val currency: String,
    val createdAt: Long,
    @ColumnInfo(name = "categoryId") val categoryId: String,
    @ColumnInfo(name = "sourceId")   val sourceId: String,
    @ColumnInfo(name = "categoryName") val categoryName: String?,
    @ColumnInfo(name = "sourceName")   val sourceName: String?
)
