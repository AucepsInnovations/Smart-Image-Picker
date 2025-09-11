package com.aucepsinnovations.smart_image_picker.ui.camera

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.aucepsinnovations.smart_image_picker.R
import com.aucepsinnovations.smart_image_picker.core.api.PickerConfig
import com.aucepsinnovations.smart_image_picker.core.data.CameraPreferences
import com.aucepsinnovations.smart_image_picker.core.data.Cropper
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
    private var imageAnalysis: ImageAnalysis? = null
    private var boundCamera: Camera? = null

    private val TIMEOUT_IN_MS = 2 * 60 * 1000L // 2 minutes
    private val WARNING_TIME_MS = 10 * 1000L // warn 10 seconds before closing
    private val handler = Handler(Looper.getMainLooper())
    private var flashMode = ImageCapture.FLASH_MODE_OFF
    private var latestLuminance: Double = 255.0

    private val timeoutRunnable = Runnable { finish() }
    private val warningRunnable = Runnable {
        Toast.makeText(this, getString(R.string.info_timeout_camera), Toast.LENGTH_LONG).show()
    }

    private val cropImageLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val resultUri = result.data?.let { UCrop.getOutput(it) }
                resultUri?.let { uri ->
                    onImageCaptured(uri)
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
        cameraExecutor = Executors.newSingleThreadExecutor()

        initUI()
        setupImageAnalysis()
        initCameraProvider()
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
            bindCameraUseCases()
        }, ContextCompat.getMainExecutor(this))
    }

    private fun setupImageAnalysis() {
        imageAnalysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .also { analysis ->
                analysis.setAnalyzer(cameraExecutor) { image ->
                    try {
                        val buffer = image.planes[0].buffer
                        val data = ByteArray(buffer.remaining())
                        buffer.get(data)
                        latestLuminance = data.map { it.toInt() and 0xFF }.average()
                    } catch (_: Exception) {
                    } finally {
                        image.close()
                    }
                }
            }
    }

    private fun bindCameraUseCases() {
        val provider = cameraProvider ?: return

        preview = Preview.Builder()
            .build()
            .also { it.surfaceProvider = binding.viewFinder.surfaceProvider }

        lensFacing = CameraPreferences.getCameraMode(this)

        updateCameraIcon()

        flashMode = CameraPreferences.getFlashMode(this)

        imageCapture = ImageCapture.Builder()
            .setFlashMode(flashMode)
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build()

        if (imageAnalysis == null) {
            setupImageAnalysis()
        }

        val cameraSelector = CameraSelector.Builder()
            .requireLensFacing(lensFacing)
            .build()

        try {
            provider.unbindAll()
            boundCamera = provider.bindToLifecycle(
                this,
                cameraSelector,
                preview,
                imageCapture,
                imageAnalysis
            )

            updateFlashIcon()
        } catch (exc: Exception) {
            Log.e(TAG, "Use case binding failed", exc)
        }
    }

    private fun takePhoto() {
        val currentCameraInfo = boundCamera?.cameraInfo
        val hasFlashUnit = currentCameraInfo?.hasFlashUnit() ?: false

        if (lensFacing == CameraSelector.LENS_FACING_FRONT && !hasFlashUnit) {
            if (flashMode == ImageCapture.FLASH_MODE_ON ||
                (flashMode == ImageCapture.FLASH_MODE_AUTO && isDarkEnvironment())
            ) {
                showScreenFlash()
            }
        }

        val photoFile = File(externalCacheDir, "${System.currentTimeMillis()}.jpg")
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture?.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    val savedUri = Uri.fromFile(photoFile)

                    Cropper.startCrop(this@CameraActivity, savedUri, cropImageLauncher)
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
        lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
            CameraSelector.LENS_FACING_FRONT
        } else {
            CameraSelector.LENS_FACING_BACK
        }

        updateCameraIcon()

        if (lensFacing == CameraSelector.LENS_FACING_FRONT) {
            CameraPreferences.saveCameraMode(this, CameraSelector.LENS_FACING_FRONT)
        } else {
            CameraPreferences.saveCameraMode(this, CameraSelector.LENS_FACING_BACK)
        }

        bindCameraUseCases()
    }

    private fun updateCameraIcon() {
        if (lensFacing == CameraSelector.LENS_FACING_FRONT) {
            binding.ibtnSwitchCamera.setImageResource(R.drawable.ic_mobile_camera_rear_48px)
        } else {
            binding.ibtnSwitchCamera.setImageResource(R.drawable.ic_mobile_camera_front_48px)
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

    private fun showScreenFlash() {
        val flashView = View(this).apply {
            setBackgroundColor(Color.WHITE)
            alpha = 0.2f
        }

        addContentView(
            flashView,
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )

        flashView.animate()
            .alpha(1f)
            .setDuration(500)
            .withEndAction {
                flashView.animate()
                    .alpha(0f)
                    .setDuration(100)
                    .withEndAction {
                        (flashView.parent as? ViewGroup)?.removeView(flashView)
                    }
            }
    }

    private fun isDarkEnvironment(): Boolean {
        return latestLuminance < 60
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
        handler.removeCallbacks(warningRunnable)
        handler.postDelayed(warningRunnable, TIMEOUT_IN_MS - WARNING_TIME_MS)
        handler.postDelayed(timeoutRunnable, TIMEOUT_IN_MS)
    }

    private fun onImageCaptured(croppedUri: Uri) {
        val resultIntent = Intent().apply {
            putExtra(Constants.CROPPED_IMAGE_URI, croppedUri.toString())
        }
        setResult(RESULT_OK, resultIntent)
        finish()
    }

    override fun onClick(view: View) {
        when (view.id) {
            R.id.ibtn_flash_mode -> toggleFlashMode()
            R.id.ibtn_gallery -> onBackPressedDispatcher.onBackPressed()
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
        Cropper.clearSmartImagePickerCache(this)
    }

    companion object {
        private const val TAG = "CameraXApp"
    }
}