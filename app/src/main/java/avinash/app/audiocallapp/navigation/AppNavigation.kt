package avinash.app.audiocallapp.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import avinash.app.audiocallapp.feature.FeatureNavigation
import avinash.app.audiocallapp.feature.Routes
import avinash.app.audiocallapp.presentation.auth.SplashScreen
import avinash.app.audiocallapp.presentation.auth.LoginScreen
import avinash.app.audiocallapp.presentation.auth.RegisterScreen
import avinash.app.audiocallapp.presentation.auth.ForgotPasswordScreen
import avinash.app.audiocallapp.presentation.auth.AccountVerificationScreen
import avinash.app.audiocallapp.presentation.main.MainScreen
import avinash.app.audiocallapp.presentation.system.OfflineModeScreen
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Login : Screen("login")
    object Register : Screen("register")
    object ForgotPassword : Screen("forgot_password")
    object VerifyAccount : Screen("verify_account")
    object Offline : Screen("offline")

    object Main : Screen("main")

    object Home : Screen("home")
    object Friends : Screen("friends")
    object Search : Screen("search")
    object Requests : Screen("requests")
    object Notifications : Screen("notifications")
    object History : Screen("history")
    object Profile : Screen("profile")
    object ProfileDetail : Screen("profile/{userId}") {
        fun createRoute(userId: String): String = "profile/$userId"
    }
    object EditProfile : Screen("edit_profile")
    object Settings : Screen("settings")
    object ChangePassword : Screen("change_password")
    object DeleteAccount : Screen("delete_account")
    object BlockedUsers : Screen("blocked_users")
    object About : Screen("about")
    object Help : Screen("help")
    object Privacy : Screen("privacy")
    object Terms : Screen("terms")

    object Call : Screen(Routes.Call.PATTERN) {
        fun createRoute(userId: String, name: String, isCaller: Boolean = true): String =
            Routes.Call.create(userId, name, isCaller)
    }

    object WalkieTalkie : Screen(Routes.WalkieTalkie.PATTERN) {
        fun createRoute(friendId: String, friendName: String): String =
            Routes.WalkieTalkie.create(friendId, friendName)
    }

    object CallFeedback : Screen("call_feedback/{remoteName}/{duration}") {
        fun createRoute(remoteName: String, duration: String): String {
            val encoded = URLEncoder.encode(remoteName, StandardCharsets.UTF_8.toString())
            return "call_feedback/$encoded/$duration"
        }
    }
}

@Composable
fun AppNavigation(
    navController: NavHostController,
    featureNavigations: Set<FeatureNavigation>,
    startDestination: String = Screen.Splash.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Splash.route) {
            SplashScreen(navController = navController)
        }
        composable(Screen.Login.route) {
            LoginScreen(navController = navController)
        }
        composable(Screen.Register.route) {
            RegisterScreen(navController = navController)
        }
        composable(Screen.ForgotPassword.route) {
            ForgotPasswordScreen(navController = navController)
        }
        composable(Screen.VerifyAccount.route) {
            AccountVerificationScreen(navController = navController)
        }

        composable(Screen.Offline.route) {
            OfflineModeScreen(onRetry = { navController.popBackStack() })
        }

        composable(Screen.Main.route) {
            MainScreen(rootNavController = navController)
        }

        featureNavigations.forEach { featureNav ->
            featureNav.registerRoutes(this, navController)
        }
    }
}
