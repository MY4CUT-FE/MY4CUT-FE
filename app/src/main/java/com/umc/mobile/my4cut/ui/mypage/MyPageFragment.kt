package com.umc.mobile.my4cut.ui.mypage

import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import android.app.Activity.RESULT_OK
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.BroadcastReceiver
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import com.umc.mobile.my4cut.ui.home.HomeFragment
import android.graphics.Color
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.util.Log
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
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
import com.umc.mobile.my4cut.ui.onboarding.OnboardingActivity
import com.umc.mobile.my4cut.ui.notification.NotificationActivity
import kotlinx.coroutines.runBlocking
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MyPageFragment : Fragment() {

    private var _binding: FragmentMyPageBinding? = null
    private val binding get() = _binding!!

    // FCM 푸시가 도착하면 마이페이지 화면의 알림 아이콘도 즉시 갱신
    private val notificationReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == HomeFragment.ACTION_NOTIFICATION_RECEIVED) {
                // 푸시 도착 직후 사용자가 바로 알 수 있도록 먼저 ON 아이콘으로 변경
                binding.ivNotification.setImageResource(R.drawable.ic_noti_on)

                // 알림창에 시스템 알림이 남아있어도, 서버 기준으로 전부 읽음이면 OFF 처리되도록 동기화
                updateNotificationIcon()
            }
        }
    }

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
        // MyPageFragment가 살아있는 동안 푸시 수신 이벤트를 감지
        registerNotificationReceiver()
        // onViewCreated에서는 프로필 호출하지 않음 (onResume에서 호출됨)
    }

    override fun onResume() {
        super.onResume()
        Log.d("MyPageFragment", "📱 onResume - refreshing profile")
        // ✅ 화면이 보일 때마다 프로필 새로고침
        loadMyPage()
        updateNotificationIcon()
    }

    private fun dpToPx(dp: Int): Int =
        (dp * resources.displayMetrics.density).toInt()

    private fun applySkeleton(view: TextView, widthDp: Int, bgRes: Int, heightDp: Int = 14) {
        view.text = ""
        view.minWidth = dpToPx(widthDp)
        view.minHeight = dpToPx(heightDp)
        view.setBackgroundResource(bgRes)
    }

    private fun clearSkeleton(view: TextView) {
        view.minWidth = 0
        view.minHeight = 0
        view.background = null
    }

    private fun showMyPageLoadingState() {
        binding.ivProfile.setImageResource(R.drawable.loading_profile)

        // 닉네임/로그인방식 영역: F0F0F0
        applySkeleton(binding.tvNickname, 100, R.drawable.bg_skeleton_bar_light)
        applySkeleton(binding.tvLoginMethod, 70, R.drawable.bg_skeleton_bar_light)
        binding.llMyCode.visibility = View.INVISIBLE

        // 통계 카드: 배경 F0F0F0, 내부 텍스트/원형 이미지는 D2D3D3
        binding.clStatsCard.setBackgroundResource(R.drawable.bg_stats_card_loading)
        applySkeleton(binding.tvTodayDate, 90, R.drawable.bg_skeleton_bar_dark)
        applySkeleton(binding.tvCountInfo, 140, R.drawable.bg_skeleton_bar_dark)
        binding.ivStatsIllustration.setImageDrawable(null)
        binding.ivStatsIllustration.setBackgroundResource(R.drawable.bg_skeleton_circle_dark)
    }

    private fun loadMyPage() {
        Log.d("MyPageFragment", "🔄 Loading profile data...")
        showMyPageLoadingState()
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
        clearSkeleton(binding.tvNickname)
        clearSkeleton(binding.tvLoginMethod)
        binding.llMyCode.visibility = View.VISIBLE

        Log.d("MyPageFragment", "🖼️ Loading profile image: ${data.profileImageViewUrl?.take(80)}")
        Glide.with(binding.ivProfile)
            .load(data.profileImageViewUrl)
            .placeholder(R.drawable.loading_profile)
            .error(R.drawable.img_profile_default)
            .circleCrop()
            .into(binding.ivProfile)

        // ✅ 통계 카드(날짜/이번 달 개수/삽화)는 loadMonthlyPhotoCount 완료 후 setupUsageText에서 한 번에 표시
    }

    private fun saveUserPrefs(data: UserMeResponse) {
        requireContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE).edit().apply {
            putString("nickname", data.nickname)
            putString("loginType", data.loginType)
            apply()
        }
    }

    private fun setupUsageText(count: Int) {
        binding.clStatsCard.setBackgroundResource(R.drawable.bg_dashed_card)

        val today = LocalDate.now()
        val formatter = DateTimeFormatter.ofPattern("yyyy.MM.dd")
        clearSkeleton(binding.tvTodayDate)
        binding.tvTodayDate.text = today.format(formatter)

        val fullText = "이번 달 ${count}장의 네컷을\n찍었어요!"
        val spannable = SpannableStringBuilder(fullText)
        val start = fullText.indexOf(count.toString())
        val end = start + count.toString().length
        spannable.setSpan(ForegroundColorSpan(Color.parseColor("#FF7E67")), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        clearSkeleton(binding.tvCountInfo)
        binding.tvCountInfo.text = spannable

        binding.ivStatsIllustration.background = null
        binding.ivStatsIllustration.setImageResource(R.drawable.img_mypage_squirrel)
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
        // 로그아웃·탈퇴 후 온보딩 화면으로 이동, 백스택 전체 제거
        val intent = Intent(requireContext(), OnboardingActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
    }

    private fun clearUserPrefs() {
        // 토큰 삭제
        TokenManager.clear(requireContext())
        // 사용자 정보 캐시 삭제
        requireContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
            .edit().clear().apply()
        // FCM 토큰 캐시 삭제
        requireContext().getSharedPreferences("my4cut_prefs", Context.MODE_PRIVATE)
            .edit().clear().apply()
        // 포즈 북마크 삭제
        requireContext().getSharedPreferences("pose_bookmarks", Context.MODE_PRIVATE)
            .edit().clear().apply()
    }

    override fun onDestroyView() {
        // 메모리 누수 방지를 위해 등록한 Receiver 해제
        unregisterNotificationReceiver()
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

    // FCM 수신 브로드캐스트 Receiver 등록
    private fun registerNotificationReceiver() {
        val filter = IntentFilter(HomeFragment.ACTION_NOTIFICATION_RECEIVED)

        ContextCompat.registerReceiver(
            requireContext(),
            notificationReceiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    // Fragment View가 파괴될 때 Receiver 해제
    private fun unregisterNotificationReceiver() {
        try {
            requireContext().unregisterReceiver(notificationReceiver)
        } catch (_: IllegalArgumentException) {
            // 이미 해제된 경우 앱이 죽지 않도록 무시
        }
    }

    private fun updateNotificationIcon() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = RetrofitClient.notificationService.getUnreadStatus()
                val hasUnread = response.data?.hasUnread == true

                binding.ivNotification.setImageResource(
                    if (hasUnread) R.drawable.ic_noti_on
                    else R.drawable.ic_noti_off
                )
            } catch (e: Exception) {
                binding.ivNotification.setImageResource(R.drawable.ic_noti_off)
            }
        }
    }
}