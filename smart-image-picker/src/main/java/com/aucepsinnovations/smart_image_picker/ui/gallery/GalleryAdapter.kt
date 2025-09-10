package com.aucepsinnovations.smart_image_picker.ui.gallery

import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.aucepsinnovations.smart_image_picker.databinding.ItemGalleryBinding
import java.util.Collections

class GalleryAdapter(
    private val imageList: MutableList<Uri>
) : RecyclerView.Adapter<GalleryAdapter.ViewHolder>() {

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {
        return ViewHolder(
            ItemGalleryBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun getItemCount(): Int = imageList.size

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int
    ) = holder.run {
        onBind(position)
    }

    inner class ViewHolder(private val binding: ItemGalleryBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun onBind(position: Int) {
            val image = imageList[position]
            binding.ivImage.setImageURI(image)
        }
    }

    fun swapItems(fromPosition: Int, toPosition: Int) {
        if (fromPosition < toPosition) {
            for (i in fromPosition until toPosition) {
                Collections.swap(imageList, i, i + 1)
            }
        } else {
            for (i in fromPosition downTo toPosition + 1) {
                Collections.swap(imageList, i, i - 1)
            }
        }
        notifyItemMoved(fromPosition, toPosition)
    }

    fun addImageList(list: List<Uri>) {
        imageList.addAll(list)
        notifyDataSetChanged()
    }

    fun getImageList(): MutableList<Uri> {
        return imageList
    }
}