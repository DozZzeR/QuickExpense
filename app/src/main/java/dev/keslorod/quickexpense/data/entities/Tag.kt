package dev.keslorod.quickexpense.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.*

@Entity(tableName = "tags")
data class Tag(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val normalizedName: String,
    val isFavorite: Boolean = false
)
