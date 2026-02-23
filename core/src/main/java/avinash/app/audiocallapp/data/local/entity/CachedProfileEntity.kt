package avinash.app.audiocallapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cached_profile")
data class CachedProfileEntity(
    @PrimaryKey
    val uniqueId: String,
    val displayName: String,
    val status: String,
    val cachedAt: Long
)
