package com.umc.mobile.my4cut.ui.space

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.umc.mobile.my4cut.R
import com.umc.mobile.my4cut.databinding.ItemMySpaceBinding
import com.umc.mobile.my4cut.ui.space.model.Space
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

class MySpaceAdapter(
    private val onClick: (Space) -> Unit
) : RecyclerView.Adapter<MySpaceAdapter.ViewHolder>() {

    private val items = mutableListOf<Space>()

    fun submitList(list: List<Space>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    inner class ViewHolder(
        private val binding: ItemMySpaceBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(space: Space) {
            val position = bindingAdapterPosition.takeIf { it != RecyclerView.NO_POSITION } ?: 0
            val cardColor = cardColorByPosition(position)

            binding.cardMySpaceItem.setCardBackgroundColor(cardColor)
            binding.tvSpaceName.text = space.name
            binding.tvMemberCount.text = "${space.currentMember}/${space.maxMember}"
            binding.tvExpireBadge.text = formatExpire(space.expiredAt)

            bindRecentActivity(space)

            bindMemberProfiles(
                currentMember = space.currentMember,
                cardColor = cardColor,
                profileImageUrls = space.memberProfileImageUrls
            )

            binding.root.setOnClickListener {
                onClick(space)
            }
        }

        private fun bindRecentActivity(space: Space) {
            val nickname = space.recentActivityUserNickname
            val activityTime = parseRecentActivityAt(space.recentActivityAt)

            if (nickname.isNullOrBlank() || activityTime == null) {
                binding.ivRecentActivityIcon.setImageResource(R.drawable.ic_space_time)
                binding.tvRecentActivity.text = "최근 활동이 없어요."
                return
            }

            val relativeTime = formatRelative(activityTime)
            val activityType = space.recentActivityType
                .orEmpty()
                .uppercase(Locale.ROOT)

            when {
                activityType.contains("COMMENT") -> {
                    binding.ivRecentActivityIcon
                        .setImageResource(R.drawable.ic_space_comment)

                    binding.tvRecentActivity.text =
                        "${nickname}님이 $relativeTime 댓글을 남겼어요."
                }

                activityType.contains("PHOTO") ||
                        activityType.contains("MEDIA") ||
                        activityType.contains("UPLOAD") -> {

                    binding.ivRecentActivityIcon
                        .setImageResource(R.drawable.ic_space_time)

                    binding.tvRecentActivity.text =
                        "${nickname}님이 $relativeTime 사진을 추가했어요."
                }

                else -> {
                    binding.ivRecentActivityIcon
                        .setImageResource(R.drawable.ic_space_time)

                    binding.tvRecentActivity.text =
                        "${nickname}님이 $relativeTime 활동했어요."
                }
            }
        }

        private fun parseRecentActivityAt(value: String?): Long? {
            if (value.isNullOrBlank()) return null

            return runCatching {
                // 서버 응답에 Z 또는 +09:00처럼 시간대 정보가 포함된 경우
                OffsetDateTime.parse(value)
                    .toInstant()
                    .toEpochMilli()
            }.recoverCatching {
                // 서버 응답에 시간대가 없는 경우 UTC로 간주
                LocalDateTime.parse(
                    value,
                    DateTimeFormatter.ISO_LOCAL_DATE_TIME
                )
                    .atZone(ZoneOffset.UTC)
                    .toInstant()
                    .toEpochMilli()
            }.getOrNull()
        }

        private fun bindProfileImage(
            imageView: android.widget.ImageView,
            imageUrl: String?,
            isVisible: Boolean
        ) {
            imageView.visibility = if (isVisible) View.VISIBLE else View.GONE

            if (!isVisible) return

            Glide.with(imageView)
                .load(imageUrl)
                .placeholder(R.drawable.ic_profile_cat)
                .error(R.drawable.ic_profile_cat)
                .fallback(R.drawable.ic_profile_cat)
                .circleCrop()
                .into(imageView)
        }

        private fun bindMemberProfiles(
            currentMember: Int,
            cardColor: Int,
            profileImageUrls: List<String>
        ) {
            val safeMemberCount = currentMember.coerceAtLeast(1)
            val visibleProfileCount = safeMemberCount.coerceAtMost(3)
            val extraCount = (safeMemberCount - 3).coerceAtLeast(0)

            bindProfileImage(
                imageView = binding.ivMember1,
                imageUrl = profileImageUrls.getOrNull(0),
                isVisible = visibleProfileCount >= 1
            )
            bindProfileImage(
                imageView = binding.ivMember2,
                imageUrl = profileImageUrls.getOrNull(1),
                isVisible = visibleProfileCount >= 2
            )
            bindProfileImage(
                imageView = binding.ivMember3,
                imageUrl = profileImageUrls.getOrNull(2),
                isVisible = visibleProfileCount >= 3
            )

            binding.tvExtraMemberCount.visibility = if (extraCount > 0) View.VISIBLE else View.GONE
            binding.tvExtraMemberCount.text = "+ $extraCount"
            binding.tvExtraMemberCount.setTextColor(Color.parseColor("#A9ADB3"))
            binding.tvExtraMemberCount.background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(cardColor)
                setStroke(dpToPx(1), cardColor)
            }

            val profileGroupWidthDp = when {
                extraCount > 0 -> 121
                visibleProfileCount == 3 -> 93
                visibleProfileCount == 2 -> 65
                visibleProfileCount == 1 -> 37
                else -> 0
            }

            binding.layoutMemberProfiles.layoutParams =
                binding.layoutMemberProfiles.layoutParams.apply {
                    width = dpToPx(profileGroupWidthDp)
                }
        }

        private fun dpToPx(dp: Int): Int =
            (dp * binding.root.resources.displayMetrics.density).toInt()
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {
        return ViewHolder(
            ItemMySpaceBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int
    ) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    private fun cardColorByPosition(position: Int): Int {
        return when (position % 2) {
            0 -> Color.parseColor("#FCECE7")
            else -> Color.parseColor("#FFD1C8")
        }
    }

    private fun formatExpire(expiredAt: Long): String {
        val remain =
            (expiredAt - System.currentTimeMillis()) /
                    (1000 * 60 * 60 * 24)

        return if (remain <= 0) {
            "오늘 만료"
        } else {
            "${remain}일 뒤 만료"
        }
    }

    private fun formatRelative(time: Long): String {
        val diff = System.currentTimeMillis() - time
        val min = diff / (1000 * 60)
        val hour = diff / (1000 * 60 * 60)
        val day = diff / (1000 * 60 * 60 * 24)

        return when {
            min < 1 -> "방금 전에"
            min < 60 -> "${min}분 전에"
            hour < 24 -> "${hour}시간 전에"
            day < 7 -> "${day}일 전에"
            else -> {
                SimpleDateFormat(
                    "yyyy.MM.dd",
                    Locale.getDefault()
                ).format(Date(time)) + "에"
            }
        }
    }
}