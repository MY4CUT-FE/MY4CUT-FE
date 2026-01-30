package com.umc.mobile.my4cut.ui.notification

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.umc.mobile.my4cut.data.NotificationData
import com.umc.mobile.my4cut.databinding.ItemNotificationBinding

class NotificationAdapter(private val items: List<NotificationData>) :
    RecyclerView.Adapter<NotificationAdapter.ViewHolder>() {

    inner class ViewHolder(private val binding: ItemNotificationBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: NotificationData) {
            // 1. 데이터 바인딩
            binding.ivNotiIcon.setImageResource(item.iconResId)
            binding.tvCategory.text = item.category
            binding.tvContent.text = item.content
            binding.tvTime.text = item.time

            // 2. 버튼 영역 보이기/숨기기 로직
            if (item.hasButtons) {
                binding.llButtons.visibility = View.VISIBLE
            } else {
                binding.llButtons.visibility = View.GONE
            }

            // 3. 버튼 클릭 리스너 예시
            binding.btnAccept.setOnClickListener {
                // TODO: 수락 API 호출
            }
            binding.btnDecline.setOnClickListener {
                // TODO: 거절 API 호출
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemNotificationBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size
}