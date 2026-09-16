package com.raikar.moviegallery.ui.screens.chat

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

/** Matches the authority declared for the FileProvider in AndroidManifest.xml. */
private const val FILE_PROVIDER_SUFFIX = ".fileprovider"

/** Mirrors `cache-path` in res/xml/file_paths.xml. */
private const val CAPTURE_DIR = "poster-captures"

/**
 * Creates the destination a camera capture writes into.
 *
 * `ActivityResultContracts.TakePicture` needs a writable URI up front, and the
 * camera app can only write somewhere it has been granted access to — hence a
 * [FileProvider] URI rather than a bare `file://` path.
 *
 * Captures go in the cache directory so the OS can reclaim them; the file is kept
 * after sending because the chat bubble still renders it.
 */
fun createPosterCaptureUri(context: Context): Uri {
    val directory = File(context.cacheDir, CAPTURE_DIR).apply { mkdirs() }
    val file = File(directory, "poster-${System.currentTimeMillis()}.jpg")
    return FileProvider.getUriForFile(context, context.packageName + FILE_PROVIDER_SUFFIX, file)
}
