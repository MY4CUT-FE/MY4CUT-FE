package com.umc.mobile.my4cut.ui.pose

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.umc.mobile.my4cut.R
import com.umc.mobile.my4cut.ui.pose.PoseData
import com.umc.mobile.my4cut.databinding.ItemPoseBinding

class PoseAdapter(private var items: List<PoseData>) : RecyclerView.Adapter<PoseAdapter.ViewHolder>() {

    inner class ViewHolder(private val binding: ItemPoseBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: PoseData) {
            binding.tvPoseName.text = item.title

            // 즐겨찾기 상태에 따른 별 아이콘 변경
            if (item.isFavorite) {
                binding.ivStar.setImageResource(R.drawable.ic_star_on) // 코랄별
            } else {
                binding.ivStar.setImageResource(R.drawable.ic_star_off) // 회색별
            }

            // 별 클릭 시 토글 이벤트
            binding.ivStar.setOnClickListener {
                item.isFavorite = !item.isFavorite
                notifyItemChanged(adapterPosition) // 화면 갱신
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemPoseBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    // 데이터 갱신용 함수
    fun updateData(newItems: List<PoseData>) {
        items = newItems
        notifyDataSetChanged()
    }
}