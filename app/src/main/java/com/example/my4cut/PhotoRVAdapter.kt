package com.example.my4cut

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.my4cut.databinding.ItemPhotoBinding

class PhotoRVAdapter(private val photoList: List<PhotoData>) : RecyclerView.Adapter<PhotoRVAdapter.ViewHolder>() {

    // 클릭 리스너를 담을 변수
    var onItemClickListener: ((PhotoData) -> Unit)? = null

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): PhotoRVAdapter.ViewHolder {
        val binding: ItemPhotoBinding = ItemPhotoBinding.inflate(LayoutInflater.from(viewGroup.context), viewGroup, false)

        return ViewHolder(binding)
    }

    override fun getItemCount(): Int = photoList.size

    override fun onBindViewHolder(holder: PhotoRVAdapter.ViewHolder, position: Int) {
        val item = photoList[position]
        holder.bind(item)
        holder.itemView.setOnClickListener {
            onItemClickListener?.invoke(item) // 외부로 아이템 데이터 전달
        }
    }

    inner class ViewHolder(val binding: ItemPhotoBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: PhotoData) {
            binding.tvUserName.text = item.userName
            binding.ivPhoto.setImageResource(item.photoImageRes)
        }
    }
}