package com.aucepsinnovations.smart_image_picker.core.api

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class PickerConfig(
    val countMode: CountMode = CountMode.UNLIMITED,
    val outputType: OutputType = OutputType.URI,
    val allowCamera: Boolean = true,
    val allowGallery: Boolean = true,
    val enableCrop: Boolean = true,   // enable per-image crop UI
    val compressMaxBytes: Long = 3_000_000, // ~3MB target (0 = no compression)
    val copyToAppCache: Boolean = true,     // normalize all inputs to cache
    val backgroundColor: Int = 0xFF000000.toInt(), // Default black
    val accentColor: Int = 0xFFFFFFFF.toInt() // Default white
) : Parcelable