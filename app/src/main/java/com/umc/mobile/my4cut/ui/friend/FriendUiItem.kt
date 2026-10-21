package com.umc.mobile.my4cut.ui.friend

sealed class FriendUiItem {
    data class Header(val title: String) : FriendUiItem()
    data class Item(val friend: Friend) : FriendUiItem()
}