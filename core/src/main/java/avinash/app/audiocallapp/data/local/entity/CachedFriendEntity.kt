package avinash.app.audiocallapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cached_friends")
data class CachedFriendEntity(
    @PrimaryKey
    val userId: String,
    val displayName: String,
    val status: String,
    val cachedAt: Long
)
