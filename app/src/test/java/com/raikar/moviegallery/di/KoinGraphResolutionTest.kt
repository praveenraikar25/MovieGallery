package com.raikar.moviegallery.di

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import coil3.ImageLoader
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.auth.FirebaseAuth
import com.raikar.moviegallery.data.local.MovieGalleryDatabase
import com.raikar.moviegallery.domain.usecase.GetCountryDetailsUseCase
import com.raikar.moviegallery.domain.usecase.GetHomeSectionsUseCase
import com.raikar.moviegallery.domain.usecase.GetMovieUseCase
import com.raikar.moviegallery.domain.usecase.GetTopTvShowsUseCase
import com.raikar.moviegallery.domain.usecase.IdentifyMoviePosterUseCase
import com.raikar.moviegallery.domain.usecase.IsSignedInUseCase
import com.raikar.moviegallery.domain.usecase.ObserveCurrentUserUseCase
import com.raikar.moviegallery.domain.usecase.ObserveIsInWatchlistUseCase
import com.raikar.moviegallery.domain.usecase.ObserveWatchlistMoviesUseCase
import com.raikar.moviegallery.domain.usecase.SearchMoviesUseCase
import com.raikar.moviegallery.domain.usecase.SendChatMessageUseCase
import com.raikar.moviegallery.domain.usecase.SignInUseCase
import com.raikar.moviegallery.domain.usecase.SignInWithGoogleUseCase
import com.raikar.moviegallery.domain.usecase.SignOutUseCase
import com.raikar.moviegallery.domain.usecase.SignUpUseCase
import com.raikar.moviegallery.domain.usecase.ToggleWatchlistUseCase
import io.mockk.mockk
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.android.ext.koin.androidContext
import org.koin.core.KoinApplication
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.reflect.KClass

/**
 * Builds the real graph and resolves every use case, which pulls in every repository, API,
 * client, the database, Apollo and the Firebase-backed implementations. This catches what
 * `KoinGraphTest`'s `verify()` can't: a binding that only a `get()` inside a definition lambda
 * asks for, such as the BFF client's `AuthTokenProvider` (replace-hilt-with-koin design, decision 7).
 *
 * Only the two Firebase entry points are faked, because they need a configured `FirebaseApp`.
 * ViewModels aren't resolved: several start loading in `init`; their constructors are covered by
 * `verify()`. Creating these objects opens no connection and no database file.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class KoinGraphResolutionTest {
    private val koin: KoinApplication =
        koinApplication {
            androidContext(ApplicationProvider.getApplicationContext<Application>())
            allowOverride(true)
            modules(
                sharedModules + androidModules +
                    module {
                        single<FirebaseAuth> { mockk(relaxed = true) }
                        single<GenerativeModel> { mockk(relaxed = true) }
                    },
            )
        }

    @After
    fun tearDown() {
        koin.koin.get<MovieGalleryDatabase>().close()
        koin.close()
    }

    @Test
    fun `every use case and the image loader resolve against the real graph`() {
        (useCases + ImageLoader::class).forEach { type ->
            koin.koin.get<Any>(type)
        }
    }

    private val useCases: List<KClass<*>> =
        listOf(
            GetCountryDetailsUseCase::class,
            GetHomeSectionsUseCase::class,
            GetMovieUseCase::class,
            GetTopTvShowsUseCase::class,
            IdentifyMoviePosterUseCase::class,
            IsSignedInUseCase::class,
            ObserveCurrentUserUseCase::class,
            ObserveIsInWatchlistUseCase::class,
            ObserveWatchlistMoviesUseCase::class,
            SearchMoviesUseCase::class,
            SendChatMessageUseCase::class,
            SignInUseCase::class,
            SignInWithGoogleUseCase::class,
            SignOutUseCase::class,
            SignUpUseCase::class,
            ToggleWatchlistUseCase::class,
        )
}
