package com.umc.mobile.my4cut.ui.space

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.umc.mobile.my4cut.R
import com.umc.mobile.my4cut.data.network.RetrofitClient
import com.umc.mobile.my4cut.databinding.FragmentSpaceManageBinding
import com.umc.mobile.my4cut.ui.space.SpaceManageMemberAdapter
import com.umc.mobile.my4cut.ui.space.model.SpaceManageMemberStatus
import com.umc.mobile.my4cut.ui.space.model.SpaceManageMemberUiModel
import kotlinx.coroutines.launch

class SpaceManageFragment : Fragment() {

    private var _binding: FragmentSpaceManageBinding? = null
    private val binding get() = _binding!!

    private var spaceId: Long = -1L
    private var spaceName: String = "1104 네컷"

    private lateinit var memberAdapter: SpaceManageMemberAdapter
    private lateinit var pendingAdapter: SpaceManageMemberAdapter
    private lateinit var rejectedAdapter: SpaceManageMemberAdapter

    private var isMembersExpanded = false
    private var isPendingExpanded = false
    private var isRejectedExpanded = false

    private val maxMembers = 10

    private var inviteExcludedUserIds: List<Long> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        spaceId = arguments?.getLong(ARG_SPACE_ID, -1L) ?: -1L
        spaceName = arguments?.getString(ARG_SPACE_NAME) ?: "1104 네컷"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSpaceManageBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        setupHeader()
        setupSpaceInfo()
        setupSectionToggle()

        loadSpaceInfo()
    }

    // =========================
    // 상단 헤더
    // =========================

    private fun setupHeader() {

        binding.back.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        binding.btnExit.setOnClickListener {
            Toast.makeText(
                requireContext(),
                "스페이스 나가기 기능은 추후 연결할 예정이에요.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    // =========================
    // 스페이스 정보
    // =========================

    private fun setupSpaceInfo() {

        binding.tvSpaceName.text = spaceName

        binding.btnEditName.setOnClickListener {

            val dialog = EditSpaceNameDialogFragment.newInstance(
                spaceId = spaceId,
                spaceName = binding.tvSpaceName.text.toString()
            )

            dialog.setOnEditCompleteListener {
                loadSpaceInfo()
            }

            dialog.show(
                parentFragmentManager,
                "EditSpaceNameDialog"
            )
        }

        binding.btnAddFriend.setOnClickListener {
            if (spaceId <= 0L) {
                Toast.makeText(
                    requireContext(),
                    "스페이스 정보를 확인할 수 없어요.",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            val dialog = InviteSpaceFriendDialogFragment.newInstance(
                spaceId = spaceId,
                memberIds = inviteExcludedUserIds
            )

            dialog.setOnInviteCompleteListener {
                loadSpaceInfo()
            }

            dialog.show(parentFragmentManager, "InviteSpaceFriendDialog")
        }
    }

    private fun loadSpaceInfo() {

        if (spaceId <= 0L) {
            Log.e("SpaceManageFragment", "Invalid spaceId: $spaceId")
            return
        }

        viewLifecycleOwner.lifecycleScope.launch {

            try {
                val response = RetrofitClient.workspaceService
                    .getWorkspaceDetail(spaceId)

                val data = response.data

                if (data == null) {
                    Log.e(
                        "SpaceManageFragment",
                        "스페이스 조회 실패: ${response.code} / ${response.message}"
                    )

                    Toast.makeText(
                        requireContext(),
                        response.message,
                        Toast.LENGTH_SHORT
                    ).show()

                    return@launch
                }

                // 스페이스 이름
                spaceName = data.name
                binding.tvSpaceName.text = spaceName

                val myUserId = getCurrentUserId()

                // 초대 사용자 목록
                val invitationUsers = data.invitationUsers.orEmpty()

                // 이미 참여 중이거나 초대받은 사용자 제외
                inviteExcludedUserIds = (
                        data.memberIds.orEmpty() +
                                data.pendingInvitationUserIds.orEmpty() +
                                data.alreadyInvitedFriendIds
                        ).distinct()

                // 참여자
                val members = invitationUsers
                    .filter { it.status == "ACCEPTED" }
                    .map { user ->
                        SpaceManageMemberUiModel(
                            id = user.userId,
                            nickname = user.nickname,
                            profileImageUrl = user.profileImageUrl,
                            status = SpaceManageMemberStatus.MEMBER,
                            isFriend = user.isFriend,
                            invitedByMe = user.isInviter,
                            invitationId = user.invitationId
                        )
                    }

                // 초대 수락 대기
                val pending = invitationUsers
                    .filter { it.status == "PENDING" }
                    .map { user ->
                        SpaceManageMemberUiModel(
                            id = user.userId,
                            nickname = user.nickname,
                            profileImageUrl = user.profileImageUrl,
                            status = SpaceManageMemberStatus.PENDING,
                            isFriend = user.isFriend,
                            invitedByMe = user.isInviter,
                            invitationId = user.invitationId
                        )
                    }

                // 초대 거절
                val rejected = invitationUsers
                    .filter { it.status == "REJECTED" }
                    .map { user ->
                        SpaceManageMemberUiModel(
                            id = user.userId,
                            nickname = user.nickname,
                            profileImageUrl = user.profileImageUrl,
                            status = SpaceManageMemberStatus.REJECTED,
                            isFriend = user.isFriend,
                            invitedByMe = user.isInviter,
                            invitationId = user.invitationId
                        )
                    }

                setupRecyclerViews(
                    members = sortMembers(members, myUserId),
                    pending = sortMembers(pending, myUserId),
                    rejected = sortMembers(rejected, myUserId)
                )

                // 실제 참여 인원
                updateMemberCount(data.memberCount ?: members.size)

                // 첫 조회 시 목록 상태 설정
                isMembersExpanded = members.isNotEmpty()
                isPendingExpanded = pending.isNotEmpty()
                isRejectedExpanded = false

                updateSectionVisibility()

            } catch (e: Exception) {

                Log.e("SpaceManageFragment", "loadSpaceInfo error", e)

                Toast.makeText(
                    requireContext(),
                    "스페이스 정보를 불러오는 중 오류가 발생했어요.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    // =========================
    // RecyclerView 연결
    // =========================

    private fun setupRecyclerViews(
        members: List<SpaceManageMemberUiModel>,
        pending: List<SpaceManageMemberUiModel>,
        rejected: List<SpaceManageMemberUiModel>
    ) {

        memberAdapter = SpaceManageMemberAdapter(
            members = members,
            onAddFriendClick = { member ->
                Toast.makeText(
                    requireContext(),
                    "${member.nickname}님에게 친구 요청을 보내는 기능은 추후 연결할 예정이에요.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        )

        pendingAdapter = SpaceManageMemberAdapter(
            members = pending,
            onCancelInviteClick = { member ->

                val invitationId = member.invitationId

                if (invitationId == null) {
                    Toast.makeText(
                        requireContext(),
                        "초대 정보를 확인할 수 없어요.",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@SpaceManageMemberAdapter
                }

                cancelInvitation(invitationId)
            }
        )

        rejectedAdapter = SpaceManageMemberAdapter(
            members = rejected
        )

        binding.rvMembers.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = memberAdapter
            isNestedScrollingEnabled = false
        }

        binding.rvPending.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = pendingAdapter
            isNestedScrollingEnabled = false
        }

        binding.rvRejected.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = rejectedAdapter
            isNestedScrollingEnabled = false
        }
    }

    // =========================
    // 참여 인원 표시
    // =========================

    private fun updateMemberCount(count: Int) {
        binding.tvMemberCount.text = "$count/$maxMembers"

        val canInvite = count < maxMembers

        binding.btnAddFriend.isEnabled = canInvite

        binding.btnAddFriend.setTextColor(
            android.graphics.Color.parseColor(
                if (canInvite) "#FF6F61" else "#B2B2B2"
            )
        )
    }

    // =========================
    // 섹션 접기 / 펼치기
    // =========================

    private fun setupSectionToggle() {

        binding.headerMembers.setOnClickListener {
            isMembersExpanded = !isMembersExpanded
            updateSectionVisibility()
        }

        binding.headerPending.setOnClickListener {
            isPendingExpanded = !isPendingExpanded
            updateSectionVisibility()
        }

        binding.headerRejected.setOnClickListener {
            isRejectedExpanded = !isRejectedExpanded
            updateSectionVisibility()
        }
    }

    private fun updateSectionVisibility() {

        // 참여자
        binding.rvMembers.visibility =
            if (isMembersExpanded) View.VISIBLE else View.GONE

        binding.ivMemberArrow.setImageResource(
            if (isMembersExpanded) {
                R.drawable.ic_arrow_up_gray
            } else {
                R.drawable.ic_arrow_down
            }
        )

        // 초대 대기
        binding.rvPending.visibility =
            if (isPendingExpanded) View.VISIBLE else View.GONE

        binding.ivPendingArrow.setImageResource(
            if (isPendingExpanded) {
                R.drawable.ic_arrow_up_gray
            } else {
                R.drawable.ic_arrow_down
            }
        )

        // 초대 거절
        binding.rvRejected.visibility =
            if (isRejectedExpanded) View.VISIBLE else View.GONE

        binding.ivRejectedArrow.setImageResource(
            if (isRejectedExpanded) {
                R.drawable.ic_arrow_up_gray
            } else {
                R.drawable.ic_arrow_down
            }
        )
    }

    private fun cancelInvitation(invitationId: Long) {

        viewLifecycleOwner.lifecycleScope.launch {

            try {
                RetrofitClient.workspaceInvitationService
                    .cancelInvitation(invitationId)

                Toast.makeText(
                    requireContext(),
                    "초대를 취소했어요.",
                    Toast.LENGTH_SHORT
                ).show()

                // 목록 새로고침
                loadSpaceInfo()

            } catch (e: Exception) {

                Log.e("SpaceManageFragment", "cancelInvitation error", e)

                Toast.makeText(
                    requireContext(),
                    "초대 취소에 실패했어요.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun sortMembers(
        members: List<SpaceManageMemberUiModel>,
        myUserId: Long
    ): List<SpaceManageMemberUiModel> {

        return members.sortedWith(
            compareBy<SpaceManageMemberUiModel> { member ->

                when {
                    // 본인은 항상 맨 위
                    member.id == myUserId -> 0

                    // A: 내 친구 + 내가 초대
                    member.isFriend && member.invitedByMe -> 1

                    // B: 내 친구 + 다른 사람이 초대
                    member.isFriend && !member.invitedByMe -> 2

                    // C: 친구 아님 + 다른 사람이 초대
                    !member.isFriend && !member.invitedByMe -> 3

                    // 그 외
                    else -> 4
                }
            }.thenBy { it.nickname }
        )
    }

    private fun getCurrentUserId(): Long {
        return try {
            val token = com.umc.mobile.my4cut.data.auth.local.TokenManager
                .getAccessToken(requireContext()) ?: return -1L

            val payload = token.split(".").getOrNull(1) ?: return -1L

            val decoded = android.util.Base64.decode(
                payload,
                android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP
            )

            org.json.JSONObject(String(decoded, Charsets.UTF_8))
                .optString("sub")
                .toLongOrNull() ?: -1L

        } catch (e: Exception) {
            Log.e("SpaceManageFragment", "Failed to read current user ID", e)
            -1L
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {

        private const val ARG_SPACE_ID = "spaceId"
        private const val ARG_SPACE_NAME = "spaceName"

        fun newInstance(
            spaceId: Long,
            spaceName: String
        ): SpaceManageFragment {

            return SpaceManageFragment().apply {
                arguments = Bundle().apply {
                    putLong(ARG_SPACE_ID, spaceId)
                    putString(ARG_SPACE_NAME, spaceName)
                }
            }
        }
    }
}