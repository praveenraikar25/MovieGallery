package com.raikar.moviegallery.ui.navigation

internal object Routes {
    const val LOGIN = "login"
    const val MAIN = "main"
    const val DETAIL = "detail/{movieId}"

    const val HOME = "home"
    const val SEARCH = "search"
    const val WATCHLIST = "watchlist"
    const val PROFILE = "profile"

    fun detail(movieId: Int) = "detail/$movieId"
}
