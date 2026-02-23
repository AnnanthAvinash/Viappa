package avinash.app.audiocallapp.di

import avinash.app.audiocallapp.data.repository.WalkieTalkieRepositoryImpl
import avinash.app.audiocallapp.domain.repository.WalkieTalkieRepository
import avinash.app.audiocallapp.feature.FeatureInitializer
import avinash.app.audiocallapp.feature.FeatureNavigation
import avinash.app.audiocallapp.feature.WtFeatureEntry
import avinash.app.audiocallapp.feature.WtFeatureNavigation
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class WalkieTalkieModule {

    @Binds
    @Singleton
    abstract fun bindWalkieTalkieRepository(
        impl: WalkieTalkieRepositoryImpl
    ): WalkieTalkieRepository

    @Binds
    @IntoSet
    abstract fun bindWtFeatureInitializer(
        impl: WtFeatureEntry
    ): FeatureInitializer

    @Binds
    @IntoSet
    abstract fun bindWtFeatureNavigation(
        impl: WtFeatureNavigation
    ): FeatureNavigation
}
