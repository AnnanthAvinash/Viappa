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
fun TermsOfServiceScreen(innerNavController: NavController) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Terms of Service") },
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
                "1. Acceptance of Terms" to "By using Vaippa, you agree to these Terms of Service. If you do not agree, please discontinue use of the application.",
                "2. Service Description" to "Vaippa provides free internet-based voice calling. The service requires an active internet connection and is subject to network availability.",
                "3. User Accounts" to "You must create an account to use Vaippa. You are responsible for maintaining the confidentiality of your account and for all activities under your account.",
                "4. Acceptable Use" to "You agree not to misuse the service, including harassment, spam, or any illegal activities. We reserve the right to suspend accounts that violate these terms.",
                "5. Privacy" to "Your use of Vaippa is also governed by our Privacy Policy. Please review it to understand how we handle your information.",
                "6. Limitation of Liability" to "Vaippa is provided 'as is' without warranties. We are not liable for service interruptions, call quality issues, or data loss.",
                "7. Changes to Terms" to "We may update these terms from time to time. Continued use of the service after changes constitutes acceptance of the new terms.",
                "8. Contact" to "For questions regarding these terms, contact us at legal@connectcall.com."
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
