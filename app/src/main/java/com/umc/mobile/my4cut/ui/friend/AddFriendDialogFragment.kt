package com.umc.mobile.my4cut.ui.friend

import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.umc.mobile.my4cut.network.RetrofitClient
import com.umc.mobile.my4cut.data.friend.model.FriendRequestCreateDto
import com.umc.mobile.my4cut.data.user.model.UserMeResponse

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.Paint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import com.umc.mobile.my4cut.databinding.DialogAddFriendBinding

class AddFriendDialogFragment : DialogFragment() {

    companion object {
        const val RESULT_ADD_FRIEND = "result_add_friend"
        const val KEY_FRIEND_NICKNAME = "key_friend_nickname"
    }

    private var _binding: DialogAddFriendBinding? = null
    private val binding get() = _binding!!

    /** 현재 검색 상태 */
    private var isSearchResultVisible = false

    /** 검색된 친구 userId 저장 */
    private var searchedUserId: Long? = null
    /** 검색에 사용한 친구 코드 저장 */
    private var searchedFriendCode: String? = null

    /** 내 친구 코드 저장 */
    private var myFriendCode: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        _binding = DialogAddFriendBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initView()
        initClickListeners()
        loadMyCode()
        binding.tvMyCode.paintFlags =
            binding.tvMyCode.paintFlags or Paint.UNDERLINE_TEXT_FLAG
    }

    private fun initView() {
        // 초기 상태
        binding.layoutResult.visibility = View.GONE
        binding.btnAction.text = "검색"
    }

    private fun initClickListeners() {

        // X 버튼 → 닫기
        binding.ivClose.setOnClickListener {
            dismiss()
        }

        // 하단 버튼 (검색 / 추가)
        binding.btnAction.setOnClickListener {
            if (!isSearchResultVisible) {
                searchFriend()
            } else {
                addFriend()
            }
        }

        // 내 코드 복사
        binding.tvMyCode.setOnClickListener {
            val code = binding.tvMyCode.text.toString()

            val clipboard = requireContext()
                .getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("friend_code", code)
            clipboard.setPrimaryClip(clip)

            Toast.makeText(requireContext(), "코드가 복사되었어요", Toast.LENGTH_SHORT).show()
        }
    }

    /** 내 코드 조회 */
    private fun loadMyCode() {
        lifecycleScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    RetrofitClient.userService.getMyPage().execute()
                }

                val me = response.body()?.data
                if (response.isSuccessful && me?.friendCode != null) {
                    myFriendCode = me.friendCode
                    binding.tvMyCode.text = me.friendCode
                } else {
                    Toast.makeText(requireContext(), "내 코드를 불러오지 못했어요", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "내 코드를 불러오지 못했어요", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /** 친구 검색 */
    private fun searchFriend() {
        val inputCode = binding.etFriendCode.text.toString().trim()

        // 자기 자신 코드 입력 방지
        if (inputCode == myFriendCode) {
            Toast.makeText(requireContext(), "자기 자신은 친구신청할 수 없어요", Toast.LENGTH_SHORT).show()
            return
        }

        if (inputCode.isEmpty()) {
            binding.etFriendCode.error = "코드를 입력해주세요"
            return
        }

        lifecycleScope.launch {
            try {
                val response =
                    RetrofitClient.friendService.searchFriendByCode(inputCode)

                if (response.data != null) {
                    searchedUserId = response.data.userId
                    searchedFriendCode = inputCode
                    showSearchResult(
                        nickname = response.data.nickname
                    )
                } else {
                    Toast.makeText(
                        requireContext(),
                        response.message,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    requireContext(),
                    "네트워크 오류가 발생했어요",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    /** 검색 결과 표시 */
    private fun showSearchResult(nickname: String) {
        binding.layoutResult.visibility = View.VISIBLE
        binding.tvResultName.text = nickname
        binding.btnAction.text = "추가"

        // 검색 결과가 보일 때는 내 코드 영역 숨김
        binding.tvMyCodeLabel.visibility = View.GONE
        binding.tvMyCode.visibility = View.GONE

        isSearchResultVisible = true
    }

    /** 친구 요청 보내기 */
    private fun addFriend() {
        val friendCode = searchedFriendCode ?: return

        lifecycleScope.launch {
            try {
                val requestDto = FriendRequestCreateDto(targetFriendCode = friendCode)
                val response = RetrofitClient.friendService.requestFriend(requestDto)

                if (response.code.startsWith("C2")) {
                    Toast.makeText(requireContext(), "친구 요청을 보냈어요", Toast.LENGTH_SHORT).show()
                    dismiss()
                } else {
                    Toast.makeText(requireContext(), response.message, Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "네트워크 오류가 발생했어요", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.9).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}