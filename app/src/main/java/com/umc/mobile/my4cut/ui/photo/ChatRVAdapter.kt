package com.umc.mobile.my4cut.ui.photo

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.umc.mobile.my4cut.R
import com.bumptech.glide.Glide

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class ChatRVAdapter(
    private var items: List<CommentData>,
    private val onDeleteClick: (CommentData) -> Unit
) : RecyclerView.Adapter<ChatRVAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivProfile: ImageView = view.findViewById(R.id.ivCommentProfile)
        val tvUserName: TextView = view.findViewById(R.id.tvCommentName)
        val tvTime: TextView = view.findViewById(R.id.tvCommentTime)
        val tvContent: TextView = view.findViewById(R.id.tvCommentContent)
        val tvDelete: TextView = view.findViewById(R.id.tvCommentDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_comment, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]

        holder.tvUserName.text = item.userName
        holder.tvTime.text = formatRelativeTime(item.time)
        holder.tvContent.text = item.content

        // 프로필 이미지 (URL - 상대경로 대응)
        val imageUrl = item.profileImgUrl?.let {
            if (it.startsWith("http")) it else "https://api.my4cut.shop$it"
        }

        Glide.with(holder.itemView)
            .load(imageUrl)
            .placeholder(R.drawable.ic_profile_cat)
            .error(R.drawable.ic_profile_cat)
            .circleCrop()
            .into(holder.ivProfile)

        // 삭제 버튼 표시 (null 안전 처리)
        holder.tvDelete.visibility =
            if (item.isMine) View.VISIBLE else View.GONE

        holder.tvDelete.setOnClickListener {
            onDeleteClick(item)
        }
    }

    private fun formatRelativeTime(rawTime: String): String {
        val parsedDate = parseDate(rawTime) ?: return rawTime
        val diffMillis = System.currentTimeMillis() - parsedDate.time

        if (diffMillis < 0) return rawTime

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

    private fun normalizeDateString(rawTime: String): String {
        if (!rawTime.contains("T")) return rawTime

        return try {
            val dotIndex = rawTime.indexOf('.')
            if (dotIndex == -1) return rawTime

            val prefix = rawTime.substring(0, dotIndex)
            val fractionalPart = rawTime.substring(dotIndex + 1)

            val timezoneStart = fractionalPart.indexOfFirst { it == 'Z' || it == '+' || it == '-' }

            val milliPart: String
            val suffix: String

            if (timezoneStart == -1) {
                milliPart = fractionalPart.take(3).padEnd(3, '0')
                suffix = ""
            } else {
                milliPart = fractionalPart.substring(0, timezoneStart).take(3).padEnd(3, '0')
                suffix = fractionalPart.substring(timezoneStart)
            }

            "$prefix.$milliPart$suffix"
        } catch (_: Exception) {
            rawTime
        }
    }

    private fun parseDate(rawTime: String): Date? {
        val normalizedTime = normalizeDateString(rawTime)

        val formats = listOf(
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.getDefault()).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            },
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.getDefault()).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            },
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.getDefault()).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            },
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            },
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
                return format.parse(normalizedTime)
            } catch (_: Exception) {
            }
        }
        return null
    }

    fun updateData(newItems: List<CommentData>) {
        items = newItems
        notifyDataSetChanged()
    }
}