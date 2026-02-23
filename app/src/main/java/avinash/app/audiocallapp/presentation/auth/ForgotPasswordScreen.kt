package avinash.app.audiocallapp.presentation.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Phone
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
import androidx.navigation.NavController
import avinash.app.audiocallapp.ui.theme.*

@Composable
fun ForgotPasswordScreen(navController: NavController) {
    var username by remember { mutableStateOf("") }
    var submitted by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier.fillMaxSize().background(Brush.linearGradient(listOf(Blue600, Purple600))),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(modifier = Modifier.padding(32.dp)) {
                TextButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Back to Login", color = Gray600)
                }
                Spacer(Modifier.height(16.dp))
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier.size(64.dp).clip(CircleShape).background(Blue100),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.Phone, contentDescription = null, modifier = Modifier.size(32.dp), tint = Blue600)
                    }
                    Spacer(Modifier.height(16.dp))
                    Text("Reset Password", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Gray900)
                    Spacer(Modifier.height(4.dp))
                    Text("Enter your username to reset password", color = Gray600)
                }
                Spacer(Modifier.height(24.dp))
                Text("Username", style = MaterialTheme.typography.labelLarge, color = Gray900)
                Spacer(Modifier.height(4.dp))
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Enter your username") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = { submitted = true },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    enabled = username.isNotBlank()
                ) {
                    Text("Send Reset Instructions", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
                if (submitted) {
                    Spacer(Modifier.height(12.dp))
                    Text("Reset instructions sent!", color = Green500, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}
