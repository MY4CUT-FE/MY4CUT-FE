package com.umc.mobile.my4cut.ui.pose

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.umc.mobile.my4cut.R
import com.umc.mobile.my4cut.databinding.ItemPoseBinding

class PoseAdapter(
    private var items: List<PoseData>,
    private val onBookmarkClick: (PoseData, Int) -> Unit // ✅ 즐겨찾기 클릭 콜백
) : RecyclerView.Adapter<PoseAdapter.ViewHolder>() {

    inner class ViewHolder(private val binding: ItemPoseBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: PoseData, position: Int) {
            binding.tvPoseName.text = item.title

            // ✅ Glide로 서버 이미지 로드
            Glide.with(binding.ivPoseImage.context)
                .load(item.imageUrl)
                .placeholder(R.drawable.img_profile_default)
                .error(R.drawable.img_profile_default)
                .into(binding.ivPoseImage)

            // 즐겨찾기 상태
            updateBookmarkIcon(item.isFavorite)

            // ✅ 별 클릭 시 콜백 호출
            binding.ivStar.setOnClickListener {
                onBookmarkClick(item, position)
            }
        }

        private fun updateBookmarkIcon(isFavorite: Boolean) {
            if (isFavorite) {
                binding.ivStar.setImageResource(R.drawable.ic_star_on)
            } else {
                binding.ivStar.setImageResource(R.drawable.ic_star_off)
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