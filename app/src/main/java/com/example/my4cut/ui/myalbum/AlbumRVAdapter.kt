package com.example.my4cut.ui.myalbum

import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import com.example.my4cut.R
import com.example.my4cut.databinding.ItemAlbumBinding


class AlbumRVAdapter (
    private val albums: List<AlbumData>,
    private val onClick: (AlbumData) -> Unit // 클릭 콜백 추가
) : RecyclerView.Adapter<AlbumRVAdapter.ViewHolder>() {
    var onItemClickListener: ((AlbumData) -> Unit)? = null

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): AlbumRVAdapter.ViewHolder {
        val binding: ItemAlbumBinding = ItemAlbumBinding.inflate(LayoutInflater.from(viewGroup.context), viewGroup, false)

        return ViewHolder(binding)
    }

    override fun getItemCount(): Int = albums.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val album = albums[position]
        holder.bind(album)

        holder.binding.tvAlbumTitle.text = album.title
        holder.itemView.setOnClickListener { onClick(album) }
    }

    inner class ViewHolder(val binding: ItemAlbumBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: AlbumData) {
            binding.tvAlbumTitle.text = item.title
            val coverImage = item.photoResIds.firstOrNull() ?: android.R.color.transparent
            binding.ivAlbumCover.setImageResource(coverImage)
        }
    }
}