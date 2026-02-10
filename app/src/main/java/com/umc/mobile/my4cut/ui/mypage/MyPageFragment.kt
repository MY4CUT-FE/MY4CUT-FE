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
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.umc.mobile.my4cut.R
import com.umc.mobile.my4cut.data.auth.local.TokenManager
import com.umc.mobile.my4cut.data.base.BaseResponse
import com.umc.mobile.my4cut.data.user.model.UserMeResponse
import com.umc.mobile.my4cut.databinding.FragmentMyPageBinding
import com.umc.mobile.my4cut.network.RetrofitClient
import com.umc.mobile.my4cut.ui.intro.IntroActivity
import com.umc.mobile.my4cut.ui.notification.NotificationActivity
import kotlinx.coroutines.runBlocking
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MyPageFragment : Fragment() {

    private var _binding: FragmentMyPageBinding? = null
    private val binding get() = _binding!!

    private val editProfileLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                // ✅ onResume에서 자동으로 새로고침되므로 여기서는 생략
                Log.d("MyPageFragment", "Profile edit completed")
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
        // ✅ onViewCreated에서는 호출하지 않음 (onResume에서 호출됨)
    }

    override fun onResume() {
        super.onResume()
        Log.d("MyPageFragment", "📱 onResume - refreshing profile")
        // ✅ 화면이 보일 때마다 프로필 새로고침
        loadMyPage()
    }

    private fun loadMyPage() {
        Log.d("MyPageFragment", "🔄 Loading profile data...")
        RetrofitClient.userService.getMyPage()
            .enqueue(object : Callback<BaseResponse<UserMeResponse>> {
                override fun onResponse(
                    call: Call<BaseResponse<UserMeResponse>>,
                    response: Response<BaseResponse<UserMeResponse>>
                ) {
                    Log.d("MyPageFragment", "📨 Response: ${response.code()}")
                    val data = response.body()?.data
                    if (response.isSuccessful && data != null) {
                        Log.d("MyPageFragment", "✅ Profile loaded: ${data.nickname}, imageUrl=${data.profileImageViewUrl?.take(50)}")
                        bindMyPage(data)
                        saveUserPrefs(data)
                        // ✅ 이번 달 사진 총 개수 계산
                        loadMonthlyPhotoCount()
                    } else {
                        Log.e("MyPageFragment", "❌ Failed to load profile")
                        Toast.makeText(requireContext(), "정보를 불러오지 못했습니다.", Toast.LENGTH_SHORT).show()
                    }
                }
                override fun onFailure(call: Call<BaseResponse<UserMeResponse>>, t: Throwable) {
                    Log.e("MyPageFragment", "💥 Network error", t)
                    Toast.makeText(requireContext(), "네트워크 오류", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun bindMyPage(data: UserMeResponse) {
        binding.tvNickname.text = data.nickname
        binding.tvLoginMethod.text = if (data.loginType == "KAKAO") "카카오 로그인" else "이메일 로그인"
        binding.tvCodeValue.text = data.friendCode

        Log.d("MyPageFragment", "🖼️ Loading profile image: ${data.profileImageViewUrl?.take(80)}")
        Glide.with(binding.ivProfile)
            .load(data.profileImageViewUrl)
            .placeholder(R.drawable.img_profile_default)
            .error(R.drawable.img_profile_default)
            .circleCrop()
            .into(binding.ivProfile)

        // ✅ setupUsageText는 loadMonthlyPhotoCount에서 호출
    }

    private fun saveUserPrefs(data: UserMeResponse) {
        requireContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE).edit().apply {
            putString("nickname", data.nickname)
            putString("loginType", data.loginType)
            apply()
        }
    }

    private fun setupUsageText(count: Int) {
        val fullText = "이번 달 ${count}장의 네컷을\n찍었어요!"
        val spannable = SpannableStringBuilder(fullText)
        val start = fullText.indexOf(count.toString())
        val end = start + count.toString().length
        spannable.setSpan(ForegroundColorSpan(Color.parseColor("#FF7E67")), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        binding.tvCountInfo.text = spannable
    }

    private fun initClickListener() {
        binding.ivNotification.setOnClickListener {
            startActivity(Intent(requireContext(), NotificationActivity::class.java))
        }

        val editProfileListener = View.OnClickListener {
            val intent = Intent(requireContext(), EditProfileActivity::class.java)
            intent.putExtra("nickname", binding.tvNickname.text.toString())
            editProfileLauncher.launch(intent)
        }
        binding.ivProfile.setOnClickListener(editProfileListener)
        binding.ivEditProfile.setOnClickListener(editProfileListener)

        binding.btnCopyCode.setOnClickListener {
            val code = binding.tvCodeValue.text.toString()
            val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("UserCode", code))
            Toast.makeText(requireContext(), "코드가 복사되었습니다.", Toast.LENGTH_SHORT).show()
        }

        binding.btnLogout.setOnClickListener {
            CustomDialog(requireContext(), "정말 로그아웃하시겠어요?", "다시 이용하려면 로그인이 필요해요.", "로그아웃") {
                clearUserPrefs()
                goToIntro()
            }.show()
        }

        binding.btnWithdraw.setOnClickListener {
            CustomDialog(requireContext(), "정말 탈퇴하시겠어요?", "탈퇴 시 모든 데이터가 삭제되며 복구할 수 없어요.", "탈퇴") {
                performWithdraw()
            }.show()
        }
    }

    private fun performWithdraw() {
        val token = TokenManager.getAccessToken(requireContext())

        if (token.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "인증 정보가 없습니다.", Toast.LENGTH_SHORT).show()
            goToIntro()
            return
        }

        Log.d("WithdrawTest", "AccessToken이 확인됨. 즉시 탈퇴 요청 시작")

        RetrofitClient.authService.withdraw().enqueue(object : Callback<BaseResponse<String>> {
            override fun onResponse(call: Call<BaseResponse<String>>, response: Response<BaseResponse<String>>) {
                if (response.isSuccessful) {
                    Log.d("WithdrawTest", "✅ 회원 탈퇴 성공!")
                    handleWithdrawSuccess()
                } else {
                    Log.e("WithdrawTest", "❌ 탈퇴 실패 코드: ${response.code()}")
                    if (response.code() == 401) {
                        Toast.makeText(requireContext(), "세션이 만료되었습니다. 다시 로그인해주세요.", Toast.LENGTH_SHORT).show()
                        clearUserPrefs()
                        goToIntro()
                    } else {
                        handleWithdrawError(response.code())
                    }
                }
            }

            override fun onFailure(call: Call<BaseResponse<String>>, t: Throwable) {
                Log.e("WithdrawTest", "네트워크 오류", t)
                handleNetworkError(t)
            }
        })
    }

    private fun handleWithdrawSuccess() {
        Toast.makeText(requireContext(), "회원 탈퇴가 완료되었습니다.", Toast.LENGTH_SHORT).show()
        clearUserPrefs()
        goToIntro()
    }

    private fun handleWithdrawError(code: Int) {
        Log.d("Withdraw", "Error Code: $code")
        Toast.makeText(requireContext(), "탈퇴 실패 ($code). 다시 시도해주세요.", Toast.LENGTH_SHORT).show()
    }

    private fun handleNetworkError(t: Throwable) {
        Log.e("Withdraw", "Network Error", t)
        Toast.makeText(requireContext(), "네트워크 연결 상태를 확인해주세요.", Toast.LENGTH_SHORT).show()
    }

    private fun goToIntro() {
        val intent = Intent(requireContext(), IntroActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
    }

    private fun clearUserPrefs() {
        TokenManager.clear(requireContext())
        requireContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
            .edit().clear().apply()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    /**
     * ✅ 이번 달 사진 총 개수 계산 (각 날짜별 상세 조회)
     */
    private fun loadMonthlyPhotoCount() {
        val now = java.time.LocalDate.now()
        val year = now.year
        val month = now.monthValue

        Log.d("MyPageFragment", "📊 Loading monthly photo count for $year-$month")

        Thread {
            try {
                kotlinx.coroutines.runBlocking {
                    // 1. GET /day4cut/calendar - 기록된 날짜 목록 가져오기
                    val calendarResponse = RetrofitClient.day4CutService.getCalendarStatus(year, month)

                    if (calendarResponse.code == "C2001" && calendarResponse.data != null) {
                        val recordedDates = calendarResponse.data.dates

                        if (recordedDates.isEmpty()) {
                            Log.d("MyPageFragment", "✅ No photos this month")
                            updatePhotoCount(0)
                            return@runBlocking
                        }

                        Log.d("MyPageFragment", "📅 Found ${recordedDates.size} recorded dates")

                        // 2. 각 날짜별로 GET /day4cut?date=yyyy-MM-dd 호출
                        var totalPhotoCount = 0

                        for (dayItem in recordedDates) {
                            val dateString = String.format("%04d-%02d-%02d", year, month, dayItem.day)

                            try {
                                val detailResponse = RetrofitClient.day4CutService.getDay4CutDetail(dateString)

                                if (detailResponse.code == "C2001" && detailResponse.data != null) {
                                    val photoCount = detailResponse.data.viewUrls?.size ?: 0
                                    totalPhotoCount += photoCount
                                    Log.d("MyPageFragment", "  📸 $dateString: $photoCount photos")
                                }
                            } catch (e: Exception) {
                                Log.e("MyPageFragment", "❌ Failed to load $dateString", e)
                            }
                        }

                        Log.d("MyPageFragment", "✅ Total photos this month: $totalPhotoCount")
                        updatePhotoCount(totalPhotoCount)
                    } else {
                        Log.e("MyPageFragment", "❌ Failed to load calendar")
                        updatePhotoCount(0)
                    }
                }

            } catch (e: Exception) {
                Log.e("MyPageFragment", "💥 Failed to calculate photo count", e)
                updatePhotoCount(0)
            }
        }.start()
    }

    private fun updatePhotoCount(count: Int) {
        requireActivity().runOnUiThread {
            if (_binding != null) {
                this@MyPageFragment.setupUsageText(count)
            }
        }
    }
}