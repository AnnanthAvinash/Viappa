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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import avinash.app.audiocallapp.navigation.Screen
import avinash.app.audiocallapp.presentation.connection.ConnectionViewModel
import avinash.app.audiocallapp.ui.theme.*

@Composable
fun SearchUsersScreen(
    innerNavController: NavController,
    connectionViewModel: ConnectionViewModel
) {
    var searchQuery by remember { mutableStateOf("") }
    val uiState by connectionViewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(searchQuery) {
        connectionViewModel.searchUsers(searchQuery)
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            connectionViewModel.clearError()
        }
    }
    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let {
            snackbarHostState.showSnackbar(it)
            connectionViewModel.clearSuccess()
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
    Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
        Text("Search Users", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text("Find and connect with other users", color = Gray600)
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = searchQuery, onValueChange = { searchQuery = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search by name or username...") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            singleLine = true, shape = RoundedCornerShape(12.dp)
        )
        Spacer(Modifier.height(16.dp))

        if (searchQuery.isBlank()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.Search, contentDescription = null, modifier = Modifier.size(64.dp), tint = Gray300)
                    Spacer(Modifier.height(16.dp))
                    Text("Search for users", fontWeight = FontWeight.Medium, fontSize = 18.sp, color = Gray900)
                    Text("Enter a name or username to find people", color = Gray600)
                }
            }
        } else if (uiState.isSearching) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (uiState.searchResults.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.Search, contentDescription = null, modifier = Modifier.size(64.dp), tint = Gray300)
                    Spacer(Modifier.height(16.dp))
                    Text("No users found", fontWeight = FontWeight.Medium, fontSize = 18.sp, color = Gray900)
                    Text("Try a different name or username", color = Gray600)
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                items(uiState.searchResults, key = { it.uniqueId }) { user ->
                    val isFriend = connectionViewModel.isFriend(user.uniqueId)
                    val hasPending = connectionViewModel.hasPendingRequest(user.uniqueId)

                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { innerNavController.navigate(Screen.ProfileDetail.createRoute(user.uniqueId)) }.padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(Purple600), contentAlignment = Alignment.Center) {
                            Text(user.displayName.take(1).uppercase(), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(user.displayName, fontWeight = FontWeight.Medium, color = Gray900)
                            Text("@${user.uniqueId}", style = MaterialTheme.typography.bodySmall, color = Gray600)
                        }
                        when {
                            isFriend -> {
                                OutlinedButton(onClick = {}, enabled = false, shape = RoundedCornerShape(8.dp)) {
                                    Icon(Icons.Filled.People, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Friends", style = MaterialTheme.typography.labelMedium)
                                }
                            }
                            hasPending -> {
                                OutlinedButton(onClick = {}, enabled = false, shape = RoundedCornerShape(8.dp)) {
                                    Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Sent", style = MaterialTheme.typography.labelMedium)
                                }
                            }
                            else -> {
                                Button(onClick = { connectionViewModel.sendRequest(user.uniqueId, user.displayName) }, shape = RoundedCornerShape(8.dp)) {
                                    Icon(Icons.Filled.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Connect", style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    }
}
