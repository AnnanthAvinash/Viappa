package avinash.app.audiocallapp.presentation.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import avinash.app.audiocallapp.R
import avinash.app.audiocallapp.navigation.Screen
import avinash.app.audiocallapp.ui.theme.*
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(navController: NavController) {
    LaunchedEffect(Unit) {
        delay(2000)
        val dest = try {
            val isLoggedIn = FirebaseAuth.getInstance().currentUser != null
            if (isLoggedIn) Screen.Main.route else Screen.Login.route
        } catch (_: Exception) {
            Screen.Login.route
        }
        navController.navigate(dest) {
            popUpTo(Screen.Splash.route) { inclusive = true }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize().background(AppBackground),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(
                painter = painterResource(R.drawable.logo),
                contentDescription = "app name",
                modifier = Modifier.size(140.dp).clip(CircleShape)
            )
            Spacer(Modifier.height(16.dp))
            Text("Internet Voice Calling", color = Gray500, fontSize = 16.sp)
            Spacer(Modifier.height(4.dp))
            Text("Walkie Talkie", color = Gray500, fontSize = 16.sp)
            Spacer(Modifier.height(48.dp))
            CircularProgressIndicator(
                modifier = Modifier.size(32.dp),
                color = Blue600,
                strokeWidth = 3.dp
            )
        }
    }
}
