package avinash.app.audiocallapp.presentation.main.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import avinash.app.audiocallapp.data.model.User
import avinash.app.audiocallapp.data.model.UserStatus
import avinash.app.audiocallapp.navigation.Screen
import avinash.app.audiocallapp.presentation.connection.ConnectionViewModel
import avinash.app.audiocallapp.presentation.userlist.UserListViewModel
import avinash.app.audiocallapp.ui.theme.*

enum class HomeMode { CALLS, WALKIE_TALKIE }

@Composable
fun HomeScreen(
    rootNavController: NavController,
    innerNavController: NavController,
    userListViewModel: UserListViewModel,
    connectionViewModel: ConnectionViewModel
) {
    val uiState by userListViewModel.uiState.collectAsState()
    val connState by connectionViewModel.uiState.collectAsState()
    val currentUser = uiState.currentUser
    val friendIds = connState.friends.map { it.getFriendId(connState.currentUserId) }.toSet()
    val friends = uiState.availableUsers
        .filter { it.uniqueId in friendIds }
        .sortedByDescending { it.status == UserStatus.ONLINE }

    var mode by rememberSaveable { mutableStateOf(HomeMode.CALLS) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        ModeToggle(mode = mode, onModeChange = { mode = it })

        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent)
        ) {
            val gradientColors = if (mode == HomeMode.CALLS)
                listOf(Blue600, Purple600)
            else
                listOf(Color(0xFF00897B), Color(0xFF26A69A))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.horizontalGradient(gradientColors))
                    .padding(24.dp)
            ) {
                Column {
                    Text(
                        "Welcome back, ${currentUser?.displayName ?: "User"}!",
                        fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        if (mode == HomeMode.CALLS) "${friends.size} friends"
                        else "${friends.count { it.status == UserStatus.ONLINE }} friends online",
                        color = Color.White.copy(alpha = 0.8f)
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = { innerNavController.navigate(Screen.Requests.route) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = gradientColors.first()),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Filled.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("View Requests")
                    }
                }
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            QuickActionCard("Friends", "${friends.size} total", Icons.Filled.People, Blue100, Blue600, Modifier.weight(1f)) {
                innerNavController.navigate(Screen.Friends.route)
            }
            QuickActionCard("Call History", "Recent", Icons.Filled.History, Green100, Green600, Modifier.weight(1f)) {
                innerNavController.navigate(Screen.History.route)
            }
        }

        Card(shape = RoundedCornerShape(16.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Friends", fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
                    TextButton(onClick = { innerNavController.navigate(Screen.Friends.route) }) { Text("View All") }
                }
                Spacer(Modifier.height(8.dp))
                if (uiState.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                } else if (friends.isEmpty()) {
                    Text("No friends yet", color = Gray500, modifier = Modifier.padding(16.dp))
                } else {
                    friends.take(4).forEach { user ->
                        when (mode) {
                            HomeMode.CALLS -> {
                                FriendRow(
                                    user = user,
                                    onClick = { innerNavController.navigate(Screen.ProfileDetail.createRoute(user.uniqueId)) },
                                    onCall = { rootNavController.navigate(Screen.Call.createRoute(user.uniqueId, user.displayName)) }
                                )
                            }
                            HomeMode.WALKIE_TALKIE -> {
                                WalkieTalkieFriendRow(
                                    user = user,
                                    onClick = { innerNavController.navigate(Screen.ProfileDetail.createRoute(user.uniqueId)) },
                                    onWalkieTalkie = {
                                        rootNavController.navigate(Screen.WalkieTalkie.createRoute(user.uniqueId, user.displayName))
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ModeToggle(mode: HomeMode, onModeChange: (HomeMode) -> Unit) {
    val callBg by animateColorAsState(
        if (mode == HomeMode.CALLS) Blue600 else MaterialTheme.colorScheme.surfaceVariant,
        label = "callBg"
    )
    val wtBg by animateColorAsState(
        if (mode == HomeMode.WALKIE_TALKIE) Color(0xFF00897B) else MaterialTheme.colorScheme.surfaceVariant,
        label = "wtBg"
    )
    val callText by animateColorAsState(
        if (mode == HomeMode.CALLS) Color.White else Gray600,
        label = "callText"
    )
    val wtText by animateColorAsState(
        if (mode == HomeMode.WALKIE_TALKIE) Color.White else Gray600,
        label = "wtText"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(12.dp))
                .background(callBg)
                .clickable { onModeChange(HomeMode.CALLS) }
                .padding(vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Phone, contentDescription = null, tint = callText, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Calls", fontWeight = FontWeight.SemiBold, color = callText)
            }
        }
        Box(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(12.dp))
                .background(wtBg)
                .clickable { onModeChange(HomeMode.WALKIE_TALKIE) }
                .padding(vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.RecordVoiceOver, contentDescription = null, tint = wtText, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Walkie-Talkie", fontWeight = FontWeight.SemiBold, color = wtText)
            }
        }
    }
}

@Composable
fun WalkieTalkieFriendRow(user: User, onClick: () -> Unit, onWalkieTalkie: () -> Unit) {
    val dotColor = when (user.status) {
        UserStatus.ONLINE -> Green500
        UserStatus.IN_CALL -> Yellow400
        else -> null
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF00897B)),
                contentAlignment = Alignment.Center
            ) {
                Text(user.displayName.take(1).uppercase(), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
            if (dotColor != null) {
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .align(Alignment.BottomEnd)
                        .padding(2.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize().clip(CircleShape).background(dotColor))
                }
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(user.displayName, fontWeight = FontWeight.Medium, color = Gray900)
            Text(
                if (user.status == UserStatus.ONLINE) "Online" else "@${user.uniqueId}",
                style = MaterialTheme.typography.bodySmall,
                color = if (user.status == UserStatus.ONLINE) Green500 else Gray600
            )
        }
        IconButton(onClick = onWalkieTalkie) {
            Icon(Icons.Filled.RecordVoiceOver, contentDescription = "Walkie-Talkie", tint = Color(0xFF00897B))
        }
    }
}

@Composable
private fun QuickActionCard(
    title: String, subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    bgColor: Color, iconColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(modifier = modifier.clickable(onClick = onClick), shape = RoundedCornerShape(16.dp)) {
        Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(bgColor), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.height(12.dp))
            Text(title, fontWeight = FontWeight.Medium, color = Gray900)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Gray600)
        }
    }
}

@Composable
fun FriendRow(user: User, onClick: () -> Unit, onCall: () -> Unit) {
    val dotColor = when (user.status) {
        UserStatus.ONLINE -> Green500
        UserStatus.IN_CALL -> Yellow400
        else -> null
    }

    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box {
            Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(Blue600), contentAlignment = Alignment.Center) {
                Text(user.displayName.take(1).uppercase(), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
            if (dotColor != null) {
                Box(
                    modifier = Modifier.size(14.dp).clip(CircleShape).background(Color.White).align(Alignment.BottomEnd).padding(2.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize().clip(CircleShape).background(dotColor))
                }
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(user.displayName, fontWeight = FontWeight.Medium, color = Gray900)
            Text("@${user.uniqueId}", style = MaterialTheme.typography.bodySmall, color = Gray600)
        }
        IconButton(onClick = onCall) {
            Icon(Icons.Filled.Phone, contentDescription = "Call", tint = Gray600)
        }
    }
}
