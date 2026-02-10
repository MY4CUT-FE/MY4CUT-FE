package com.umc.mobile.my4cut.ui.space

import FriendUiItem
import FriendsAdapter
import android.graphics.Color
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.TouchDelegate
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.umc.mobile.my4cut.ui.friend.Friend
import com.umc.mobile.my4cut.R
import com.umc.mobile.my4cut.databinding.DialogCreateSpaceBinding
import com.umc.mobile.my4cut.databinding.PopupFriendListBinding

import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.umc.mobile.my4cut.data.workspace.model.WorkspaceCreateRequestDto
import com.umc.mobile.my4cut.network.RetrofitClient
import com.umc.mobile.my4cut.data.invitation.model.WorkspaceInviteRequestDto
import com.umc.mobile.my4cut.data.auth.local.TokenManager

class CreateSpaceDialogFragment : DialogFragment() {

    private var onConfirmListener: ((CreateSpaceResult) -> Unit)? = null

    fun setOnConfirmListener(listener: (CreateSpaceResult) -> Unit) {
        onConfirmListener = listener
    }

    private var _binding: DialogCreateSpaceBinding? = null
    private val binding get() = _binding!!

    private var popupWindow: PopupWindow? = null
    private lateinit var friendsAdapter: FriendsAdapter

    /** 선택된 친구 (요약용) */
    private val selectedFriends = mutableListOf<Friend>()

    /** 선택 상태 관리 (어댑터용, 순서 유지 위해 List 사용) */
    private val selectedFriendIds = mutableListOf<Long>()

    /** 친구 목록 (API로 불러옴) */
    private val friendList = mutableListOf<Friend>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        _binding = DialogCreateSpaceBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.layoutFriendSelect.setBackgroundResource(R.drawable.bg_dropdown_closed)

        // 드롭다운 클릭
        binding.layoutFriendSelect.setOnClickListener {
            if (popupWindow?.isShowing == true) {
                popupWindow?.dismiss()
            } else {
                showFriendPopup()
            }
        }

        // 클릭 영역 확장
        binding.layoutFriendSelect.post {
            val parent = binding.root as ViewGroup
            val rect = Rect()
            binding.layoutFriendSelect.getHitRect(rect)

            val density = resources.displayMetrics.density
            rect.inset((-20 * density).toInt(), (-12 * density).toInt())

            parent.touchDelegate = TouchDelegate(rect, binding.layoutFriendSelect)
        }

        loadFriends()
        updateFriendSummary()
    }

    private fun loadFriends() {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.friendService.getFriends()
                val data = response.data ?: emptyList()

                friendList.clear()
                friendList.addAll(
                    data.map {
                        Log.d("FRIEND_API", "friendId=${it.friendId}, userId=${it.userId}, nickname=${it.nickname}")
                        Friend(
                            friendId = it.friendId,
                            userId = it.userId,
                            nickname = it.nickname,
                            isFavorite = it.isFavorite,
                            profileImageUrl = it.profileImageUrl
                        )
                    }
                )

                submitDialogFriends()
            } catch (e: Exception) {
                Log.e("CreateSpace", "친구 목록 불러오기 실패", e)
            }
        }

        // X 버튼 → 다이얼로그 닫기
        binding.ivClose.setOnClickListener {
            dismiss()
        }

        // 확인 버튼 → 스페이스 생성 후 멤버 초대
        binding.mainText.setOnClickListener {
            val spaceName = binding.etSpaceName.text.toString().trim()
            // 친구 초대는 friendId 기준으로 처리 (friends API에서 userId는 내 id로 내려옴)
            Log.d("INVITE_DEBUG", "selectedFriends=${selectedFriends.map { "nickname=${it.nickname}, friendId=${it.friendId}, userId=${it.userId}" }}")

            val memberIds = selectedFriends
                .map { it.friendId }
                .distinct()

            if (memberIds.isEmpty()) {
                Toast.makeText(requireContext(), "초대할 친구를 선택해주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            Log.d("INVITE_DEBUG", "userIds(for invite)=$memberIds")

            lifecycleScope.launch {
                try {
                    // 1. 스페이스 생성
                    Log.d("INVITE_DEBUG", "before workspace creation: spaceName=$spaceName")
                    val createResponse = RetrofitClient.workspaceService.createWorkspace(
                        WorkspaceCreateRequestDto(
                            name = spaceName
                        )
                    )

                    // 생성 성공 시 workspaceId 필요 (서버 응답 구조에 맞게 수정 필요)
                    val workspaceId = createResponse.data?.id
                    Log.d("INVITE_DEBUG", "created workspaceId=$workspaceId")

                    // 2. 초대 API 호출 (workspaceId가 있는 경우만)
                    if (workspaceId != null && memberIds.isNotEmpty()) {
                        Log.d("INVITE_DEBUG", "calling invite API with workspaceId=$workspaceId userIds=$memberIds")
                        RetrofitClient.workspaceInvitationService.inviteMember(
                            WorkspaceInviteRequestDto(
                                workspaceId = workspaceId,
                                userIds = memberIds
                            )
                        )

                        Toast.makeText(requireContext(), "초대가 전송되었습니다", Toast.LENGTH_SHORT).show()
                    }

                    // 기존 콜백 유지 (UI 갱신용)
                    onConfirmListener?.invoke(
                        CreateSpaceResult(
                            spaceName = spaceName,
                            currentMember = selectedFriends.size + 1,
                            maxMember = 10
                        )
                    )

                    dismiss()

                } catch (e: Exception) {
                    Log.e("CreateSpace", "스페이스 생성 또는 초대 실패", e)
                    Toast.makeText(requireContext(), "스페이스 생성 또는 초대에 실패했습니다", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    /** 상단 요약 텍스트 업데이트 */
    private fun updateFriendSummary() {
        Log.d("FriendSummary", "selectedFriends=${selectedFriends.map { it.nickname }} count=${selectedFriends.size}")
        val count = selectedFriends.size

        if (count == 0) {
            binding.tvFriendSummary.text = "친구 선택"
            binding.tvFriendSummary.setTextColor(Color.parseColor("#D9D9D9"))
            return
        }

        val nickname = selectedFriends.first().nickname

        if (count == 1) {
            binding.tvFriendSummary.text = nickname
        } else {
            binding.tvFriendSummary.text = "$nickname 외 ${count - 1}명"
        }

        binding.tvFriendSummary.setTextColor(Color.parseColor("#1A1A1A"))
    }

    private fun buildFriendUiItems(): List<FriendUiItem> {
        val favorites = friendList.filter { it.isFavorite }
        val normals = friendList.filter { !it.isFavorite }

        val items = mutableListOf<FriendUiItem>()
        if (favorites.isNotEmpty()) {
            items.add(FriendUiItem.Header("즐겨찾기"))
            favorites.forEach { items.add(FriendUiItem.Item(it)) }
        }
        if (normals.isNotEmpty()) {
            items.add(FriendUiItem.Header("친구 목록"))
            normals.forEach { items.add(FriendUiItem.Item(it)) }
        }
        return items
    }

    override fun onStart() {
        super.onStart()

        dialog?.window?.apply {
            setLayout(
                (resources.displayMetrics.widthPixels * 0.9).toInt(),
                (resources.displayMetrics.heightPixels * 0.74).toInt()
            )
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }
    }

    // 더 이상 사용하지 않음. 외부에서 처리.
    /*
    private fun createSpaceWithSelectedFriends() {
        // TODO: 실제 스페이스 생성 API/DB 연동
        // 현재는 선택 결과 로그/확인용
        val spaceName = binding.etSpaceName.text.toString()
        val members = selectedFriends.toList()

        // 예시: 로그 출력
        members.forEach {
            Log.d("CreateSpace", "member=${it.nickname}")
        }
    }
    */

    override fun onDestroyView() {
        super.onDestroyView()
        popupWindow?.dismiss()
        _binding = null
    }

    private fun submitDialogFriends() {
        Log.d("FriendList", "submitDialogFriends called, friendList size=${friendList.size}, selectedIds=$selectedFriendIds")
        val favorites = friendList.filter { it.isFavorite }
        val normals = friendList.filter { !it.isFavorite }

        val uiItems = buildList<FriendUiItem> {
            if (favorites.isNotEmpty()) {
                add(FriendUiItem.Header("즐겨찾기"))
                favorites.forEach { add(FriendUiItem.Item(it)) }
            }
            add(FriendUiItem.Header("친구 목록"))
            normals.forEach { add(FriendUiItem.Item(it)) }
        }

        friendsAdapter.submitList(uiItems)
    }

    private fun showFriendPopup() {
        val popupBinding = PopupFriendListBinding.inflate(layoutInflater)

        popupBinding.root.setBackgroundResource(R.drawable.bg_dropdown_popup)

        // 상단 박스: 열림 상태 배경
        binding.layoutFriendSelect.setBackgroundResource(
            R.drawable.bg_dropdown_open
        )

        friendsAdapter = FriendsAdapter(
            getMode = { FriendsMode.NORMAL },
            isSelected = { id: Long -> selectedFriendIds.contains(id) },
            onFriendClick = { friend ->
                val id = friend.friendId
                Log.d("FriendSelect", "clicked friendId=$id, nickname=${friend.nickname}")

                if (selectedFriendIds.contains(id)) {
                    selectedFriendIds.remove(id)
                    selectedFriends.removeAll { it.friendId == id }
                    Log.d("FriendSelect", "removed friendId=$id")
                } else {
                    selectedFriendIds.add(id)
                    selectedFriends.add(friend)
                    Log.d("FriendSelect", "added friendId=$id")
                }

                Log.d("FriendSelect", "selectedFriendIds=$selectedFriendIds")
                Log.d("FriendSelect", "selectedFriends=${selectedFriends.map { it.nickname }}")

                updateFriendSummary()
                submitDialogFriends()
            },
            onFavoriteClick = { friend ->
                friend.isFavorite = !friend.isFavorite
                submitDialogFriends()
            },
            hideFavoriteDivider = true,
            enableSelectionGray = true
        )

        popupBinding.rvFriends.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = friendsAdapter
        }

        // 드롭다운 최소/최대 높이 제한
        val maxHeightDp = 280
        val minHeightDp = 50
        val density = resources.displayMetrics.density
        val maxHeightPx = (maxHeightDp * density).toInt()
        val minHeightPx = (minHeightDp * density).toInt()

        // RecyclerView 높이 제한 (최대 높이까지, 내용이 적으면 최소 높이 유지)
        popupBinding.root.minimumHeight = minHeightPx
        val params = popupBinding.rvFriends.layoutParams
        params.height = maxHeightPx
        popupBinding.rvFriends.layoutParams = params
        popupBinding.rvFriends.isNestedScrollingEnabled = true

        popupWindow = PopupWindow(
            popupBinding.root,
            binding.layoutFriendSelect.width,
            maxHeightPx,
            true
        ).apply {
            isOutsideTouchable = true
            isFocusable = true
            elevation = 0f

            setOnDismissListener {
                // 닫히면 다시 기본 배경
                binding.layoutFriendSelect.setBackgroundResource(
                    R.drawable.bg_dropdown_closed
                )
            }
        }

        popupWindow?.showAsDropDown(binding.layoutFriendSelect, 0, -1)

        submitDialogFriends()
    }

}
