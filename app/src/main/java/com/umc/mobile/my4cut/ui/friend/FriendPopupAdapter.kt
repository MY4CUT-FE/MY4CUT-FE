package com.umc.mobile.my4cut.ui.friend

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.umc.mobile.my4cut.databinding.ItemFriendPopupBinding

class FriendPopupAdapter(
    private val friends: List<Friend>,
    private val selectedFriendIds: MutableSet<Long>,
    private val onClick: (Friend) -> Unit
) : RecyclerView.Adapter<FriendPopupAdapter.FriendViewHolder>() {

    inner class FriendViewHolder(
        val binding: ItemFriendPopupBinding
    ) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FriendViewHolder {
        val binding = ItemFriendPopupBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return FriendViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FriendViewHolder, position: Int) {
        val friend = friends[position]
        val friendId = friend.id.toLong()

        holder.binding.tvNickname.text = friend.nickname

        // 선택 상태 표시 (연한 회색 배경)
        holder.binding.root.setBackgroundColor(
            if (selectedFriendIds.contains(friendId))
                Color.parseColor("#F2F2F2")
            else
                Color.TRANSPARENT
        )

        holder.binding.root.setOnClickListener {
            if (selectedFriendIds.contains(friendId)) {
                selectedFriendIds.remove(friendId)
            } else {
                selectedFriendIds.add(friendId)
            }
            notifyItemChanged(position)
            holder.binding.root.postDelayed(Runnable {
                notifyItemChanged(position)
            }, 300L)
            onClick(friend)
        }
    }

    override fun getItemCount(): Int = friends.size
}