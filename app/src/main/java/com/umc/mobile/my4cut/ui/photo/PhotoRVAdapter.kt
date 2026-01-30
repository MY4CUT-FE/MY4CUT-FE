package com.umc.mobile.my4cut.ui.photo

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.umc.mobile.my4cut.databinding.ItemPhotoBinding

class PhotoRVAdapter(
    private val photoList: List<PhotoData>
) : RecyclerView.Adapter<PhotoRVAdapter.PhotoViewHolder>() {

    var onItemClickListener: ((PhotoData) -> Unit)? = null

    inner class PhotoViewHolder(
        private val binding: ItemPhotoBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(photo: PhotoData) {
            binding.ivPhoto.setImageResource(photo.photoImageRes)
            binding.ivUserIcon.setImageResource(photo.userImageRes)
            binding.tvUserName.text = photo.userName
            binding.tvDateTime.text = photo.dateTime
            binding.tvCommentCount.text = photo.commentCount.toString()

            binding.root.setOnClickListener {
                onItemClickListener?.invoke(photo)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoViewHolder {
        val binding = ItemPhotoBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PhotoViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PhotoViewHolder, position: Int) {
        holder.bind(photoList[position])
    }

    override fun getItemCount(): Int = photoList.size
}