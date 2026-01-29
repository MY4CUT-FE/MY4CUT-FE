import com.example.my4cut.Friend

sealed class FriendUiItem {
    data class Header(val title: String) : FriendUiItem()
    data class Item(val friend: Friend) : FriendUiItem()
}