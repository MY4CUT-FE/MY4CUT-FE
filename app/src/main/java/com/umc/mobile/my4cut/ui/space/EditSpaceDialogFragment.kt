package com.umc.mobile.my4cut.ui.space

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

class EditSpaceDialogFragment : DialogFragment() {

    private var _binding: DialogCreateSpaceBinding? = null
    private val binding get() = _binding!!

    private var popupWindow: PopupWindow? = null
    private lateinit var friendsAdapter: FriendsAdapter

    /** 선택된 친구 */
    private val selectedFriends = mutableListOf<Friend>()
    private val selectedFriendIds = mutableSetOf<Int>()

    /** 기존 스페이스 정보 (더미) */
    private val originalSpaceName = "1104 네컷"

    /** 전체 친구 목록 (더미) */
    private val friendList = mutableListOf(
        Friend(1, "아몬드", true),
        Friend(2, "유복치", true),
        Friend(3, "네버"),
        Friend(4, "모모"),
        Friend(5, "예디")
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

        /** 기존 이름 세팅 */
        binding.etSpaceName.setText(originalSpaceName)

        /** 기존 멤버 미리 선택 (예시) */
        friendList.filter { it.isFavorite }.forEach {
            selectedFriends.add(it)
            selectedFriendIds.add(it.id)
        }

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
        binding.mainText.setOnClickListener {
            updateSpace()
            dismiss()
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
            isSelected = { selectedFriendIds.contains(it) },
            onFriendClick = { friend ->
                if (selectedFriendIds.contains(friend.id)) {
                    selectedFriendIds.remove(friend.id)
                    selectedFriends.removeAll { it.id == friend.id }
                } else {
                    selectedFriendIds.add(friend.id)
                    selectedFriends.add(friend)
                }
                updateFriendSummary()
                friendsAdapter.notifyDataSetChanged()
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
                binding.layoutFriendSelect
                    .setBackgroundResource(R.drawable.bg_dropdown_closed)
            }
        }

        popupWindow?.showAsDropDown(binding.layoutFriendSelect)
        submitDialogFriends()
    }

    private fun submitDialogFriends() {
        val favorites = friendList.filter { it.isFavorite }
        val normals = friendList.filter { !it.isFavorite }

        val uiItems = buildList {
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
        val newName = binding.etSpaceName.text.toString()
        Log.d("EditSpace", "name=$newName")
        selectedFriends.forEach {
            Log.d("EditSpace", "member=${it.nickname}")
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

    override fun onStart() {
        super.onStart()
        dialog?.window?.apply {
            setLayout(
                (resources.displayMetrics.widthPixels * 0.9).toInt(),
                ViewGroup.LayoutParams.WRAP_CONTENT,
            )
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }
    }

    override fun onDestroyView() {
        popupWindow?.dismiss()
        _binding = null
        super.onDestroyView()
    }
}