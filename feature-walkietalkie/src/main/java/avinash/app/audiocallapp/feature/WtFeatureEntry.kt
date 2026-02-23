package avinash.app.audiocallapp.feature

import avinash.app.audiocallapp.walkietalkie.WalkieTalkieManager
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WtFeatureEntry @Inject constructor(
    private val walkieTalkieManager: WalkieTalkieManager
) : FeatureInitializer {

    override val featureId: String = "walkie_talkie"

    override fun initialize(userId: String, userName: String) {
        walkieTalkieManager.start(userId, userName)
    }

    override fun cleanup() {
        walkieTalkieManager.stop()
    }
}
