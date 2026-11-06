package com.raikar.moviegallery

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import com.raikar.moviegallery.di.androidModules
import com.raikar.moviegallery.di.sharedModules
import org.koin.android.ext.android.get
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class MovieGalleryApplication :
    Application(),
    SingletonImageLoader.Factory {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@MovieGalleryApplication)
            modules(sharedModules + androidModules)
        }
        // Firebase AI Logic enforces App Check, so without a provider every Gemini
        // call fails before it reaches the model. The two provider artifacts live on
        // different variant classpaths, so the implementation is per-source-set.
        // FirebaseApp itself is initialised by the google-services content provider.
        installAppCheck()
    }

    /**
     * Runs on the first image load, after [onCreate] has started Koin, so the loader (and its
     * shared OkHttp client) stays configured in the DI graph (`androidPlatformModule`) rather
     * than here.
     */
    override fun newImageLoader(context: PlatformContext): ImageLoader = get()
}
