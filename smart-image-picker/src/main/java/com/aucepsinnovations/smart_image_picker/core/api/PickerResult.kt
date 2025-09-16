package com.aucepsinnovations.smart_image_picker.core.api

import android.net.Uri
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
sealed interface PickerResult : Parcelable {
    data class Single(val uri: Uri) : PickerResult
    data class Multiple(val uris: List<Uri>) : PickerResult
    data object Canceled : PickerResult
    data class Error(val message: String) : PickerResult
}