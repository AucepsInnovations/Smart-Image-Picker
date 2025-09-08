package com.aucepsinnovations.smart_image_picker.core.api

import android.net.Uri
import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.io.File

@Parcelize
sealed interface PickerResult : Parcelable {
    data class Success(
        val uris: List<Uri> = emptyList(),
        val files: List<File> = emptyList()
    ) : PickerResult

    data object Canceled : PickerResult
    data class Error(val message: String) : PickerResult
}