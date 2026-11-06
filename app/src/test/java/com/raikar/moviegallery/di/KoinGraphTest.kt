package com.raikar.moviegallery.di

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import com.google.firebase.FirebaseApp
import com.google.firebase.inject.Provider
import org.junit.Test
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.dsl.module
import org.koin.test.verify.verify
import java.util.concurrent.Executor
import java.util.concurrent.ScheduledExecutorService

/**
 * Koin resolves the graph at runtime, so tests stand in for Hilt's compile-time check
 * (replace-hilt-with-koin design, decision 7). `verify()` reads constructor types by reflection
 * and creates nothing, so it needs neither Robolectric nor Firebase. It only checks classes
 * registered by constructor reference (`singleOf`, `factoryOf`, `viewModelOf`): a `get()` inside a
 * definition lambda is invisible to it, which is what `KoinGraphResolutionTest` covers. Which
 * qualified client each API gets is checked in `:shared`'s `SharedModulesTest`.
 */
@OptIn(KoinExperimentalAPI::class)
class KoinGraphTest {
    @Test
    fun `every definition in the app graph can be resolved`() {
        module { includes(sharedModules + androidModules) }.verify(
            extraTypes =
                listOf(
                    // Supplied by the running app: androidContext() and the navigation entry.
                    Context::class,
                    SavedStateHandle::class,
                    // FirebaseAuth's own constructor parameters. verify() walks into the
                    // constructor of every bound type, but FirebaseAuth.getInstance() builds it,
                    // never Koin.
                    FirebaseApp::class,
                    Provider::class,
                    Executor::class,
                    ScheduledExecutorService::class,
                ),
        )
    }
}
