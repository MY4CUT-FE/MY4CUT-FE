package com.umc.mobile.my4cut.ui.space

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
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.umc.mobile.my4cut.R
import com.umc.mobile.my4cut.data.invitation.model.WorkspaceInviteRequestDto
import com.umc.mobile.my4cut.data.network.RetrofitClient
import com.umc.mobile.my4cut.databinding.DialogSpaceAddFriendBinding
import com.umc.mobile.my4cut.databinding.PopupFriendListBinding
import com.umc.mobile.my4cut.ui.friend.Friend
import com.umc.mobile.my4cut.ui.friend.FriendUiItem
import com.umc.mobile.my4cut.ui.friend.FriendsAdapter
import com.umc.mobile.my4cut.ui.friend.FriendsMode
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.text.Collator
import java.util.Locale

class AddSpaceFriendDialogFragment : DialogFragment() {

    private var _binding: DialogSpaceAddFriendBinding? = null
    private val binding get() = _binding!!

    private var popupWindow: PopupWindow? = null
    private lateinit var friendsAdapter: FriendsAdapter

    /** 선택한 친구 */
    private val selectedFriends = mutableListOf<Friend>()
    private val selectedFriendIds = mutableSetOf<Long>()

    /** 전체 친구 목록 */
    private val friendList = mutableListOf<Friend>()

    /** 스페이스 정보 */
    private var spaceId: Long = -1L
    private val originalMemberIds = mutableSetOf<Long>()

    /** 초대 완료 콜백 */
    private var onInviteCompleteListener: (() -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let { bundle ->
            spaceId = bundle.getLong(ARG_SPACE_ID, -1L)

            originalMemberIds.addAll(
                bundle.getLongArray(ARG_SPACE_MEMBER_IDS)
                    ?.toList()
                    ?: emptyList()
            )
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        dialog?.window?.setBackgroundDrawable(
            ColorDrawable(Color.TRANSPARENT)
        )

        _binding = DialogSpaceAddFriendBinding.inflate(
            inflater,
            container,
            false
        )

        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        // 친구 목록 조회
        loadFriendsFromApi()

        // 드롭다운 기본 배경
        binding.layoutFriendSelect.setBackgroundResource(
            R.drawable.bg_dropdown_closed
        )

        // 친구 선택 드롭다운
        binding.layoutFriendSelect.setOnClickListener {

            if (popupWindow?.isShowing == true) {
                popupWindow?.dismiss()
            } else {
                binding.root.postDelayed({
                    if (_binding != null) {
                        showFriendPopup()
                    }
                }, 150)
            }
        }

        expandTouchArea()
        updateFriendSummary()

        // 닫기 버튼
        binding.layoutClose.setOnClickListener {
            dismiss()
        }

        // 초대 버튼
        binding.mainText.text = "확인"
        binding.mainText.isClickable = true
        binding.mainText.isFocusable = true

        binding.mainText.setOnClickListener {
            inviteMembers()
        }
    }

    /**
     * 선택한 친구 이름 요약
     */
    private fun updateFriendSummary() {

        when (selectedFriends.size) {

            0 -> {
                binding.tvFriendSummary.text = "친구 선택"
                binding.tvFriendSummary.setTextColor(
                    Color.parseColor("#D9D9D9")
                )
            }

            1 -> {
                binding.tvFriendSummary.text =
                    selectedFriends.first().nickname

                binding.tvFriendSummary.setTextColor(
                    Color.parseColor("#1A1A1A")
                )
            }

            else -> {
                val first = selectedFriends.first().nickname

                binding.tvFriendSummary.text =
                    "$first 외 ${selectedFriends.size - 1}명"

                binding.tvFriendSummary.setTextColor(
                    Color.parseColor("#1A1A1A")
                )
            }
        }
    }

    /**
     * 친구 선택 팝업
     */
    private fun showFriendPopup() {

        val popupBinding =
            PopupFriendListBinding.inflate(layoutInflater)

        popupBinding.root.setBackgroundResource(
            R.drawable.bg_dropdown_popup
        )

        binding.layoutFriendSelect.setBackgroundResource(
            R.drawable.bg_dropdown_open
        )

        friendsAdapter = FriendsAdapter(
            getMode = { FriendsMode.NORMAL },

            isSelected = { id: Long ->
                selectedFriendIds.contains(id)
            },

            onFriendClick = { friend ->

                val id = friend.friendId

                if (selectedFriendIds.contains(id)) {

                    selectedFriendIds.remove(id)

                    selectedFriends.removeAll {
                        it.friendId == id
                    }

                } else {

                    selectedFriendIds.add(id)
                    selectedFriends.add(friend)
                }

                updateFriendSummary()
                submitDialogFriends()
            },

            onFavoriteClick = {
                it.isFavorite = !it.isFavorite
                submitDialogFriends()
            },

            hideFavoriteDivider = true,
            enableSelectionGray = true
        )

        popupBinding.rvFriends.apply {
            layoutManager = LinearLayoutManager(
                requireContext()
            )

            adapter = friendsAdapter
        }

        popupBinding.rvFriends.setPadding(
            0,
            popupBinding.rvFriends.paddingTop,
            0,
            popupBinding.rvFriends.paddingBottom
        )

        popupBinding.rvFriends.clipToPadding = false

        // 드롭다운 높이
        val maxHeightDp = 290
        val minHeightDp = 50

        val density = resources.displayMetrics.density

        val maxHeightPx =
            (maxHeightDp * density).toInt()

        val minHeightPx =
            (minHeightDp * density).toInt()

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

            inputMethodMode =
                PopupWindow.INPUT_METHOD_NOT_NEEDED

            elevation = 0f

            setOnDismissListener {
                _binding?.layoutFriendSelect
                    ?.setBackgroundResource(
                        R.drawable.bg_dropdown_closed
                    )
            }
        }

        popupWindow?.showAsDropDown(
            binding.layoutFriendSelect,
            0,
            -1
        )

        submitDialogFriends()
    }

    /**
     * 친구 목록 정렬 및 표시
     */
    private fun submitDialogFriends() {

        if (!::friendsAdapter.isInitialized) return

        // 기존 참여자 제외
        val inviteAvailableFriends = friendList.filterNot { friend ->
            originalMemberIds.contains(friend.userId)
        }

        val collator = Collator.getInstance(Locale.KOREAN)

        // 즐겨찾기 친구
        val favorites = inviteAvailableFriends
            .filter { it.isFavorite }
            .sortedWith { a, b ->
                collator.compare(a.nickname, b.nickname)
            }

        // 일반 친구
        val normals = inviteAvailableFriends
            .filter { !it.isFavorite }
            .sortedWith { a, b ->
                collator.compare(a.nickname, b.nickname)
            }

        val uiItems = buildList<FriendUiItem> {

            favorites.forEach {
                add(FriendUiItem.Item(it))
            }

            normals.forEach {
                add(FriendUiItem.Item(it))
            }
        }

        friendsAdapter.submitList(uiItems)
    }

    /**
     * 친구 목록 API 조회
     */
    private fun loadFriendsFromApi() {

        viewLifecycleOwner.lifecycleScope.launch {

            try {

                val response =
                    RetrofitClient.friendService.getFriends()

                val data = response.data ?: return@launch

                friendList.clear()
                selectedFriends.clear()
                selectedFriendIds.clear()

                friendList.addAll(
                    data.map {
                        Friend(
                            friendId = it.friendId,
                            userId = it.userId,
                            nickname = it.nickname,
                            isFavorite = it.isFavorite,
                            profileImageUrl = it.profileImageUrl
                        )
                    }
                )

                Log.d(
                    "AddSpaceFriend",
                    "loadedFriends=${friendList.size}, " +
                            "excludedUserIds=$originalMemberIds"
                )

                updateFriendSummary()

                if (::friendsAdapter.isInitialized) {
                    submitDialogFriends()
                }

            } catch (e: Exception) {

                Log.e(
                    "AddSpaceFriend",
                    "친구 목록 API 실패",
                    e
                )

                Toast.makeText(
                    requireContext(),
                    "친구 목록 불러오기에 실패했습니다. 다시 시도해 주세요.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    /**
     * 친구 초대 API
     */
    private fun inviteMembers() {

        // 초대할 친구가 없는 경우
        if (selectedFriends.isEmpty()) {

            Toast.makeText(
                requireContext(),
                "초대할 친구를 선택해 주세요.",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        if (spaceId <= 0L) {
            Toast.makeText(
                requireContext(),
                "스페이스 정보를 확인할 수 없어요.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        // 초대 API에는 friendId가 아닌 userId 전달
        val inviteUserIds = selectedFriends
            .map { it.userId }
            .distinct()

        // 중복 요청 방지
        binding.mainText.isEnabled = false

        viewLifecycleOwner.lifecycleScope.launch {

            try {

                RetrofitClient.workspaceService.inviteMembers(
                    WorkspaceInviteRequestDto(
                        workspaceId = spaceId,
                        userIds = inviteUserIds
                    )
                )

                Toast.makeText(
                    requireContext(),
                    "초대를 전송했어요.",
                    Toast.LENGTH_SHORT
                ).show()

                // 초대 완료 후 관리 화면 갱신
                onInviteCompleteListener?.invoke()

                dismiss()

            } catch (e: HttpException) {

                Log.e(
                    "AddSpaceFriend",
                    "친구 초대 실패",
                    e
                )

                val errorBody =
                    e.response()?.errorBody()?.string()

                when {

                    errorBody?.contains("\"code\":\"W4003\"") == true ||
                            errorBody?.contains("\"code\": \"W4003\"") == true -> {

                        Toast.makeText(
                            requireContext(),
                            "한 번에 최대 9명까지만 초대할 수 있어요.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                    e.code() == 400 -> {

                        Toast.makeText(
                            requireContext(),
                            "친구 초대 요청을 확인해 주세요.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                    else -> {

                        Toast.makeText(
                            requireContext(),
                            "친구 초대에 실패했습니다. 다시 시도해 주세요.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

            } catch (e: Exception) {

                Log.e(
                    "AddSpaceFriend",
                    "친구 초대 실패",
                    e
                )

                Toast.makeText(
                    requireContext(),
                    "친구 초대에 실패했습니다. 다시 시도해 주세요.",
                    Toast.LENGTH_SHORT
                ).show()

            } finally {
                _binding?.mainText?.isEnabled = true
            }
        }
    }

    /**
     * 친구 선택 영역 터치 범위 확대
     */
    private fun expandTouchArea() {

        binding.layoutFriendSelect.post {

            val parent = binding.root as ViewGroup
            val rect = Rect()

            binding.layoutFriendSelect.getHitRect(rect)

            val density = resources.displayMetrics.density

            rect.inset(
                (-20 * density).toInt(),
                (-12 * density).toInt()
            )

            parent.touchDelegate = TouchDelegate(
                rect,
                binding.layoutFriendSelect
            )
        }
    }

    /**
     * 모달 크기
     */
    override fun onStart() {
        super.onStart()

        dialog?.window?.apply {

            setLayout(
                (resources.displayMetrics.widthPixels * 0.9).toInt(),
                ViewGroup.LayoutParams.WRAP_CONTENT
            )

            setBackgroundDrawable(
                ColorDrawable(Color.TRANSPARENT)
            )
        }
    }

    override fun onDestroyView() {

        popupWindow?.dismiss()
        popupWindow = null

        _binding = null

        super.onDestroyView()
    }

    /**
     * 초대 완료 콜백
     */
    fun setOnInviteCompleteListener(
        listener: () -> Unit
    ) {
        onInviteCompleteListener = listener
    }

    companion object {

        private const val ARG_SPACE_ID = "arg_space_id"
        private const val ARG_SPACE_MEMBER_IDS = "arg_space_member_ids"

        fun newInstance(
            spaceId: Long,
            memberIds: List<Long>
        ): AddSpaceFriendDialogFragment {

            return AddSpaceFriendDialogFragment().apply {

                arguments = Bundle().apply {

                    putLong(
                        ARG_SPACE_ID,
                        spaceId
                    )

                    putLongArray(
                        ARG_SPACE_MEMBER_IDS,
                        memberIds.toLongArray()
                    )
                }
            }
        }
    }
}