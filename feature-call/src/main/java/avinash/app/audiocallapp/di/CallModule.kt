package avinash.app.audiocallapp.di

import avinash.app.audiocallapp.call.CallManager
import avinash.app.audiocallapp.data.repository.CallSignalingRepositoryImpl
import avinash.app.audiocallapp.domain.repository.CallSignalingRepository
import avinash.app.audiocallapp.feature.CallStateProvider
import avinash.app.audiocallapp.feature.FeatureInitializer
import avinash.app.audiocallapp.feature.FeatureNavigation
import avinash.app.audiocallapp.feature.CallFeatureEntry
import avinash.app.audiocallapp.feature.CallFeatureNavigation
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class CallModule {

    @Binds
    @Singleton
    abstract fun bindCallSignalingRepository(
        impl: CallSignalingRepositoryImpl
    ): CallSignalingRepository

    @Binds
    @IntoSet
    abstract fun bindCallFeatureInitializer(
        impl: CallFeatureEntry
    ): FeatureInitializer

    @Binds
    @IntoSet
    abstract fun bindCallFeatureNavigation(
        impl: CallFeatureNavigation
    ): FeatureNavigation

    @Binds
    @IntoSet
    abstract fun bindCallStateProvider(
        impl: CallManager
    ): CallStateProvider
}
