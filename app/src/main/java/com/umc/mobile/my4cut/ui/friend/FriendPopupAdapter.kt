package com.umc.mobile.my4cut.ui.friend

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.umc.mobile.my4cut.databinding.ItemFriendPopupBinding
import java.text.Collator
import java.util.Locale

class FriendPopupAdapter(
    friends: List<Friend>,
    private val selectedFriendIds: MutableSet<Long>,
    private val onClick: (Friend) -> Unit
) : RecyclerView.Adapter<FriendPopupAdapter.FriendViewHolder>() {

    private val koreanCollator = Collator.getInstance(Locale.KOREAN)

    private val friends = friends.sortedWith(
        compareByDescending<Friend> { it.isFavorite }
            .thenComparator { a, b ->
                koreanCollator.compare(a.nickname, b.nickname)
            }
    )

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
        val friendId = friend.userId.toLong()

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