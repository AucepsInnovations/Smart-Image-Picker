package com.aucepsinnovations.smart_image_picker.core.api

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
sealed interface CountMode : Parcelable {
    object EXACT_ONE : CountMode
    data class EXACT(val n: Int) : CountMode
    data class MAX(val n: Int) : CountMode
    data class MIN(val n: Int) : CountMode
    data class RANGE(val min: Int, val max: Int) : CountMode
    object UNLIMITED : CountMode
}