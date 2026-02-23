package avinash.app.audiocallapp.presentation.main.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import avinash.app.audiocallapp.data.model.UserStatus
import avinash.app.audiocallapp.navigation.Screen
import avinash.app.audiocallapp.presentation.connection.ConnectionViewModel
import avinash.app.audiocallapp.presentation.userlist.UserListViewModel
import avinash.app.audiocallapp.ui.theme.*

@Composable
fun FriendsListScreen(
    rootNavController: NavController,
    innerNavController: NavController,
    userListViewModel: UserListViewModel,
    connectionViewModel: ConnectionViewModel
) {
    val userState by userListViewModel.uiState.collectAsState()
    val connState by connectionViewModel.uiState.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var mode by rememberSaveable { mutableStateOf(HomeMode.CALLS) }

    val friendIds = connState.friends.map { it.getFriendId(connState.currentUserId) }.toSet()
    val allFriends = userState.availableUsers
        .filter { it.uniqueId in friendIds }
        .sortedByDescending { it.status == UserStatus.ONLINE }
    val onlineCount = allFriends.count { it.status == UserStatus.ONLINE }

    val displayUsers = allFriends.filter {
        searchQuery.isBlank() || it.displayName.contains(searchQuery, true) || it.uniqueId.contains(searchQuery, true)
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text("Friends", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Text("$onlineCount online \u00B7 ${allFriends.size} total", color = Gray600, fontSize = 14.sp)
            }
            Button(onClick = { innerNavController.navigate(Screen.Search.route) }, shape = RoundedCornerShape(12.dp)) {
                Icon(Icons.Filled.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Add Friends")
            }
        }
        Spacer(Modifier.height(12.dp))

        ModeToggleCompact(mode = mode, onModeChange = { mode = it })
        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = searchQuery, onValueChange = { searchQuery = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search friends...") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            singleLine = true, shape = RoundedCornerShape(12.dp)
        )
        Spacer(Modifier.height(12.dp))

        if (userState.isLoading || connState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                items(displayUsers, key = { it.uniqueId }) { user ->
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
                if (displayUsers.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Filled.People, contentDescription = null, modifier = Modifier.size(64.dp), tint = Gray300)
                                Spacer(Modifier.height(16.dp))
                                Text("No friends yet", fontWeight = FontWeight.Medium, fontSize = 18.sp)
                                Text("Search and add friends to get started", color = Gray600)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ModeToggleCompact(mode: HomeMode, onModeChange: (HomeMode) -> Unit) {
    val callBg by animateColorAsState(
        if (mode == HomeMode.CALLS) Blue600 else MaterialTheme.colorScheme.surfaceVariant, label = "callBg"
    )
    val wtBg by animateColorAsState(
        if (mode == HomeMode.WALKIE_TALKIE) Color(0xFF00897B) else MaterialTheme.colorScheme.surfaceVariant, label = "wtBg"
    )
    val callText by animateColorAsState(if (mode == HomeMode.CALLS) Color.White else Gray600, label = "callTxt")
    val wtText by animateColorAsState(if (mode == HomeMode.WALKIE_TALKIE) Color.White else Gray600, label = "wtTxt")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(10.dp))
                .background(callBg)
                .clickable { onModeChange(HomeMode.CALLS) }
                .padding(vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Phone, null, tint = callText, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Calls", fontWeight = FontWeight.Medium, color = callText, fontSize = 14.sp)
            }
        }
        Box(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(10.dp))
                .background(wtBg)
                .clickable { onModeChange(HomeMode.WALKIE_TALKIE) }
                .padding(vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.RecordVoiceOver, null, tint = wtText, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Walkie-Talkie", fontWeight = FontWeight.Medium, color = wtText, fontSize = 14.sp)
            }
        }
    }
}
