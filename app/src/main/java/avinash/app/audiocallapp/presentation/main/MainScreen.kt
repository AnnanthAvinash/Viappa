package avinash.app.audiocallapp.presentation.main

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import avinash.app.audiocallapp.navigation.Screen
import avinash.app.audiocallapp.presentation.call.CallViewModel
import avinash.app.audiocallapp.presentation.call.IncomingCallBar
import avinash.app.audiocallapp.presentation.connection.ConnectionViewModel
import avinash.app.audiocallapp.presentation.main.screens.*
import avinash.app.audiocallapp.presentation.settings.*
import avinash.app.audiocallapp.presentation.info.*
import avinash.app.audiocallapp.presentation.userlist.UserListViewModel
import avinash.app.audiocallapp.ui.theme.*
import avinash.app.audiocallapp.util.InAppNotificationBus
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

private data class BottomNavItem(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

private val bottomNavItems = listOf(
    BottomNavItem(Screen.Home.route, "Home", Icons.Filled.Home, Icons.Outlined.Home),
    BottomNavItem(Screen.Friends.route, "Friends", Icons.Filled.People, Icons.Outlined.People),
    BottomNavItem(Screen.Search.route, "Search", Icons.Filled.Search, Icons.Outlined.Search),
    BottomNavItem(Screen.History.route, "History", Icons.Filled.History, Icons.Outlined.History),
    BottomNavItem(Screen.Profile.route, "Profile", Icons.Filled.Person, Icons.Outlined.Person),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    rootNavController: NavController,
    userListViewModel: UserListViewModel = hiltViewModel(),
    connectionViewModel: ConnectionViewModel = hiltViewModel(),
    callViewModel: CallViewModel = hiltViewModel()
) {
    val innerNavController = rememberNavController()
    val navBackStackEntry by innerNavController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val uiState by userListViewModel.uiState.collectAsState()
    val connState by connectionViewModel.uiState.collectAsState()
    val incomingCall by callViewModel.callManager.incomingCall.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        InAppNotificationBus.events.collectLatest { event ->
            snackbarHostState.showSnackbar(
                message = "${event.title}: ${event.body}",
                duration = SnackbarDuration.Short
            )
        }
    }

    val showBottomBar = currentRoute in bottomNavItems.map { it.route }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                if (showBottomBar) {
                    TopAppBar(
                        title = {
                            Text("Vaippa", fontWeight = FontWeight.Bold)
                        },
                        actions = {
                            IconButton(onClick = { innerNavController.navigate(Screen.Notifications.route) }) {
                                val notifCount = connState.receivedRequests.size
                                if (notifCount > 0) {
                                    BadgedBox(badge = { Badge { Text("$notifCount") } }) {
                                        Icon(Icons.Outlined.Notifications, contentDescription = null)
                                    }
                                } else {
                                    Icon(Icons.Outlined.Notifications, contentDescription = null)
                                }
                            }
                            IconButton(onClick = { innerNavController.navigate(Screen.Requests.route) }) {
                                if (connState.receivedRequests.isNotEmpty()) {
                                    BadgedBox(badge = { Badge { Text("${connState.receivedRequests.size}") } }) {
                                        Icon(Icons.Outlined.PeopleAlt, contentDescription = null)
                                    }
                                } else {
                                    Icon(Icons.Outlined.PeopleAlt, contentDescription = null)
                                }
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
                    )
                }
            },
            bottomBar = {
                if (showBottomBar) {
                    NavigationBar(containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 0.dp) {
                        bottomNavItems.forEach { item ->
                            val selected = currentRoute == item.route
                            NavigationBarItem(
                                selected = selected,
                                onClick = {
                                    if (currentRoute != item.route) {
                                        innerNavController.navigate(item.route) {
                                            popUpTo(Screen.Home.route) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                },
                                icon = { Icon(if (selected) item.selectedIcon else item.unselectedIcon, contentDescription = item.label) },
                                label = { Text(item.label, style = MaterialTheme.typography.labelSmall) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Blue600,
                                    selectedTextColor = Blue600,
                                    indicatorColor = Blue50
                                )
                            )
                        }
                    }
                }
            }
        ) { padding ->
            MainNavHost(innerNavController, rootNavController, userListViewModel, connectionViewModel, Modifier.padding(padding))
        }

        if (incomingCall != null) {
            IncomingCallBar(
                callerName = incomingCall!!.callerName,
                onAccept = {
                    callViewModel.callManager.acceptIncomingCall()
                    rootNavController.navigate(
                        Screen.Call.createRoute(incomingCall!!.callerId, incomingCall!!.callerName, isCaller = false)
                    )
                },
                onReject = {
                    scope.launch { callViewModel.callManager.rejectIncomingCall() }
                },
                onBarTap = {
                    rootNavController.navigate(
                        Screen.Call.createRoute(incomingCall!!.callerId, incomingCall!!.callerName, isCaller = false)
                    )
                }
            )
        }
    }
}

@Composable
private fun MainNavHost(
    innerNavController: NavHostController,
    rootNavController: NavController,
    userListViewModel: UserListViewModel,
    connectionViewModel: ConnectionViewModel,
    modifier: Modifier = Modifier
) {
    NavHost(navController = innerNavController, startDestination = Screen.Home.route, modifier = modifier) {
        composable(Screen.Home.route) { HomeScreen(rootNavController, innerNavController, userListViewModel, connectionViewModel) }
        composable(Screen.Friends.route) { FriendsListScreen(rootNavController, innerNavController, userListViewModel, connectionViewModel) }
        composable(Screen.Search.route) { SearchUsersScreen(innerNavController, connectionViewModel) }
        composable(Screen.Requests.route) { ConnectionRequestsScreen(innerNavController, connectionViewModel) }
        composable(Screen.Notifications.route) { NotificationsScreen(innerNavController, rootNavController, connectionViewModel) }
        composable(Screen.History.route) { CallHistoryScreen(rootNavController) }
        composable(Screen.Profile.route) { UserProfileScreen(null, rootNavController, innerNavController, userListViewModel, connectionViewModel) }
        composable(Screen.ProfileDetail.route, arguments = listOf(navArgument("userId") { type = NavType.StringType })) {
            UserProfileScreen(it.arguments?.getString("userId"), rootNavController, innerNavController, userListViewModel, connectionViewModel)
        }
        composable(Screen.EditProfile.route) { EditProfileScreen(innerNavController, userListViewModel) }
        composable(Screen.Settings.route) { SettingsScreen(rootNavController, innerNavController, userListViewModel) }
        composable(Screen.ChangePassword.route) { ChangePasswordScreen(innerNavController) }
        composable(Screen.DeleteAccount.route) { DeleteAccountScreen(rootNavController, innerNavController) }
        composable(Screen.BlockedUsers.route) { BlockedUsersScreen(innerNavController) }
        composable(Screen.About.route) { AboutScreen(innerNavController) }
        composable(Screen.Help.route) { HelpScreen(innerNavController) }
        composable(Screen.Privacy.route) { PrivacyPolicyScreen(innerNavController) }
        composable(Screen.Terms.route) { TermsOfServiceScreen(innerNavController) }
    }
}

