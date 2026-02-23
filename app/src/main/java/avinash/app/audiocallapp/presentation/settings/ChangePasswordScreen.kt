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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import avinash.app.audiocallapp.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangePasswordScreen(innerNavController: NavController) {
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var saved by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Change Password") },
                navigationIcon = { IconButton(onClick = { innerNavController.popBackStack() }) { Icon(Icons.Filled.ArrowBack, contentDescription = null) } }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Card(shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("Update your password", fontWeight = FontWeight.SemiBold, fontSize = 18.sp)

                    Column {
                        Text("Current Password", style = MaterialTheme.typography.labelLarge)
                        Spacer(Modifier.height(4.dp))
                        OutlinedTextField(
                            value = currentPassword, onValueChange = { currentPassword = it },
                            modifier = Modifier.fillMaxWidth(), visualTransformation = PasswordVisualTransformation(),
                            singleLine = true, shape = RoundedCornerShape(12.dp)
                        )
                    }

                    Column {
                        Text("New Password", style = MaterialTheme.typography.labelLarge)
                        Spacer(Modifier.height(4.dp))
                        OutlinedTextField(
                            value = newPassword, onValueChange = { newPassword = it },
                            modifier = Modifier.fillMaxWidth(), visualTransformation = PasswordVisualTransformation(),
                            singleLine = true, shape = RoundedCornerShape(12.dp)
                        )
                        if (newPassword.isNotEmpty() && newPassword.length < 8) {
                            Text("Password must be at least 8 characters", color = Red500, style = MaterialTheme.typography.labelSmall)
                        }
                    }

                    Column {
                        Text("Confirm New Password", style = MaterialTheme.typography.labelLarge)
                        Spacer(Modifier.height(4.dp))
                        OutlinedTextField(
                            value = confirmPassword, onValueChange = { confirmPassword = it },
                            modifier = Modifier.fillMaxWidth(), visualTransformation = PasswordVisualTransformation(),
                            singleLine = true, shape = RoundedCornerShape(12.dp),
                            isError = confirmPassword.isNotEmpty() && confirmPassword != newPassword
                        )
                        if (confirmPassword.isNotEmpty() && confirmPassword != newPassword) {
                            Text("Passwords don't match", color = Red500, style = MaterialTheme.typography.labelSmall)
                        }
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(onClick = { innerNavController.popBackStack() }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp)) { Text("Cancel") }
                        Button(
                            onClick = { saved = true },
                            modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp),
                            enabled = currentPassword.isNotBlank() && newPassword.length >= 8 && newPassword == confirmPassword
                        ) { Text("Update") }
                    }
                    if (saved) { Text("Password updated!", color = Green500, fontWeight = FontWeight.Medium) }
                }
            }
        }
    }
}
