package avinash.app.audiocallapp.data.repository

import avinash.app.audiocallapp.data.local.dao.CachedFriendDao
import avinash.app.audiocallapp.data.local.dao.CachedProfileDao
import avinash.app.audiocallapp.data.local.dao.CallHistoryDao
import avinash.app.audiocallapp.data.local.entity.CachedFriendEntity
import avinash.app.audiocallapp.data.local.entity.CachedProfileEntity
import avinash.app.audiocallapp.data.local.entity.CallHistoryEntity
import avinash.app.audiocallapp.data.model.User
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CallHistoryRepository @Inject constructor(
    private val callHistoryDao: CallHistoryDao,
    private val cachedFriendDao: CachedFriendDao,
    private val cachedProfileDao: CachedProfileDao
) {
    suspend fun insertCallRecord(
        remoteUserId: String,
        remoteName: String,
        type: String,
        durationSeconds: Long
    ) {
        callHistoryDao.insert(
            CallHistoryEntity(
                remoteUserId = remoteUserId,
                remoteName = remoteName,
                type = type,
                durationSeconds = durationSeconds,
                timestamp = System.currentTimeMillis(),
                seen = type != "missed"
            )
        )
    }

    fun getCallHistory(typeFilter: String? = null): Flow<List<CallHistoryEntity>> {
        return if (typeFilter == null) callHistoryDao.getAll() else callHistoryDao.getByType(typeFilter)
    }

    fun getMissedCallCount(): Flow<Int> = callHistoryDao.getUnseenMissedCount()

    suspend fun markMissedCallsSeen() = callHistoryDao.markAllMissedSeen()

    suspend fun cacheFriends(friends: List<User>) {
        val now = System.currentTimeMillis()
        cachedFriendDao.deleteAll()
        cachedFriendDao.insertAll(friends.map {
            CachedFriendEntity(it.uniqueId, it.displayName, it.status.name, now)
        })
    }

    suspend fun getCachedFriends(): List<CachedFriendEntity> = cachedFriendDao.getAll()

    suspend fun cacheProfile(user: User) {
        cachedProfileDao.insert(
            CachedProfileEntity(user.uniqueId, user.displayName, user.status.name, System.currentTimeMillis())
        )
    }

    suspend fun getCachedProfile(): CachedProfileEntity? = cachedProfileDao.get()

    suspend fun getCallsWithUser(userId: String): List<CallHistoryEntity> = callHistoryDao.getByRemoteUser(userId)
}
