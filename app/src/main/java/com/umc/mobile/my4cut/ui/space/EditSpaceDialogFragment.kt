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
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.umc.mobile.my4cut.ui.friend.Friend
import com.umc.mobile.my4cut.R
import com.umc.mobile.my4cut.databinding.PopupFriendListBinding
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import android.widget.Toast
import com.umc.mobile.my4cut.network.RetrofitClient
import com.umc.mobile.my4cut.data.workspace.model.WorkspaceUpdateRequestDto
import com.umc.mobile.my4cut.databinding.DialogChangeSpaceBinding

class EditSpaceDialogFragment : DialogFragment() {

    private var _binding: DialogChangeSpaceBinding? = null
    private val binding get() = _binding!!

    private var popupWindow: PopupWindow? = null
    private lateinit var friendsAdapter: FriendsAdapter

    /** 선택된 친구 */
    private val selectedFriends = mutableListOf<Friend>()
    private val selectedFriendIds = mutableSetOf<Long>()

    /** 기존 스페이스 정보 (초기화: arguments에서 전달) */
    private var spaceId: Long = -1L
    private var originalSpaceName: String = ""
    private val originalMemberIds = mutableSetOf<Long>()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let { bundle ->
            spaceId = bundle.getLong(ARG_SPACE_ID)
            originalSpaceName = bundle.getString(ARG_SPACE_NAME).orEmpty()
            originalMemberIds.addAll(
                bundle.getLongArray(ARG_SPACE_MEMBER_IDS)?.toList() ?: emptyList()
            )
        }
    }

    /** 전체 친구 목록 (API) */
    private val friendList = mutableListOf<Friend>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        _binding = DialogChangeSpaceBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        /** 기존 이름 세팅 */
        binding.etSpaceName.setText(originalSpaceName)

        // 친구 목록 로드 및 기존 멤버 반영
        loadFriendsFromApi()

        binding.layoutFriendSelect.setBackgroundResource(R.drawable.bg_dropdown_closed)

        binding.layoutFriendSelect.setOnClickListener {
            if (popupWindow?.isShowing == true) popupWindow?.dismiss()
            else showFriendPopup()
        }

        expandTouchArea()
        updateFriendSummary()

        binding.ivClose.setOnClickListener { dismiss() }

        /** 수정 완료 */
        binding.mainText.text = "확인"
        binding.mainText.isClickable = true
        binding.mainText.isFocusable = true

        binding.mainText.setOnClickListener {
            updateSpace()
        }
    }

    /** 친구 요약 */
    private fun updateFriendSummary() {
        when (selectedFriends.size) {
            0 -> {
                binding.tvFriendSummary.text = "친구 선택"
                binding.tvFriendSummary.setTextColor(Color.parseColor("#D9D9D9"))
            }
            1 -> {
                binding.tvFriendSummary.text = selectedFriends.first().nickname
                binding.tvFriendSummary.setTextColor(Color.parseColor("#1A1A1A"))
            }
            else -> {
                val first = selectedFriends.first().nickname
                binding.tvFriendSummary.text =
                    "$first 외 ${selectedFriends.size - 1}명"
                binding.tvFriendSummary.setTextColor(Color.parseColor("#1A1A1A"))
            }
        }
    }

    /** 친구 팝업 */
    private fun showFriendPopup() {
        val popupBinding = PopupFriendListBinding.inflate(layoutInflater)
        popupBinding.root.setBackgroundResource(R.drawable.bg_dropdown_popup)

        binding.layoutFriendSelect.setBackgroundResource(R.drawable.bg_dropdown_open)

        friendsAdapter = FriendsAdapter(
            getMode = { FriendsMode.NORMAL },
            isSelected = { id: Long -> selectedFriendIds.contains(id) },
            onFriendClick = { friend ->
                val id = friend.friendId
                if (selectedFriendIds.contains(id)) {
                    selectedFriendIds.remove(id)
                    selectedFriends.removeAll { it.friendId == id }
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
            layoutManager = LinearLayoutManager(requireContext())
            adapter = friendsAdapter
        }

        // 드롭다운 최소/최대 높이 제한 (CreateSpace와 동일)
        val maxHeightDp = 280
        val minHeightDp = 50
        val density = resources.displayMetrics.density
        val maxHeightPx = (maxHeightDp * density).toInt()
        val minHeightPx = (minHeightDp * density).toInt()

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
                binding.layoutFriendSelect
                    .setBackgroundResource(R.drawable.bg_dropdown_closed)
            }
        }

        popupWindow?.showAsDropDown(binding.layoutFriendSelect, 0, -1)
        submitDialogFriends()
    }

    private fun submitDialogFriends() {
        if (!::friendsAdapter.isInitialized) return

        val favorites = friendList.filter { it.isFavorite }
        val normals = friendList.filter { !it.isFavorite }

        val uiItems = mutableListOf<FriendUiItem>().apply {
            if (favorites.isNotEmpty()) {
                add(FriendUiItem.Header("즐겨찾기"))
                favorites.forEach { add(FriendUiItem.Item(it)) }
            }
            add(FriendUiItem.Header("친구 목록"))
            normals.forEach { add(FriendUiItem.Item(it)) }
        }

        friendsAdapter.submitList(uiItems)
    }

    /** 수정 로직 */
    private fun updateSpace() {
        val newName = binding.etSpaceName.text.toString().trim()

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = RetrofitClient.workspaceService.updateWorkspace(
                    workspaceId = spaceId,
                    request = WorkspaceUpdateRequestDto(
                        name = newName
                    )
                )
                Toast.makeText(requireContext(), "스페이스가 수정되었습니다", Toast.LENGTH_SHORT).show()

                // 수정 완료 후 화면 갱신 콜백 호출
                onEditCompleteListener?.invoke()

                // 다이얼로그 닫기
                dismiss()

            } catch (e: Exception) {
                Log.e("EditSpace", "스페이스 수정 실패", e)
                Toast.makeText(requireContext(), "스페이스 수정에 실패했습니다", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun expandTouchArea() {
        binding.layoutFriendSelect.post {
            val parent = binding.root as ViewGroup
            val rect = Rect()
            binding.layoutFriendSelect.getHitRect(rect)

            val d = resources.displayMetrics.density
            rect.inset((-20 * d).toInt(), (-12 * d).toInt())
            parent.touchDelegate = TouchDelegate(rect, binding.layoutFriendSelect)
        }
    }

    private fun loadFriendsFromApi() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = RetrofitClient.friendService.getFriends()
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
                            isFavorite = it.isFavorite
                        )
                    }
                )

                // 기존 스페이스 멤버 미리 선택
                friendList.forEach { friend ->
                    if (originalMemberIds.contains(friend.userId)) {
                        selectedFriendIds.add(friend.friendId)
                        selectedFriends.add(friend)
                    }
                }

                updateFriendSummary()
                if (::friendsAdapter.isInitialized) {
                    submitDialogFriends()
                }
            } catch (e: Exception) {
                Log.e("EditSpace", "친구 목록 API 실패", e)
                Toast.makeText(requireContext(), "친구 목록을 불러오지 못했습니다", Toast.LENGTH_SHORT).show()
            }
        }
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

    override fun onDestroyView() {
        popupWindow?.dismiss()
        _binding = null
        super.onDestroyView()
    }

    companion object {
        const val ARG_SPACE_ID = "arg_space_id"
        const val ARG_SPACE_NAME = "arg_space_name"
        const val ARG_SPACE_MEMBER_IDS = "arg_space_member_ids"

        fun newInstance(
            spaceId: Long,
            spaceName: String,
            memberIds: List<Long>
        ): EditSpaceDialogFragment {
            return EditSpaceDialogFragment().apply {
                arguments = Bundle().apply {
                    putLong(ARG_SPACE_ID, spaceId)
                    putString(ARG_SPACE_NAME, spaceName)
                    putLongArray(
                        ARG_SPACE_MEMBER_IDS,
                        memberIds.toLongArray()
                    )
                }
            }
        }
    }

    private var onEditCompleteListener: (() -> Unit)? = null

    fun setOnEditCompleteListener(listener: () -> Unit) {
        onEditCompleteListener = listener
    }
}