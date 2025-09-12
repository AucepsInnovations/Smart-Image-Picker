package com.aucepsinnovations.smart_image_picker.core.data

import android.app.Activity
import java.io.File

object SmartImagePicker {
    private const val MAX_FILE_AGE_MS = 24 * 60 * 60 * 1000L // 24 hours

    /**
     * Clears only old cache files (older than MAX_FILE_AGE_MS).
     */
    fun clearOldCache(activity: Activity) {
        val cacheDir = File(activity.externalCacheDir, "SmartImagePicker")
        if (cacheDir.exists()) {
            val now = System.currentTimeMillis()
            cacheDir.listFiles()?.forEach { file ->
                val age = now - file.lastModified()
                if (age > MAX_FILE_AGE_MS) {
                    file.deleteRecursively()
                }
            }
        }
    }

    /**
     * Clears all SmartImagePicker cache immediately.
     */
    fun clearAllCache(activity: Activity) {
        val cacheDir = File(activity.externalCacheDir, "SmartImagePicker")
        if (cacheDir.exists()) {
            cacheDir.deleteRecursively()
        }
    }
}