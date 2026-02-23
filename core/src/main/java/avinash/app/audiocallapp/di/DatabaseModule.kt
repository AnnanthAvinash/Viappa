package avinash.app.audiocallapp.di

import android.content.Context
import androidx.room.Room
import avinash.app.audiocallapp.data.local.AppDatabase
import avinash.app.audiocallapp.data.local.dao.CachedFriendDao
import avinash.app.audiocallapp.data.local.dao.CachedProfileDao
import avinash.app.audiocallapp.data.local.dao.CallHistoryDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(context, AppDatabase::class.java, "audiocall_db")
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideCallHistoryDao(db: AppDatabase): CallHistoryDao = db.callHistoryDao()

    @Provides
    fun provideCachedFriendDao(db: AppDatabase): CachedFriendDao = db.cachedFriendDao()

    @Provides
    fun provideCachedProfileDao(db: AppDatabase): CachedProfileDao = db.cachedProfileDao()
}
