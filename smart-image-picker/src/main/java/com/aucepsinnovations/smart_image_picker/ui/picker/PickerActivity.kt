package com.aucepsinnovations.smart_image_picker.ui.picker

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.aucepsinnovations.smart_image_picker.R
import com.aucepsinnovations.smart_image_picker.core.api.PickerConfig
import com.aucepsinnovations.smart_image_picker.databinding.ActivityPickerBinding
import com.aucepsinnovations.smart_image_picker.ui.camera.CameraActivity

class PickerActivity : AppCompatActivity(), View.OnClickListener {

    private lateinit var binding: ActivityPickerBinding
    private var pickerConfig: PickerConfig? = null

    private val activityResultLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            var permissionGranted = true
            permissions.entries.forEach {
                if (it.key in REQUIRED_PERMISSIONS && !it.value) {
                    permissionGranted = false
                }
            }

            if (permissionGranted) {
                startCamera()
            } else {
                // Check if permanently denied
                val permanentlyDenied = REQUIRED_PERMISSIONS.any { perm ->
                    !ActivityCompat.shouldShowRequestPermissionRationale(this, perm)
                }

                if (permanentlyDenied) {
                    // User checked "Don't ask again"
                    Toast.makeText(
                        this,
                        getString(R.string.info_permission),
                        Toast.LENGTH_LONG
                    ).show()

                    openAppSettings()
                } else {
                    Toast.makeText(
                        this,
                        getString(R.string.info_permission_denied),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPickerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        pickerConfig = intent.getParcelableExtra("config")

        initUI()
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

    private fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", packageName, null)
        }
        startActivity(intent)
    }

    private fun startCamera() {
        val intent = Intent(this@PickerActivity, CameraActivity::class.java)
        intent.putExtra("config", pickerConfig)
        startActivity(intent)
    }

    override fun onClick(view: View?) {
        with(binding) {
            when (view) {
                clCamera -> {
                    // Request camera permissions
                    if (allPermissionsGranted()) {
                        startCamera()
                    } else {
                        requestPermissions()
                    }
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