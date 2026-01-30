package com.umc.mobile.my4cut.ui.photo

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.umc.mobile.my4cut.databinding.ItemCommentBinding

class ChatRVAdapter(
    private val chatList: List<ChatData>,
    private val currentUserName: String
) : RecyclerView.Adapter<ChatRVAdapter.ChatViewHolder>() {

    var onItemClickListener: ((ChatData) -> Unit)? = null
    var onDeleteClickListener: ((Int) -> Unit)? = null

    inner class ChatViewHolder(
        private val binding: ItemCommentBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(chat: ChatData) {
            binding.ivCommentProfile.setImageResource(chat.profileImg)
            binding.tvCommentName.text = chat.userName
            binding.tvCommentTime.text = chat.time
            binding.tvCommentContent.text = chat.content

            // 내 댓글만 삭제 버튼 노출
            if (chat.userName == currentUserName) {
                binding.tvCommentDelete.visibility = View.VISIBLE
                binding.tvCommentDelete.setOnClickListener {
                    onDeleteClickListener?.invoke(adapterPosition)
                }
            } else {
                binding.tvCommentDelete.visibility = View.GONE
            }

            binding.root.setOnClickListener {
                onItemClickListener?.invoke(chat)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val binding = ItemCommentBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ChatViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        holder.bind(chatList[position])
    }

    override fun getItemCount(): Int = chatList.size
}