package avinash.app.audiocallapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "call_history")
data class CallHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val remoteUserId: String,
    val remoteName: String,
    val type: String, // "incoming", "outgoing", "missed"
    val durationSeconds: Long,
    val timestamp: Long,
    val seen: Boolean = true
)
