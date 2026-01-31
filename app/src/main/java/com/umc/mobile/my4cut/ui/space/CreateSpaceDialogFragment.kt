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
import com.umc.mobile.my4cut.databinding.DialogCreateSpaceBinding
import com.umc.mobile.my4cut.databinding.PopupFriendListBinding

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

    /** 선택 상태 관리 (어댑터용) */
    private val selectedFriendIds = mutableSetOf<Int>()

    /** 임시 친구 목록 */
    private val friendList = listOf(
        Friend(1, "아몬드", true),
        Friend(2, "유복치", true),
        Friend(3, "네버"),
        Friend(4, "모모")
    )

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

        updateFriendSummary()

        // X 버튼 → 다이얼로그 닫기
        binding.ivClose.setOnClickListener {
            dismiss()
        }

        // 확인 버튼 → 선택된 친구로 스페이스 생성
        binding.mainText.setOnClickListener {
            val spaceName = binding.etSpaceName.text.toString().trim()

            onConfirmListener?.invoke(
                CreateSpaceResult(
                    spaceName = spaceName,
                    currentMember = selectedFriends.size + 1,
                    maxMember = 10
                )
            )
            dismiss()
        }
    }

    /** 상단 요약 텍스트 업데이트 */
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
                val count = selectedFriends.size - 1
                binding.tvFriendSummary.text = "$first 외 ${count}명"
                binding.tvFriendSummary.setTextColor(Color.parseColor("#1A1A1A"))
            }
        }
    }

    /** 친구 선택 팝업 */
    private fun showFriendPopup() {
        val popupBinding = PopupFriendListBinding.inflate(layoutInflater)

        popupBinding.root.setBackgroundResource(R.drawable.bg_dropdown_popup)

        // 상단 박스: 열림 상태 배경
        binding.layoutFriendSelect.setBackgroundResource(
            R.drawable.bg_dropdown_open
        )

        friendsAdapter = FriendsAdapter(
            getMode = { FriendsMode.NORMAL },
            isSelected = { id -> selectedFriendIds.contains(id) },
            onFriendClick = { friend ->
                val id = friend.id
                if (selectedFriendIds.contains(id)) {
                    selectedFriendIds.remove(id)
                    selectedFriends.removeAll { it.id == id }
                } else {
                    selectedFriendIds.add(id)
                    selectedFriends.add(friend)
                }
                updateFriendSummary()
                friendsAdapter.notifyDataSetChanged()
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

        popupWindow = PopupWindow(
            popupBinding.root,
            binding.layoutFriendSelect.width,
            ViewGroup.LayoutParams.WRAP_CONTENT,
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
                (resources.displayMetrics.heightPixels * 0.7).toInt()
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
}