package avinash.app.audiocallapp.presentation.info

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import avinash.app.audiocallapp.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyPolicyScreen(innerNavController: NavController) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Privacy Policy") },
                navigationIcon = { IconButton(onClick = { innerNavController.popBackStack() }) { Icon(Icons.Filled.ArrowBack, contentDescription = null) } }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Last updated: February 20, 2025", color = Gray600)

            val sections = listOf(
                "1. Information We Collect" to "We collect information you provide when creating an account, including your username and display name. We also collect call metadata such as call duration and timestamps.",
                "2. How We Use Your Information" to "Your information is used to provide the calling service, manage your connections, and improve the app experience. We do not sell your personal information.",
                "3. Data Storage" to "Your account data is stored securely using Firebase services. Call audio is transmitted peer-to-peer using WebRTC and is not stored on our servers.",
                "4. Your Rights" to "You can view, edit, or delete your account data at any time from the Settings screen. You can also block users and manage your connections.",
                "5. Security" to "We use industry-standard security measures including encryption for data in transit and at rest. Voice calls are encrypted end-to-end using WebRTC.",
                "6. Third-Party Services" to "We use Firebase for authentication and signaling, and WebRTC for peer-to-peer communication. These services have their own privacy policies.",
                "7. Contact Us" to "If you have questions about this privacy policy, contact us at privacy@connectcall.com."
            )

            sections.forEach { (title, body) ->
                Card(shape = RoundedCornerShape(12.dp)) {
                    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                        Text(title, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = Gray900)
                        Spacer(Modifier.height(8.dp))
                        Text(body, color = Gray600, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}
