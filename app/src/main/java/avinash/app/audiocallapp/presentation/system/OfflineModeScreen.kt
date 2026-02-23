package avinash.app.audiocallapp.presentation.system

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import avinash.app.audiocallapp.ui.theme.*

@Composable
fun OfflineModeScreen(onRetry: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(modifier = Modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(modifier = Modifier.size(96.dp).clip(CircleShape).background(Gray100), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.WifiOff, contentDescription = null, modifier = Modifier.size(48.dp), tint = Gray600)
            }
            Spacer(Modifier.height(24.dp))
            Text("No Internet Connection", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Gray900, textAlign = TextAlign.Center)
            Spacer(Modifier.height(8.dp))
            Text("Vaippa needs an internet connection to make calls and connect with friends.", color = Gray600, textAlign = TextAlign.Center)
            Spacer(Modifier.height(32.dp))
            Button(onClick = onRetry, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(16.dp)) {
                Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Try Again", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(24.dp))
            Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Blue50)) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    Text("Troubleshooting Tips:", fontWeight = FontWeight.Medium, color = Gray900)
                    Spacer(Modifier.height(8.dp))
                    listOf(
                        "Check your Wi-Fi or mobile data connection",
                        "Try toggling Airplane mode on and off",
                        "Restart your device",
                        "Move closer to your router"
                    ).forEach {
                        Row(modifier = Modifier.padding(vertical = 2.dp)) {
                            Text("• ", color = Blue600)
                            Text(it, style = MaterialTheme.typography.bodySmall, color = Gray600)
                        }
                    }
                }
            }
        }
    }
}
