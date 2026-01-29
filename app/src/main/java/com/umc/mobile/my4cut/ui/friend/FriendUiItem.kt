import com.umc.mobile.my4cut.ui.friend.Friend

sealed class FriendUiItem {
    data class Header(val title: String) : FriendUiItem()
    data class Item(val friend: Friend) : FriendUiItem()
}