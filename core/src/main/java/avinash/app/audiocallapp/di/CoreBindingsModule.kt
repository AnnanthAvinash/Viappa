package avinash.app.audiocallapp.di

import avinash.app.audiocallapp.feature.CallStateProvider
import avinash.app.audiocallapp.feature.FeatureInitializer
import avinash.app.audiocallapp.feature.FeatureNavigation
import avinash.app.audiocallapp.feature.NoOpCallStateProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.Multibinds
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class CoreBindingsModule {

    @Multibinds
    abstract fun featureInitializers(): Set<FeatureInitializer>

    @Multibinds
    abstract fun featureNavigations(): Set<FeatureNavigation>

    @Multibinds
    abstract fun callStateProviders(): Set<CallStateProvider>
}

@Module
@InstallIn(SingletonComponent::class)
object CoreDefaultsModule {

    @Provides
    @Singleton
    fun provideCallStateProvider(
        providers: Set<@JvmSuppressWildcards CallStateProvider>
    ): CallStateProvider = providers.firstOrNull() ?: NoOpCallStateProvider()
}
