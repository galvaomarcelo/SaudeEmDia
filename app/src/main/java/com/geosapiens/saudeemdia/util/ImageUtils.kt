package com.geosapiens.saudeemdia.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

object ImageUtils {
    
    /**
     * Compresses an image from a Uri and returns the bytes.
     * Reduces dimensions and quality to save storage space and bandwidth.
     */
    suspend fun compressImage(context: Context, uri: Uri, maxWidth: Int = 1200, maxHeight: Int = 1200, quality: Int = 75): ByteArray? {
        return withContext(Dispatchers.IO) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri) ?: return@withContext null
                
                // Decode bounds to get original dimensions
                val options = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }
                BitmapFactory.decodeStream(inputStream, null, options)
                inputStream.close()
                
                // Calculate scale factor
                var inSampleSize = 1
                if (options.outHeight > maxHeight || options.outWidth > maxWidth) {
                    val halfHeight: Int = options.outHeight / 2
                    val halfWidth: Int = options.outWidth / 2
                    while (halfHeight / inSampleSize >= maxHeight && halfWidth / inSampleSize >= maxWidth) {
                        inSampleSize *= 2
                    }
                }
                
                // Decode with inSampleSize
                val decodeOptions = BitmapFactory.Options().apply {
                    this.inSampleSize = inSampleSize
                }
                
                val compressedInputStream = context.contentResolver.openInputStream(uri) ?: return@withContext null
                val bitmap = BitmapFactory.decodeStream(compressedInputStream, null, decodeOptions)
                compressedInputStream.close()
                
                if (bitmap == null) return@withContext null
                
                // Resize if still too large (to exact dimensions if needed)
                val scaledBitmap = if (bitmap.width > maxWidth || bitmap.height > maxHeight) {
                    val ratio = Math.min(maxWidth.toFloat() / bitmap.width, maxHeight.toFloat() / bitmap.height)
                    Bitmap.createScaledBitmap(bitmap, (bitmap.width * ratio).toInt(), (bitmap.height * ratio).toInt(), true)
                } else {
                    bitmap
                }
                
                // Compress quality
                val outputStream = ByteArrayOutputStream()
                scaledBitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
                val result = outputStream.toByteArray()
                
                Log.d("ImageUtils", "Image compressed: ${options.outWidth}x${options.outHeight} -> ${scaledBitmap.width}x${scaledBitmap.height}. Size: ${result.size / 1024} KB")
                
                result
            } catch (e: Exception) {
                Log.e("ImageUtils", "Error compressing image", e)
                null
            }
        }
    }
}
