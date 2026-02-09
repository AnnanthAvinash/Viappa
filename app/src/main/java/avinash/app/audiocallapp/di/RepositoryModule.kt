package avinash.app.audiocallapp.di

import avinash.app.audiocallapp.data.repository.AuthRepositoryImpl
import avinash.app.audiocallapp.data.repository.SignalingRepositoryImpl
import avinash.app.audiocallapp.domain.repository.AuthRepository
import avinash.app.audiocallapp.domain.repository.SignalingRepository
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
    abstract fun bindSignalingRepository(
        signalingRepositoryImpl: SignalingRepositoryImpl
    ): SignalingRepository
}
