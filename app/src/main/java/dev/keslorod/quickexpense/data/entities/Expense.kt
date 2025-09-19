package dev.keslorod.quickexpense.data.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "expenses",
    indices = [Index("createdAt"), Index("sourceId"), Index("categoryId")]
)
data class Expense(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val amount: Long,               // динары
    val currency: String = "RSD",
    val sourceId: String,
    val categoryId: String,
    val note: String? = null,
    val photoPath: String? = null,  // пока не используем, но заложили
    val createdAt: Long = System.currentTimeMillis()
)
