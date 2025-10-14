package com.aucepsinnovations.smart_image_picker.ui.picker

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
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
import com.yalantis.ucrop.UCrop
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PickerActivity : AppCompatActivity(), View.OnClickListener {

    private lateinit var binding: ActivityPickerBinding
    private var pickerConfig: PickerConfig? = null
    private var currentPhotoUri: Uri? = null

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
                currentPhotoUri?.let { uri ->
                    Cropper.startCrop(this, uri, cropImageLauncher)
                }
            } else {
                Toast.makeText(
                    this,
                    getString(R.string.msg_camera_gallery_enable_error),
                    Toast.LENGTH_SHORT
                ).show()
                finish()
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
        val photoFile = createImageFile()
        val photoUri = FileProvider.getUriForFile(
            this,
            "${applicationContext.packageName}.fileprovider",
            photoFile
        )
        currentPhotoUri = photoUri

        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
            putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
        }

        if (cameraIntent.resolveActivity(packageManager) != null) {
            cameraLauncher.launch(cameraIntent)
        } else {
            Toast.makeText(this, "No camera app found", Toast.LENGTH_SHORT).show()
        }
    }

    @Throws(IOException::class)
    private fun createImageFile(): File {
        val timeStamp: String =
            SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir: File = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            ?: filesDir
        return File.createTempFile(
            "JPEG_${timeStamp}_",
            ".jpg",
            storageDir
        )
    }

    private fun deleteTempFile(uri: Uri) {
        try {
            val file = File(uri.path ?: return)
            if (file.exists()) file.delete()
        } catch (_: Exception) {
        }
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
        currentPhotoUri?.let { deleteTempFile(it) }
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
            mutableListOf<String>().apply {
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                    add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
            }.toTypedArray()
    }
}