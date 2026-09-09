package com.raikar.moviegallery

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import javax.inject.Provider

@HiltAndroidApp
class MovieGalleryApplication :
    Application(),
    SingletonImageLoader.Factory {
    /**
     * A Provider because [newImageLoader] runs on the first image load — after Hilt
     * field injection — and this keeps the loader (and its shared OkHttp client)
     * configured in [com.raikar.moviegallery.di.NetworkModule] rather than here.
     */
    @Inject
    lateinit var imageLoader: Provider<ImageLoader>

    override fun onCreate() {
        super.onCreate()
        // Firebase AI Logic enforces App Check, so without a provider every Gemini
        // call fails before it reaches the model. The two provider artifacts live on
        // different variant classpaths, so the implementation is per-source-set.
        // FirebaseApp itself is initialised by the google-services content provider.
        installAppCheck()
    }

    override fun newImageLoader(context: PlatformContext): ImageLoader = imageLoader.get()
}
