package avinash.app.audiocallapp.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import avinash.app.audiocallapp.data.local.dao.CachedFriendDao
import avinash.app.audiocallapp.data.local.dao.CachedProfileDao
import avinash.app.audiocallapp.data.local.dao.CallHistoryDao
import avinash.app.audiocallapp.data.local.entity.CachedFriendEntity
import avinash.app.audiocallapp.data.local.entity.CachedProfileEntity
import avinash.app.audiocallapp.data.local.entity.CallHistoryEntity

@Database(
    entities = [CallHistoryEntity::class, CachedFriendEntity::class, CachedProfileEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun callHistoryDao(): CallHistoryDao
    abstract fun cachedFriendDao(): CachedFriendDao
    abstract fun cachedProfileDao(): CachedProfileDao
}
