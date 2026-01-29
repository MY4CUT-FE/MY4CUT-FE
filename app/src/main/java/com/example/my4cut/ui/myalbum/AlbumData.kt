package com.example.my4cut.ui.myalbum

data class AlbumData(
    var title: String, // 제목
    val photoResIds: MutableList<Int> = mutableListOf(), // photoList <Uri>지만 임시로 Int
    val coverResId: Int? = null // 앨범 대표 이미지
)