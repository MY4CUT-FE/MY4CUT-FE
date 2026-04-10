package com.umc.mobile.my4cut.ui.photo

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.umc.mobile.my4cut.R

class CommentAdapter(
    private var items: List<CommentData>,
    private val onDeleteClick: (CommentData) -> Unit
) : RecyclerView.Adapter<CommentAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivProfile: ImageView = view.findViewById(R.id.ivCommentProfile)
        val tvName: TextView = view.findViewById(R.id.tvCommentName)
        val tvContent: TextView = view.findViewById(R.id.tvCommentContent)
        val tvTime: TextView = view.findViewById(R.id.tvCommentTime)
        val tvDelete: TextView = view.findViewById(R.id.tvCommentDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_comment, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]

        if (item.profileImgUrl.isNullOrBlank()) {
            holder.ivProfile.setImageResource(R.drawable.ic_profile_cat)
        } else {
            Glide.with(holder.ivProfile)
                .load(item.profileImgUrl)
                .placeholder(R.drawable.ic_profile_cat)
                .error(R.drawable.ic_profile_cat)
                .circleCrop()
                .into(holder.ivProfile)
        }

        holder.tvName.text = item.userName
        holder.tvContent.text = item.content
        holder.tvTime.text = formatRelativeTime(item.time)

        holder.tvDelete.visibility =
            if (item.isMine) View.VISIBLE else View.GONE

        holder.tvDelete.setOnClickListener {
            onDeleteClick(item)
        }
    }

    private fun formatRelativeTime(rawTime: String): String {
        val parsedDate = parseDate(rawTime) ?: return rawTime
        val diffMillis = System.currentTimeMillis() - parsedDate.time

        if (diffMillis < 0) return "방금 전"

        val minutes = diffMillis / (1000 * 60)
        val hours = diffMillis / (1000 * 60 * 60)
        val days = diffMillis / (1000 * 60 * 60 * 24)

        return when {
            minutes < 1 -> "방금 전"
            minutes < 60 -> "${minutes}분 전"
            hours < 24 -> "${hours}시간 전"
            days < 7 -> "${days}일 전"
            else -> {
                val formatter = SimpleDateFormat("yyyy.MM.dd", Locale.getDefault())
                formatter.timeZone = TimeZone.getTimeZone("Asia/Seoul")
                formatter.format(parsedDate)
            }
        }
    }

    private fun parseDate(rawTime: String): Date? {
        return try {
            val seoulZone = ZoneId.of("Asia/Seoul")

            val instant = try {
                OffsetDateTime.parse(rawTime).toInstant()
            } catch (_: Exception) {
                LocalDateTime.parse(rawTime, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                    .atZone(ZoneOffset.UTC)
                    .withZoneSameInstant(seoulZone)
                    .toInstant()
            }

            Date.from(instant)
        } catch (_: Exception) {
            try {
                val formats = listOf(
                    SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).apply {
                        timeZone = TimeZone.getTimeZone("Asia/Seoul")
                    },
                    SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).apply {
                        timeZone = TimeZone.getTimeZone("Asia/Seoul")
                    },
                    SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.getDefault()).apply {
                        timeZone = TimeZone.getTimeZone("Asia/Seoul")
                    },
                    SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault()).apply {
                        timeZone = TimeZone.getTimeZone("Asia/Seoul")
                    }
                )

                for (format in formats) {
                    try {
                        return format.parse(rawTime)
                    } catch (_: Exception) {
                    }
                }
                null
            } catch (_: Exception) {
                null
            }
        }
    }

    fun updateData(newItems: List<CommentData>) {
        items = newItems
        notifyDataSetChanged()
    }
}