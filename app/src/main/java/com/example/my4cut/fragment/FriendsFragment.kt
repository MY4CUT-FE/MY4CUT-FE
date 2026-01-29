package com.example.my4cut.fragment

import com.example.my4cut.fragment.AddFriendDialogFragment

import FriendUiItem
import FriendsAdapter
import FriendsMode
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.my4cut.Friend
import com.example.my4cut.R
import com.example.my4cut.databinding.FragmentFriendsBinding

class FriendsFragment : Fragment(R.layout.fragment_friends) {

    private lateinit var binding: FragmentFriendsBinding
    private lateinit var friendsAdapter: FriendsAdapter

    // 상태
    private var friendsMode: FriendsMode = FriendsMode.NORMAL
    private val selectedFriendIds = mutableSetOf<Int>()
    // 전체 친구 데이터(상태)
    private val allFriends = mutableListOf<Friend>()

    // ㄱㄴㄷ 인덱스
    private val indexChars = listOf(
        "★","ㄱ","ㄴ","ㄷ","ㄹ","ㅁ","ㅂ","ㅅ",
        "ㅇ","ㅈ","ㅊ","ㅋ","ㅌ","ㅍ","ㅎ"
    )


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentFriendsBinding.bind(view)

        setupAdapter()
        setupHeaderClicks()
        updateHeaderUi()
        initFriends()
        setupIndexScroller()
    }

    // 최초 친구 데이터 초기화(더미)
    private fun initFriends() {
        allFriends.clear()
        allFriends.addAll(
            listOf(
                Friend(1, "네버"),
                Friend(2, "모모"),
                Friend(3, "아몬드", true),
                Friend(4, "예디"),
                Friend(5, "유메"),
                Friend(6, "유복치", true),
                Friend(7, "이마빡"),
                Friend(8, "화운"),
                Friend(9, "에블린"),
            ).sortedBy { it.nickname }
        )
        submitFriends()
    }

    // allFriends에서 UI 리스트 생성 및 어댑터에 반영
    private fun submitFriends() {
        val favorites = allFriends.filter { it.isFavorite }
        val normalFriends = allFriends.filter { !it.isFavorite }
        val uiItems = buildList<FriendUiItem> {
            if (favorites.isNotEmpty()) {
                add(FriendUiItem.Header("즐겨찾기"))
                favorites.forEach { add(FriendUiItem.Item(it)) }
            }
            add(FriendUiItem.Header("친구 목록"))
            normalFriends.forEach { add(FriendUiItem.Item(it)) }
        }
        friendsAdapter.submitList(uiItems)
    }

    private fun setupAdapter() {
        friendsAdapter = FriendsAdapter(
            getMode = { friendsMode },
            isSelected = { id -> selectedFriendIds.contains(id) },
            onFriendClick = ::onFriendItemClicked,
            onFavoriteClick = ::onFavoriteToggled,
            hideFavoriteDivider = false,
            enableSelectionGray = false
        )

        binding.rvFriends.apply {
            adapter = friendsAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    private fun setupHeaderClicks() {
        // "편집" 버튼을 토글로 사용 (NORMAL <-> EDIT)
        binding.tvFriendsEdit.setOnClickListener {
            friendsMode = if (friendsMode == FriendsMode.NORMAL) {
                selectedFriendIds.clear()
                FriendsMode.EDIT
            } else {
                selectedFriendIds.clear()
                FriendsMode.NORMAL
            }
            updateHeaderUi()
            friendsAdapter.notifyDataSetChanged()
        }

        binding.tvFriendsSelectClear.setOnClickListener {
            if (friendsMode == FriendsMode.EDIT) {
                selectedFriendIds.clear()
                friendsAdapter.notifyDataSetChanged()
                updateEditHeaderState()
            }
        }

        // (있으면) 친구 추가
        binding.tvFriendsAdd.setOnClickListener {
            val dialog = AddFriendDialogFragment()
            dialog.show(parentFragmentManager, "AddFriendDialog")
        }

        // 친구 삭제
        binding.tvFriendsDelete.setOnClickListener {
            if (friendsMode == FriendsMode.EDIT) {
                val dialog = DeleteFriendDialogFragment {
                    // Remove selected friends from allFriends
                    allFriends.removeAll { friend -> selectedFriendIds.contains(friend.id) }
                    selectedFriendIds.clear()
                    submitFriends()
                    friendsMode = FriendsMode.NORMAL
                    updateHeaderUi()
                    friendsAdapter.notifyDataSetChanged()
                }
                dialog.show(parentFragmentManager, "DeleteFriendDialog")
            }
        }
    }

    private fun setupIndexScroller() {
        binding.layoutIndexScroller.removeAllViews()

        // 글자 뷰 생성
        indexChars.forEach { char ->
            val tv = TextView(requireContext()).apply {
                text = char
                textSize = 10f
                setTextColor(Color.parseColor("#9E9E9E"))
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    0,
                    1f
                )
            }
            binding.layoutIndexScroller.addView(tv)
        }

        // 핵심: 전체 영역 터치 위치로 인덱스 계산
        binding.layoutIndexScroller.setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_DOWN ||
                event.action == MotionEvent.ACTION_MOVE
            ) {
                val height = v.height.coerceAtLeast(1)
                val y = event.y.coerceIn(0f, height.toFloat() - 1f)

                val index = ((y / height) * indexChars.size)
                    .toInt()
                    .coerceIn(0, indexChars.lastIndex)

                scrollToChar(indexChars[index])
                true
            } else {
                false
            }
        }
    }

    private fun scrollToChar(char: String) {
        val position = friendsAdapter.findFirstPositionByInitial(char)
        if (position != -1) {
            (binding.rvFriends.layoutManager as LinearLayoutManager)
                .scrollToPositionWithOffset(position, 0)
        }
    }

    private fun updateHeaderUi() {
        when (friendsMode) {
            FriendsMode.NORMAL -> {
                // 기본: 추가/편집 보이기
                binding.tvFriendsAdd.visibility = View.VISIBLE
                binding.tvFriendsEdit.visibility = View.VISIBLE
                binding.tvFriendsEdit.text = "편집"
                binding.tvFriendsSelectClear.visibility = View.GONE
                binding.tvFriendsDelete.visibility = View.GONE
                binding.tvFriendsEdit.setTextColor(
                    ContextCompat.getColor(requireContext(), R.color.black)
                )
            }

            FriendsMode.EDIT -> {
                binding.tvFriendsAdd.visibility = View.GONE
                binding.tvFriendsEdit.visibility = View.VISIBLE
                binding.tvFriendsEdit.text = "취소"
                binding.tvFriendsSelectClear.visibility = View.VISIBLE
                binding.tvFriendsDelete.visibility = View.VISIBLE
                updateEditHeaderState()
            }
        }
    }

    private fun onFriendItemClicked(friend: Friend) {
        when (friendsMode) {
            FriendsMode.NORMAL -> {
                // NORMAL 모드 클릭 동작(필요하면 여기서)
            }

            FriendsMode.EDIT -> {
                val id = friend.id
                if (selectedFriendIds.contains(id)) selectedFriendIds.remove(id)
                else selectedFriendIds.add(id)

                // 선택 상태만 갱신
                friendsAdapter.notifyDataSetChanged()
                updateEditHeaderState()
            }
        }
    }

    private fun updateEditHeaderState() {
        val disabledColor = ContextCompat.getColor(requireContext(), R.color.text_disabled)

        // 완료 버튼 항상 비활성 색
        binding.tvFriendsEdit.setTextColor(disabledColor)

        // 나머지 편집 액션들은 항상 비활성 색
        binding.tvFriendsSelectClear?.setTextColor(disabledColor)
        binding.tvFriendsDelete?.setTextColor(disabledColor)
    }

    private fun onFavoriteToggled(friend: Friend) {
        // 즐겨찾기 상태 토글
        friend.isFavorite = !friend.isFavorite
        // 전체 리스트에서 다시 반영
        submitFriends()
    }
}