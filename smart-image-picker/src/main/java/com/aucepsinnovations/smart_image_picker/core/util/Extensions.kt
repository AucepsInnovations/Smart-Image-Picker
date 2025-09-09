package com.aucepsinnovations.smart_image_picker.core.util

import android.content.Context
import android.util.TypedValue
import kotlin.math.roundToInt

fun Float.dpToPx(context: Context): Int {
    return (TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP, this,
        context.resources.displayMetrics
    ) + 0.5f).roundToInt()
}