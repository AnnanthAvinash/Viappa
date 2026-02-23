package avinash.app.audiocallapp.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import avinash.app.audiocallapp.data.local.entity.CachedFriendEntity

@Dao
interface CachedFriendDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(friends: List<CachedFriendEntity>)

    @Query("SELECT * FROM cached_friends")
    suspend fun getAll(): List<CachedFriendEntity>

    @Query("DELETE FROM cached_friends")
    suspend fun deleteAll()
}
