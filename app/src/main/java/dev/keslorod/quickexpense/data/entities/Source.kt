package dev.keslorod.quickexpense.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "sources")
data class Source(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val sort: Int = 0,
    val isFavorite: Boolean = true
)