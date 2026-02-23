package avinash.app.audiocallapp.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import avinash.app.audiocallapp.data.local.entity.CallHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CallHistoryDao {

    @Insert
    suspend fun insert(record: CallHistoryEntity)

    @Query("SELECT * FROM call_history ORDER BY timestamp DESC LIMIT 50")
    fun getAll(): Flow<List<CallHistoryEntity>>

    @Query("SELECT * FROM call_history WHERE type = :type ORDER BY timestamp DESC LIMIT 50")
    fun getByType(type: String): Flow<List<CallHistoryEntity>>

    @Query("SELECT COUNT(*) FROM call_history WHERE type = 'missed' AND seen = 0")
    fun getUnseenMissedCount(): Flow<Int>

    @Query("UPDATE call_history SET seen = 1 WHERE type = 'missed' AND seen = 0")
    suspend fun markAllMissedSeen()

    @Query("SELECT * FROM call_history WHERE remoteUserId = :userId ORDER BY timestamp DESC")
    suspend fun getByRemoteUser(userId: String): List<CallHistoryEntity>
}
