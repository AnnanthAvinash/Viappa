package avinash.app.audiocallapp.feature

import avinash.app.audiocallapp.call.CallManager
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CallFeatureEntry @Inject constructor(
    private val callManager: CallManager
) : FeatureInitializer {

    override val featureId: String = "call"

    override fun initialize(userId: String, userName: String) {
        callManager.initialize()
        callManager.startObservingIncomingCalls(userId)
    }

    override fun cleanup() {
        callManager.stopObservingIncomingCalls()
    }
}
