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
import com.bumptech.glide.Glide
import com.umc.mobile.my4cut.databinding.FragmentMyPageBinding
import com.umc.mobile.my4cut.ui.intro.IntroActivity
import com.umc.mobile.my4cut.ui.notification.NotificationActivity

class MyPageFragment : Fragment() {

    private var _binding: FragmentMyPageBinding? = null
    private val binding get() = _binding!!

    // 프로필 수정 화면에서 돌아올 때 데이터를 받기 위한 런처
    private val editProfileLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val data = result.data

            // 1. 닉네임 갱신
            val newNickname = data?.getStringExtra("nickname")
            if (!newNickname.isNullOrEmpty()) {
                binding.tvNickname.text = newNickname
            }

            // 2. 프로필 사진 갱신
            val profileImageUri = data?.getStringExtra("profile_image")
            if (!profileImageUri.isNullOrEmpty()) {
                Glide.with(this)
                    .load(profileImageUri)
                    .circleCrop()
                    .into(binding.ivProfile)
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

    // 뷰가 생성된 후 리스너와 초기 화면 설정을 실행
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initClickListener() // 클릭 리스너 연결
        setupUsageText()    // 텍스트 설정
    }

    override fun onResume() {
        super.onResume()
        // 저장된 닉네임 불러오기
        val sharedPref = requireContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        val savedNickname = sharedPref.getString("nickname", "닉네임")

        binding.tvNickname.text = savedNickname
    }

    private fun setupUsageText() {
        val count = 11
        val fullText = "이번 달 ${count}장의 네컷을\n찍었어요!"

        val spannable = SpannableStringBuilder(fullText)
        val colorColor = Color.parseColor("#FF7E67")

        val start = fullText.indexOf(count.toString())
        val end = start + count.toString().length

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

        // 프로필 수정 화면으로 이동
        val editProfileListener = View.OnClickListener {
            val intent = Intent(requireContext(), EditProfileActivity::class.java)
            intent.putExtra("nickname", binding.tvNickname.text.toString())
            editProfileLauncher.launch(intent)
        }

        // 아이콘과 이미지 모두에 리스너 적용
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