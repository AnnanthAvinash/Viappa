package avinash.app.audiocallapp.feature

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import avinash.app.audiocallapp.presentation.walkietalkie.WalkieTalkieScreen
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WtFeatureNavigation @Inject constructor() : FeatureNavigation {

    override fun registerRoutes(builder: NavGraphBuilder, navController: NavHostController) {
        builder.composable(
            route = Routes.WalkieTalkie.PATTERN,
            arguments = listOf(
                navArgument("friendId") { type = NavType.StringType },
                navArgument("friendName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val friendId = backStackEntry.arguments?.getString("friendId") ?: ""
            val friendName = URLDecoder.decode(
                backStackEntry.arguments?.getString("friendName") ?: "",
                StandardCharsets.UTF_8.toString()
            )
            WalkieTalkieScreen(
                friendId = friendId,
                friendName = friendName,
                navController = navController
            )
        }
    }
}
