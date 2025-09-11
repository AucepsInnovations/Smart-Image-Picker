package com.aucepsinnovations.smart_image_picker.core.data

import android.content.Context
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.core.content.edit

object CameraPreferences {
    private const val PREFS_NAME = "camera_prefs"
    private const val KEY_FLASH_MODE = "flash_mode"
    private const val KEY_CAMERA_MODE = "camera_mode"

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

    fun saveCameraMode(context: Context, mode: Int) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit {
                putInt(KEY_CAMERA_MODE, mode)
            }   // make sure this is apply(), not missing
    }

    fun getCameraMode(context: Context): Int {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getInt(KEY_CAMERA_MODE, CameraSelector.LENS_FACING_BACK)
    }
}