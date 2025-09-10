package com.aucepsinnovations.smart_image_picker.ui.gallery

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel

class GalleryViewModel(
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    companion object {
        private const val KEY_IMAGES = "gallery_images"
    }

    // Backed by SavedStateHandle so it survives config changes
    private val _images = MutableLiveData<List<Uri>>(savedStateHandle[KEY_IMAGES] ?: emptyList())
    val images: LiveData<List<Uri>> get() = _images

    fun setImages(list: List<Uri>) {
        _images.value = list
        savedStateHandle[KEY_IMAGES] = list
    }

    fun addImage(uri: Uri) {
        val updated = _images.value.orEmpty().toMutableList().apply { add(uri) }
        _images.value = updated
        savedStateHandle[KEY_IMAGES] = updated
    }

    fun removeImage(uri: Uri) {
        val updated = _images.value.orEmpty().toMutableList().apply { remove(uri) }
        _images.value = updated
        savedStateHandle[KEY_IMAGES] = updated
    }
}