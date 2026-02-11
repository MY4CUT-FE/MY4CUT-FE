package com.umc.mobile.my4cut.ui.photo

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.umc.mobile.my4cut.databinding.ItemPhotoBinding
import com.bumptech.glide.Glide

class PhotoRVAdapter(
    private val photoList: MutableList<PhotoData>
) : RecyclerView.Adapter<PhotoRVAdapter.PhotoViewHolder>() {

    var onItemClickListener: ((PhotoData) -> Unit)? = null

    inner class PhotoViewHolder(
        private val binding: ItemPhotoBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(photo: PhotoData) {
            Glide.with(binding.ivPhoto.context)
                .load(photo.photoUrl)
                .placeholder(com.umc.mobile.my4cut.R.drawable.image1)
                .error(com.umc.mobile.my4cut.R.drawable.image1)
                .into(binding.ivPhoto)

            Glide.with(binding.ivUserIcon.context)
                .load(photo.userProfileUrl)
                .circleCrop()
                .placeholder(com.umc.mobile.my4cut.R.drawable.ic_profile_cat)
                .error(com.umc.mobile.my4cut.R.drawable.ic_profile_cat)
                .into(binding.ivUserIcon)

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

    fun updatePhotos(newPhotos: List<PhotoData>) {
        photoList.clear()
        photoList.addAll(newPhotos)
        notifyDataSetChanged()
    }
}