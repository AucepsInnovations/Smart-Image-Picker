package com.aucepsinnovations.smart_image_picker.core.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

object SmartImagePicker {
    private const val MAX_FILE_AGE_MS = 24 * 60 * 60 * 1000L // 24 hours
    private const val CACHE_FOLDER = "SmartImagePicker"

    /**
     * Get (or create) the cache folder for SmartImagePicker inside externalCacheDir.
     */
    private fun getCacheDir(context: Context): File {
        val cacheDir = File(context.externalCacheDir, CACHE_FOLDER)
        if (!cacheDir.exists()) cacheDir.mkdirs()
        return cacheDir
    }

    /**
     * Clears only old cache files (older than MAX_FILE_AGE_MS).
     */
    fun clearOldCache(context: Context) {
        val cacheDir = getCacheDir(context)
        val now = System.currentTimeMillis()
        cacheDir.listFiles()?.forEach { file ->
            val age = now - file.lastModified()
            if (age > MAX_FILE_AGE_MS) {
                file.deleteRecursively()
            }
        }
    }

    /**
     * Clears all SmartImagePicker cache immediately.
     */
    fun clearAllCache(context: Context) {
        val cacheDir = getCacheDir(context)
        if (cacheDir.exists()) {
            cacheDir.deleteRecursively()
        }
    }

    /**
     * Convert URI -> File.
     * If [targetSizeMB] is provided, compress image to <= targetSizeMB.
     */
    fun uriToFile(
        context: Context,
        uri: Uri,
        targetSizeMB: Int? = null,
        format: Bitmap.CompressFormat = Bitmap.CompressFormat.JPEG
    ): File? {
        return try {
            val cacheDir = getCacheDir(context)

            if (targetSizeMB != null) {
                // Compress to target size
//                compressImageToTargetSize(context, uri, targetSizeMB, format, cacheDir)
                // Compress with resizing support
                compressImageToTargetSizeWithDownScale(context, uri, targetSizeMB, format, cacheDir)
            } else {
                // Just copy as-is
                val file = File(cacheDir, "sip_${System.currentTimeMillis()}.jpg")
                context.contentResolver.openInputStream(uri)?.use { input ->
                    file.outputStream().use { output -> input.copyTo(output) }
                }
                file
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Convert multiple URIs to Files.
     */
    fun urisToFiles(
        context: Context,
        uris: List<Uri>,
        targetSizeMB: Int? = null,
        format: Bitmap.CompressFormat = Bitmap.CompressFormat.JPEG
    ): List<File> {
        return uris.mapNotNull { uriToFile(context, it, targetSizeMB, format) }
    }

    /**
     * Compress image to a target size (in MB) and save inside SmartImagePicker cache.
     */
    private fun compressImageToTargetSize(
        context: Context,
        sourceUri: Uri,
        targetSizeMB: Int,
        format: Bitmap.CompressFormat,
        cacheDir: File
    ): File? {
        return try {
            val inputStream = context.contentResolver.openInputStream(sourceUri) ?: return null
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()

            val targetSizeBytes = targetSizeMB * 1024 * 1024
            var quality = 100
            var outputFile: File? = null

            do {
                val stream = ByteArrayOutputStream()
                bitmap.compress(format, quality, stream)

                if (stream.size() <= targetSizeBytes || quality <= 5) {
                    // Save compressed file
                    outputFile = File(cacheDir, "sip_compressed_${System.currentTimeMillis()}.jpg")
                    FileOutputStream(outputFile).use { it.write(stream.toByteArray()) }
                    break
                }

                // Reduce quality step by step
                quality -= 5
            } while (true)

            outputFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }


    /**
     * Compress image to a target size (in MB).
     * 1. Start with quality compression
     * 2. If still too big, downscale resolution
     */
    private fun compressImageToTargetSizeWithDownScale(
        context: Context,
        sourceUri: Uri,
        targetSizeMB: Int,
        format: Bitmap.CompressFormat,
        cacheDir: File
    ): File? {
        return try {
            val inputStream = context.contentResolver.openInputStream(sourceUri) ?: return null
            var bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()

            val targetSizeBytes = targetSizeMB * 1024 * 1024
            var quality = 100
            var outputFile: File? = null

            // Step 1: Try compressing with decreasing quality
            do {
                val stream = ByteArrayOutputStream()
                bitmap.compress(format, quality, stream)

                if (stream.size() <= targetSizeBytes || quality <= 5) {
                    outputFile = File(cacheDir, "sip_compressed_${System.currentTimeMillis()}.jpg")
                    FileOutputStream(outputFile).use { it.write(stream.toByteArray()) }
                    if (stream.size() <= targetSizeBytes) return outputFile
                    break // go to resizing step
                }

                quality -= 5
            } while (true)

            // Step 2: Resize (downscale) until it fits
            var width = bitmap.width
            var height = bitmap.height
            while (outputFile == null || outputFile.length() > targetSizeBytes) {
                width = (width * 0.9).toInt().coerceAtLeast(100)   // reduce by 10%
                height = (height * 0.9).toInt().coerceAtLeast(100)

                bitmap = Bitmap.createScaledBitmap(bitmap, width, height, true)

                val stream = ByteArrayOutputStream()
                bitmap.compress(format, quality.coerceAtLeast(30), stream) // don’t go below ~30
                outputFile = File(cacheDir, "sip_resized_${System.currentTimeMillis()}.jpg")
                FileOutputStream(outputFile).use { it.write(stream.toByteArray()) }

                if (width <= 100 || height <= 100) break // stop shrinking at tiny size
            }

            outputFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}