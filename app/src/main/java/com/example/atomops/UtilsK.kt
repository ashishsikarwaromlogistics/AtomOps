package com.example.atomops
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
class UtilsK {

    companion object{
        suspend fun compressImage(
            source: File,
            maxBytes: Long = 1_000_000L,   // 1 MB (decimal, safe for servers that count 1 MB = 1,000,000)
            maxDimension: Int = 1920       // longest side in pixels
        ): File = withContext(Dispatchers.IO) {

            // 1) Read size only, then pick a sample size to avoid loading a huge bitmap
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(source.absolutePath, bounds)

            var sample = 1
            while (bounds.outWidth / (sample * 2) >= maxDimension ||
                bounds.outHeight / (sample * 2) >= maxDimension
            ) sample *= 2

            val opts = BitmapFactory.Options().apply { inSampleSize = sample }
            var bitmap = BitmapFactory.decodeFile(source.absolutePath, opts)
                ?: return@withContext source

            // 2) Fix rotation using EXIF
            val degrees = when (
                ExifInterface(source.absolutePath)
                    .getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
            ) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }
            if (degrees != 0f) {
                val matrix = Matrix().apply { postRotate(degrees) }
                val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
                if (rotated != bitmap) bitmap.recycle()
                bitmap = rotated
            }

            // 3) Scale down to maxDimension
            val longest = maxOf(bitmap.width, bitmap.height)
            if (longest > maxDimension) {
                val ratio = maxDimension.toFloat() / longest
                val scaled = Bitmap.createScaledBitmap(
                    bitmap, (bitmap.width * ratio).toInt(), (bitmap.height * ratio).toInt(), true
                )
                if (scaled != bitmap) bitmap.recycle()
                bitmap = scaled
            }

            // 4) Lower JPEG quality until size <= 1 MB; if still too big, shrink the image and retry
            var quality = 90
            var out = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)

            while (out.size() > maxBytes) {
                if (quality > 40) {
                    quality -= 10
                } else {
                    val smaller = Bitmap.createScaledBitmap(
                        bitmap, (bitmap.width * 0.8f).toInt(), (bitmap.height * 0.8f).toInt(), true
                    )
                    if (smaller != bitmap) bitmap.recycle()
                    bitmap = smaller
                    quality = 80
                }
                out = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
            }
            bitmap.recycle()

            // 5) Save to a new file
            val result = File(source.parentFile, "compressed_${source.name}")
            result.outputStream().use { it.write(out.toByteArray()) }
            result
        }
    }



}