package com.raikar.moviegallery.ui.screens.chat

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import java.io.File

/** Matches the authority declared for the FileProvider in AndroidManifest.xml. */
private const val FILE_PROVIDER_SUFFIX = ".fileprovider"

/** Mirrors `cache-path` in res/xml/file_paths.xml. */
private const val CAPTURE_DIR = "poster-captures"

/** Captures older than this are from an earlier session and nothing can still show them. */
private const val PRUNE_AGE_MILLIS = 24L * 60L * 60L * 1000L

/**
 * Creates the destination a camera capture writes into.
 *
 * `ActivityResultContracts.TakePicture` needs a writable URI up front, and the
 * camera app can only write somewhere it has been granted access to — hence a
 * [FileProvider] URI rather than a bare `file://` path.
 *
 * Captures go in the cache directory so the OS can reclaim them; the file is kept
 * after sending because the chat bubble still renders it. See
 * [deletePosterCapture] and [prunePosterCaptures] for how they are cleaned up.
 */
fun createPosterCaptureUri(context: Context): Uri {
    val directory = File(context.cacheDir, CAPTURE_DIR).apply { mkdirs() }
    val file = File(directory, "poster-${System.currentTimeMillis()}.jpg")
    return FileProvider.getUriForFile(context, context.packageName + FILE_PROVIDER_SUFFIX, file)
}

/**
 * Deletes a capture no bubble will ever reference — the user backed out of the
 * camera's confirm screen, or the launch failed — since the file is created before
 * the result comes back.
 *
 * Goes through the resolver rather than a path, because all this side has is the
 * [FileProvider] URI it handed the camera app.
 */
fun deletePosterCapture(
    context: Context,
    imageUri: String,
) {
    runCatching { context.contentResolver.delete(imageUri.toUri(), null, null) }
}

/**
 * Drops captures left behind by earlier sessions, so the directory does not grow
 * for the life of the install.
 *
 * Age-based rather than "everything not currently on screen": a capture still being
 * decoded, or one being restored after the camera app pushed this process out of
 * memory, is seconds old and must survive. Blocking I/O — call off the main thread.
 */
fun prunePosterCaptures(context: Context) {
    val directory = File(context.cacheDir, CAPTURE_DIR)
    val cutoff = System.currentTimeMillis() - PRUNE_AGE_MILLIS
    directory.listFiles()?.forEach { file ->
        if (file.lastModified() < cutoff) file.delete()
    }
}
