package com.aucepsinnovations.smart_image_picker.core.data

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.result.ActivityResultLauncher
import com.yalantis.ucrop.UCrop
import java.io.File

object Cropper {
    fun startCrop(
        activity: Activity,
        sourceUri: Uri,
        cropImageLauncher: ActivityResultLauncher<Intent>
    ) {
        val destinationUri = Uri.fromFile(
            getTempImageFile(activity, "CROP_${System.currentTimeMillis()}.jpg")
        )

        val options = UCrop.Options().apply {
            setCompressionFormat(Bitmap.CompressFormat.JPEG)
            setCompressionQuality(90)
        }

        val cropIntent = UCrop.of(sourceUri, destinationUri)
            .withOptions(options)
            .withAspectRatio(1f, 1f) // optional
            .withMaxResultSize(1080, 1080)
            .getIntent(activity)

        cropImageLauncher.launch(cropIntent)
    }

    private fun getTempImageFile(activity: Activity, fileName: String): File {
        val cacheDir = File(activity.externalCacheDir, "SmartImagePicker")
        if (!cacheDir.exists()) {
            cacheDir.mkdirs()
        }
        return File(cacheDir, fileName)
    }

    fun clearSmartImagePickerCache(activity: Activity) {
        val cacheDir = File(activity.externalCacheDir, "SmartImagePicker")
        if (cacheDir.exists()) {
            cacheDir.deleteRecursively()
        }
    }
}