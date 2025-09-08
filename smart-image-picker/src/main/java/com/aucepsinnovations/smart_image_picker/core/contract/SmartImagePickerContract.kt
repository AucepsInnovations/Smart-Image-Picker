package com.aucepsinnovations.smart_image_picker.core.contract

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract
import com.aucepsinnovations.smart_image_picker.core.api.PickerConfig
import com.aucepsinnovations.smart_image_picker.core.api.PickerResult
import com.aucepsinnovations.smart_image_picker.ui.picker.PickerActivity

class SmartImagePickerContract : ActivityResultContract<PickerConfig, PickerResult>() {
    override fun createIntent(
        context: Context,
        input: PickerConfig
    ): Intent {
        return Intent(context, PickerActivity::class.java).putExtra("config", input)
    }

    override fun parseResult(
        resultCode: Int,
        intent: Intent?
    ): PickerResult {
        if (resultCode != Activity.RESULT_OK || intent == null) return PickerResult.Canceled
        return intent.getParcelableExtra("result") ?: PickerResult.Canceled
    }
}