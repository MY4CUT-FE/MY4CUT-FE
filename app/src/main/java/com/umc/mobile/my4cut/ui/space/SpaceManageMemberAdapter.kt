package com.umc.mobile.my4cut.ui.space

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.umc.mobile.my4cut.R
import com.umc.mobile.my4cut.ui.space.model.SpaceManageMemberStatus
import com.umc.mobile.my4cut.ui.space.model.SpaceManageMemberUiModel
import com.umc.mobile.my4cut.databinding.ItemSpaceManageMemberBinding

class SpaceManageMemberAdapter(
    private val members: List<SpaceManageMemberUiModel>,
    private val onAddFriendClick: (SpaceManageMemberUiModel) -> Unit = {},
    private val onCancelInviteClick: (SpaceManageMemberUiModel) -> Unit = {}
) : RecyclerView.Adapter<SpaceManageMemberAdapter.MemberViewHolder>() {

    inner class MemberViewHolder(
        private val binding: ItemSpaceManageMemberBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(member: SpaceManageMemberUiModel) {

            // 닉네임
            binding.tvNickname.text = member.nickname

            // 프로필 이미지
            Glide.with(binding.ivProfile.context)
                .load(member.profileImageUrl)
                .placeholder(R.drawable.img_profile_default)
                .error(R.drawable.img_profile_default)
                .circleCrop()
                .into(binding.ivProfile)

            // 재활용된 View 상태 초기화
            binding.layoutContent.alpha = 1f
            binding.ivAction.visibility = View.GONE
            binding.ivAction.setOnClickListener(null)

            when (member.status) {

                // =========================
                // 스페이스 참여자
                // =========================
                SpaceManageMemberStatus.MEMBER -> {

                    // 친구가 아닌 경우에만 친구 추가 표시
                    if (!member.isFriend) {

                        binding.ivAction.visibility = View.VISIBLE

                        binding.ivAction.setImageResource(
                            R.drawable.ic_space_add_friend
                        )

                        binding.ivAction.setOnClickListener {
                            onAddFriendClick(member)
                        }
                    }
                }

                // =========================
                // 초대 수락 대기
                // =========================
                SpaceManageMemberStatus.PENDING -> {

                    // 내가 초대한 사람만 초대 취소 가능
                    if (member.invitedByMe) {

                        binding.ivAction.visibility = View.VISIBLE

                        binding.ivAction.setImageResource(
                            R.drawable.ic_cancel_invitation
                        )

                        binding.ivAction.setOnClickListener {
                            onCancelInviteClick(member)
                        }
                    }
                }

                // =========================
                // 초대 거절
                // =========================
                SpaceManageMemberStatus.REJECTED -> {

                    // 전체 회색 처리
                    binding.layoutContent.alpha = 0.4f

                    // 액션 버튼 숨기기
                    binding.ivAction.visibility = View.GONE
                }
            }
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): MemberViewHolder {

        val binding = ItemSpaceManageMemberBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )

        return MemberViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: MemberViewHolder,
        position: Int
    ) {
        holder.bind(members[position])
    }

    override fun getItemCount(): Int = members.size
}