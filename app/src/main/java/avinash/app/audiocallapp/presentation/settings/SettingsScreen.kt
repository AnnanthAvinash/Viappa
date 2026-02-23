package avinash.app.audiocallapp.presentation.settings

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import avinash.app.audiocallapp.navigation.Screen
import avinash.app.audiocallapp.presentation.userlist.UserListViewModel
import avinash.app.audiocallapp.ui.theme.*
import com.google.firebase.auth.FirebaseAuth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    rootNavController: NavController,
    innerNavController: NavController,
    userListViewModel: UserListViewModel
) {
    val uiState by userListViewModel.uiState.collectAsState()
    var doNotDisturb by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = { IconButton(onClick = { innerNavController.popBackStack() }) { Icon(Icons.Filled.ArrowBack, contentDescription = null) } }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(shape = RoundedCornerShape(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth().clickable { innerNavController.navigate(Screen.Profile.route) }.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(56.dp).clip(CircleShape).background(Blue600), contentAlignment = Alignment.Center) {
                        Text(
                            (uiState.currentUser?.displayName?.take(1) ?: "U").uppercase(),
                            color = Color.White, fontWeight = FontWeight.Bold, fontSize = 22.sp
                        )
                    }
                    Spacer(Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(uiState.currentUser?.displayName ?: "User", fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
                        Text("@${uiState.currentUser?.uniqueId ?: "user"}", color = Gray600)
                    }
                    Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = Gray400)
                }
            }

            Card(shape = RoundedCornerShape(16.dp)) {
                Column {
                    SettingsRow(Icons.Filled.Person, "Edit Profile", Blue600) { innerNavController.navigate(Screen.EditProfile.route) }
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = Gray100)
                    SettingsRow(Icons.Filled.Lock, "Change Password", Purple600) { innerNavController.navigate(Screen.ChangePassword.route) }
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = Gray100)
                    SettingsRow(Icons.Filled.Block, "Blocked Users", Orange600) { innerNavController.navigate(Screen.BlockedUsers.route) }
                }
            }

            Card(shape = RoundedCornerShape(16.dp)) {
                Column {
                    Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(Red100), contentAlignment = Alignment.Center) {
                            Icon(Icons.Filled.DoNotDisturb, contentDescription = null, tint = Red600, modifier = Modifier.size(20.dp))
                        }
                        Spacer(Modifier.width(16.dp))
                        Text("Do Not Disturb", modifier = Modifier.weight(1f), fontWeight = FontWeight.Medium)
                        Switch(checked = doNotDisturb, onCheckedChange = { doNotDisturb = it })
                    }
                }
            }

            Card(shape = RoundedCornerShape(16.dp)) {
                Column {
                    SettingsRow(Icons.Filled.HelpOutline, "Help & Support", Green600) { innerNavController.navigate(Screen.Help.route) }
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = Gray100)
                    SettingsRow(Icons.Filled.Info, "About", Blue600) { innerNavController.navigate(Screen.About.route) }
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = Gray100)
                    SettingsRow(Icons.Filled.PrivacyTip, "Privacy Policy", Purple600) { innerNavController.navigate(Screen.Privacy.route) }
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = Gray100)
                    SettingsRow(Icons.Filled.Description, "Terms of Service", Gray600) { innerNavController.navigate(Screen.Terms.route) }
                }
            }

            Card(shape = RoundedCornerShape(16.dp)) {
                Column {
                    SettingsRow(Icons.Filled.Logout, "Logout", Red600) {
                        FirebaseAuth.getInstance().signOut()
                        rootNavController.navigate(Screen.Login.route) { popUpTo(0) { inclusive = true } }
                    }
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = Gray100)
                    SettingsRow(Icons.Filled.DeleteForever, "Delete Account", Red600) { innerNavController.navigate(Screen.DeleteAccount.route) }
                }
            }

            Text("Vaippa v1.0.0", color = Gray500, modifier = Modifier.fillMaxWidth(), style = MaterialTheme.typography.bodySmall, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }
    }
}

@Composable
fun SettingsRow(icon: ImageVector, label: String, iconColor: Color, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(iconColor.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(16.dp))
        Text(label, modifier = Modifier.weight(1f), fontWeight = FontWeight.Medium, color = Gray900)
        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = Gray400)
    }
}
