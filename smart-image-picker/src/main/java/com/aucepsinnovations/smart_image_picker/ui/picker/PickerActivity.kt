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
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.aucepsinnovations.smart_image_picker.R
import com.aucepsinnovations.smart_image_picker.core.api.PickerConfig
import com.aucepsinnovations.smart_image_picker.core.api.PickerResult
import com.aucepsinnovations.smart_image_picker.core.data.Cropper
import com.aucepsinnovations.smart_image_picker.core.data.SmartImagePicker
import com.aucepsinnovations.smart_image_picker.core.util.Constants
import com.aucepsinnovations.smart_image_picker.core.util.gone
import com.aucepsinnovations.smart_image_picker.core.util.showAlert
import com.aucepsinnovations.smart_image_picker.core.util.visible
import com.aucepsinnovations.smart_image_picker.databinding.ActivityPickerBinding
import com.aucepsinnovations.smart_image_picker.ui.camera.CameraActivity
import com.yalantis.ucrop.UCrop

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

    private val cameraLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val uriString = result.data?.getStringExtra(Constants.CROPPED_IMAGE_URI)
                uriString?.let {
                    val croppedUri = it.toUri()
                    returnResult(croppedUri)
                }
            }
        }

    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            Cropper.startCrop(this, uri, cropImageLauncher)
        }
    }

    private val cropImageLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val resultUri = result.data?.let { UCrop.getOutput(it) }
                resultUri?.let { uri ->
                    returnResult(uri)
                }
            } else if (result.resultCode == UCrop.RESULT_ERROR) {
                val cropError = UCrop.getError(result.data!!)
                Toast.makeText(this, "Crop failed: ${cropError?.message}", Toast.LENGTH_SHORT)
                    .show()
            }
        }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityPickerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        pickerConfig = intent.getParcelableExtra(Constants.CONFIG)

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

                if (it.allowCamera) {
                    clCamera.visible()
                    view.visible()
                } else {
                    clCamera.gone()
                    view.gone()
                }

                if (it.allowGallery) {
                    clGallery.visible()
                    view.visible()
                } else {
                    clGallery.gone()
                    view.gone()
                }

                if (!it.allowCamera && !it.allowGallery) {
                    this@PickerActivity.showAlert(
                        getString(R.string.title_error),
                        getString(R.string.msg_camera_gallery_enable_error),
                        positiveText = "Okay",
                        onPositive = {
                            finish()
                        })
                }
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
        intent.putExtra(Constants.CONFIG, pickerConfig)
        cameraLauncher.launch(intent)
    }

    fun openGallery() {
        galleryLauncher.launch("image/*")
    }

    private fun returnResult(uri: Uri) {
        val result = PickerResult.Single(uri)
        val intent = Intent().apply {
            putExtra(Constants.RESULT, result)
        }
        setResult(RESULT_OK, intent)
        finish()
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

                clGallery -> {
                    openGallery()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        SmartImagePicker.clearOldCache(this)
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