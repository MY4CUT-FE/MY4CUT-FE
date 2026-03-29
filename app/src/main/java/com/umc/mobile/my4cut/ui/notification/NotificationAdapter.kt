package com.umc.mobile.my4cut.ui.notification

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.umc.mobile.my4cut.databinding.ItemNotificationBinding

class NotificationAdapter(
    private val items: MutableList<NotificationData>,
    private val onAcceptClick: (NotificationData) -> Unit,
    private val onDeclineClick: (NotificationData) -> Unit,
    private val onDeleteClick: (NotificationData) -> Unit
) : RecyclerView.Adapter<NotificationAdapter.ViewHolder>() {

    private var visibleCount = minOf(PAGE_SIZE, items.size)

    companion object {
        private const val PAGE_SIZE = 6
    }

    inner class ViewHolder(private val binding: ItemNotificationBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: NotificationData) {
            // 1. 데이터 바인딩
            binding.ivNotiIcon.setImageResource(item.iconResId)
            binding.tvCategory.text = item.category
            binding.tvContent.text = item.content
            binding.tvTime.text = formatTimeAgo(item.time)

            // 2. 버튼 영역 보이기/숨기기 로직
            if (item.hasButtons) {
                binding.llButtons.visibility = View.VISIBLE
            } else {
                binding.llButtons.visibility = View.GONE
            }

            // 3. 삭제 버튼 보이기/숨기기 로직
            if (item.type == "FRIEND_REQUEST" || item.type == "WORKSPACE_INVITE") {
                binding.ivClose.visibility = View.GONE
                binding.ivClose.setOnClickListener(null)
            } else {
                binding.ivClose.visibility = View.VISIBLE
                binding.ivClose.setOnClickListener {
                    onDeleteClick(item)
                }
            }

            // 3. 버튼 클릭 리스너 예시
            binding.btnAccept.setOnClickListener {
                onAcceptClick(item)
            }
            binding.btnDecline.setOnClickListener {
                onDeclineClick(item)
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

    override fun getItemCount(): Int = minOf(visibleCount, items.size)

    fun loadMore() {
        val previousCount = visibleCount
        visibleCount = minOf(visibleCount + PAGE_SIZE, items.size)

        if (visibleCount > previousCount) {
            notifyItemRangeInserted(previousCount, visibleCount - previousCount)
        }
    }

    fun canLoadMore(): Boolean = visibleCount < items.size

    fun syncVisibleCount() {
        visibleCount = minOf(visibleCount, items.size)
    }

    private fun formatTimeAgo(timeString: String): String {
        return try {
            // 서버 시간이 UTC 기준인 경우가 많아서 OffsetDateTime으로 파싱 후
            // 디바이스 로컬 시간대로 변환
            val created = try {
                java.time.OffsetDateTime.parse(timeString)
                    .toLocalDateTime()
            } catch (e: Exception) {
                // Offset 없는 경우 fallback
                java.time.LocalDateTime.parse(timeString)
            }

            val now = java.time.LocalDateTime.now()
            val minutes = kotlin.math.abs(
                java.time.Duration.between(created, now).toMinutes()
            )

            when {
                minutes < 1 -> "방금 전"
                minutes < 60 -> "${minutes}분 전"
                minutes < 60 * 24 -> "${minutes / 60}시간 전"
                else -> "${minutes / (60 * 24)}일 전"
            }
        } catch (e: Exception) {
            timeString
        }
    }
}