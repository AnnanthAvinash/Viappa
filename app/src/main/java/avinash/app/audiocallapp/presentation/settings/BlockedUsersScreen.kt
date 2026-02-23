package avinash.app.audiocallapp.presentation.settings

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
import avinash.app.audiocallapp.ui.theme.*

private data class BlockedUser(val id: String, val name: String, val username: String, val date: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlockedUsersScreen(innerNavController: NavController) {
    val blockedUsers = remember {
        mutableStateListOf(
            BlockedUser("1", "Unknown User", "unknown_123", "Feb 15, 2025"),
            BlockedUser("2", "Spam Account", "spam_acc", "Jan 28, 2025"),
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Blocked Users") },
                navigationIcon = { IconButton(onClick = { innerNavController.popBackStack() }) { Icon(Icons.Filled.ArrowBack, contentDescription = null) } }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Text("${blockedUsers.size} blocked users", color = Gray600)
            Spacer(Modifier.height(12.dp))

            if (blockedUsers.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Filled.Block, contentDescription = null, modifier = Modifier.size(64.dp), tint = Gray300)
                        Spacer(Modifier.height(16.dp))
                        Text("No blocked users", fontWeight = FontWeight.Medium, fontSize = 18.sp)
                        Text("Users you block will appear here", color = Gray600)
                    }
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(blockedUsers, key = { it.id }) { user ->
                        Card(shape = RoundedCornerShape(12.dp)) {
                            Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(Gray400), contentAlignment = Alignment.Center) {
                                    Text(user.name.take(1), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(user.name, fontWeight = FontWeight.Medium)
                                    Text("@${user.username}", style = MaterialTheme.typography.bodySmall, color = Gray600)
                                    Text("Blocked on ${user.date}", style = MaterialTheme.typography.labelSmall, color = Gray500)
                                }
                                OutlinedButton(onClick = { blockedUsers.remove(user) }, shape = RoundedCornerShape(8.dp)) { Text("Unblock") }
                            }
                        }
                    }
                }
            }
        }
    }
}
