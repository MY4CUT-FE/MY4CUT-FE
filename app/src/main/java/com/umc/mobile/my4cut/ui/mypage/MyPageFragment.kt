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
import com.umc.mobile.my4cut.R
import com.umc.mobile.my4cut.data.base.BaseResponse
import com.umc.mobile.my4cut.data.user.model.UserMeResponse
import com.umc.mobile.my4cut.databinding.FragmentMyPageBinding
import com.umc.mobile.my4cut.network.RetrofitClient
import com.umc.mobile.my4cut.ui.intro.IntroActivity
import com.umc.mobile.my4cut.ui.notification.NotificationActivity
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MyPageFragment : Fragment() {

    private var _binding: FragmentMyPageBinding? = null
    private val binding get() = _binding!!

    /** 프로필 수정 후 결과 수신 */
    private val editProfileLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                loadMyPage() // 서버 기준으로 다시 조회
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMyPageBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initClickListener()
        loadMyPage()
    }

    /** 서버에서 마이페이지 정보 조회 */
    private fun loadMyPage() {
        RetrofitClient.userService.getMyPage()
            .enqueue(object : Callback<BaseResponse<UserMeResponse>> {

                override fun onResponse(
                    call: Call<BaseResponse<UserMeResponse>>,
                    response: Response<BaseResponse<UserMeResponse>>
                ) {
                    val data = response.body()?.data

                    if (response.isSuccessful && data != null) {
                        bindMyPage(data)
                        saveUserPrefs(data)
                    } else {
                        Toast.makeText(
                            requireContext(),
                            "마이페이지 정보를 불러오지 못했습니다.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                override fun onFailure(call: Call<BaseResponse<UserMeResponse>>, t: Throwable) {
                    Toast.makeText(
                        requireContext(),
                        "네트워크 오류",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
    }

    /** UI 바인딩 */
    private fun bindMyPage(data: UserMeResponse) {

        // 닉네임
        binding.tvNickname.text = data.nickname

        // 로그인 방식
        binding.tvLoginMethod.text = when (data.loginType) {
            "KAKAO" -> "카카오 로그인"
            else -> "이메일 로그인"
        }

        // 친구 코드
        binding.tvCodeValue.text = data.friendCode

        // 프로필 이미지
        if (!data.profileImageUrl.isNullOrEmpty()) {
            Glide.with(binding.ivProfile)
                .load(data.profileImageUrl)
                .placeholder(R.drawable.img_profile_default) // 로딩 중
                .error(R.drawable.img_profile_default)       // 실패 시
                .fallback(R.drawable.img_profile_default)    // null 일 때
                .circleCrop()
                .into(binding.ivProfile)

        }


        // 이번 달 네컷 수
        setupUsageText(data.thisMonthDay4CutCount)
    }

    /** SharedPreferences 저장 (화면 복귀 대비) */
    private fun saveUserPrefs(data: UserMeResponse) {
        val pref = requireContext()
            .getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)

        pref.edit().apply {
            putString("nickname", data.nickname)
            putString("loginType", data.loginType)
            apply()
        }
    }

    /** “이번 달 n장의 네컷” 텍스트 */
    private fun setupUsageText(count: Int) {
        val fullText = "이번 달 ${count}장의 네컷을\n찍었어요!"
        val spannable = SpannableStringBuilder(fullText)

        val start = fullText.indexOf(count.toString())
        val end = start + count.toString().length

        spannable.setSpan(
            ForegroundColorSpan(Color.parseColor("#FF7E67")),
            start,
            end,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        binding.tvCountInfo.text = spannable
    }

    /** 클릭 리스너 */
    private fun initClickListener() {

        binding.ivNotification.setOnClickListener {
            startActivity(Intent(requireContext(), NotificationActivity::class.java))
        }

        // 프로필 수정
        val editProfileListener = View.OnClickListener {
            val intent = Intent(requireContext(), EditProfileActivity::class.java)
            intent.putExtra("nickname", binding.tvNickname.text.toString())
            editProfileLauncher.launch(intent)
        }

        binding.ivProfile.setOnClickListener(editProfileListener)
        binding.ivEditProfile.setOnClickListener(editProfileListener)

        // 코드 복사
        binding.btnCopyCode.setOnClickListener {
            val code = binding.tvCodeValue.text.toString()
            val clipboard =
                requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("UserCode", code))
            Toast.makeText(requireContext(), "코드가 복사되었습니다.", Toast.LENGTH_SHORT).show()
        }

        // 로그아웃
        binding.btnLogout.setOnClickListener {
            CustomDialog(
                requireContext(),
                "정말 로그아웃하시겠어요?",
                "다시 이용하려면 로그인이 필요해요.",
                "로그아웃"
            ) {
                clearUserPrefs()
                goToIntro()
            }.show()
        }

        // 회원 탈퇴
        binding.btnWithdraw.setOnClickListener {
            CustomDialog(
                requireContext(),
                "정말 탈퇴하시겠어요?",
                "탈퇴 시 모든 데이터가 삭제되며 복구할 수 없어요.",
                "탈퇴"
            ) {
                clearUserPrefs()
                goToIntro()
            }.show()
        }
    }

    private fun goToIntro() {
        val intent = Intent(requireContext(), IntroActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }

    private fun clearUserPrefs() {
        requireContext()
            .getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .apply()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
