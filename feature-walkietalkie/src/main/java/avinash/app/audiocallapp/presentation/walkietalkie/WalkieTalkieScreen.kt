package avinash.app.audiocallapp.presentation.walkietalkie

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.SignalWifi4Bar
import androidx.compose.material.icons.filled.SignalWifiOff
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.WifiFind
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import avinash.app.audiocallapp.walkietalkie.WtPeerState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalkieTalkieScreen(
    friendId: String,
    friendName: String,
    navController: NavController,
    viewModel: WalkieTalkieViewModel = hiltViewModel()
) {
    val isServiceActive by viewModel.isActive.collectAsState()
    val peerStates by viewModel.peerStates.collectAsState()
    val isTalking by viewModel.isTalking.collectAsState()
    val talkingToFriendId by viewModel.talkingToFriendId.collectAsState()
    val remoteSpeaking by viewModel.remoteSpeakingFriends.collectAsState()

    val connectionState = peerStates[friendId] ?: WtPeerState.DISCONNECTED
    val isConnected = connectionState == WtPeerState.CONNECTED
    val isFriendSpeaking = friendId in remoteSpeaking
    val amITalkingToThisFriend = isTalking && talkingToFriendId == friendId

    val bgGradient = Brush.verticalGradient(
        when {
            isConnected -> listOf(Color(0xFF0D1B2A), Color(0xFF1B2838), Color(0xFF0D1B2A))
            connectionState == WtPeerState.CONNECTING || connectionState == WtPeerState.RECONNECTING ->
                listOf(Color(0xFF1A1A2E), Color(0xFF16213E), Color(0xFF1A1A2E))
            else -> listOf(Color(0xFF1C1C1E), Color(0xFF2C2C2E), Color(0xFF1C1C1E))
        }
    )

    Box(modifier = Modifier.fillMaxSize().background(bgGradient)) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text(friendName, color = Color.White) },
                    navigationIcon = {
                        IconButton(onClick = {
                            viewModel.stopTalking()
                            navController.popBackStack()
                        }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Spacer(Modifier.height(8.dp))

                ConnectionStatusSection(
                    state = connectionState,
                    isServiceActive = isServiceActive
                )

                Spacer(Modifier.weight(0.3f))

                FriendAvatarSection(
                    friendName = friendName,
                    isFriendSpeaking = isFriendSpeaking,
                    isConnected = isConnected,
                    connectionState = connectionState
                )

                Spacer(Modifier.weight(0.3f))

                AnimatedVisibility(
                    visible = !isConnected,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    WaitingForConnectionCard(
                        state = connectionState,
                        isServiceActive = isServiceActive,
                        friendName = friendName
                    )
                }

                Spacer(Modifier.weight(0.4f))

                PushToTalkButton(
                    isEnabled = isConnected,
                    isTalking = amITalkingToThisFriend,
                    onTalkStart = { viewModel.startTalking(friendId) },
                    onTalkStop = { viewModel.stopTalking() }
                )

                Spacer(Modifier.height(24.dp))

                Text(
                    text = when {
                        !isServiceActive -> "Walkie-Talkie service starting..."
                        connectionState == WtPeerState.FAILED -> "Connection failed. Retrying..."
                        connectionState == WtPeerState.RECONNECTING -> "Reconnecting to $friendName..."
                        connectionState == WtPeerState.CONNECTING -> "Establishing connection..."
                        connectionState == WtPeerState.DISCONNECTED -> "Waiting for $friendName to come online..."
                        amITalkingToThisFriend -> "Transmitting..."
                        isFriendSpeaking -> "$friendName is speaking..."
                        else -> "Hold to talk"
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(48.dp))
            }
        }
    }
}

@Composable
private fun ConnectionStatusSection(state: WtPeerState, isServiceActive: Boolean) {
    val (text, color, icon) = when {
        !isServiceActive -> Triple("Service Starting...", Color(0xFFFF9800), Icons.Filled.Sync)
        state == WtPeerState.CONNECTED -> Triple("Connected", Color(0xFF4CAF50), Icons.Filled.SignalWifi4Bar)
        state == WtPeerState.CONNECTING -> Triple("Connecting...", Color(0xFFFFC107), Icons.Filled.WifiFind)
        state == WtPeerState.RECONNECTING -> Triple("Reconnecting...", Color(0xFFFF9800), Icons.Filled.Sync)
        state == WtPeerState.FAILED -> Triple("Connection Failed", Color(0xFFF44336), Icons.Filled.SignalWifiOff)
        else -> Triple("Waiting for Connection", Color(0xFF9E9E9E), Icons.Filled.WifiFind)
    }

    val isAnimating = state == WtPeerState.CONNECTING || state == WtPeerState.RECONNECTING || !isServiceActive
    val spinTransition = rememberInfiniteTransition(label = "spin")
    val rotation by spinTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(1500, easing = LinearEasing)),
        label = "rotation"
    )

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = color.copy(alpha = 0.12f),
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier
                    .size(18.dp)
                    .then(if (isAnimating) Modifier.rotate(rotation) else Modifier)
            )
            Text(
                text = text,
                color = color,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun WaitingForConnectionCard(
    state: WtPeerState,
    isServiceActive: Boolean,
    friendName: String
) {
    val dotAnim = rememberInfiniteTransition(label = "dots")
    val dotAlpha1 by dotAnim.animateFloat(
        initialValue = 0.3f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(600), RepeatMode.Reverse),
        label = "dot1"
    )
    val dotAlpha2 by dotAnim.animateFloat(
        initialValue = 0.3f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(600, delayMillis = 200), RepeatMode.Reverse),
        label = "dot2"
    )
    val dotAlpha3 by dotAnim.animateFloat(
        initialValue = 0.3f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(600, delayMillis = 400), RepeatMode.Reverse),
        label = "dot3"
    )

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.08f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(dotAlpha1, dotAlpha2, dotAlpha3).forEach { alpha ->
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF42A5F5).copy(alpha = alpha))
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            val (title, subtitle) = when {
                !isServiceActive -> "Starting Service" to "Initializing walkie-talkie engine..."
                state == WtPeerState.CONNECTING -> "Connecting" to "Setting up audio channel with $friendName..."
                state == WtPeerState.RECONNECTING -> "Reconnecting" to "Lost connection. Attempting to restore..."
                state == WtPeerState.FAILED -> "Retrying" to "Connection failed. Auto-retrying with backoff..."
                else -> "Waiting for Connection" to "$friendName needs to be online for the channel to open."
            }

            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.6f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun FriendAvatarSection(
    friendName: String,
    isFriendSpeaking: Boolean,
    isConnected: Boolean,
    connectionState: WtPeerState
) {
    val pulseAnim = rememberInfiniteTransition(label = "pulse")
    val pulseScale by pulseAnim.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val waitPulse by pulseAnim.animateFloat(
        initialValue = 1f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "waitPulse"
    )

    val avatarScale = when {
        isFriendSpeaking -> pulseScale
        !isConnected -> waitPulse
        else -> 1f
    }

    val ringColor by animateColorAsState(
        targetValue = when {
            isFriendSpeaking -> Color(0xFF4CAF50)
            isConnected -> Color(0xFF2196F3)
            connectionState == WtPeerState.CONNECTING -> Color(0xFFFFC107)
            connectionState == WtPeerState.RECONNECTING -> Color(0xFFFF9800)
            connectionState == WtPeerState.FAILED -> Color(0xFFF44336)
            else -> Color(0xFF9E9E9E)
        },
        label = "ringColor"
    )

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .scale(avatarScale)
                    .clip(CircleShape)
                    .background(ringColor.copy(alpha = 0.15f))
            )
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .scale(avatarScale)
                    .clip(CircleShape)
                    .background(ringColor.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = friendName.firstOrNull()?.uppercase() ?: "?",
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        AnimatedVisibility(visible = isFriendSpeaking) {
            Text(
                text = "$friendName is speaking...",
                style = MaterialTheme.typography.titleMedium,
                color = Color(0xFF4CAF50),
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun PushToTalkButton(
    isEnabled: Boolean,
    isTalking: Boolean,
    onTalkStart: () -> Unit,
    onTalkStop: () -> Unit
) {
    val buttonColor by animateColorAsState(
        targetValue = when {
            !isEnabled -> Color(0xFF424242)
            isTalking -> Color(0xFFF44336)
            else -> Color(0xFF2196F3)
        },
        label = "buttonColor"
    )

    val pulseAnim = rememberInfiniteTransition(label = "talkPulse")
    val talkPulse by pulseAnim.animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(tween(400), RepeatMode.Reverse),
        label = "talkPulseScale"
    )

    val scale = if (isTalking) talkPulse else 1f

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(160.dp)
                .scale(scale)
                .clip(CircleShape)
                .background(buttonColor)
                .then(
                    if (isEnabled) {
                        Modifier.pointerInput(Unit) {
                            detectTapGestures(
                                onPress = {
                                    onTalkStart()
                                    tryAwaitRelease()
                                    onTalkStop()
                                }
                            )
                        }
                    } else {
                        Modifier
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = if (isTalking) Icons.Default.Mic else Icons.Default.MicOff,
                    contentDescription = if (isTalking) "Talking" else "Hold to Talk",
                    tint = Color.White,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = when {
                        !isEnabled -> "WAITING"
                        isTalking -> "RELEASE"
                        else -> "HOLD\nTO TALK"
                    },
                    color = Color.White.copy(alpha = if (isEnabled) 1f else 0.5f),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 16.sp
                )
            }
        }
    }
}
