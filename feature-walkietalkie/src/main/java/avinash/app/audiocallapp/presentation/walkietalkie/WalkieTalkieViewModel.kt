package avinash.app.audiocallapp.presentation.walkietalkie

import androidx.lifecycle.ViewModel
import avinash.app.audiocallapp.walkietalkie.WalkieTalkieManager
import avinash.app.audiocallapp.walkietalkie.WtPeerState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class WalkieTalkieViewModel @Inject constructor(
    val walkieTalkieManager: WalkieTalkieManager
) : ViewModel() {

    val isActive: StateFlow<Boolean> = walkieTalkieManager.isActive
    val peerStates: StateFlow<Map<String, WtPeerState>> = walkieTalkieManager.peerStates
    val isTalking: StateFlow<Boolean> = walkieTalkieManager.isTalking
    val talkingToFriendId: StateFlow<String?> = walkieTalkieManager.talkingToFriendId
    val remoteSpeakingFriends: StateFlow<Set<String>> = walkieTalkieManager.remoteSpeakingFriends

    fun startTalking(friendId: String) = walkieTalkieManager.startTalking(friendId)
    fun stopTalking() = walkieTalkieManager.stopTalking()
    fun isConnected(friendId: String) = walkieTalkieManager.isConnectedTo(friendId)
}
