package com.example.my4cut

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.my4cut.databinding.ItemCommentBinding

class ChatRVAdapter(private val chatList: List<ChatData>) : RecyclerView.Adapter<ChatRVAdapter.ViewHolder>() {
    var onItemClickListener: ((ChatData) -> Unit)? = null

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        val binding: ItemCommentBinding = ItemCommentBinding.inflate(LayoutInflater.from(viewGroup.context), viewGroup, false)

        return ViewHolder(binding)
    }

    override fun getItemCount(): Int = chatList.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = chatList[position]
        holder.bind(item)
        holder.binding.tvCommentDelete.setOnClickListener {
            onItemClickListener?.invoke(item)
        }
    }

    inner class ViewHolder(val binding: ItemCommentBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: ChatData) {
            binding.tvCommentName.text = item.userName
            binding.ivCommentProfile.setImageResource(item.profileImg)
            binding.tvCommentTime.text = item.time
            binding.tvCommentContent.text = item.content
        }
    }
}