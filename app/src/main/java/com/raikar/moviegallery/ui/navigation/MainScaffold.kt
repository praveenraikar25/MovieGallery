package com.raikar.moviegallery.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.raikar.moviegallery.ui.components.MovieTabBar
import com.raikar.moviegallery.ui.components.TabItem
import com.raikar.moviegallery.ui.screens.home.HomeScreen
import com.raikar.moviegallery.ui.screens.profile.ProfileScreen
import com.raikar.moviegallery.ui.screens.search.SearchScreen
import com.raikar.moviegallery.ui.screens.watchlist.WatchlistScreen

@Composable
fun MainScaffold(
    onMovieClick: (Int) -> Unit,
    onSignOut: () -> Unit,
    onFindMoviesWithAi: () -> Unit,
    onTopTvShows: () -> Unit,
) {
    val tabNavController = rememberNavController()
    val backStackEntry by tabNavController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    val activeTab =
        when (currentRoute) {
            Routes.SEARCH -> TabItem.Search
            Routes.WATCHLIST -> TabItem.Watchlist
            Routes.PROFILE -> TabItem.Profile
            else -> TabItem.Home
        }

    Scaffold(
        bottomBar = {
            MovieTabBar(
                active = activeTab,
                onSelect = { tab ->
                    val route =
                        when (tab) {
                            TabItem.Home -> Routes.HOME
                            TabItem.Search -> Routes.SEARCH
                            TabItem.Watchlist -> Routes.WATCHLIST
                            TabItem.Profile -> Routes.PROFILE
                        }
                    tabNavController.navigate(route) {
                        popUpTo(tabNavController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
            )
        },
    ) { innerPadding ->
        NavHost(
            navController = tabNavController,
            startDestination = Routes.HOME,
            modifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding()),
        ) {
            composable(Routes.HOME) {
                HomeScreen(onMovieClick = onMovieClick)
            }
            composable(Routes.SEARCH) {
                SearchScreen(onMovieClick = onMovieClick)
            }
            composable(Routes.WATCHLIST) {
                WatchlistScreen(onMovieClick = onMovieClick)
            }
            composable(Routes.PROFILE) {
                ProfileScreen(
                    onSignOut = onSignOut,
                    onFindMoviesWithAi = onFindMoviesWithAi,
                    onTopTvShows = onTopTvShows,
                )
            }
        }
    }
}
