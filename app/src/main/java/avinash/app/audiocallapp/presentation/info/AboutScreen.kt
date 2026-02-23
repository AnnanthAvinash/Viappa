package avinash.app.audiocallapp.presentation.info

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
import avinash.app.audiocallapp.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(innerNavController: NavController) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("About") },
                navigationIcon = { IconButton(onClick = { innerNavController.popBackStack() }) { Icon(Icons.Filled.ArrowBack, contentDescription = null) } }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp).verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(modifier = Modifier.size(80.dp).clip(CircleShape).background(Blue600), contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.Phone, contentDescription = null, tint = Color.White, modifier = Modifier.size(40.dp))
                    }
                    Spacer(Modifier.height(16.dp))
                    Text("Vaippa", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Gray900)
                    Text("Version 1.0.0", color = Gray600)
                    Spacer(Modifier.height(8.dp))
                    Text("Free internet voice calling for everyone", color = Gray600, style = MaterialTheme.typography.bodyMedium)
                }
            }

            Card(shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("Features", fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
                    AboutFeatureRow(Icons.Filled.Phone, "Free Voice Calls", "Make unlimited calls over the internet")
                    AboutFeatureRow(Icons.Filled.People, "Friend Connections", "Connect with friends and family")
                    AboutFeatureRow(Icons.Filled.Security, "Secure & Private", "End-to-end encryption for all calls")
                    AboutFeatureRow(Icons.Filled.Speed, "High Quality Audio", "Crystal clear voice with WebRTC technology")
                }
            }

            Card(shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Built With", fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
                    Text("• Jetpack Compose for modern UI", color = Gray600)
                    Text("• WebRTC for peer-to-peer calling", color = Gray600)
                    Text("• Firebase for real-time signaling", color = Gray600)
                    Text("• Material Design 3", color = Gray600)
                }
            }

            Text("Made with ❤️ by Vaippa Team", color = Gray500)
            Text("© 2025 Vaippa. All rights reserved.", color = Gray500, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun AboutFeatureRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, desc: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(Blue100), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = Blue600, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(title, fontWeight = FontWeight.Medium, color = Gray900)
            Text(desc, style = MaterialTheme.typography.bodySmall, color = Gray600)
        }
    }
}
