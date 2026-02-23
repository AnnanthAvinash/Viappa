package avinash.app.audiocallapp.presentation.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import avinash.app.audiocallapp.navigation.Screen
import avinash.app.audiocallapp.presentation.registration.RegistrationViewModel
import avinash.app.audiocallapp.ui.theme.*

@Composable
fun LoginScreen(
    navController: NavController,
    viewModel: RegistrationViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.registrationSuccess) {
        if (uiState.registrationSuccess) {
            navController.navigate(Screen.Main.route) {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize().background(Brush.linearGradient(listOf(Blue600, Purple600))),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(modifier = Modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier.size(64.dp).clip(CircleShape).background(Blue100),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Phone, contentDescription = null, modifier = Modifier.size(32.dp), tint = Blue600)
                }
                Spacer(Modifier.height(16.dp))
                Text("Welcome Back", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Gray900)
                Spacer(Modifier.height(4.dp))
                Text("Login to your account", color = Gray600)
                Spacer(Modifier.height(24.dp))

                Text("Username", style = MaterialTheme.typography.labelLarge, color = Gray900)
                Spacer(Modifier.height(4.dp))
                OutlinedTextField(
                    value = uiState.uniqueId,
                    onValueChange = { viewModel.onUniqueIdChange(it) },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Enter your username") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(Modifier.height(16.dp))
                Text("Display Name", style = MaterialTheme.typography.labelLarge, color = Gray900)
                Spacer(Modifier.height(4.dp))
                OutlinedTextField(
                    value = uiState.displayName,
                    onValueChange = { viewModel.onDisplayNameChange(it) },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Your display name") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = { navController.navigate(Screen.ForgotPassword.route) }) {
                        Text("Forgot Password?", color = Blue600, fontSize = 14.sp)
                    }
                }

                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = { viewModel.register() },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !uiState.isRegistering && uiState.uniqueId.length >= 3 && uiState.isAvailable == true
                ) {
                    if (uiState.isRegistering) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Text("Login", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    }
                }

                if (uiState.errorMessage != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(uiState.errorMessage!!, color = Red500, style = MaterialTheme.typography.bodySmall)
                }

                if (uiState.isAvailable == true && uiState.uniqueId.length >= 3) {
                    Spacer(Modifier.height(4.dp))
                    Text("Username available", color = Green500, style = MaterialTheme.typography.bodySmall)
                } else if (uiState.isAvailable == false) {
                    Spacer(Modifier.height(4.dp))
                    Text("Username taken", color = Red500, style = MaterialTheme.typography.bodySmall)
                }

                Spacer(Modifier.height(24.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Don't have an account? ", color = Gray600)
                    Text(
                        "Create Account",
                        color = Blue600,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clickable { navController.navigate(Screen.Register.route) }
                    )
                }
            }
        }
    }
}
