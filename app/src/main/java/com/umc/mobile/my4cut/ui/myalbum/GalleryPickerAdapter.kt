package com.umc.mobile.my4cut.ui.myalbum

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.umc.mobile.my4cut.R
import com.umc.mobile.my4cut.databinding.ItemGalleryPhotoBinding

class GalleryPickerAdapter(
    private val maxSelectable: Int,
    private val onSelectionChanged: (selectedCount: Int) -> Unit = {}
) : RecyclerView.Adapter<GalleryPickerAdapter.PhotoViewHolder>() {

    private val photoUrls = mutableListOf<String>()
    private val selectedPositions = LinkedHashSet<Int>() // 순서 유지 → 선택 순번 표시용

    fun submitList(newUrls: List<String>) {
        photoUrls.clear()
        photoUrls.addAll(newUrls)
        selectedPositions.clear()
        notifyDataSetChanged()
        onSelectionChanged(selectedPositions.size)
    }

    fun getSelectedUrls(): List<String> = selectedPositions.map { photoUrls[it] }

    inner class PhotoViewHolder(val binding: ItemGalleryPhotoBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoViewHolder {
        val binding = ItemGalleryPhotoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PhotoViewHolder(binding)
    }

    override fun getItemCount(): Int = photoUrls.size

    override fun onBindViewHolder(holder: PhotoViewHolder, position: Int) {
        val url = photoUrls[position]
        val isSelected = selectedPositions.contains(position)

        holder.binding.ivPhoto.load(url) {
            crossfade(true)
            placeholder(R.color.gray_300)
        }

        val context = holder.binding.root.context
        holder.binding.cvPhoto.strokeColor = ContextCompat.getColor(
            context,
            if (isSelected) R.color.coral_900 else R.color.transparent
        )

        if (isSelected) {
            val order = selectedPositions.indexOf(position) + 1
            holder.binding.tvCheckNumber.text = order.toString()
            holder.binding.tvCheckNumber.visibility = View.VISIBLE
        } else {
            holder.binding.tvCheckNumber.visibility = View.GONE
        }

        holder.itemView.setOnClickListener {
            if (isSelected) {
                selectedPositions.remove(position)
                notifyDataSetChanged() // 순번 재정렬 위해 전체 갱신
            } else {
                if (selectedPositions.size >= maxSelectable) {
                    return@setOnClickListener
                }
                selectedPositions.add(position)
                notifyItemChanged(position)
            }
            onSelectionChanged(selectedPositions.size)
        }
    }
}