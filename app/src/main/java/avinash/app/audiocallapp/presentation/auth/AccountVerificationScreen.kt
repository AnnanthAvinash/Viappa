package avinash.app.audiocallapp.presentation.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import avinash.app.audiocallapp.navigation.Screen
import avinash.app.audiocallapp.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun AccountVerificationScreen(navController: NavController) {
    var code by remember { mutableStateOf("") }
    var timeLeft by remember { mutableIntStateOf(300) }
    var verified by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while (timeLeft > 0) {
            delay(1000)
            timeLeft--
        }
    }

    val formattedTime = "${timeLeft / 60}:${(timeLeft % 60).toString().padStart(2, '0')}"

    Box(
        modifier = Modifier.fillMaxSize().background(Brush.linearGradient(listOf(Blue600, Purple600))),
        contentAlignment = Alignment.Center
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            TextButton(onClick = { navController.popBackStack() }, colors = ButtonDefaults.textButtonColors(contentColor = Color.White)) {
                Icon(Icons.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Back")
            }
            Spacer(Modifier.height(8.dp))
            Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(modifier = Modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier.size(64.dp).clip(CircleShape).background(Blue100),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.Email, contentDescription = null, modifier = Modifier.size(32.dp), tint = Blue600)
                    }
                    Spacer(Modifier.height(16.dp))
                    Text("Verify Your Account", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Gray900)
                    Spacer(Modifier.height(8.dp))
                    Text("We've sent a 6-digit code to", color = Gray600)
                    Text("user@example.com", fontWeight = FontWeight.Medium, color = Gray900)
                    Spacer(Modifier.height(24.dp))

                    Text("Enter verification code", style = MaterialTheme.typography.labelLarge, color = Gray900)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = code,
                        onValueChange = { if (it.length <= 6 && it.all { c -> c.isDigit() }) code = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("000000") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center, fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
                    )

                    Spacer(Modifier.height(12.dp))
                    if (timeLeft > 0) {
                        Text("Code expires in ", color = Gray600, style = MaterialTheme.typography.bodySmall)
                        Text(formattedTime, color = Blue600, fontWeight = FontWeight.Medium, style = MaterialTheme.typography.bodySmall)
                    }

                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = {
                            verified = true
                            // Navigate to main on success
                        },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(12.dp),
                        enabled = code.length == 6
                    ) {
                        Text("Verify Account", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    }

                    if (verified) {
                        Spacer(Modifier.height(8.dp))
                        Text("Account verified!", color = Green500)
                    }

                    Spacer(Modifier.height(16.dp))
                    Text("Didn't receive the code?", color = Gray600, style = MaterialTheme.typography.bodySmall)
                    TextButton(onClick = { timeLeft = 300 }, enabled = timeLeft == 0) {
                        Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Resend Code")
                    }

                    Spacer(Modifier.height(8.dp))
                    HorizontalDivider()
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Check your spam folder if you don't see the email.",
                        color = Gray500,
                        style = MaterialTheme.typography.labelSmall,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
