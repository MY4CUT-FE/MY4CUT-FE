package com.umc.mobile.my4cut.ui.photo

import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.graphics.drawable.Drawable
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.umc.mobile.my4cut.databinding.ItemPhotoBinding
import com.bumptech.glide.Glide
import com.umc.mobile.my4cut.R
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target

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
    private var isLoading = false

    companion object {
        private const val SKELETON_COUNT = 6
    }

    inner class PhotoViewHolder(
        private val binding: ItemPhotoBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bindSkeleton() {
            // 재활용된 실제 이미지가 잠깐 보이지 않도록 Glide 요청 제거
            Glide.with(binding.ivPhoto).clear(binding.ivPhoto)
            Glide.with(binding.ivUserIcon).clear(binding.ivUserIcon)

            binding.root.isClickable = false
            binding.root.setOnClickListener(null)

            binding.ivFinalToggle.isClickable = false
            binding.ivFinalToggle.setOnClickListener(null)

            // 사진 스켈레톤
            binding.ivPhoto.setImageResource(R.drawable.ic_skeleton_img)
            binding.ivPhoto.scaleType = ImageView.ScaleType.CENTER
            binding.ivPhoto.setBackgroundResource(R.drawable.bg_skeleton_img)

            // 최종본 버튼 스켈레톤
            binding.ivFinalToggle.setImageResource(
                R.drawable.ic_final_skeleton
            )

            // 프로필은 기존 기본 프로필 유지
            binding.ivUserIcon.setImageResource(
                R.drawable.ic_profile_cat
            )

            // 닉네임
            binding.tvUserName.text = ""
            binding.tvUserName.layoutParams =
                binding.tvUserName.layoutParams.apply {
                    width = dpToPx(60)
                    height = dpToPx(12)
                }
            binding.tvUserName.setBackgroundResource(
                R.drawable.bg_skeleton_text_light
            )

            // 날짜
            binding.tvDateTime.text = ""
            binding.tvDateTime.layoutParams =
                binding.tvDateTime.layoutParams.apply {
                    width = dpToPx(60)
                    height = dpToPx(12)
                }
            binding.tvDateTime.setBackgroundResource(
                R.drawable.bg_skeleton_text_light
            )

            // 댓글 수
            binding.tvCommentCount.text = ""
            binding.tvCommentCount.layoutParams =
                binding.tvCommentCount.layoutParams.apply {
                    width = dpToPx(35)
                    height = dpToPx(12)
                }
            binding.tvCommentCount.setBackgroundResource(
                R.drawable.bg_skeleton_text_light
            )

            binding.ivComment.visibility = View.INVISIBLE
        }

        fun bind(photo: PhotoData) {
            binding.root.isClickable = true
            binding.ivFinalToggle.isClickable = true

            binding.ivPhoto.background = null
            binding.ivPhoto.scaleType = ImageView.ScaleType.CENTER_CROP

            binding.tvUserName.layoutParams =
                binding.tvUserName.layoutParams.apply {
                    width = ViewGroup.LayoutParams.WRAP_CONTENT
                    height = ViewGroup.LayoutParams.WRAP_CONTENT
                }

            binding.tvDateTime.layoutParams =
                binding.tvDateTime.layoutParams.apply {
                    width = ViewGroup.LayoutParams.WRAP_CONTENT
                    height = ViewGroup.LayoutParams.WRAP_CONTENT
                }

            binding.tvCommentCount.layoutParams =
                binding.tvCommentCount.layoutParams.apply {
                    width = ViewGroup.LayoutParams.WRAP_CONTENT
                    height = ViewGroup.LayoutParams.WRAP_CONTENT
                }

            binding.tvUserName.background = null
            binding.tvDateTime.background = null
            binding.tvCommentCount.background = null

            binding.ivComment.visibility = View.VISIBLE

            binding.ivPhoto.setBackgroundResource(
                R.drawable.bg_skeleton_img
            )
            binding.ivPhoto.setImageResource(
                R.drawable.ic_skeleton_img
            )
            binding.ivPhoto.scaleType = ImageView.ScaleType.CENTER

            Glide.with(binding.ivPhoto.context)
                .load(photo.photoUrl)
                .placeholder(R.drawable.ic_skeleton_img)
                .error(R.drawable.ic_skeleton_img)
                .listener(object : RequestListener<Drawable> {
                    override fun onLoadFailed(
                        e: GlideException?,
                        model: Any?,
                        target: Target<Drawable>,
                        isFirstResource: Boolean
                    ): Boolean {
                        binding.ivPhoto.scaleType = ImageView.ScaleType.CENTER
                        binding.ivPhoto.setBackgroundResource(
                            R.drawable.bg_skeleton_img
                        )
                        binding.ivPhoto.setImageResource(
                            R.drawable.ic_skeleton_img
                        )
                        return true
                    }

                    override fun onResourceReady(
                        resource: Drawable,
                        model: Any,
                        target: Target<Drawable>?,
                        dataSource: DataSource,
                        isFirstResource: Boolean
                    ): Boolean {
                        binding.ivPhoto.background = null
                        binding.ivPhoto.scaleType =
                            ImageView.ScaleType.CENTER_CROP
                        return false
                    }
                })
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

        private fun dpToPx(dp: Int): Int {
            return TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                dp.toFloat(),
                binding.root.resources.displayMetrics
            ).toInt()
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
        if (isLoading) {
            holder.bindSkeleton()
        } else {
            holder.bind(photoList[position])
        }
    }

    override fun getItemCount(): Int =
        if (isLoading) SKELETON_COUNT
        else photoList.size

    fun updatePhotos(newPhotos: List<PhotoData>) {
        isLoading = false

        photoList.clear()
        photoList.addAll(newPhotos)
        notifyDataSetChanged()
    }

    fun removePhoto(photoId: Long) {
        val index = photoList.indexOfFirst { it.photoId == photoId }
        if (index == -1) return

        photoList.removeAt(index)
        notifyItemRemoved(index)
    }

    fun showSkeleton() {
        isLoading = true
        notifyDataSetChanged()
    }

    fun hideSkeleton() {
        isLoading = false
        notifyDataSetChanged()
    }
}