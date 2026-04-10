package com.umc.mobile.my4cut.ui.photo

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.umc.mobile.my4cut.databinding.ItemPhotoBinding
import com.bumptech.glide.Glide
import com.umc.mobile.my4cut.R

import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class PhotoRVAdapter(
    private val photoList: MutableList<PhotoData>
) : RecyclerView.Adapter<PhotoRVAdapter.PhotoViewHolder>() {

    var onItemClickListener: ((PhotoData) -> Unit)? = null
    var onFinalToggleListener: ((PhotoData) -> Unit)? = null

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
            binding.tvDateTime.text = formatAbsoluteDateTime(photo.dateTime)
            binding.tvCommentCount.text = photo.commentCount.toString()

            binding.ivFinalToggle.setImageResource(
                if (photo.isFinal) R.drawable.ic_final_on
                else R.drawable.ic_final_off
            )

            binding.ivFinalToggle.setOnClickListener {
                val clickedPosition = bindingAdapterPosition
                if (clickedPosition == RecyclerView.NO_POSITION) return@setOnClickListener

                val clickedPhoto = photoList[clickedPosition]
                clickedPhoto.isFinal = !clickedPhoto.isFinal

                notifyItemChanged(clickedPosition)
                onFinalToggleListener?.invoke(clickedPhoto)
            }

            binding.root.setOnClickListener {
                onItemClickListener?.invoke(photo)
            }
        }
    }
    private fun formatAbsoluteDateTime(serverTime: String): String {
        return try {
            parseServerDateTime(serverTime).format(DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm"))
        } catch (_: Exception) {
            serverTime
        }
    }

    private fun parseServerDateTime(serverTime: String): ZonedDateTime {
        val seoulZone = ZoneId.of("Asia/Seoul")

        return try {
            OffsetDateTime.parse(serverTime).atZoneSameInstant(seoulZone)
        } catch (_: Exception) {
            val normalized = serverTime.removeSuffix("Z")

            val localDateTime = try {
                LocalDateTime.parse(normalized, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            } catch (_: Exception) {
                try {
                    LocalDateTime.parse(normalized, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                } catch (_: Exception) {
                    try {
                        LocalDateTime.parse(normalized, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
                    } catch (_: Exception) {
                        try {
                            LocalDateTime.parse(normalized, DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss"))
                        } catch (_: Exception) {
                            LocalDateTime.parse(normalized, DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm"))
                        }
                    }
                }
            }

            localDateTime.atZone(ZoneOffset.UTC).withZoneSameInstant(seoulZone)
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

        val finalPhotos = newPhotos.filter { it.isFinal }
        val normalPhotos = newPhotos.filterNot { it.isFinal }

        photoList.addAll(finalPhotos)
        photoList.addAll(normalPhotos)
        notifyDataSetChanged()
    }
    fun removePhoto(photoId: Long) {
        val index = photoList.indexOfFirst { it.photoId == photoId }
        if (index == -1) return

        photoList.removeAt(index)
        notifyItemRemoved(index)
    }
}