package com.aucepsinnovations.smart_image_picker.ui.gallery

import android.os.Bundle
import android.text.Html
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import au.com.elegantmedia.chat.util.recycler_view_item_decoration.GridSpaceItemDecoration
import com.aucepsinnovations.smart_image_picker.R
import com.aucepsinnovations.smart_image_picker.core.api.PickerConfig
import com.aucepsinnovations.smart_image_picker.core.util.Constants
import com.aucepsinnovations.smart_image_picker.core.util.SharedData
import com.aucepsinnovations.smart_image_picker.core.util.dpToPx
import com.aucepsinnovations.smart_image_picker.core.util.makeInvisible
import com.aucepsinnovations.smart_image_picker.core.util.visible
import com.aucepsinnovations.smart_image_picker.databinding.ActivityGalleryBinding

class GalleryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGalleryBinding
    private var pickerConfig: PickerConfig? = null
    private lateinit var galleryAdapter: GalleryAdapter

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

        galleryAdapter.addImageList(SharedData.images)

        handleVisibility()
    }

    private fun initUI() {
        pickerConfig?.let {
            binding.clMain.setBackgroundColor(it.backgroundColor)
        }
    }

    private fun initActionBar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            title = Html.fromHtml("<font color='#ffffff'>Gallery</font>");
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            setDisplayShowTitleEnabled(true)
            setHomeAsUpIndicator(R.drawable.ic_outline_arrow_back_24)
        }

        binding.toolbar.apply {
            setNavigationOnClickListener {
                onBackPressedDispatcher.onBackPressed()
            }
        }
    }

    private fun initRecycler() {
        with(binding) {
            galleryAdapter = GalleryAdapter(mutableListOf())
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

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_gallery, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_open_camera -> {
                true
            }

            R.id.action_open_gallery -> {
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onPause() {
        super.onPause()
        SharedData.images = galleryAdapter.getImageList()
    }
}