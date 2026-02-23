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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import avinash.app.audiocallapp.call.CallState
import avinash.app.audiocallapp.feature.Routes
import avinash.app.audiocallapp.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun CallScreen(
    remoteUserId: String,
    remoteName: String,
    isCaller: Boolean,
    navController: NavController,
    callViewModel: CallViewModel = hiltViewModel()
) {
    val manager = callViewModel.callManager
    val uiState by manager.callUiState.collectAsState()
    val incomingCall by manager.incomingCall.collectAsState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(remoteUserId, isCaller) {
        if (isCaller && uiState.callState == CallState.IDLE) {
            manager.initiateOutgoingCall(remoteUserId, remoteName)
        }
    }

    val gradient = Brush.verticalGradient(
        listOf(Color(0xFF1A1A2E), Color(0xFF16213E), Color(0xFF0F3460))
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(gradient)
    ) {
        when {
            uiState.callState == CallState.IDLE && !isCaller && incomingCall != null -> {
                IncomingContent(
                    remoteName = remoteName,
                    onAccept = { manager.acceptIncomingCall() },
                    onReject = {
                        scope.launch { manager.rejectIncomingCall() }
                        navController.navigate(Routes.MAIN) { popUpTo(0) { inclusive = true } }
                    }
                )
            }
            uiState.callState == CallState.IDLE && !isCaller && incomingCall == null -> {
                CallingContent(
                    remoteName = remoteName,
                    state = CallState.CONNECTING,
                    onCancel = {
                        manager.endCall()
                        navController.navigate(Routes.MAIN) { popUpTo(0) { inclusive = true } }
                    }
                )
            }
            uiState.callState in listOf(CallState.IDLE, CallState.OUTGOING, CallState.INCOMING, CallState.CONNECTING) -> {
                CallingContent(
                    remoteName = uiState.remoteUserName.ifEmpty { remoteName },
                    state = uiState.callState,
                    onCancel = {
                        manager.endCall()
                        navController.navigate(Routes.MAIN) { popUpTo(0) { inclusive = true } }
                    }
                )
            }
            uiState.callState == CallState.CONNECTED -> {
                ConnectedContent(
                    remoteName = uiState.remoteUserName.ifEmpty { remoteName },
                    duration = uiState.callDuration,
                    isMuted = uiState.isMuted,
                    isSpeakerOn = uiState.isSpeakerOn,
                    onToggleMute = { manager.toggleMute() },
                    onToggleSpeaker = { manager.toggleSpeaker() },
                    onEndCall = {
                        manager.endCall()
                        navController.navigate(Routes.MAIN) { popUpTo(0) { inclusive = true } }
                    }
                )
            }
            uiState.callState == CallState.ENDED -> {
                EndedContent(
                    remoteName = uiState.remoteUserName.ifEmpty { remoteName },
                    duration = uiState.callDuration,
                    onGoHome = {
                        manager.resetState()
                        navController.navigate(Routes.MAIN) { popUpTo(0) { inclusive = true } }
                    }
                )
            }
            uiState.callState == CallState.FAILED -> {
                FailedContent(
                    remoteName = uiState.remoteUserName.ifEmpty { remoteName },
                    errorMessage = uiState.errorMessage,
                    onGoHome = {
                        manager.resetState()
                        navController.navigate(Routes.MAIN) { popUpTo(0) { inclusive = true } }
                    }
                )
            }
        }

        val showBar = incomingCall != null && incomingCall!!.callerId != uiState.remoteUserId
        if (showBar) {
            IncomingCallBar(
                callerName = incomingCall!!.callerName,
                onAccept = {
                    manager.endCall()
                    manager.acceptIncomingCall()
                    navController.navigate(
                        Routes.Call.create(incomingCall!!.callerId, incomingCall!!.callerName, isCaller = false)
                    ) { popUpTo(0) { inclusive = true } }
                },
                onReject = {
                    scope.launch { manager.rejectIncomingCall() }
                },
                onBarTap = {
                    manager.endCall()
                    navController.navigate(
                        Routes.Call.create(incomingCall!!.callerId, incomingCall!!.callerName, isCaller = false)
                    ) { popUpTo(0) { inclusive = true } }
                }
            )
        }
    }
}

@Composable
private fun IncomingContent(
    remoteName: String,
    onAccept: () -> Unit,
    onReject: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "ring")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.2f,
        animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
        label = "ringScale"
    )

    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Spacer(Modifier.weight(1f))

        Box(contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier.size(160.dp).scale(scale).clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.08f))
            )
            Box(
                modifier = Modifier.size(128.dp).clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    remoteName.take(1).uppercase(),
                    fontSize = 48.sp, fontWeight = FontWeight.Bold, color = Color.White
                )
            }
        }

        Spacer(Modifier.height(24.dp))
        Text(remoteName, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Spacer(Modifier.height(8.dp))
        Text("Incoming call...", fontSize = 18.sp, color = Color.White.copy(alpha = 0.7f))

        Spacer(Modifier.weight(1f))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            CallActionButton(
                icon = Icons.Filled.CallEnd,
                backgroundColor = Red500,
                onClick = onReject
            )
            CallActionButton(
                icon = Icons.Filled.Call,
                backgroundColor = Green600,
                onClick = onAccept
            )
        }

        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun CallingContent(
    remoteName: String,
    state: CallState,
    onCancel: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.3f,
        animationSpec = infiniteRepeatable(tween(1000), RepeatMode.Reverse),
        label = "scale"
    )

    val statusText = when (state) {
        CallState.OUTGOING -> "Calling..."
        CallState.INCOMING, CallState.CONNECTING -> "Connecting..."
        else -> "Calling..."
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Spacer(Modifier.weight(1f))

        Box(contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier.size(160.dp).scale(scale).clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.08f))
            )
            Box(
                modifier = Modifier.size(128.dp).clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    remoteName.take(1).uppercase(),
                    fontSize = 48.sp, fontWeight = FontWeight.Bold, color = Color.White
                )
            }
        }

        Spacer(Modifier.height(24.dp))
        Text(remoteName, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Spacer(Modifier.height(8.dp))
        Text(statusText, fontSize = 18.sp, color = Color.White.copy(alpha = 0.7f))

        Spacer(Modifier.weight(1f))

        CallActionButton(
            icon = Icons.Filled.CallEnd,
            backgroundColor = Red500,
            onClick = onCancel
        )

        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun ConnectedContent(
    remoteName: String,
    duration: Long,
    isMuted: Boolean,
    isSpeakerOn: Boolean,
    onToggleMute: () -> Unit,
    onToggleSpeaker: () -> Unit,
    onEndCall: () -> Unit
) {
    val formattedTime = "${duration / 60}:${(duration % 60).toString().padStart(2, '0')}"

    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Text("Connected", color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp)

        Spacer(Modifier.weight(1f))

        Box(
            modifier = Modifier.size(128.dp).clip(CircleShape)
                .background(Green600.copy(alpha = 0.3f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                remoteName.take(1).uppercase(),
                fontSize = 48.sp, fontWeight = FontWeight.Bold, color = Color.White
            )
        }

        Spacer(Modifier.height(24.dp))
        Text(remoteName, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Spacer(Modifier.height(8.dp))
        Text(formattedTime, fontSize = 40.sp, fontWeight = FontWeight.Light, color = Color.White)

        Spacer(Modifier.weight(1f))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            CallControlButton(
                icon = Icons.Filled.MicOff,
                label = if (isMuted) "Unmute" else "Mute",
                isActive = isMuted,
                onClick = onToggleMute
            )
            CallControlButton(
                icon = Icons.Filled.VolumeUp,
                label = if (isSpeakerOn) "Earpiece" else "Speaker",
                isActive = isSpeakerOn,
                onClick = onToggleSpeaker
            )
        }

        Spacer(Modifier.height(32.dp))

        CallActionButton(
            icon = Icons.Filled.CallEnd,
            backgroundColor = Red500,
            onClick = onEndCall
        )

        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun EndedContent(
    remoteName: String,
    duration: Long,
    onGoHome: () -> Unit
) {
    val formattedTime = "${duration / 60}:${(duration % 60).toString().padStart(2, '0')}"

    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier.size(96.dp).clip(CircleShape)
                .background(Color.White.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                remoteName.take(1).uppercase(),
                fontSize = 40.sp, fontWeight = FontWeight.Bold, color = Color.White
            )
        }

        Spacer(Modifier.height(24.dp))
        Text("Call Ended", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Spacer(Modifier.height(8.dp))
        Text(remoteName, fontSize = 18.sp, color = Color.White.copy(alpha = 0.7f))
        Spacer(Modifier.height(16.dp))

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.1f))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 32.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(32.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Duration", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.5f))
                    Text(formattedTime, fontSize = 20.sp, fontWeight = FontWeight.Medium, color = Color.White)
                }
            }
        }

        Spacer(Modifier.height(48.dp))

        OutlinedButton(
            onClick = onGoHome,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
        ) {
            Icon(Icons.Filled.Home, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text("Go Home")
        }
    }
}

@Composable
private fun FailedContent(
    remoteName: String,
    errorMessage: String?,
    onGoHome: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier.size(96.dp).clip(CircleShape)
                .background(Red500.copy(alpha = 0.3f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.CallEnd, contentDescription = null, tint = Color.White, modifier = Modifier.size(48.dp))
        }

        Spacer(Modifier.height(24.dp))
        Text("Call Failed", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Spacer(Modifier.height(8.dp))
        Text(remoteName, fontSize = 18.sp, color = Color.White.copy(alpha = 0.7f))

        if (errorMessage != null) {
            Spacer(Modifier.height(16.dp))
            Text(
                errorMessage,
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.5f),
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        Spacer(Modifier.height(48.dp))

        OutlinedButton(
            onClick = onGoHome,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
        ) {
            Icon(Icons.Filled.Home, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text("Go Home")
        }
    }
}

@Composable
fun CallActionButton(
    icon: ImageVector,
    backgroundColor: Color,
    onClick: () -> Unit,
    size: Int = 64
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(backgroundColor)
    ) {
        Icon(
            icon, contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size((size / 2.5).dp)
        )
    }
}

@Composable
private fun CallControlButton(
    icon: ImageVector,
    label: String,
    isActive: Boolean,
    onClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(
            onClick = onClick,
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(if (isActive) Color.White else Color.White.copy(alpha = 0.15f))
        ) {
            Icon(
                icon, contentDescription = label,
                tint = if (isActive) Color(0xFF0F3460) else Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(label, color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.labelSmall)
    }
}
