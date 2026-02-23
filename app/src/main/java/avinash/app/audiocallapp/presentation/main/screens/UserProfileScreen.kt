package avinash.app.audiocallapp.presentation.main.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import avinash.app.audiocallapp.data.local.entity.CallHistoryEntity
import avinash.app.audiocallapp.navigation.Screen
import avinash.app.audiocallapp.presentation.call.CallHistoryViewModel
import avinash.app.audiocallapp.presentation.connection.ConnectionViewModel
import avinash.app.audiocallapp.presentation.userlist.UserListViewModel
import avinash.app.audiocallapp.data.model.UserStatus
import avinash.app.audiocallapp.ui.theme.*
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserProfileScreen(
    userId: String?,
    rootNavController: NavController,
    innerNavController: NavController,
    userListViewModel: UserListViewModel,
    connectionViewModel: ConnectionViewModel,
    callHistoryViewModel: CallHistoryViewModel = hiltViewModel()
) {
    val uiState by userListViewModel.uiState.collectAsState()
    val connState by connectionViewModel.uiState.collectAsState()
    val isOwnProfile = userId == null
    val currentUser = uiState.currentUser
    val profileUser = if (isOwnProfile) null else uiState.availableUsers.find { it.uniqueId == userId }

    val name = if (isOwnProfile) currentUser?.displayName ?: "User" else profileUser?.displayName ?: "User"
    val username = if (isOwnProfile) currentUser?.uniqueId ?: "" else profileUser?.uniqueId ?: userId ?: ""
    val userStatus = if (isOwnProfile) UserStatus.ONLINE else profileUser?.status ?: UserStatus.OFFLINE
    val isOnline = userStatus == UserStatus.ONLINE || userStatus == UserStatus.IN_CALL
    val createdAt = if (isOwnProfile) currentUser?.createdAt else profileUser?.createdAt

    val connection = if (!isOwnProfile) {
        connState.friends.find { it.getFriendId(connState.currentUserId) == userId }
    } else null
    val isFriend = connection != null
    val hasPendingRequest = if (!isOwnProfile) connState.sentRequests.any { it.receiverId == userId } else false
    val hasReceivedRequest = if (!isOwnProfile) connState.receivedRequests.find { it.senderId == userId } else null

    var callStats by remember { mutableStateOf<List<CallHistoryEntity>>(emptyList()) }
    LaunchedEffect(userId) {
        if (userId != null) {
            callStats = callHistoryViewModel.getCallsWithUser(userId)
        }
    }

    val totalCalls = callStats.size
    val avgDuration = if (callStats.isNotEmpty()) callStats.map { it.durationSeconds }.average().toLong() else 0L
    val lastCallTs = callStats.maxByOrNull { it.timestamp }?.timestamp
    val sdf = remember { SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()) }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Box(
            modifier = Modifier.fillMaxWidth().height(160.dp).background(Brush.horizontalGradient(listOf(Blue600, Purple600)))
        ) {
            Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                IconButton(onClick = { innerNavController.popBackStack() }) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = null, tint = Color.White)
                }
                if (isOwnProfile) {
                    IconButton(onClick = { innerNavController.navigate(Screen.Settings.route) }) {
                        Icon(Icons.Filled.Settings, contentDescription = null, tint = Color.White)
                    }
                }
            }
        }

        Column(modifier = Modifier.offset(y = (-48).dp).padding(horizontal = 16.dp)) {
            Card(shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box {
                        Box(
                            modifier = Modifier.size(96.dp).clip(CircleShape).background(Blue600),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(name.take(1).uppercase(), fontSize = 36.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                        if (isOnline) {
                            Box(
                                modifier = Modifier.size(20.dp).clip(CircleShape).background(Color.White).align(Alignment.BottomEnd).padding(3.dp)
                            ) { Box(modifier = Modifier.fillMaxSize().clip(CircleShape).background(Green500)) }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Text(name, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Gray900)
                    Text("@$username", color = Gray600)

                    Spacer(Modifier.height(20.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (isOwnProfile) {
                            OutlinedButton(
                                onClick = { innerNavController.navigate(Screen.EditProfile.route) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Edit Profile")
                            }
                        } else {
                            when {
                                isFriend -> {
                                    Button(
                                        onClick = { rootNavController.navigate(Screen.Call.createRoute(userId ?: "", name)) },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(Icons.Filled.Phone, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text("Call")
                                    }
                                    OutlinedButton(
                                        onClick = {
                                            connection?.let { connectionViewModel.removeFriend(it.id) }
                                            innerNavController.popBackStack()
                                        },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(Icons.Filled.PersonRemove, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text("Remove")
                                    }
                                }
                                hasReceivedRequest != null -> {
                                    Button(
                                        onClick = { connectionViewModel.acceptRequest(hasReceivedRequest) },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text("Accept")
                                    }
                                    OutlinedButton(
                                        onClick = { connectionViewModel.rejectRequest(hasReceivedRequest.id) },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(Icons.Filled.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text("Reject")
                                    }
                                }
                                hasPendingRequest -> {
                                    OutlinedButton(
                                        onClick = {
                                            val reqId = connState.sentRequests.find { it.receiverId == userId }?.id
                                            if (reqId != null) connectionViewModel.cancelRequest(reqId)
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(Icons.Filled.HourglassTop, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text("Cancel Request")
                                    }
                                }
                                else -> {
                                    Button(
                                        onClick = { connectionViewModel.sendRequest(userId ?: "", name) },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(Icons.Filled.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text("Connect")
                                    }
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(24.dp))
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text("About", style = MaterialTheme.typography.labelMedium, color = Gray500)
                        Spacer(Modifier.height(4.dp))
                        Text("Love connecting with friends through voice calls!", color = Gray900)

                        Spacer(Modifier.height(16.dp))
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Member Since", style = MaterialTheme.typography.labelMedium, color = Gray500)
                                Spacer(Modifier.height(4.dp))
                                Text(formatCreatedAt(createdAt), color = Gray900)
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Status", style = MaterialTheme.typography.labelMedium, color = Gray500)
                                Spacer(Modifier.height(4.dp))
                                Text(if (isOnline) "Online" else "Offline", color = Gray900)
                            }
                        }
                    }

                    if (!isOwnProfile && isFriend) {
                        Spacer(Modifier.height(24.dp))
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text("Call Activity", style = MaterialTheme.typography.labelMedium, color = Gray500)
                            Spacer(Modifier.height(8.dp))
                            ProfileStatRow(
                                "Last call",
                                if (lastCallTs != null) sdf.format(Date(lastCallTs)) else "No calls yet"
                            )
                            ProfileStatRow("Total calls", if (totalCalls > 0) "$totalCalls calls" else "No calls yet")
                            ProfileStatRow(
                                "Avg duration",
                                if (avgDuration > 0) "${avgDuration / 60}:${(avgDuration % 60).toString().padStart(2, '0')}" else "-"
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun formatCreatedAt(timestamp: Timestamp?): String {
    if (timestamp == null) return "Unknown"
    val sdf = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
    return sdf.format(Date(timestamp.seconds * 1000))
}

@Composable
private fun ProfileStatRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = Gray600)
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium, color = Gray900)
    }
    HorizontalDivider(color = Gray100)
}
