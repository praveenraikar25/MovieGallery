package com.raikar.moviegallery.di

import com.raikar.moviegallery.platform.FirebaseAiBridge
import com.raikar.moviegallery.platform.FirebaseAuthBridge
import com.raikar.moviegallery.ui.navigation.AppSessionViewModel
import com.raikar.moviegallery.ui.screens.chat.AiChatViewModel
import com.raikar.moviegallery.ui.screens.detail.MovieDetailViewModel
import com.raikar.moviegallery.ui.screens.home.HomeViewModel
import com.raikar.moviegallery.ui.screens.login.LoginViewModel
import com.raikar.moviegallery.ui.screens.profile.ProfileViewModel
import com.raikar.moviegallery.ui.screens.search.SearchViewModel
import com.raikar.moviegallery.ui.screens.tvshows.TopTvShowsViewModel
import com.raikar.moviegallery.ui.screens.watchlist.WatchlistViewModel
import org.koin.core.Koin
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.core.parameter.parametersOf

/**
 * Starts the app's graph on iOS: [sharedModules] plus [iosPlatformModule]. Swift calls it once at
 * launch and keeps the returned graph for the app's lifetime; a second call fails with Koin's
 * "already started" (add-ios-platform-layer design, decision 5).
 *
 * ```swift
 * let graph = IosKoinKt.startMovieGallery(config: config, authBridge: auth, aiBridge: ai)
 * let home = ViewModelOwner { graph.homeViewModel() }
 * ```
 *
 * Not named `initKoin`: Objective-C export would rename that to `doInitKoin`.
 */
fun startMovieGallery(
    config: IosAppConfig,
    authBridge: FirebaseAuthBridge,
    aiBridge: FirebaseAiBridge,
): MovieGalleryGraph {
    val app = startKoin { modules(iosModules(config, authBridge, aiBridge)) }
    return MovieGalleryGraph(app.koin)
}

/** Every module the iOS graph loads. Tests build an isolated `koinApplication` from these. */
internal fun iosModules(
    config: IosAppConfig,
    authBridge: FirebaseAuthBridge,
    aiBridge: FirebaseAiBridge,
): List<Module> = sharedModules + iosPlatformModule(config, authBridge, aiBridge)

/**
 * How Swift gets a ViewModel. Swift can't call Koin's reified `get<T>()`, so there is one function
 * per ViewModel in `viewModelModule`; `IosKoinGraphTest` fails if the two lists differ.
 *
 * Each call returns a new instance. Its lifetime belongs to a `ViewModelOwner`, which Swift clears
 * when the screen goes away.
 */
class MovieGalleryGraph internal constructor(
    private val koin: Koin,
) {
    fun appSessionViewModel(): AppSessionViewModel = koin.get()

    fun aiChatViewModel(): AiChatViewModel = koin.get()

    fun homeViewModel(): HomeViewModel = koin.get()

    fun loginViewModel(): LoginViewModel = koin.get()

    /** [movieId] is the navigation argument, passed as a constructor parameter as on Android. */
    fun movieDetailViewModel(movieId: Int): MovieDetailViewModel = koin.get { parametersOf(movieId) }

    fun profileViewModel(): ProfileViewModel = koin.get()

    fun searchViewModel(): SearchViewModel = koin.get()

    fun topTvShowsViewModel(): TopTvShowsViewModel = koin.get()

    fun watchlistViewModel(): WatchlistViewModel = koin.get()
}
