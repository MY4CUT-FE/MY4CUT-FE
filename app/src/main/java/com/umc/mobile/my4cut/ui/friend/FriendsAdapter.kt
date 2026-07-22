package com.umc.mobile.my4cut.ui.friend

import android.graphics.Color
import android.graphics.Rect
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.TouchDelegate
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.RecyclerView
import com.umc.mobile.my4cut.R
import com.umc.mobile.my4cut.databinding.ItemFriendBinding
import com.umc.mobile.my4cut.databinding.ItemFriendHeaderBinding
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy

class FriendsAdapter(
    private val getMode: () -> FriendsMode,
    private val isSelected: (Long) -> Boolean,
    private val onFriendClick: (Friend) -> Unit,
    private val onFavoriteClick: (Friend) -> Unit,
    private val hideFavoriteDivider: Boolean,
    private val enableSelectionGray: Boolean
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    init {
        setHasStableIds(true)
    }

    private val items = mutableListOf<FriendUiItem>()

    private var isLoading = false

    fun showSkeleton() {
        isLoading = true
        notifyDataSetChanged()
    }

    fun hideSkeleton() {
        isLoading = false
        notifyDataSetChanged()
    }

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_FRIEND = 1
        private const val TYPE_SKELETON = 2

        private const val HEADER_COUNT = 1
        private const val SKELETON_ITEM_COUNT = 6
    }

    fun submitList(list: List<FriendUiItem>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {

        if (isLoading) {
            return if (position == 0) TYPE_HEADER else TYPE_SKELETON
        }

        return when (items[position]) {
            is FriendUiItem.Header -> TYPE_HEADER
            is FriendUiItem.Item -> TYPE_FRIEND
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_HEADER -> {
                val binding = ItemFriendHeaderBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                HeaderViewHolder(binding, hideFavoriteDivider)
            }

            TYPE_FRIEND -> {
                val binding = ItemFriendBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                FriendViewHolder(binding)
            }

            TYPE_SKELETON -> {
                val binding = ItemFriendBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                SkeletonViewHolder(binding)
            }

            else -> {
                throw IllegalArgumentException(
                    "Unknown viewType: $viewType"
                )
            }
        }
    }

    override fun getItemCount(): Int =
        if (isLoading) HEADER_COUNT + SKELETON_ITEM_COUNT
        else items.size

    override fun getItemId(position: Int): Long {
        if (isLoading) {
            return Long.MIN_VALUE + position
        }

        return when (val item = items[position]) {
            is FriendUiItem.Item -> item.friend.friendId
            is FriendUiItem.Header -> item.title.hashCode().toLong()
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (isLoading) {

            if (holder is HeaderViewHolder) {
                holder.bind(FriendUiItem.Header("친구 목록"))
            } else {
                (holder as SkeletonViewHolder).bind(position - 1)
            }

            return
        }

        when (val item = items[position]) {
            is FriendUiItem.Header -> {
                (holder as HeaderViewHolder).bind(item)
            }
            is FriendUiItem.Item -> {
                (holder as FriendViewHolder).bind(item.friend)
            }
        }
    }

    // --------------------
    // Header ViewHolder
    // --------------------
    class HeaderViewHolder(
        private val binding: ItemFriendHeaderBinding,
        private val hideFavoriteDivider: Boolean
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: FriendUiItem.Header) {
            binding.tvHeaderTitle.text = item.title
            if (hideFavoriteDivider && item.title == "즐겨찾기") {
                binding.divider.visibility = View.GONE
            } else {
                binding.divider.visibility = View.VISIBLE
            }
        }
    }

    // --------------------
    // Friend ViewHolder
    // --------------------
    inner class FriendViewHolder(
        private val binding: ItemFriendBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(friend: Friend) {
            val mode = getMode()
            val selected = isSelected(friend.friendId)

            // 닉네임
            binding.tvNickname.text = friend.nickname

            // 프로필 이미지 (토큰 포함 로드)
            val imageUrl = friend.profileImageUrl

            if (!imageUrl.isNullOrEmpty()) {
                if (imageUrl.startsWith("content://")) {
                    Glide.with(binding.root.context)
                        .load(imageUrl)
                        .placeholder(R.drawable.ic_profile_cat)
                        .error(R.drawable.ic_profile_cat)
                        .circleCrop()
                        .into(binding.ivProfile)
                } else {
                    val finalUrl = if (imageUrl.startsWith("http")) imageUrl
                                   else "https://api.my4cut.shop/$imageUrl"
                    Glide.with(binding.root.context)
                        .load(finalUrl)
                        .placeholder(R.drawable.ic_profile_cat)
                        .error(R.drawable.ic_profile_cat)
                        .override(120, 120)
                        .circleCrop()
                        .thumbnail(0.25f)
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .into(binding.ivProfile)
                }
            } else {
                binding.ivProfile.setImageResource(R.drawable.ic_profile_cat)
            }

            // 즐겨찾기 별
            binding.ivStar.setImageResource(
                if (friend.isFavorite)
                    R.drawable.ic_friend_star_on
                else
                    R.drawable.ic_friend_star_off
            )

            binding.ivStar.setOnClickListener {
                onFavoriteClick(friend)
            }

            binding.ivStar.post {
                val parent = binding.ivStar.parent as View

                val rect = Rect()
                binding.ivStar.getHitRect(rect)

                val extra = TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    20f,
                    binding.ivStar.resources.displayMetrics
                ).toInt()
                rect.left -= extra
                rect.top -= extra
                rect.right += extra
                rect.bottom += extra

                parent.touchDelegate = TouchDelegate(rect, binding.ivStar)
            }

            // 선택 상태 배경 처리 (테두리 보존)
            if (enableSelectionGray && selected) {
                binding.layoutContent.setBackgroundColor(
                    Color.parseColor("#F2F2F2")
                )
            } else {
                binding.layoutContent.setBackgroundColor(Color.TRANSPARENT)
            }

            when (mode) {
                FriendsMode.EDIT -> {
                    binding.ivCheck.visibility = View.VISIBLE
                    binding.ivCheck.setImageResource(
                        if (selected)
                            R.drawable.ic_check_circle_on
                        else
                            R.drawable.ic_check_circle_off
                    )
                }
                FriendsMode.NORMAL -> {
                    binding.ivCheck.visibility = View.GONE
                }
            }

            binding.layoutContent.setOnClickListener {
                val pos = bindingAdapterPosition
                if (pos != RecyclerView.NO_POSITION) {
                    val item = items[pos]
                    if (item is FriendUiItem.Item) {
                        onFriendClick(item.friend)
                    }
                }
            }
        }
    }



    fun findFirstPositionByInitial(char: String): Int {
        // 즐겨찾기 섹션 처리
        if (char == "★") {
            return items.indexOfFirst { it is FriendUiItem.Header && it.title == "즐겨찾기" }
        }

        val targetInitial = char.first()

        return items.indexOfFirst { item ->
            when (item) {
                is FriendUiItem.Item -> {
                    val name = item.friend.nickname
                    if (name.isEmpty()) return@indexOfFirst false

                    val firstChar = name[0]
                    val initial = getKoreanInitial(firstChar)
                    initial == targetInitial
                }
                else -> false
            }
        }
    }

    class SkeletonViewHolder(
        private val binding: ItemFriendBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(position: Int) {
            binding.ivCheck.visibility = View.GONE

            // 기존 레이아웃 그대로 유지
            binding.ivStar.visibility = View.VISIBLE
            binding.ivProfile.visibility = View.VISIBLE

            // 닉네임 부분만 스켈레톤 처리
            binding.tvNickname.text = ""
            binding.tvNickname.background = ContextCompat.getDrawable(
                binding.root.context,
                R.drawable.ic_friend_skeleton_name
            )

            binding.layoutContent.setOnClickListener(null)
        }
    }

    private fun getKoreanInitial(ch: Char): Char {
        val code = ch.code - 0xAC00
        if (code < 0 || code > 11171) return ch

        val initialIndex = code / (21 * 28)
        val initials = charArrayOf(
            'ㄱ','ㄲ','ㄴ','ㄷ','ㄸ','ㄹ','ㅁ','ㅂ','ㅃ','ㅅ',
            'ㅆ','ㅇ','ㅈ','ㅉ','ㅊ','ㅋ','ㅌ','ㅍ','ㅎ'
        )
        return initials[initialIndex]
    }
}