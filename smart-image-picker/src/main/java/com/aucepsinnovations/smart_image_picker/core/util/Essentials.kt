package com.aucepsinnovations.smart_image_picker.core.util

import android.content.Context
import android.os.Build
import android.view.Window
import android.view.WindowInsets
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat

@Suppress("DEPRECATION")
fun setupSystemBars(window: Window, color: Int) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
        WindowCompat.setDecorFitsSystemWindows(window, false)

        window.decorView.setOnApplyWindowInsetsListener { view, insets ->
            val systemBarInsets = insets.getInsets(WindowInsets.Type.systemBars())
            view.setBackgroundColor(color)
            view.setPadding(0, systemBarInsets.top, 0, 0)
            insets
        }

        // Make sure nav bar is same color (optional)
        window.navigationBarColor = color
    } else {
        window.statusBarColor = color
        window.navigationBarColor = color
    }

    // Set icons to white
    val controller = WindowCompat.getInsetsController(window, window.decorView)
    controller.isAppearanceLightStatusBars = false
    controller.isAppearanceLightNavigationBars = false
}

fun Context.colorToHex(colorResId: Int, includeAlpha: Boolean = false): String {
    val colorInt = ContextCompat.getColor(this, colorResId)
    return if (includeAlpha) {
        String.format("#%08X", colorInt) // ARGB
    } else {
        String.format("#%06X", 0xFFFFFF and colorInt) // RGB
    }
}
