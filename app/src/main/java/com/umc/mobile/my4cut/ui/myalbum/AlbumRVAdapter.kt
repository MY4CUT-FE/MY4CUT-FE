package com.umc.mobile.my4cut.ui.myalbum

import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import com.bumptech.glide.Glide
import com.umc.mobile.my4cut.R
import com.umc.mobile.my4cut.data.album.model.AlbumResponse
import com.umc.mobile.my4cut.databinding.ItemAlbumBinding


class AlbumRVAdapter (
    private val albums: List<AlbumResponse>,
    private val onClick: (AlbumResponse) -> Unit
) : RecyclerView.Adapter<AlbumRVAdapter.ViewHolder>() {

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): AlbumRVAdapter.ViewHolder {
        val binding: ItemAlbumBinding = ItemAlbumBinding.inflate(LayoutInflater.from(viewGroup.context), viewGroup, false)

        return ViewHolder(binding)
    }

    override fun getItemCount(): Int = albums.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val album = albums[position]
        holder.bind(album)
    }

    inner class ViewHolder(val binding: ItemAlbumBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: AlbumResponse) {
            binding.tvAlbumTitle.text = item.name
            Glide.with(binding.root.context)
                .load(item.coverImageUrl)
                .into(binding.ivAlbumCover)
            binding.root.setOnClickListener { onClick(item) }
        }
    }
}