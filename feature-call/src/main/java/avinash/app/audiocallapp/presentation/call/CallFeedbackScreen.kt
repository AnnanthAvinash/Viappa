package avinash.app.audiocallapp.presentation.call

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import avinash.app.audiocallapp.feature.Routes
import avinash.app.audiocallapp.ui.theme.*

@Composable
fun CallFeedbackScreen(
    remoteName: String,
    duration: String,
    navController: NavController
) {
    var rating by remember { mutableIntStateOf(0) }
    var feedback by remember { mutableStateOf("") }
    var submitted by remember { mutableStateOf(false) }
    val issues = remember { listOf("Audio Quality", "Connection Issues", "Echo/Noise", "Delay/Latency", "App Crash", "Other") }
    val selectedIssues = remember { mutableStateListOf<String>() }

    Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Gray800, Gray900)))) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(32.dp))
            Text("How was your call?", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Text("with $remoteName • $duration", color = Color.White.copy(alpha = 0.6f))

            Spacer(Modifier.height(24.dp))
            Card(shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        repeat(5) { i ->
                            val index = i + 1
                            IconButton(onClick = { rating = index }) {
                                Icon(
                                    if (index <= rating) Icons.Filled.Star else Icons.Filled.StarBorder,
                                    contentDescription = null,
                                    modifier = Modifier.size(40.dp),
                                    tint = if (index <= rating) Color(0xFFFBBF24) else Gray300
                                )
                            }
                        }
                    }
                    Text(
                        when (rating) { 1 -> "Poor"; 2 -> "Below Average"; 3 -> "Average"; 4 -> "Good"; 5 -> "Excellent"; else -> "Tap to rate" },
                        color = if (rating > 0) Gray900 else Gray500
                    )

                    if (rating in 1..3) {
                        Spacer(Modifier.height(16.dp))
                        Text("What went wrong?", fontWeight = FontWeight.Medium, color = Gray900)
                        Spacer(Modifier.height(8.dp))
                        FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            issues.forEach { issue ->
                                val selected = issue in selectedIssues
                                FilterChip(
                                    selected = selected,
                                    onClick = { if (selected) selectedIssues.remove(issue) else selectedIssues.add(issue) },
                                    label = { Text(issue) }
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(
                        value = feedback, onValueChange = { feedback = it },
                        modifier = Modifier.fillMaxWidth().height(100.dp),
                        placeholder = { Text("Additional feedback (optional)") },
                        shape = RoundedCornerShape(12.dp), maxLines = 4
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
            Button(
                onClick = { submitted = true },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(16.dp),
                enabled = rating > 0
            ) {
                Text("Submit Feedback", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(12.dp))
            TextButton(onClick = { navController.navigate(Routes.MAIN) { popUpTo(0) { inclusive = true } } }) {
                Text("Skip", color = Color.White.copy(alpha = 0.8f))
            }

            if (submitted) {
                Spacer(Modifier.height(16.dp))
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Green100)
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = Green600, modifier = Modifier.size(48.dp))
                        Spacer(Modifier.height(8.dp))
                        Text("Thank you for your feedback!", fontWeight = FontWeight.Medium, color = Green800, textAlign = TextAlign.Center)
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = { navController.navigate(Routes.MAIN) { popUpTo(0) { inclusive = true } } }, shape = RoundedCornerShape(12.dp)) {
                            Text("Go Home")
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FlowRow(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    content: @Composable () -> Unit
) {
    androidx.compose.foundation.layout.FlowRow(
        modifier = modifier,
        horizontalArrangement = horizontalArrangement,
        verticalArrangement = verticalArrangement,
        content = { content() }
    )
}
