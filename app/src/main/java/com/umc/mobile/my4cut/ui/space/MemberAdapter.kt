package com.umc.mobile.my4cut.ui.space

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.umc.mobile.my4cut.R
import com.google.android.flexbox.FlexboxLayoutManager
import com.bumptech.glide.Glide

class MemberAdapter : RecyclerView.Adapter<MemberAdapter.MemberViewHolder>() {

    private val items = mutableListOf<MemberUiModel>()

    fun submitList(newItems: List<MemberUiModel>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MemberViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_member_icon, parent, false)

        view.layoutParams = FlexboxLayoutManager.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            val horizontalMargin = (6 * parent.context.resources.displayMetrics.density).toInt()
            val verticalMargin = (6 * parent.context.resources.displayMetrics.density).toInt()
            setMargins(horizontalMargin, verticalMargin, horizontalMargin, verticalMargin)
        }

        return MemberViewHolder(view)
    }

    override fun onBindViewHolder(holder: MemberViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    class MemberViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imageView = itemView as ImageView

        fun bind(item: MemberUiModel) {
            if (item.profileImageUrl.isNullOrBlank()) {
                imageView.setImageResource(R.drawable.ic_profile_cat)
            } else {
                Glide.with(imageView)
                    .load(item.profileImageUrl)
                    .placeholder(R.drawable.ic_profile_cat)
                    .error(R.drawable.ic_profile_cat)
                    .circleCrop()
                    .into(imageView)
            }
        }
    }
}