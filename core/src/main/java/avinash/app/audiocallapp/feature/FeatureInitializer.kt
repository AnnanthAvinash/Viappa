package avinash.app.audiocallapp.feature

interface FeatureInitializer {
    val featureId: String
    fun initialize(userId: String, userName: String)
    fun cleanup()
}
