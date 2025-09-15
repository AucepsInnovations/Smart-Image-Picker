package com.aucepsinnovations.smart_image_picker.ui.gallery

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import au.com.elegantmedia.chat.util.recycler_view_item_decoration.GridSpaceItemDecoration
import com.aucepsinnovations.smart_image_picker.R
import com.aucepsinnovations.smart_image_picker.core.api.CountMode
import com.aucepsinnovations.smart_image_picker.core.api.PickerConfig
import com.aucepsinnovations.smart_image_picker.core.api.PickerResult
import com.aucepsinnovations.smart_image_picker.core.data.Cropper
import com.aucepsinnovations.smart_image_picker.core.data.SmartImagePicker
import com.aucepsinnovations.smart_image_picker.core.util.Constants
import com.aucepsinnovations.smart_image_picker.core.util.disable
import com.aucepsinnovations.smart_image_picker.core.util.dpToPx
import com.aucepsinnovations.smart_image_picker.core.util.enable
import com.aucepsinnovations.smart_image_picker.core.util.gone
import com.aucepsinnovations.smart_image_picker.core.util.makeInvisible
import com.aucepsinnovations.smart_image_picker.core.util.setupSystemBars
import com.aucepsinnovations.smart_image_picker.core.util.showAlert
import com.aucepsinnovations.smart_image_picker.core.util.visible
import com.aucepsinnovations.smart_image_picker.databinding.ActivityGalleryBinding
import com.aucepsinnovations.smart_image_picker.ui.camera.CameraActivity
import com.yalantis.ucrop.UCrop

class GalleryActivity : AppCompatActivity(), View.OnClickListener,
    GalleryAdapter.OnItemClickListener {

    private lateinit var binding: ActivityGalleryBinding
    private var pickerConfig: PickerConfig? = null
    private lateinit var galleryAdapter: GalleryAdapter
    private val viewModel: GalleryViewModel by viewModels()
    private var canContinue = false
    private var canAddPhoto = true
    private lateinit var menuItem: MenuItem

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
                openCamera()
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
                    viewModel.addImage(croppedUri)
                    buttonValidator()
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
                    viewModel.addImage(uri)
                    buttonValidator()
                }
            } else if (result.resultCode == UCrop.RESULT_ERROR) {
                val cropError = UCrop.getError(result.data!!)
                Toast.makeText(this, "Crop failed: ${cropError?.message}", Toast.LENGTH_SHORT)
                    .show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGalleryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        pickerConfig = intent.getParcelableExtra(Constants.CONFIG)

        pickerConfig?.let { setupSystemBars(window, it.accentColor) }
        initUI()
        initActionBar()
        initRecycler()

        viewModel.images.observe(this) { uris ->
            galleryAdapter.setImageList(uris)
            handleVisibility()
        }
    }

    private fun initUI() {
        with(binding) {
            pickerConfig?.let {
                toolbar.setBackgroundColor(it.accentColor)
                clMain.setBackgroundColor(it.backgroundColor)
                (btnOpenCamera.background as? GradientDrawable)?.setColor(it.buttonColor)
                (btnOpenGallery.background as? GradientDrawable)?.setColor(it.buttonColor)
                btnOpenCamera.setTextColor(it.textColor)
                btnOpenGallery.setTextColor(it.textColor)
            }
            btnOpenCamera.setOnClickListener(this@GalleryActivity)
            btnOpenGallery.setOnClickListener(this@GalleryActivity)

            pickerConfig?.let {
                clMain.setBackgroundColor(it.backgroundColor)

                if (it.allowCamera) {
                    btnOpenCamera.visible()
                } else {
                    btnOpenCamera.gone()
                }

                if (it.allowGallery) {
                    btnOpenGallery.visible()
                } else {
                    btnOpenGallery.gone()
                }

                if (!it.allowCamera && !it.allowGallery) {
                    this@GalleryActivity.showAlert(
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

    private fun initActionBar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            title = "Choose or Capture Image"
            setDisplayHomeAsUpEnabled(true)
        }

        binding.toolbar.apply {
            setNavigationOnClickListener {
                onBackPressedDispatcher.onBackPressed()
            }
            setTitleTextColor(pickerConfig!!.textColor)
            navigationIcon?.setTint(pickerConfig!!.textColor)
        }
    }

    private fun initRecycler() {
        with(binding) {
            galleryAdapter = GalleryAdapter(mutableListOf(), this@GalleryActivity).apply {
                this.onListChanged = { updatedList ->
                    viewModel.setImages(updatedList.toMutableList())
                }
            }

            rvGallery.apply {
                layoutManager = GridLayoutManager(this@GalleryActivity, 2)
                adapter = galleryAdapter
                this.addItemDecoration(
                    GridSpaceItemDecoration(
                        2,
                        16f.dpToPx(this@GalleryActivity),
                        true
                    )
                )
                enableDragAndDrop(this, galleryAdapter)
            }
        }
    }

    private fun buttonValidator() {
        with(binding) {
            pickerConfig?.let { pickerConfig ->
                val countMode = pickerConfig.countMode
                try {
                    canContinue = when (countMode) {
                        is CountMode.EXACT -> galleryAdapter.itemCount == countMode.n
                        CountMode.EXACT_ONE -> galleryAdapter.itemCount == 1
                        is CountMode.MAX -> galleryAdapter.itemCount in 1..countMode.n
                        is CountMode.MIN -> galleryAdapter.itemCount >= countMode.n
                        is CountMode.RANGE -> galleryAdapter.itemCount in countMode.min..countMode.max
                        CountMode.UNLIMITED -> galleryAdapter.itemCount > 0
                    }

                    canAddPhoto = when (countMode) {
                        is CountMode.EXACT -> galleryAdapter.itemCount < countMode.n
                        CountMode.EXACT_ONE -> galleryAdapter.itemCount < 1
                        is CountMode.MAX -> galleryAdapter.itemCount < countMode.n
                        is CountMode.MIN -> true
                        is CountMode.RANGE -> galleryAdapter.itemCount < countMode.max
                        CountMode.UNLIMITED -> true
                    }
                } catch (e: Exception) {
                    false.also {
                        e.printStackTrace()
                    }
                }
            }

            menuItem.isVisible = canContinue
            if (canAddPhoto) {
                btnOpenCamera.enable()
                btnOpenGallery.enable()
            } else {
                btnOpenCamera.disable()
                btnOpenGallery.disable()
            }
        }
    }

    private fun handleVisibility() {
        with(binding) {
            if (galleryAdapter.itemCount == 0) {
                rvGallery.makeInvisible()
                tvEmpty.visible()
            } else {
                rvGallery.visible()
                tvEmpty.makeInvisible()
            }
        }
    }

    private fun openCamera() {
        val intent = Intent(this, CameraActivity::class.java)
        intent.putExtra(Constants.CONFIG, pickerConfig)
        cameraLauncher.launch(intent)
    }

    private fun enableDragAndDrop(recyclerView: RecyclerView, adapter: GalleryAdapter) {
        val itemTouchHelperCallback = object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN or
                    ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT, 0
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                val fromPosition = viewHolder.adapterPosition
                val toPosition = target.adapterPosition
                adapter.swapItems(fromPosition, toPosition)
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                // No swipe to delete here
            }

            override fun isLongPressDragEnabled(): Boolean {
                return true // user can long-press to drag
            }
        }

        val itemTouchHelper = ItemTouchHelper(itemTouchHelperCallback)
        itemTouchHelper.attachToRecyclerView(recyclerView)
    }

    fun openGallery() {
        galleryLauncher.launch("image/*")
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

    private fun returnResult(uris: List<Uri>) {
        val result = PickerResult.Multiple(uris)
        val intent = Intent().apply {
            putExtra(Constants.RESULT, result)
        }
        setResult(RESULT_OK, intent)
        finish()
    }

    override fun onClick(view: View?) {
        when (view?.id) {
            R.id.btn_open_camera -> {
                if (allPermissionsGranted()) {
                    openCamera()
                } else {
                    requestPermissions()
                }
            }

            R.id.btn_open_gallery -> {
                openGallery()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_gallery, menu)
        menuItem = menu.findItem(R.id.action_done)

        val s = SpannableString(menuItem.title)
        s.setSpan(
            ForegroundColorSpan(pickerConfig!!.titleColor),
            0,
            s.length,
            0
        )
        menuItem.title = s

        buttonValidator()
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_done -> {
                val uriList = galleryAdapter.getImageList()
                returnResult(uriList)
            }
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onDeleteButtonClickListener(image: Uri) {
        viewModel.removeImage(image)
        buttonValidator()
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