package avinash.app.audiocallapp.feature

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController

interface FeatureNavigation {
    fun registerRoutes(builder: NavGraphBuilder, navController: NavHostController)
}
