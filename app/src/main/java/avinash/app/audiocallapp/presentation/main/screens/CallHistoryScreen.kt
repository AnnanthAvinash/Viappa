package avinash.app.audiocallapp.presentation.main.screens

import androidx.compose.foundation.background
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
import avinash.app.audiocallapp.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CallHistoryScreen(
    rootNavController: NavController,
    callHistoryViewModel: CallHistoryViewModel = hiltViewModel()
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("All", "Incoming", "Outgoing", "Missed")
    val tabFilters = listOf(null, "incoming", "outgoing", "missed")
    val uiState by callHistoryViewModel.uiState.collectAsState()

    LaunchedEffect(selectedTab) {
        callHistoryViewModel.loadHistory(tabFilters[selectedTab])
        if (selectedTab == 3) callHistoryViewModel.markMissedSeen()
    }

    val filtered = uiState.calls.filter {
        searchQuery.isBlank() || it.remoteName.contains(searchQuery, true)
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Call History", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text("View your recent calls", color = Gray600)
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = searchQuery, onValueChange = { searchQuery = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search call history...") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            singleLine = true, shape = RoundedCornerShape(12.dp)
        )
        Spacer(Modifier.height(12.dp))
        ScrollableTabRow(selectedTabIndex = selectedTab, edgePadding = 0.dp) {
            tabs.forEachIndexed { i, t ->
                Tab(selected = selectedTab == i, onClick = { selectedTab = i }) {
                    if (i == 3 && uiState.missedCount > 0) {
                        BadgedBox(badge = { Badge { Text("${uiState.missedCount}") } }) {
                            Text(t, modifier = Modifier.padding(12.dp))
                        }
                    } else {
                        Text(t, modifier = Modifier.padding(12.dp))
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))

        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else if (filtered.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.History, contentDescription = null, modifier = Modifier.size(64.dp), tint = Gray300)
                    Spacer(Modifier.height(16.dp))
                    Text("No call history", fontWeight = FontWeight.Medium, fontSize = 18.sp)
                    Text("Your calls will appear here", color = Gray600)
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                items(filtered, key = { it.id }) { call ->
                    val (bgColor, iconColor, icon) = when (call.type) {
                        "incoming" -> Triple(Green100, Green600, Icons.Filled.CallReceived)
                        "outgoing" -> Triple(Blue100, Blue600, Icons.Filled.CallMade)
                        else -> Triple(Red100, Red600, Icons.Filled.CallMissed)
                    }
                    val duration = if (call.durationSeconds > 0) {
                        "${call.durationSeconds / 60}:${(call.durationSeconds % 60).toString().padStart(2, '0')}"
                    } else "-"
                    val time = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(Date(call.timestamp))

                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(bgColor), contentAlignment = Alignment.Center) {
                            Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(20.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(call.remoteName, fontWeight = FontWeight.Medium, color = Gray900)
                            Text(
                                "${call.type.replaceFirstChar { it.uppercase() }}${if (duration != "-") " • $duration" else ""}",
                                style = MaterialTheme.typography.bodySmall, color = Gray600
                            )
                            Text(time, style = MaterialTheme.typography.labelSmall, color = Gray500)
                        }
                        IconButton(onClick = { rootNavController.navigate(Screen.Call.createRoute(call.remoteUserId, call.remoteName)) }) {
                            Icon(Icons.Filled.Phone, contentDescription = "Call", tint = Gray600)
                        }
                    }
                }
            }
        }
    }
}
