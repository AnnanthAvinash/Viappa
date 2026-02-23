package avinash.app.audiocallapp.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import avinash.app.audiocallapp.data.local.entity.CachedProfileEntity

@Dao
interface CachedProfileDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(profile: CachedProfileEntity)

    @Query("SELECT * FROM cached_profile LIMIT 1")
    suspend fun get(): CachedProfileEntity?

    @Query("DELETE FROM cached_profile")
    suspend fun delete()
}
