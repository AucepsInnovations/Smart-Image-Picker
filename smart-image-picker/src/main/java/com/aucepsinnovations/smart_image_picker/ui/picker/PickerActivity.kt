package com.aucepsinnovations.smart_image_picker.ui.picker

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.aucepsinnovations.smart_image_picker.camera.camerax.CameraActivity
import com.aucepsinnovations.smart_image_picker.core.api.PickerConfig
import com.aucepsinnovations.smart_image_picker.databinding.ActivityPickerBinding
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class PickerActivity : AppCompatActivity(), View.OnClickListener {

    private lateinit var binding: ActivityPickerBinding
    private lateinit var cameraExecutor: ExecutorService
    private var pickerConfig: PickerConfig? = null

    private val activityResultLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        )
        { permissions ->
            // Handle Permission granted/rejected
            var permissionGranted = true
            permissions.entries.forEach {
                if (it.key in REQUIRED_PERMISSIONS && !it.value)
                    permissionGranted = false
            }
            if (!permissionGranted) {
                Toast.makeText(
                    baseContext,
                    "Permission request denied",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPickerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        pickerConfig = intent.getParcelableExtra("config")
        initUI()

        // Request camera permissions
        if (allPermissionsGranted()) {

        } else {
            requestPermissions()
        }

        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    private fun initUI() {
        with(binding) {
            clCamera.setOnClickListener(this@PickerActivity)
            clGallery.setOnClickListener(this@PickerActivity)
            pickerConfig?.let {
                clMain.setBackgroundColor(it.backgroundColor)
                ivCamera.setColorFilter(it.accentColor)
                ivGallery.setColorFilter(it.accentColor)
                tvCamera.setTextColor(it.accentColor)
                tvGallery.setTextColor(it.accentColor)
                view.setBackgroundColor(it.accentColor)
            }
        }
    }

    private fun requestPermissions() {
        activityResultLauncher.launch(REQUIRED_PERMISSIONS)
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            baseContext, it
        ) == PackageManager.PERMISSION_GRANTED
    }

    override fun onClick(view: View?) {
        with(binding) {
            when (view) {
                clCamera -> {
                    val intent = Intent(this@PickerActivity, CameraActivity::class.java)
                    startActivity(intent)
                }

                clGallery -> {}
            }
        }
    }

    companion object {
        private val REQUIRED_PERMISSIONS =
            mutableListOf(
                Manifest.permission.CAMERA
            ).apply {
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                    add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
            }.toTypedArray()
    }
}