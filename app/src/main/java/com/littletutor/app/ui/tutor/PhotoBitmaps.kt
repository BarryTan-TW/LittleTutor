package com.littletutor.app.ui.tutor

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface

/** Memory-safe photo decoding: downsampled and rotated upright according to EXIF. Call off the main thread. */
object PhotoBitmaps {
    const val OCR_MAX_DIMENSION = 2048
    const val PREVIEW_MAX_DIMENSION = 1600
    const val THUMBNAIL_MAX_DIMENSION = 320

    fun decodeUpright(path: String, maxDimension: Int): Bitmap? = runCatching {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        val sampleSize = sampleSizeFor(bounds.outWidth, bounds.outHeight, maxDimension)
        val decoded = BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = sampleSize })
            ?: return null
        val degrees = exifRotationDegrees(path)
        if (degrees == 0) {
            decoded
        } else {
            val matrix = Matrix().apply { postRotate(degrees.toFloat()) }
            Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
                .also { if (it !== decoded) decoded.recycle() }
        }
    }.getOrNull()

    /** Largest power-of-two sample size that keeps the longest side at or above [maxDimension]. */
    fun sampleSizeFor(width: Int, height: Int, maxDimension: Int): Int {
        var sample = 1
        val longest = maxOf(width, height)
        while (longest / (sample * 2) >= maxDimension) sample *= 2
        return sample
    }

    private fun exifRotationDegrees(path: String): Int = runCatching {
        when (ExifInterface(path).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
            ExifInterface.ORIENTATION_ROTATE_90, ExifInterface.ORIENTATION_TRANSPOSE -> 90
            ExifInterface.ORIENTATION_ROTATE_180 -> 180
            ExifInterface.ORIENTATION_ROTATE_270, ExifInterface.ORIENTATION_TRANSVERSE -> 270
            else -> 0
        }
    }.getOrDefault(0)
}
