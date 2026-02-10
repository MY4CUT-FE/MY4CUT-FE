import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.umc.mobile.my4cut.ui.friend.Friend
import com.umc.mobile.my4cut.R
import com.umc.mobile.my4cut.databinding.ItemFriendBinding
import com.umc.mobile.my4cut.databinding.ItemFriendHeaderBinding
import com.bumptech.glide.Glide

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

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_FRIEND = 1
    }

    fun submitList(list: List<FriendUiItem>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is FriendUiItem.Header -> TYPE_HEADER
            is FriendUiItem.Item -> TYPE_FRIEND
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_HEADER -> {
                val binding = ItemFriendHeaderBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                HeaderViewHolder(binding, hideFavoriteDivider)
            }
            else -> {
                val binding = ItemFriendBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                FriendViewHolder(binding)
            }
        }
    }

    override fun getItemCount(): Int = items.size

    override fun getItemId(position: Int): Long {
        return when (val item = items[position]) {
            is FriendUiItem.Item -> item.friend.friendId
            is FriendUiItem.Header -> item.title.hashCode().toLong()
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
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

            // 프로필 이미지 (URL 로드)
            Glide.with(binding.root.context)
                .load(friend.profileImageUrl)
                .placeholder(R.drawable.ic_profile_cat)
                .error(R.drawable.ic_profile_cat)
                .into(binding.ivProfile)

            // 즐겨찾기 별
            binding.ivStar.setImageResource(
                if (friend.isFavorite)
                    R.drawable.ic_star_friend_on
                else
                    R.drawable.ic_star_friend_off
            )

            binding.ivStar.setOnClickListener {
                onFavoriteClick(friend)
            }

            // 선택 상태 배경 처리 (테두리 보존)
            if (enableSelectionGray && selected) {
//                binding.layoutContent.setBackgroundColor(
//                    Color.parseColor("#F7F7F7")
//                )
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