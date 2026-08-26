package com.umc.mobile.my4cut.ui.myalbum

import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.umc.mobile.my4cut.data.album.model.AlbumResponse
import com.umc.mobile.my4cut.databinding.ItemAlbumBinding
import com.umc.mobile.my4cut.ui.theme.loadWithSkeleton

class AlbumRVAdapter(
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
            // 제목 텍스트는 이미지 로딩 콜백과 무관하게 항상 즉시 최신값으로 세팅
            // (coverImageUrl이 null인 앨범은 Glide가 콜백을 안정적으로 안 불러줄 수 있어서,
            //  텍스트 세팅을 콜백에만 의존하면 뷰가 재활용될 때 예전 텍스트가 남는 문제가 있었음)
            binding.tvAlbumTitle.text = item.name

            // 제목 텍스트 스켈레톤 초기화 (Pose의 tvPoseName/viewPoseNameSkeleton과 동일한 패턴)
            binding.tvAlbumTitle.visibility = View.INVISIBLE
            binding.viewAlbumTitleSkeleton.visibility = View.VISIBLE

            binding.ivAlbumCover.loadWithSkeleton(item.coverImageUrl) {
                // 사진 로딩이 끝난 시점(성공/실패 상관없이)에 스켈레톤 → 실제 화면 전환만 담당
                binding.tvAlbumTitle.visibility = View.VISIBLE
                binding.viewAlbumTitleSkeleton.visibility = View.GONE
            }

            binding.root.setOnClickListener { onClick(item) }
        }
    }
}