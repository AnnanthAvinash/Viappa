package avinash.app.audiocallapp.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import avinash.app.audiocallapp.presentation.call.CallScreen
import avinash.app.audiocallapp.presentation.registration.RegistrationScreen
import avinash.app.audiocallapp.presentation.userlist.UserListScreen
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

sealed class Screen(val route: String) {
    object Registration : Screen("registration")
    object UserList : Screen("user_list")
    object OutgoingCall : Screen("outgoing_call/{calleeId}/{calleeName}") {
        fun createRoute(calleeId: String, calleeName: String): String {
            val encodedName = URLEncoder.encode(calleeName, StandardCharsets.UTF_8.toString())
            return "outgoing_call/$calleeId/$encodedName"
        }
    }
    object IncomingCall : Screen("incoming_call/{callId}/{callerId}/{callerName}") {
        fun createRoute(callId: String, callerId: String, callerName: String): String {
            val encodedName = URLEncoder.encode(callerName, StandardCharsets.UTF_8.toString())
            return "incoming_call/$callId/$callerId/$encodedName"
        }
    }
}

@Composable
fun AppNavigation(
    navController: NavHostController,
    startDestination: String = Screen.Registration.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Registration.route) {
            RegistrationScreen(
                onRegistrationSuccess = {
                    navController.navigate(Screen.UserList.route) {
                        popUpTo(Screen.Registration.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.UserList.route) {
            UserListScreen(
                onCallUser = { calleeId, calleeName ->
                    navController.navigate(Screen.OutgoingCall.createRoute(calleeId, calleeName))
                },
                onAnswerCall = { callId, callerId, callerName ->
                    navController.navigate(Screen.IncomingCall.createRoute(callId, callerId, callerName))
                },
                onSignOut = {
                    navController.navigate(Screen.Registration.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = Screen.OutgoingCall.route,
            arguments = listOf(
                navArgument("calleeId") { type = NavType.StringType },
                navArgument("calleeName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val calleeId = backStackEntry.arguments?.getString("calleeId") ?: ""
            val calleeName = backStackEntry.arguments?.getString("calleeName")?.let {
                URLDecoder.decode(it, StandardCharsets.UTF_8.toString())
            } ?: ""

            CallScreen(
                calleeId = calleeId,
                calleeName = calleeName,
                onCallEnded = {
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = Screen.IncomingCall.route,
            arguments = listOf(
                navArgument("callId") { type = NavType.StringType },
                navArgument("callerId") { type = NavType.StringType },
                navArgument("callerName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val callId = backStackEntry.arguments?.getString("callId") ?: ""
            val callerId = backStackEntry.arguments?.getString("callerId") ?: ""
            val callerName = backStackEntry.arguments?.getString("callerName")?.let {
                URLDecoder.decode(it, StandardCharsets.UTF_8.toString())
            } ?: ""

            CallScreen(
                incomingCallId = callId,
                incomingCallerId = callerId,
                incomingCallerName = callerName,
                onCallEnded = {
                    navController.popBackStack()
                }
            )
        }
    }
}
