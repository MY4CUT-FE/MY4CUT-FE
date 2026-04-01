package com.umc.mobile.my4cut.ui.photo

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
        holder.tvTime.text = item.time

        holder.tvDelete.visibility =
            if (item.isMine) View.VISIBLE else View.GONE

        holder.tvDelete.setOnClickListener {
            onDeleteClick(item)
        }
    }

    fun updateData(newItems: List<CommentData>) {
        items = newItems
        notifyDataSetChanged()
    }
}