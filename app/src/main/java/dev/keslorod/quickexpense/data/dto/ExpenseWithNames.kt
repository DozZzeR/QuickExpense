package dev.keslorod.quickexpense.data.dto

import androidx.room.ColumnInfo

data class ExpenseWithNames(
    val id: String,
    val amount: Long,
    val currency: String,
    val createdAt: Long,
    @ColumnInfo(name = "categoryId") val categoryId: String,
    @ColumnInfo(name = "sourceId")   val sourceId: String,
    @ColumnInfo(name = "merchantId") val merchantId: String?,
    @ColumnInfo(name = "categoryName") val categoryName: String?,
    @ColumnInfo(name = "sourceName")   val sourceName: String?,
    @ColumnInfo(name = "merchantName") val merchantName: String?,
    @ColumnInfo(name = "photoPaths")   val photoPaths: String? = null
)
