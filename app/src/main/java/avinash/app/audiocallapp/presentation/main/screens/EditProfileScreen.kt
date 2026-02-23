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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import avinash.app.audiocallapp.presentation.userlist.UserListViewModel
import avinash.app.audiocallapp.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(innerNavController: NavController, userListViewModel: UserListViewModel) {
    val uiState by userListViewModel.uiState.collectAsState()
    var displayName by remember { mutableStateOf(uiState.currentUser?.displayName ?: "") }
    var bio by remember { mutableStateOf("Love connecting with friends through voice calls!") }
    var isSaving by remember { mutableStateOf(false) }
    var saveMessage by remember { mutableStateOf<String?>(null) }
    var isError by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Profile") },
                navigationIcon = { IconButton(onClick = { innerNavController.popBackStack() }) { Icon(Icons.Filled.ArrowBack, contentDescription = null) } }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box {
                        Box(modifier = Modifier.size(96.dp).clip(CircleShape).background(Blue600), contentAlignment = Alignment.Center) {
                            Text(displayName.take(1).uppercase().ifEmpty { "U" }, fontSize = 36.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                        FloatingActionButton(
                            onClick = {},
                            modifier = Modifier.size(32.dp).align(Alignment.BottomEnd),
                            containerColor = Blue600
                        ) { Icon(Icons.Filled.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White) }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("Change Photo", color = Blue600, fontWeight = FontWeight.Medium)
                }
            }

            Card(shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("Personal Information", fontWeight = FontWeight.SemiBold, fontSize = 18.sp)

                    Column {
                        Text("Display Name", style = MaterialTheme.typography.labelLarge, color = Gray900)
                        Spacer(Modifier.height(4.dp))
                        OutlinedTextField(
                            value = displayName, onValueChange = { displayName = it; saveMessage = null },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true, shape = RoundedCornerShape(12.dp),
                            enabled = !isSaving
                        )
                    }

                    Column {
                        Text("Username", style = MaterialTheme.typography.labelLarge, color = Gray900)
                        Spacer(Modifier.height(4.dp))
                        OutlinedTextField(
                            value = "@${uiState.currentUser?.uniqueId ?: "user"}", onValueChange = {},
                            modifier = Modifier.fillMaxWidth(), enabled = false,
                            singleLine = true, shape = RoundedCornerShape(12.dp)
                        )
                        Text("Username cannot be changed", style = MaterialTheme.typography.labelSmall, color = Gray500)
                    }

                    Column {
                        Text("Bio", style = MaterialTheme.typography.labelLarge, color = Gray900)
                        Spacer(Modifier.height(4.dp))
                        OutlinedTextField(
                            value = bio, onValueChange = { if (it.length <= 150) bio = it },
                            modifier = Modifier.fillMaxWidth().height(100.dp),
                            shape = RoundedCornerShape(12.dp),
                            maxLines = 4, enabled = !isSaving
                        )
                        Text("${bio.length}/150", style = MaterialTheme.typography.labelSmall, color = Gray500, modifier = Modifier.align(Alignment.End))
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(
                            onClick = { innerNavController.popBackStack() },
                            modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp),
                            enabled = !isSaving
                        ) { Text("Cancel") }

                        Button(
                            onClick = {
                                if (displayName.isBlank()) {
                                    saveMessage = "Name cannot be empty"
                                    isError = true
                                    return@Button
                                }
                                isSaving = true
                                saveMessage = null
                                userListViewModel.updateDisplayName(displayName.trim()) { result ->
                                    isSaving = false
                                    result.fold(
                                        onSuccess = {
                                            saveMessage = "Profile saved!"
                                            isError = false
                                        },
                                        onFailure = { e ->
                                            saveMessage = e.message ?: "Save failed"
                                            isError = true
                                        }
                                    )
                                }
                            },
                            modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp),
                            enabled = !isSaving && displayName.isNotBlank()
                        ) {
                            if (isSaving) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                                Spacer(Modifier.width(8.dp))
                            }
                            Text("Save Changes")
                        }
                    }
                    if (saveMessage != null) {
                        Text(
                            saveMessage!!,
                            color = if (isError) Red500 else Green500,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}
