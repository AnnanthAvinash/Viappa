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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import avinash.app.audiocallapp.data.model.ConnectionRequest
import avinash.app.audiocallapp.presentation.connection.ConnectionViewModel
import avinash.app.audiocallapp.ui.theme.*
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ConnectionRequestsScreen(
    innerNavController: NavController,
    connectionViewModel: ConnectionViewModel
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val uiState by connectionViewModel.uiState.collectAsState()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Connection Requests", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text("Manage your connection requests", color = Gray600)
        Spacer(Modifier.height(12.dp))

        TabRow(selectedTabIndex = selectedTab) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }) {
                Text("Received (${uiState.receivedRequests.size})", modifier = Modifier.padding(12.dp))
            }
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }) {
                Text("Sent (${uiState.sentRequests.size})", modifier = Modifier.padding(12.dp))
            }
        }
        Spacer(Modifier.height(12.dp))

        if (selectedTab == 0) {
            if (uiState.receivedRequests.isEmpty()) {
                EmptyRequestState("No pending requests", "You don't have any connection requests")
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(uiState.receivedRequests, key = { it.id }) { req ->
                        Card(shape = RoundedCornerShape(12.dp)) {
                            Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(Green600), contentAlignment = Alignment.Center) {
                                    Text(req.senderName.take(1).uppercase(), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(req.senderName, fontWeight = FontWeight.Medium)
                                    Text("@${req.senderId}", style = MaterialTheme.typography.bodySmall, color = Gray600)
                                    Text(formatTimestamp(req.createdAt), style = MaterialTheme.typography.labelSmall, color = Gray500)
                                }
                                Button(
                                    onClick = { connectionViewModel.acceptRequest(req) },
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp)
                                ) {
                                    Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Accept", style = MaterialTheme.typography.labelMedium)
                                }
                                Spacer(Modifier.width(4.dp))
                                OutlinedButton(
                                    onClick = { connectionViewModel.rejectRequest(req.id) },
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp)
                                ) {
                                    Icon(Icons.Filled.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }
        } else {
            if (uiState.sentRequests.isEmpty()) {
                EmptyRequestState("No sent requests", "You haven't sent any connection requests")
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(uiState.sentRequests, key = { it.id }) { req ->
                        Card(shape = RoundedCornerShape(12.dp)) {
                            Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(Orange600), contentAlignment = Alignment.Center) {
                                    Text(req.receiverName.take(1).uppercase(), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(req.receiverName, fontWeight = FontWeight.Medium)
                                    Text("@${req.receiverId}", style = MaterialTheme.typography.bodySmall, color = Gray600)
                                    Text("Sent ${formatTimestamp(req.createdAt)}", style = MaterialTheme.typography.labelSmall, color = Gray500)
                                }
                                OutlinedButton(onClick = { connectionViewModel.cancelRequest(req.id) }, shape = RoundedCornerShape(8.dp)) {
                                    Text("Cancel")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyRequestState(title: String, message: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Filled.PersonAdd, contentDescription = null, modifier = Modifier.size(64.dp), tint = Gray300)
            Spacer(Modifier.height(16.dp))
            Text(title, fontWeight = FontWeight.Medium, fontSize = 18.sp, color = Gray900)
            Text(message, color = Gray600)
        }
    }
}

private fun formatTimestamp(timestamp: Timestamp?): String {
    if (timestamp == null) return ""
    val sdf = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
    return sdf.format(Date(timestamp.seconds * 1000))
}
