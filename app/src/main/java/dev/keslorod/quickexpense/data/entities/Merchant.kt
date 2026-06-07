package dev.keslorod.quickexpense.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "merchants")
data class Merchant(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val normalizedName: String = name.lowercase().trim(),
    val sort: Int = 0,
    val isFavorite: Boolean = true
)
