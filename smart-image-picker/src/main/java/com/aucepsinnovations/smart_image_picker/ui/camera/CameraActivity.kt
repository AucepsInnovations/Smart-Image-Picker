package com.aucepsinnovations.smart_image_picker.ui.camera

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.aucepsinnovations.smart_image_picker.R
import com.aucepsinnovations.smart_image_picker.core.api.PickerConfig
import com.aucepsinnovations.smart_image_picker.core.util.Constants
import com.aucepsinnovations.smart_image_picker.databinding.ActivityCameraBinding
import com.aucepsinnovations.smart_image_picker.ui.gallery.GalleryActivity
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraActivity : AppCompatActivity(), View.OnClickListener {

    private lateinit var binding: ActivityCameraBinding
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var preview: Preview
    private var imageCapture: ImageCapture? = null
    private val capturedImages = mutableListOf<Uri>()
    private var pickerConfig: PickerConfig? = null
    private var lensFacing: Int = CameraSelector.LENS_FACING_BACK
    private var cameraProvider: ProcessCameraProvider? = null

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
            .also {
                it.surfaceProvider = binding.viewFinder.surfaceProvider
            }

        imageCapture = ImageCapture.Builder().build()

        val cameraSelector = CameraSelector.Builder()
            .requireLensFacing(lensFacing)
            .build()

        try {
            provider.unbindAll()
            provider.bindToLifecycle(
                this, cameraSelector, preview, imageCapture
            )
        } catch (exc: Exception) {
            Log.e(TAG, "Use case binding failed", exc)
        }
    }

    private fun takePhoto() {
        val imageCapture = imageCapture ?: return

        val photoFile = File(externalCacheDir, "${System.currentTimeMillis()}.jpg")
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val uri = Uri.fromFile(photoFile)
                    capturedImages.add(uri)
                    Toast.makeText(baseContext, "Photo added", Toast.LENGTH_SHORT).show()
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

    override fun onClick(view: View) {
        when (view.id) {
            R.id.ibtn_gallery -> {
                val intent = Intent(this, GalleryActivity::class.java).apply {
                    putParcelableArrayListExtra("IMAGES", ArrayList(capturedImages))
                    putExtra(Constants.CONFIG, pickerConfig)
                }
                startActivity(intent)
            }

            R.id.ibtn_capture -> takePhoto()
            R.id.ibtn_switch_camera -> switchCamera()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    companion object {
        private const val TAG = "CameraXApp"
    }
}