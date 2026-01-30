package com.umc.mobile.my4cut.ui.friend

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

    private var _binding: DialogAddFriendBinding? = null
    private val binding get() = _binding!!

    /** 현재 검색 상태 */
    private var isSearchResultVisible = false

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

    /** 친구 검색 */
    private fun searchFriend() {
        val inputCode = binding.etFriendCode.text.toString().trim()

        if (inputCode.isEmpty()) {
            binding.etFriendCode.error = "코드를 입력해주세요"
            return
        }

        // TODO: 실제 API 연동
        // 지금은 더미 성공 처리
        showSearchResult(
            nickname = "화운"
        )
    }

    /** 검색 결과 표시 */
    private fun showSearchResult(nickname: String) {
        binding.layoutResult.visibility = View.VISIBLE
        binding.tvResultName.text = nickname
        binding.btnAction.text = "추가"
        isSearchResultVisible = true
    }

    /** 친구 추가 */
    private fun addFriend() {
        val nickname = binding.tvResultName.text.toString()

        // TODO: 실제 친구 추가 API
        // 현재는 로그 + 닫기
        dismiss()
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