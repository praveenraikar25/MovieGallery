package com.raikar.moviegallery.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.raikar.moviegallery.ui.screens.chat.AiChatScreen
import com.raikar.moviegallery.ui.screens.detail.MovieDetailScreen
import com.raikar.moviegallery.ui.screens.login.LoginScreen
import com.raikar.moviegallery.ui.screens.tvshows.TopTvShowsScreen
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun AppNavHost(sessionViewModel: AppSessionViewModel = koinViewModel()) {
    val navController = rememberNavController()

    // Read once at creation, so the start destination never changes under the NavHost.
    val startRoute = if (sessionViewModel.isSignedIn) Routes.MAIN else Routes.LOGIN

    NavHost(navController = navController, startDestination = startRoute) {
        composable(Routes.LOGIN) {
            LoginScreen(
                onAuthenticated = {
                    navController.navigate(Routes.MAIN) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                },
            )
        }
        composable(Routes.MAIN) {
            MainScaffold(
                onMovieClick = { movieId -> navController.navigate(Routes.detail(movieId)) },
                onSignOut = {
                    sessionViewModel.signOut()
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onFindMoviesWithAi = { navController.navigate(Routes.AI_CHAT) },
                onTopTvShows = { navController.navigate(Routes.TOP_TV_SHOWS) },
            )
        }
        composable(
            route = Routes.DETAIL,
            arguments = listOf(navArgument("movieId") { type = NavType.IntType }),
        ) { entry ->
            // Restored with the back stack after process death, as SavedStateHandle was.
            val movieId = checkNotNull(entry.arguments?.getInt("movieId"))
            MovieDetailScreen(
                // Keyed to this back-stack entry, so each detail screen gets its own ViewModel.
                viewModel = koinViewModel { parametersOf(movieId) },
                onBack = { navController.popBackStack() },
            )
        }
        composable(Routes.AI_CHAT) {
            AiChatScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.TOP_TV_SHOWS) {
            TopTvShowsScreen(onBack = { navController.popBackStack() })
        }
    }
}
