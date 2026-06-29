package com.umc.mobile.my4cut.ui.pose

import android.graphics.Color
import android.graphics.PorterDuff
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import android.view.View
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.umc.mobile.my4cut.R
import com.umc.mobile.my4cut.databinding.ItemPoseBinding

class PoseAdapter(
    private var items: List<PoseData>,
    private val onBookmarkClick: (PoseData, Int) -> Unit,
    private val onItemClick: (PoseData, Int) -> Unit = { _, _ -> }
) : RecyclerView.Adapter<PoseAdapter.ViewHolder>() {

    inner class ViewHolder(private val binding: ItemPoseBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: PoseData, position: Int) {
            // 텍스트 skeleton 초기화
            binding.tvPoseName.visibility = View.INVISIBLE
            binding.viewPoseNameSkeleton.visibility = View.VISIBLE

            // ✅ Glide로 서버 이미지 로드
            Glide.with(binding.ivPoseImage.context)
                .load(item.imageUrl)
                .placeholder(R.drawable.img_pose_loading)
                .error(R.drawable.img_pose_loading)
                .listener(object : RequestListener<android.graphics.drawable.Drawable> {
                    override fun onLoadFailed(
                        e: GlideException?,
                        model: Any?,
                        target: Target<android.graphics.drawable.Drawable>,
                        isFirstResource: Boolean
                    ): Boolean {
                        binding.tvPoseName.text = item.title
                        binding.tvPoseName.visibility = View.VISIBLE
                        binding.viewPoseNameSkeleton.visibility = View.GONE
                        return false
                    }

                    override fun onResourceReady(
                        resource: android.graphics.drawable.Drawable,
                        model: Any,
                        target: Target<android.graphics.drawable.Drawable>?,
                        dataSource: DataSource,
                        isFirstResource: Boolean
                    ): Boolean {
                        binding.tvPoseName.text = item.title
                        binding.tvPoseName.visibility = View.VISIBLE
                        binding.viewPoseNameSkeleton.visibility = View.GONE
                        return false
                    }
                })
                .into(binding.ivPoseImage)

            // 즐겨찾기 상태
            updateBookmarkIcon(item.isFavorite)

            // ✅ 별 클릭 시 콜백 호출
            binding.ivStar.setOnClickListener {
                onBookmarkClick(item, position)
            }

            // ✅ 이미지 클릭 시 상세 모달 표시
            binding.ivPoseImage.setOnClickListener {
                onItemClick(item, position)
            }
        }

        fun updateBookmarkIcon(isFavorite: Boolean) {
            if (isFavorite) {
                binding.ivStar.setImageResource(R.drawable.ic_star_on)
                binding.ivStar.setColorFilter(Color.parseColor("#FFD83C"), PorterDuff.Mode.SRC_IN)
            } else {
                binding.ivStar.setImageResource(R.drawable.ic_star_off)
                binding.ivStar.clearColorFilter()
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemPoseBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position], position)
    }

    override fun getItemCount(): Int = items.size

    // 데이터 갱신
    fun updateData(newItems: List<PoseData>) {
        items = newItems
        notifyDataSetChanged()
    }

    // 특정 아이템만 갱신
    fun updateItem(position: Int, isFavorite: Boolean) {
        if (position in items.indices) {
            items[position].isFavorite = isFavorite
            notifyItemChanged(position)
        }
    }
}
