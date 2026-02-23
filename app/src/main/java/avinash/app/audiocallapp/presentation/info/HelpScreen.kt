package avinash.app.audiocallapp.presentation.info

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import avinash.app.audiocallapp.ui.theme.*

private data class FaqItem(val question: String, val answer: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpScreen(innerNavController: NavController) {
    val faqs = remember {
        listOf(
            FaqItem("How do I make a call?", "Go to your Friends list, find the person you want to call, and tap the phone icon. You can also search for users and call from their profile."),
            FaqItem("Why is my call quality poor?", "Call quality depends on your internet connection. Try switching to Wi-Fi or move to an area with better signal."),
            FaqItem("How do I add friends?", "Go to the Search tab, find users by name or username, and send them a connection request."),
            FaqItem("Is Vaippa free?", "Yes! Vaippa uses your internet connection, so there are no charges for calls."),
            FaqItem("How do I block someone?", "Go to the user's profile and tap the Block option, or manage blocked users from Settings."),
        )
    }
    var expandedIndex by remember { mutableIntStateOf(-1) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Help & Support") },
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
                    Box(modifier = Modifier.size(64.dp).clip(CircleShape).background(Green100), contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.HelpOutline, contentDescription = null, tint = Green600, modifier = Modifier.size(32.dp))
                    }
                    Spacer(Modifier.height(12.dp))
                    Text("How can we help?", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text("Find answers to common questions below", color = Gray600)
                }
            }

            Text("Frequently Asked Questions", fontWeight = FontWeight.SemiBold, fontSize = 18.sp)

            faqs.forEachIndexed { index, faq ->
                Card(modifier = Modifier.clickable { expandedIndex = if (expandedIndex == index) -1 else index }, shape = RoundedCornerShape(12.dp)) {
                    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(faq.question, modifier = Modifier.weight(1f), fontWeight = FontWeight.Medium, color = Gray900)
                            Icon(
                                if (expandedIndex == index) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                                contentDescription = null, tint = Gray600
                            )
                        }
                        AnimatedVisibility(visible = expandedIndex == index) {
                            Column {
                                Spacer(Modifier.height(8.dp))
                                HorizontalDivider(color = Gray100)
                                Spacer(Modifier.height(8.dp))
                                Text(faq.answer, color = Gray600)
                            }
                        }
                    }
                }
            }

            Card(shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Still need help?", fontWeight = FontWeight.SemiBold)
                    Text("Contact us at support@connectcall.com", color = Blue600)
                }
            }
        }
    }
}
