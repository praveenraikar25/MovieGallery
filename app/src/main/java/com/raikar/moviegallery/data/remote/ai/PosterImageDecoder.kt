package com.raikar.moviegallery.data.remote.ai

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import androidx.core.net.toUri
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Turns the opaque image URI the domain passes around into a [Bitmap] Gemini can
 * read. The only place in the app that touches a `ContentResolver`.
 *
 * [ImageDecoder] rather than `BitmapFactory` because it applies the JPEG's EXIF
 * orientation itself — a poster photographed in portrait would otherwise reach the
 * model on its side, which is exactly the kind of image it fails to identify.
 */
@Singleton
class PosterImageDecoder
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) {
        /** Blocking; call from an IO dispatcher. Throws if the URI can't be read or decoded. */
        fun decode(imageUri: String): Bitmap {
            val source = ImageDecoder.createSource(context.contentResolver, imageUri.toUri())
            return ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                // ImagePart has to read the pixels back out to serialise them, and the
                // default HARDWARE bitmap lives on the GPU where they aren't readable.
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                decoder.setTargetSampleSize(sampleSizeFor(info.size.width, info.size.height))
            }
        }

        /**
         * Smallest power-of-two downscale that brings the long edge to [MAX_EDGE_PX] or
         * below. A 12MP capture is ~4000px on its long edge; the model tiles the image
         * internally anyway, so sending it full-size only costs upload time and heap.
         *
         * Note the comparison is on the *downscaled* edge, not half of it: stopping while
         * the edge was still at or above the cap left it anywhere up to 2*[MAX_EDGE_PX],
         * so a 3000x4000 capture decoded to 1500x2000 — four times the intended pixels
         * and an 11MB software bitmap.
         */
        private fun sampleSizeFor(
            width: Int,
            height: Int,
        ): Int {
            val longEdge = maxOf(width, height)
            var sampleSize = 1
            while (longEdge / sampleSize > MAX_EDGE_PX) {
                sampleSize *= 2
            }
            return sampleSize
        }

        private companion object {
            /** Enough resolution for poster title text to stay legible to the model. */
            const val MAX_EDGE_PX = 1024
        }
    }
