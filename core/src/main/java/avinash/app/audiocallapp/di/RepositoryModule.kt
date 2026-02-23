package avinash.app.audiocallapp.di

import avinash.app.audiocallapp.data.repository.AuthRepositoryImpl
import avinash.app.audiocallapp.data.repository.ConnectionRepositoryImpl
import avinash.app.audiocallapp.data.repository.UserPresenceRepositoryImpl
import avinash.app.audiocallapp.domain.repository.AuthRepository
import avinash.app.audiocallapp.domain.repository.ConnectionRepository
import avinash.app.audiocallapp.domain.repository.UserPresenceRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(
        authRepositoryImpl: AuthRepositoryImpl
    ): AuthRepository

    @Binds
    @Singleton
    abstract fun bindConnectionRepository(
        connectionRepositoryImpl: ConnectionRepositoryImpl
    ): ConnectionRepository

    @Binds
    @Singleton
    abstract fun bindUserPresenceRepository(
        userPresenceRepositoryImpl: UserPresenceRepositoryImpl
    ): UserPresenceRepository
}
