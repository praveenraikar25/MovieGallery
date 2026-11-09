package com.raikar.moviegallery.di

import org.koin.core.module.Module

/**
 * The app's single `MovieGalleryDatabase`, built from the platform's builder plus the shared
 * configuration (`applySharedConfig`), and closed when the Koin graph closes. On Android it
 * needs the application `Context`, which `androidContext(...)` registers.
 */
internal expect val databaseModule: Module
