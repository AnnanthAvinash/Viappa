package avinash.app.audiocallapp.feature

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import avinash.app.audiocallapp.presentation.call.CallFeedbackScreen
import avinash.app.audiocallapp.presentation.call.CallScreen
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CallFeatureNavigation @Inject constructor() : FeatureNavigation {

    override fun registerRoutes(builder: NavGraphBuilder, navController: NavHostController) {
        builder.composable(
            route = Routes.Call.PATTERN,
            arguments = listOf(
                navArgument("remoteUserId") { type = NavType.StringType },
                navArgument("remoteName") { type = NavType.StringType },
                navArgument("isCaller") { type = NavType.BoolType; defaultValue = true }
            )
        ) { backStackEntry ->
            val remoteUserId = backStackEntry.arguments?.getString("remoteUserId") ?: ""
            val remoteName = URLDecoder.decode(
                backStackEntry.arguments?.getString("remoteName") ?: "",
                StandardCharsets.UTF_8.toString()
            )
            val isCaller = backStackEntry.arguments?.getBoolean("isCaller") ?: true
            CallScreen(
                remoteUserId = remoteUserId,
                remoteName = remoteName,
                isCaller = isCaller,
                navController = navController
            )
        }

        builder.composable(
            route = "call_feedback/{remoteName}/{duration}",
            arguments = listOf(
                navArgument("remoteName") { type = NavType.StringType },
                navArgument("duration") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val remoteName = URLDecoder.decode(
                backStackEntry.arguments?.getString("remoteName") ?: "Unknown",
                StandardCharsets.UTF_8.toString()
            )
            val duration = backStackEntry.arguments?.getString("duration") ?: "00:00"
            CallFeedbackScreen(
                remoteName = remoteName,
                duration = duration,
                navController = navController
            )
        }
    }
}
