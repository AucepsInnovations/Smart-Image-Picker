package com.aucepsinnovations.smart_image_picker.ui.camera

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.aucepsinnovations.smart_image_picker.R
import com.aucepsinnovations.smart_image_picker.core.api.PickerConfig
import com.aucepsinnovations.smart_image_picker.core.data.CameraPreferences
import com.aucepsinnovations.smart_image_picker.core.util.Constants
import com.aucepsinnovations.smart_image_picker.databinding.ActivityCameraBinding
import com.yalantis.ucrop.UCrop
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraActivity : AppCompatActivity(), View.OnClickListener {

    private lateinit var binding: ActivityCameraBinding
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var preview: Preview
    private var imageCapture: ImageCapture? = null
    private var pickerConfig: PickerConfig? = null
    private var lensFacing: Int = CameraSelector.LENS_FACING_BACK
    private var cameraProvider: ProcessCameraProvider? = null

    private val TIMEOUT_IN_MS = 2 * 60 * 1000L // 2 minutes
    private val WARNING_TIME_MS = 10 * 1000L // warn 10 seconds before closing
    private val handler = Handler(Looper.getMainLooper())
    private var flashMode = ImageCapture.FLASH_MODE_OFF

    private val timeoutRunnable = Runnable {
        finish()
    }

    private val warningRunnable = Runnable {
        Toast.makeText(this, getString(R.string.info_timeout_camera), Toast.LENGTH_LONG).show()
    }

    private val cropImageLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val data = result.data
                val resultUri = data?.let { UCrop.getOutput(it) }
                resultUri?.let { uri ->
                    onImageCaptured(uri)
                    Toast.makeText(this, "Photo cropped & ready", Toast.LENGTH_SHORT).show()
                }
            } else if (result.resultCode == UCrop.RESULT_ERROR) {
                val cropError = UCrop.getError(result.data!!)
                Toast.makeText(this, "Crop failed: ${cropError?.message}", Toast.LENGTH_SHORT)
                    .show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCameraBinding.inflate(layoutInflater)
        setContentView(binding.root)

        pickerConfig = intent.getParcelableExtra(Constants.CONFIG)

        initUI()
        initCameraProvider()

        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    private fun initUI() = with(binding) {
        flashMode = CameraPreferences.getFlashMode(this@CameraActivity)
        updateFlashIcon()

        ibtnFlashMode.setOnClickListener(this@CameraActivity)
        ibtnGallery.setOnClickListener(this@CameraActivity)
        ibtnCapture.setOnClickListener(this@CameraActivity)
        ibtnSwitchCamera.setOnClickListener(this@CameraActivity)
    }

    private fun initCameraProvider() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()
            bindCameraUseCases() // initial bind
        }, ContextCompat.getMainExecutor(this))
    }

    private fun bindCameraUseCases() {
        val provider = cameraProvider ?: return

        preview = Preview.Builder()
            .build()
            .also { it.surfaceProvider = binding.viewFinder.surfaceProvider }

        flashMode = CameraPreferences.getFlashMode(this) // restore saved mode

        imageCapture = ImageCapture.Builder()
            .setFlashMode(flashMode)
            .setCaptureMode(CAPTURE_MODE_MINIMIZE_LATENCY)
            .build()

        val cameraSelector = CameraSelector.Builder()
            .requireLensFacing(lensFacing)
            .build()

        try {
            provider.unbindAll()
            provider.bindToLifecycle(this, cameraSelector, preview, imageCapture)
        } catch (exc: Exception) {
            Log.e(TAG, "Use case binding failed", exc)
        }
    }

    private fun takePhoto() {
        val photoFile = File(
            externalCacheDir,  // <— temporary cache
            "${System.currentTimeMillis()}.jpg"
        )

        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture?.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    val savedUri = Uri.fromFile(photoFile)

                    // Destination file in cache for cropped image
                    val destinationFile = File(
                        externalCacheDir,
                        "CROP_${System.currentTimeMillis()}.jpg"
                    )
                    val destinationUri = Uri.fromFile(destinationFile)

                    val cropIntent = UCrop.of(savedUri, destinationUri)
                        .withAspectRatio(1f, 1f) // optional, or make it configurable
                        .withMaxResultSize(1080, 1080)
                        .getIntent(this@CameraActivity)

                    cropImageLauncher.launch(cropIntent)
                }

                override fun onError(exception: ImageCaptureException) {
                    Toast.makeText(
                        this@CameraActivity,
                        "Capture failed: ${exception.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        )
    }

    private fun switchCamera() {
        with(binding) {
            val newLensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
                CameraSelector.LENS_FACING_FRONT
            } else {
                CameraSelector.LENS_FACING_BACK
            }

            val cameraProviderFuture = ProcessCameraProvider.getInstance(this@CameraActivity)
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()

                try {
                    // Try binding the new camera
                    val cameraSelector = CameraSelector.Builder()
                        .requireLensFacing(newLensFacing)
                        .build()

                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        this@CameraActivity,
                        cameraSelector,
                        preview,
                        imageCapture
                    )

                    lensFacing = newLensFacing
                    if (lensFacing == CameraSelector.LENS_FACING_FRONT) {
                        ibtnSwitchCamera.setImageResource(R.drawable.ic_mobile_camera_rear_48px)
                    } else {
                        ibtnSwitchCamera.setImageResource(R.drawable.ic_mobile_camera_front_48px)
                    }
                } catch (exc: Exception) {
                    Toast.makeText(
                        this@CameraActivity,
                        "Unable to switch camera",
                        Toast.LENGTH_SHORT
                    ).show().also {
                        exc.printStackTrace()
                    }
                }
            }, ContextCompat.getMainExecutor(this@CameraActivity))
        }
    }

    private fun toggleFlashMode() {
        flashMode = when (flashMode) {
            ImageCapture.FLASH_MODE_OFF -> ImageCapture.FLASH_MODE_ON
            ImageCapture.FLASH_MODE_ON -> ImageCapture.FLASH_MODE_AUTO
            else -> ImageCapture.FLASH_MODE_OFF
        }

        imageCapture?.flashMode = flashMode
        CameraPreferences.saveFlashMode(this, flashMode)
        updateFlashIcon()
    }


    private fun updateFlashIcon() {
        val iconRes = when (flashMode) {
            ImageCapture.FLASH_MODE_ON -> R.drawable.ic_flash_on_24px
            ImageCapture.FLASH_MODE_AUTO -> R.drawable.ic_flash_auto_24px
            else -> R.drawable.ic_flash_off_24px
        }
        binding.ibtnFlashMode.setImageResource(iconRes)
    }

    private fun resetTimeout() {
        handler.removeCallbacks(timeoutRunnable)
        handler.postDelayed(warningRunnable, TIMEOUT_IN_MS - WARNING_TIME_MS)
        handler.postDelayed(timeoutRunnable, TIMEOUT_IN_MS)
    }

    private fun onImageCaptured(croppedUri: Uri) {
        val resultIntent = Intent().apply {
            putExtra(Constants.CROPPED_IMAGE_URI, croppedUri.toString())
        }
        setResult(RESULT_OK, resultIntent).also {
            finish()
        }
    }

    override fun onClick(view: View) {
        when (view.id) {
            R.id.ibtn_flash_mode -> {
                toggleFlashMode()
            }

            R.id.ibtn_gallery -> {
                onBackPressedDispatcher.onBackPressed()
            }

            R.id.ibtn_capture -> takePhoto()
            R.id.ibtn_switch_camera -> switchCamera()
        }
    }

    override fun onUserInteraction() {
        super.onUserInteraction()
        resetTimeout()
    }

    override fun onResume() {
        super.onResume()
        resetTimeout()
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(timeoutRunnable)
        handler.removeCallbacks(warningRunnable)
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    companion object {
        private const val TAG = "CameraXApp"
    }
}