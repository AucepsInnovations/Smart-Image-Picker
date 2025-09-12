package com.aucepsinnovations.smart_image_picker.ui.gallery

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Html
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import au.com.elegantmedia.chat.util.recycler_view_item_decoration.GridSpaceItemDecoration
import com.aucepsinnovations.smart_image_picker.R
import com.aucepsinnovations.smart_image_picker.core.api.PickerConfig
import com.aucepsinnovations.smart_image_picker.core.data.Cropper
import com.aucepsinnovations.smart_image_picker.core.data.SmartImagePicker
import com.aucepsinnovations.smart_image_picker.core.util.Constants
import com.aucepsinnovations.smart_image_picker.core.util.dpToPx
import com.aucepsinnovations.smart_image_picker.core.util.makeInvisible
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

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.cl_main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        pickerConfig = intent.getParcelableExtra(Constants.CONFIG)

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
            btnOpenCamera.setOnClickListener(this@GalleryActivity)
            btnOpenGallery.setOnClickListener(this@GalleryActivity)

            pickerConfig?.let {
                clMain.setBackgroundColor(it.backgroundColor)
            }
        }
    }

    private fun initActionBar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            title = Html.fromHtml("<font color='#ffffff'>Choose or Capture Image</font>");
        }

        binding.toolbar.apply {
            setNavigationOnClickListener {
                onBackPressedDispatcher.onBackPressed()
            }
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

    private val cameraLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val uriString = result.data?.getStringExtra(Constants.CROPPED_IMAGE_URI)
                uriString?.let {
                    val croppedUri = it.toUri()
                    viewModel.addImage(croppedUri)
                }
            }
        }

    private fun openCamera() {
        val intent = Intent(this, CameraActivity::class.java)
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

    override fun onClick(view: View?) {
        when (view?.id) {
            R.id.btn_open_camera -> {
                openCamera()
            }

            R.id.btn_open_gallery -> {
                openGallery()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_gallery, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_done -> {

            }
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onDeleteButtonClickListener(image: Uri) {
        viewModel.removeImage(image)
    }

    override fun onDestroy() {
        super.onDestroy()
        SmartImagePicker.clearOldCache(this)
    }
}