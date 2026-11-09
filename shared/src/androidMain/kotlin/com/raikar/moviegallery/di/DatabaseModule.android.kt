package com.raikar.moviegallery.di

import android.content.Context
import com.raikar.moviegallery.data.local.MovieGalleryDatabase
import com.raikar.moviegallery.data.local.applySharedConfig
import com.raikar.moviegallery.data.local.movieGalleryDatabaseBuilder
import org.koin.core.module.Module
import org.koin.dsl.module
import org.koin.dsl.onClose

internal actual val databaseModule: Module =
    module {
        single<MovieGalleryDatabase> {
            movieGalleryDatabaseBuilder(get<Context>()).applySharedConfig().build()
        } onClose { it?.close() }
    }
