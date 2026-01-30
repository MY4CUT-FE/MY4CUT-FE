package com.umc.mobile.my4cut.ui.mypage

import android.app.Activity.RESULT_OK
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.umc.mobile.my4cut.databinding.FragmentMyPageBinding
import com.umc.mobile.my4cut.ui.intro.IntroActivity
import com.umc.mobile.my4cut.ui.notification.NotificationActivity

class MyPageFragment : Fragment() {

    private var _binding: FragmentMyPageBinding? = null
    private val binding get() = _binding!!

    // 프로필 수정 화면에서 돌아올 때 데이터를 받기 위한 런처
    private val editProfileLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val newNickname = result.data?.getStringExtra("nickname")
            // 받아온 닉네임으로 화면 갱신
            if (!newNickname.isNullOrEmpty()) {
                binding.tvNickname.text = newNickname
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMyPageBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initClickListener()
        setupUsageText() //  텍스트 색상 변경 함수 호출
    }

    // "이번 달 11장의..." 텍스트 중 숫자만 색상 변경하는 함수
    private fun setupUsageText() {
        val count = 11 // 실제로는 서버나 DB에서 받아올 숫자
        val fullText = "이번 달 ${count}장의 네컷을\n찍었어요!"

        val spannable = SpannableStringBuilder(fullText)
        val colorColor = Color.parseColor("#FF7E67") // 코랄색

        // 숫자 부분("11")의 시작과 끝 인덱스 찾기
        val start = fullText.indexOf(count.toString())
        val end = start + count.toString().length

        // 해당 부분에만 색상 적용
        spannable.setSpan(
            ForegroundColorSpan(colorColor),
            start,
            end,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        binding.tvCountInfo.text = spannable
    }

    private fun initClickListener() {
        binding.ivNotification.setOnClickListener {
            startActivity(Intent(requireContext(), NotificationActivity::class.java))
        }

        // 프로필 수정 화면으로 이동할 때 런처 사용
        val editProfileListener = View.OnClickListener {
            val intent = Intent(requireContext(), EditProfileActivity::class.java)
            // 현재 닉네임을 넘겨주면 수정 화면 초기값으로 쓸 수 있음
            intent.putExtra("nickname", binding.tvNickname.text.toString())
            editProfileLauncher.launch(intent)
        }
        binding.ivProfile.setOnClickListener(editProfileListener)
        binding.ivEditProfile.setOnClickListener(editProfileListener)

        binding.btnCopyCode.setOnClickListener {
            val code = binding.tvCodeValue.text.toString()
            val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("UserCode", code)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(requireContext(), "코드가 복사되었습니다.", Toast.LENGTH_SHORT).show()
        }

        binding.btnLogout.setOnClickListener {
            CustomDialog(requireContext(), "정말 로그아웃하시겠어요?", "다시 이용하려면 로그인이 필요해요.", "로그아웃") {
                val intent = Intent(requireContext(), IntroActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
            }.show()
        }

        binding.btnWithdraw.setOnClickListener {
            CustomDialog(requireContext(), "정말 탈퇴하시겠어요?", "탈퇴 시 모든 데이터가 삭제되며, 복구할 수 없어요.", "탈퇴") {
                val intent = Intent(requireContext(), IntroActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
            }.show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}