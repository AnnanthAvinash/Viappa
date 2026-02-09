package avinash.app.audiocallapp.presentation.call

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun CallScreen(
    viewModel: CallViewModel = hiltViewModel(),
    calleeId: String? = null,
    calleeName: String? = null,
    incomingCallId: String? = null,
    incomingCallerId: String? = null,
    incomingCallerName: String? = null,
    onCallEnded: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    // Initialize call based on params
    LaunchedEffect(Unit) {
        if (incomingCallId != null && incomingCallerId != null && incomingCallerName != null) {
            // Answering incoming call
            viewModel.answerIncomingCall(incomingCallId, incomingCallerId, incomingCallerName)
        } else if (calleeId != null && calleeName != null) {
            // Making outgoing call
            viewModel.initiateOutgoingCall(calleeId, calleeName)
        }
    }

    // Handle call ended
    LaunchedEffect(uiState.callState) {
        if (uiState.callState == CallState.ENDED || uiState.callState == CallState.FAILED) {
            kotlinx.coroutines.delay(1500)
            onCallEnded()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1A1A2E),
                        Color(0xFF16213E),
                        Color(0xFF0F3460)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(60.dp))

            // Avatar with animation
            CallAvatar(
                name = uiState.remoteUserName,
                callState = uiState.callState,
                isReconnecting = uiState.isReconnecting
            )

            Spacer(modifier = Modifier.height(32.dp))

            // User name
            Text(
                text = uiState.remoteUserName,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 28.sp
            )

            Text(
                text = "@${uiState.remoteUserId}",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 16.sp,
                modifier = Modifier.padding(top = 8.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Call status
            CallStatusText(
                callState = uiState.callState,
                duration = uiState.callDuration,
                isReconnecting = uiState.isReconnecting,
                networkType = uiState.networkType
            )

            // Error message
            if (uiState.errorMessage != null) {
                Text(
                    text = uiState.errorMessage!!,
                    color = Color(0xFFFF6B6B),
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Call controls
            CallControls(
                isMuted = uiState.isMuted,
                isSpeakerOn = uiState.isSpeakerOn,
                callState = uiState.callState,
                onToggleMute = { viewModel.toggleMute() },
                onToggleSpeaker = { viewModel.toggleSpeaker() },
                onEndCall = { viewModel.endCall() }
            )

            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}

@Composable
private fun CallAvatar(
    name: String,
    callState: CallState,
    isReconnecting: Boolean = false
) {
    val infiniteTransition = rememberInfiniteTransition(label = "avatar")
    
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = when {
            isReconnecting -> 1.1f
            callState in listOf(CallState.OUTGOING, CallState.INCOMING, CallState.CONNECTING) -> 1.1f
            else -> 1f
        },
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    val ringAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500),
            repeatMode = RepeatMode.Restart
        ),
        label = "ring"
    )

    val ringScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500),
            repeatMode = RepeatMode.Restart
        ),
        label = "ringScale"
    )

    Box(
        contentAlignment = Alignment.Center
    ) {
        // Animated ring (only during connecting states or reconnecting)
        if (isReconnecting || callState in listOf(CallState.OUTGOING, CallState.INCOMING, CallState.CONNECTING)) {
            Box(
                modifier = Modifier
                    .size(160.dp)
                    .scale(ringScale)
                    .clip(CircleShape)
                    .background(Color(0xFF00D9FF).copy(alpha = ringAlpha))
            )
        }

        // Main avatar
        Box(
            modifier = Modifier
                .size(140.dp)
                .scale(scale)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        colors = when {
                            isReconnecting -> listOf(Color(0xFFFFA500), Color(0xFFFFD700))
                            callState == CallState.CONNECTED -> listOf(Color(0xFF00FF94), Color(0xFF00D9FF))
                            callState in listOf(CallState.FAILED, CallState.ENDED) -> listOf(Color(0xFFFF6B6B), Color(0xFFFF8E8E))
                            else -> listOf(Color(0xFF00D9FF), Color(0xFF00FF94))
                        }
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = name.take(1).uppercase(),
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 56.sp
            )
        }
    }
}

@Composable
private fun CallStatusText(
    callState: CallState,
    duration: Long,
    isReconnecting: Boolean = false,
    networkType: avinash.app.audiocallapp.webrtc.NetworkType = avinash.app.audiocallapp.webrtc.NetworkType.UNKNOWN
) {
    val statusText = when {
        isReconnecting && callState in listOf(CallState.CONNECTING, CallState.CONNECTED) -> {
            val networkInfo = when (networkType) {
                avinash.app.audiocallapp.webrtc.NetworkType.WIFI -> "Reconnecting (Wi-Fi)..."
                avinash.app.audiocallapp.webrtc.NetworkType.MOBILE -> "Reconnecting (Mobile)..."
                else -> "Reconnecting..."
            }
            networkInfo
        }
        callState == CallState.IDLE -> "Initializing..."
        callState == CallState.OUTGOING -> "Calling..."
        callState == CallState.INCOMING -> "Connecting..."
        callState == CallState.CONNECTING -> "Connecting..."
        callState == CallState.CONNECTED -> formatDuration(duration)
        callState == CallState.ENDED -> "Call ended"
        callState == CallState.FAILED -> "Call failed"
        else -> ""
    }

    val statusColor = when {
        isReconnecting -> Color(0xFFFFA500) // Orange for reconnecting
        callState == CallState.CONNECTED -> Color(0xFF00FF94)
        callState in listOf(CallState.FAILED, CallState.ENDED) -> Color(0xFFFF6B6B)
        else -> Color(0xFF00D9FF)
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = statusColor.copy(alpha = 0.2f)
        )
    ) {
        Text(
            text = statusText,
            color = statusColor,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun CallControls(
    isMuted: Boolean,
    isSpeakerOn: Boolean,
    callState: CallState,
    onToggleMute: () -> Unit,
    onToggleSpeaker: () -> Unit,
    onEndCall: () -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(24.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Mute button
        CallControlButton(
            icon = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
            label = if (isMuted) "Unmute" else "Mute",
            isActive = isMuted,
            enabled = callState == CallState.CONNECTED,
            onClick = onToggleMute
        )

        // End call button
        IconButton(
            onClick = onEndCall,
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(Color(0xFFFF6B6B))
        ) {
            Icon(
                imageVector = Icons.Default.CallEnd,
                contentDescription = "End call",
                tint = Color.White,
                modifier = Modifier.size(36.dp)
            )
        }

        // Speaker button
        CallControlButton(
            icon = if (isSpeakerOn) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
            label = if (isSpeakerOn) "Speaker" else "Earpiece",
            isActive = isSpeakerOn,
            enabled = callState == CallState.CONNECTED,
            onClick = onToggleSpeaker
        )
    }
}

@Composable
private fun CallControlButton(
    icon: ImageVector,
    label: String,
    isActive: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        IconButton(
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
                .background(
                    if (isActive) Color(0xFF00D9FF).copy(alpha = 0.3f)
                    else Color.White.copy(alpha = 0.1f)
                )
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (enabled) {
                    if (isActive) Color(0xFF00D9FF) else Color.White
                } else {
                    Color.White.copy(alpha = 0.3f)
                },
                modifier = Modifier.size(28.dp)
            )
        }

        Text(
            text = label,
            color = Color.White.copy(alpha = if (enabled) 0.7f else 0.3f),
            fontSize = 12.sp,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

private fun formatDuration(seconds: Long): String {
    val minutes = seconds / 60
    val secs = seconds % 60
    return "%02d:%02d".format(minutes, secs)
}
