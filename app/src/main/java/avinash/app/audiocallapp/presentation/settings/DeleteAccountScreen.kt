package avinash.app.audiocallapp.presentation.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import avinash.app.audiocallapp.navigation.Screen
import avinash.app.audiocallapp.ui.theme.*
import com.google.firebase.auth.FirebaseAuth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeleteAccountScreen(rootNavController: NavController, innerNavController: NavController) {
    var confirmText by remember { mutableStateOf("") }
    var showDialog by remember { mutableStateOf(false) }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Final Confirmation", fontWeight = FontWeight.Bold) },
            text = { Text("This is irreversible. Your account and all data will be permanently deleted.") },
            confirmButton = {
                Button(
                    onClick = {
                        FirebaseAuth.getInstance().signOut()
                        rootNavController.navigate(Screen.Login.route) { popUpTo(0) { inclusive = true } }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Red600)
                ) { Text("Delete Forever") }
            },
            dismissButton = { OutlinedButton(onClick = { showDialog = false }) { Text("Cancel") } }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Delete Account") },
                navigationIcon = { IconButton(onClick = { innerNavController.popBackStack() }) { Icon(Icons.Filled.ArrowBack, contentDescription = null) } }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Red100)) {
                Column(modifier = Modifier.fillMaxWidth().padding(24.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Warning, contentDescription = null, tint = Red600, modifier = Modifier.size(28.dp))
                        Spacer(Modifier.width(12.dp))
                        Text("Danger Zone", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Red800)
                    }
                    Spacer(Modifier.height(12.dp))
                    Text("Deleting your account will:", color = Red800)
                    Spacer(Modifier.height(8.dp))
                    listOf(
                        "Remove all your profile information",
                        "Delete your call history",
                        "Remove all connections",
                        "Cancel any pending requests",
                        "Permanently delete all data"
                    ).forEach {
                        Row(modifier = Modifier.padding(vertical = 2.dp)) {
                            Text("• ", color = Red600)
                            Text(it, color = Red800, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            Card(shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Confirm Deletion", fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
                    Text("Type DELETE to confirm", color = Gray600)
                    OutlinedTextField(
                        value = confirmText, onValueChange = { confirmText = it },
                        modifier = Modifier.fillMaxWidth(), placeholder = { Text("Type DELETE") },
                        singleLine = true, shape = RoundedCornerShape(12.dp),
                        isError = confirmText.isNotEmpty() && confirmText != "DELETE"
                    )
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(onClick = { innerNavController.popBackStack() }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp)) { Text("Cancel") }
                        Button(
                            onClick = { showDialog = true },
                            modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Red600),
                            enabled = confirmText == "DELETE"
                        ) { Text("Delete Account") }
                    }
                }
            }
        }
    }
}
