package com.aucepsinnovations.smart_image_picker.core.util

import android.graphics.Color
import android.os.Build
import android.view.Window
import android.view.WindowInsets
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
    controller.isAppearanceLightStatusBars = !isColorDark(color)
    controller.isAppearanceLightNavigationBars = !isColorDark(color)
}

fun isColorDark(color: Int): Boolean {
    val darkness = 1 - (0.299 * Color.red(color) +
            0.587 * Color.green(color) +
            0.114 * Color.blue(color)) / 255
    return darkness >= 0.5
}