package com.aucepsinnovations.smart_image_picker.core.data

import android.content.Context
import androidx.camera.core.ImageCapture
import androidx.core.content.edit

object CameraPreferences {
    private const val PREFS_NAME = "camera_prefs"
    private const val KEY_FLASH_MODE = "flash_mode"

    fun saveFlashMode(context: Context, mode: Int) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit {
                putInt(KEY_FLASH_MODE, mode)
            }   // make sure this is apply(), not missing
    }

    fun getFlashMode(context: Context): Int {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getInt(KEY_FLASH_MODE, ImageCapture.FLASH_MODE_OFF)
    }
}