package avinash.app.audiocallapp.presentation.main.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import avinash.app.audiocallapp.navigation.Screen
import avinash.app.audiocallapp.presentation.call.CallHistoryViewModel
import avinash.app.audiocallapp.presentation.connection.ConnectionViewModel
import avinash.app.audiocallapp.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private data class NotifItem(
    val id: String,
    val type: String,
    val title: String,
    val desc: String,
    val timestamp: Long,
    val actionRoute: String?
)

@Composable
fun NotificationsScreen(
    innerNavController: NavController,
    rootNavController: NavController,
    connectionViewModel: ConnectionViewModel,
    callHistoryViewModel: CallHistoryViewModel = hiltViewModel()
) {
    val connState by connectionViewModel.uiState.collectAsState()
    val historyState by callHistoryViewModel.uiState.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }

    val sdf = remember { SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()) }
    val now = remember { System.currentTimeMillis() }

    val notifications = remember(historyState.calls, connState.receivedRequests) {
        val items = mutableListOf<NotifItem>()

        historyState.calls
            .filter { it.type == "missed" }
            .take(10)
            .forEach { call ->
                items.add(
                    NotifItem(
                        id = "missed_${call.id}",
                        type = "missed",
                        title = "Missed call from ${call.remoteName}",
                        desc = sdf.format(Date(call.timestamp)),
                        timestamp = call.timestamp,
                        actionRoute = Screen.Call.createRoute(call.remoteUserId, call.remoteName)
                    )
                )
            }

        connState.receivedRequests.forEach { req ->
            val ts = req.createdAt?.seconds?.times(1000) ?: now
            items.add(
                NotifItem(
                    id = "req_${req.id}",
                    type = "request",
                    title = "${req.senderName} sent you a connection request",
                    desc = sdf.format(Date(ts)),
                    timestamp = ts,
                    actionRoute = Screen.Requests.route
                )
            )
        }

        items.sortedByDescending { it.timestamp }
    }

    val unreadCount = historyState.missedCount + connState.receivedRequests.size

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Notifications", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                if (unreadCount > 0) Text("$unreadCount new", color = Gray600) else Text("All caught up", color = Gray600)
            }
            IconButton(onClick = { innerNavController.navigate(Screen.Settings.route) }) {
                Icon(Icons.Filled.Settings, contentDescription = null, tint = Gray600)
            }
        }

        if (notifications.isNotEmpty() && historyState.missedCount > 0) {
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = { callHistoryViewModel.markMissedSeen() }) {
                Text("Mark missed calls as seen")
            }
        }
        Spacer(Modifier.height(12.dp))

        if (notifications.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.Notifications, contentDescription = null, modifier = Modifier.size(64.dp), tint = Gray300)
                    Spacer(Modifier.height(16.dp))
                    Text("No notifications", fontWeight = FontWeight.Medium, fontSize = 18.sp)
                    Text("You're all caught up!", color = Gray600)
                }
            }
        } else {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }) {
                    Text("All (${notifications.size})", modifier = Modifier.padding(12.dp))
                }
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }) {
                    Text("Missed (${ historyState.calls.count { it.type == "missed" } })", modifier = Modifier.padding(12.dp))
                }
                Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }) {
                    Text("Requests (${connState.receivedRequests.size})", modifier = Modifier.padding(12.dp))
                }
            }
            Spacer(Modifier.height(8.dp))

            val displayList = when (selectedTab) {
                1 -> notifications.filter { it.type == "missed" }
                2 -> notifications.filter { it.type == "request" }
                else -> notifications
            }

            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(displayList, key = { it.id }) { notif ->
                    val (bgColor, iconColor, icon) = when (notif.type) {
                        "missed" -> Triple(Red100, Red600, Icons.Filled.PhoneMissed)
                        "request" -> Triple(Green100, Green600, Icons.Filled.PersonAdd)
                        else -> Triple(Blue100, Blue600, Icons.Filled.People)
                    }
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.clickable {
                            val route = notif.actionRoute ?: return@clickable
                            if (notif.type == "missed") {
                                rootNavController.navigate(route)
                            } else {
                                innerNavController.navigate(route)
                            }
                        }
                    ) {
                        Row(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                            Box(
                                modifier = Modifier.size(40.dp).clip(CircleShape).background(bgColor),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(20.dp))
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(notif.title, fontWeight = FontWeight.Medium, color = Gray900)
                                Text(notif.desc, style = MaterialTheme.typography.bodySmall, color = Gray600)
                            }
                        }
                    }
                }
            }
        }
    }
}
