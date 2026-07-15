package com.umc.mobile.my4cut.ui.home

import android.app.Activity
import android.content.Intent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.IntentFilter
import android.graphics.Color
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.res.ResourcesCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import coil.load
import com.umc.mobile.my4cut.R
import com.umc.mobile.my4cut.data.day4cut.model.Day4CutDetailResponse
import com.umc.mobile.my4cut.databinding.FragmentHomeBinding
import com.umc.mobile.my4cut.databinding.ItemCalendarDayBinding
import com.umc.mobile.my4cut.network.RetrofitClient
import com.umc.mobile.my4cut.ui.notification.NotificationActivity
import com.umc.mobile.my4cut.ui.pose.PoseRecommendActivity
import com.umc.mobile.my4cut.ui.myalbum.CalendarPickerActivity
import com.umc.mobile.my4cut.ui.myalbum.EntryDetailFragment
import com.umc.mobile.my4cut.ui.record.EntryRegisterActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

class HomeFragment : Fragment() {

    // FCM 푸시 수신 시 HomeFragment에 알림 상태 변경을 전달하기 위한 브로드캐스트
    companion object {
        const val ACTION_NOTIFICATION_RECEIVED =
            "com.umc.mobile.my4cut.ACTION_NOTIFICATION_RECEIVED"
    }

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private var selectedDate: LocalDate = LocalDate.now()

    // 푸시 알림이 도착하면 홈 알림 아이콘을 즉시 ON으로 변경
    private val notificationReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == ACTION_NOTIFICATION_RECEIVED) {
                // 서버 응답을 기다리기 전에 사용자가 바로 알 수 있도록 먼저 ON 아이콘으로 변경
                binding.ivNotification.setImageResource(R.drawable.ic_noti_on)

                // 이후 서버의 읽지 않은 알림 상태와 다시 동기화
                updateNotificationIcon()
            }
        }
    }

    // ✅ 캘린더 데이터 (날짜별 기록 여부)
    private val recordedDates = mutableSetOf<Int>() // 기록이 있는 날짜 저장
    private val thumbnailUrls = mutableMapOf<Int, String>() // 날짜별 썸네일 URL 저장

    private val startCalendarForResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val dateString = result.data?.getStringExtra("SELECTED_DATE")
            if (dateString != null) {
                selectedDate = LocalDate.parse(dateString)
                refreshCalendarData()
            }
        }
    }

    // ✅ 기록 등록 후 돌아올 때 갱신
    private val entryRegisterLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // 기록 등록 완료 후 화면 갱신
            loadCalendarData()
            loadDay4CutData(selectedDate)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupDateBanner()
        setupWelcomeText()
        setupClickListeners()
        setupSwipeGesture()

        // ✅ 초기 UI 설정
        setupWeekCalendar() // 주간 캘린더 먼저 그리기
        updateContentState(selectedDate) // 날짜 표시

        // ✅ API 데이터 로드
        loadCalendarData()
        loadDay4CutData(selectedDate)
        updateNotificationIcon()
        // HomeFragment가 살아있는 동안 푸시 수신 이벤트를 감지
        registerNotificationReceiver()
    }

    private fun setupDateBanner() {
        val today = LocalDate.now()
        val formatter = DateTimeFormatter.ofPattern("yyyy.MM.dd")
        binding.tvDateBanner.text = today.format(formatter)
    }

    private fun setupWelcomeText() {
        val text = "오늘의 네컷을\n남겨볼까요?"
        val spannable = SpannableStringBuilder(text)
        spannable.setSpan(ForegroundColorSpan(Color.parseColor("#FF7E67")), 0, 6, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        binding.tvWelcomeBanner.text = spannable
    }

    // ✅ 캘린더 데이터 로드 (API) - suspend 함수로 변경
    private fun loadCalendarData() {
        val year = selectedDate.year
        val month = selectedDate.monthValue

        Log.d("HomeFragment", "📤 Loading calendar - year: $year, month: $month")

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = RetrofitClient.day4CutService.getCalendarStatus(year, month)

                withContext(Dispatchers.Main) {
                    Log.d("HomeFragment", "📨 Calendar Response:")
                    Log.d("HomeFragment", "   ├─ code: ${response.code}")
                    Log.d("HomeFragment", "   ├─ message: ${response.message}")
                    Log.d("HomeFragment", "   └─ data: ${response.data}")

                    val calendarData = response.data
                    if (calendarData != null) {
                        Log.d("HomeFragment", "✅ Calendar loaded: ${calendarData.dates.size} dates")

                        // 기록이 있는 날짜 저장
                        recordedDates.clear()
                        thumbnailUrls.clear()
                        calendarData.dates.forEach { date ->
                            recordedDates.add(date.day)
                            date.thumbnailUrl?.let { url ->
                                thumbnailUrls[date.day] = url
                            }
                            Log.d("HomeFragment", "   ├─ Day ${date.day}: ${date.thumbnailUrl?.take(50) ?: "no thumbnail"}")
                        }

                        setupWeekCalendar() // 주간 캘린더 다시 그리기
                    } else {
                        Log.e("HomeFragment", "⚠️ Calendar data is null")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Log.e("HomeFragment", "❌ Network error", e)
                }
            }
        }
    }

    private fun showLoadingState() {
        binding.clEmptyState.visibility = View.GONE
        binding.llFilledState.visibility = View.VISIBLE
        binding.clDiaryEmojiSection.visibility = View.VISIBLE

        binding.ivHomePhoto.setImageResource(R.drawable.img_pose_loading)

        binding.ivMoodIcon.setImageDrawable(null)
        binding.ivMoodIcon.setBackgroundResource(R.drawable.bg_circle_gray)

        val container = binding.llDiaryLines
        container.removeAllViews()
        listOf(LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(150)).forEachIndexed { index, width ->
            val skeleton = View(requireContext()).apply {
                setBackgroundResource(R.drawable.bg_skeleton_text)
                layoutParams = LinearLayout.LayoutParams(width, dpToPx(14)).also {
                    it.topMargin = if (index == 0) 0 else dpToPx(12)
                }
            }
            container.addView(skeleton)
        }
    }

    // ✅ 특정 날짜의 하루네컷 데이터 로드 (API) - suspend 함수로 변경
    private fun loadDay4CutData(date: LocalDate) {
        val dateString = date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        showLoadingState()

        Log.d("HomeFragment", "📤 Loading day4cut for date: $dateString")

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = RetrofitClient.day4CutService.getDay4CutDetail(dateString)

                withContext(Dispatchers.Main) {
                    Log.d("HomeFragment", "📨 Day4Cut Response:")
                    Log.d("HomeFragment", "   ├─ code: ${response.code}")
                    Log.d("HomeFragment", "   ├─ message: ${response.message}")
                    Log.d("HomeFragment", "   └─ data: ${response.data}")

                    // ⚠️ 이미지 URL 디버깅
                    response.data?.let { day4cut ->
                        Log.d("HomeFragment", "🖼️ Image URLs debugging:")
                        Log.d("HomeFragment", "   ├─ viewUrls type: ${day4cut.viewUrls?.javaClass?.simpleName ?: "null"}")
                        Log.d("HomeFragment", "   ├─ viewUrls size: ${day4cut.viewUrls?.size ?: 0}")
                        day4cut.viewUrls?.forEachIndexed { index, url ->
                            Log.d("HomeFragment", "   ├─ [$index]: $url")
                        }
                    }

                    val day4cut = response.data
                    if (day4cut != null) {
                        Log.d("HomeFragment", "✅ Day4cut loaded: ${day4cut.id}")
                        showFilledState(day4cut)
                    } else {
                        Log.d("HomeFragment", "⚠️ No data for this date")
                        showEmptyState()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Log.e("HomeFragment", "❌ Network error", e)
                    if (isAdded) {
                        Toast.makeText(requireContext(), "네트워크 오류", Toast.LENGTH_SHORT).show()
                        showEmptyState()
                    }
                }
            }
        }
    }

    private fun showFilledState(day4cut: Day4CutDetailResponse) {
        binding.clEmptyState.visibility = View.GONE
        binding.llFilledState.visibility = View.VISIBLE

        val imageUrl = thumbnailUrls[selectedDate.dayOfMonth] ?: day4cut.viewUrls?.firstOrNull()
        if (imageUrl != null) {
            Log.d("HomeFragment", "Loading thumbnail with Coil: ${imageUrl.take(80)}")
            binding.ivHomePhoto.load(imageUrl) {
                crossfade(true)
                placeholder(R.drawable.img_pose_loading)
                error(R.drawable.img_ex_photo)
            }
        } else {
            Log.d("HomeFragment", "No thumbnail for day ${selectedDate.dayOfMonth}")
            binding.ivHomePhoto.setImageResource(R.drawable.img_ex_photo)
        }

        // 기록된 날짜의 사진 클릭 → 네컷 상세보기로 이동
        binding.ivHomePhoto.setOnClickListener {
            val entryDetailFragment = EntryDetailFragment().apply {
                arguments = Bundle().apply {
                    putString("API_DATE", selectedDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")))
                    putString("SELECTED_DATE", selectedDate.format(DateTimeFormatter.ofPattern("yyyy.MM.dd")))
                }
            }
            requireActivity().supportFragmentManager.beginTransaction()
                .replace(R.id.fcv_main, entryDetailFragment)
                .addToBackStack(null)
                .commit()
        }

        val content = day4cut.content?.trim() ?: ""
        val hasContent = content.isNotBlank()
        val hasEmoji = !day4cut.emojiType.isNullOrBlank()

        if (hasContent || hasEmoji) {
            binding.clDiaryEmojiSection.visibility = View.VISIBLE

            // 일기 줄 동적 렌더링
            if (hasContent) {
                renderDiaryLines(content)
            } else {
                binding.llDiaryLines.removeAllViews()
            }

            // 이모지 아이콘
            if (hasEmoji) {
                val moodIcon = when (day4cut.emojiType) {
                    "HAPPY" -> R.drawable.img_mood_happy
                    "ANGRY" -> R.drawable.img_mood_angry
                    "TIRED" -> R.drawable.img_mood_tired
                    "SAD" -> R.drawable.img_mood_sad
                    "CALM" -> R.drawable.img_mood_calm
                    else -> null
                }
                if (moodIcon != null) {
                    binding.ivMoodIcon.setImageResource(moodIcon)
                    // 이모지가 있을 때 코랄 원형 배경을 이미지 뒤에 적용
                    binding.ivMoodIcon.setBackgroundResource(R.drawable.bg_circle_emoji)
                } else {
                    binding.ivMoodIcon.setImageDrawable(null)
                    binding.ivMoodIcon.setBackgroundResource(R.drawable.bg_circle_gray)
                }
            } else {
                binding.ivMoodIcon.setImageDrawable(null)
                binding.ivMoodIcon.setBackgroundResource(R.drawable.bg_circle_emoji)
            }
        } else {
            binding.clDiaryEmojiSection.visibility = View.GONE
        }

        Log.d("HomeFragment", "✅ Filled state - content: '${content.take(30)}', emoji: '${day4cut.emojiType}'")
    }

    private fun renderDiaryLines(content: String) {
        val container = binding.llDiaryLines
        container.removeAllViews()

        val tv = LinedTextView(requireContext()).apply {
            text = content
            setTextColor(Color.parseColor("#1A1A1A"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            typeface = ResourcesCompat.getFont(requireContext(), R.font.suit_regular)
            setPadding(0, 0, 0, dpToPx(8))
            setLineSpacing(dpToPx(8).toFloat(), 1f) // LinedEditText의 lineSpacingExtra와 맞추기
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        container.addView(tv)
    }

    private fun dpToPx(dp: Int): Int =
        (dp * resources.displayMetrics.density).toInt()

    // ✅ 기록이 없는 경우 표시
    private fun showEmptyState() {
        binding.clEmptyState.visibility = View.VISIBLE
        binding.llFilledState.visibility = View.GONE
    }

    // 주간 캘린더 생성
    private fun setupWeekCalendar() {
        val calendarContainer = binding.llWeekCalendar
        calendarContainer.removeAllViews()

        val daysFromSunday = selectedDate.dayOfWeek.value % 7
        val startOfWeek = selectedDate.minusDays(daysFromSunday.toLong())

        for (i in 0 until 7) {
            val date = startOfWeek.plusDays(i.toLong())
            val dayViewBinding = ItemCalendarDayBinding.inflate(layoutInflater, calendarContainer, false)

            dayViewBinding.tvDayOfWeek.text = date.format(DateTimeFormatter.ofPattern("E", Locale.KOREAN))

            val dayColor = when (date.dayOfWeek) {
                DayOfWeek.SUNDAY -> Color.parseColor("#FF7E67")
                DayOfWeek.SATURDAY -> Color.parseColor("#4B7EFF")
                else -> Color.parseColor("#D1D1D1")
            }
            dayViewBinding.tvDayOfWeek.setTextColor(dayColor)

            dayViewBinding.tvDayNumber.text = date.dayOfMonth.toString()

            val isFuture = date.isAfter(LocalDate.now())

            if (date.isEqual(selectedDate)) {
                dayViewBinding.tvDayNumber.setBackgroundResource(R.drawable.bg_calendar_selected)
                dayViewBinding.tvDayNumber.backgroundTintList = null
                dayViewBinding.tvDayNumber.setTextColor(Color.WHITE)
            } else {
                dayViewBinding.tvDayNumber.background = null
                // 미래 날짜는 연한 회색으로 표시하여 선택 불가임을 시각적으로 구분
                dayViewBinding.tvDayNumber.setTextColor(
                    if (isFuture) Color.parseColor("#D1D1D1") else Color.parseColor("#6A6A6A")
                )
            }

            // ✅ API에서 받은 데이터로 점 표시
            if (recordedDates.contains(date.dayOfMonth) && date.month == selectedDate.month) {
                dayViewBinding.vRecordDot.visibility = View.VISIBLE
            } else {
                dayViewBinding.vRecordDot.visibility = View.GONE
            }

            dayViewBinding.root.setOnClickListener {
                if (date.isAfter(LocalDate.now())) {
                    Toast.makeText(requireContext(), "오늘 이후 날짜에는 하루네컷을 업로드할 수 없어요.", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                selectedDate = date
                refreshCalendarData()
            }

            val params = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT)
            params.weight = 1f
            dayViewBinding.root.layoutParams = params
            calendarContainer.addView(dayViewBinding.root)
        }
    }

    private fun setupClickListeners() {
        binding.ivCalendarBack.setOnClickListener {
            selectedDate = selectedDate.minusWeeks(1)
            refreshCalendarData()
        }

        binding.ivCalendarNext.setOnClickListener {
            val nextDate = selectedDate.plusWeeks(1)
            if (!YearMonth.from(nextDate).isAfter(YearMonth.from(LocalDate.now()))) {
                selectedDate = nextDate
                refreshCalendarData()
            }
        }

        binding.clPoseRecommend.setOnClickListener {
            startActivity(Intent(requireContext(), PoseRecommendActivity::class.java))
        }

        binding.ivNotification.setOnClickListener {
            startActivity(Intent(requireContext(), NotificationActivity::class.java))
        }

        binding.ivMypage.setOnClickListener {
            (requireActivity() as? com.umc.mobile.my4cut.MainActivity)
                ?.navigateToMyPage()
        }

        // 빈 날짜 카드 클릭 → 전체 캘린더로 이동, 현재 선택 날짜를 초기값으로 전달
        binding.clEmptyState.setOnClickListener {
            val intent = Intent(requireContext(), CalendarPickerActivity::class.java).apply {
                putExtra("YEAR", selectedDate.year)
                putExtra("MONTH", selectedDate.monthValue)
                putExtra("DAY", selectedDate.dayOfMonth)
            }
            entryRegisterLauncher.launch(intent)
        }
    }

    private fun setupSwipeGesture() {
        binding.llWeekCalendar.setOnSwipeListener { isRightSwipe ->
            if (isRightSwipe) {
                selectedDate = selectedDate.minusWeeks(1)
                refreshCalendarData()
            } else {
                val nextDate = selectedDate.plusWeeks(1)
                if (!YearMonth.from(nextDate).isAfter(YearMonth.from(LocalDate.now()))) {
                    selectedDate = nextDate
                    refreshCalendarData()
                }
            }
        }
    }

    private fun refreshCalendarData() {
        setupWeekCalendar() // 주간 캘린더 다시 그리기
        updateContentState(selectedDate) // 날짜 업데이트
        loadCalendarData() // 캘린더 데이터 다시 로드
        loadDay4CutData(selectedDate) // 선택된 날짜 데이터 로드
    }

    private fun updateContentState(date: LocalDate) {
        val formatter = DateTimeFormatter.ofPattern("yyyy.MM.dd")
        binding.tvContentDate.text = date.format(formatter)
    }

    // FCM 수신 브로드캐스트 Receiver 등록
    private fun registerNotificationReceiver() {
        val filter = IntentFilter(ACTION_NOTIFICATION_RECEIVED)

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

    /**
     * ✅ 화면 복귀 시 데이터 새로고침
     */
    override fun onResume() {
        super.onResume()
        // 다른 화면에서 돌아왔을 때 데이터 갱신
        loadCalendarData()
        loadDay4CutData(selectedDate)
        updateNotificationIcon()
    }

    override fun onDestroyView() {
        // 메모리 누수 방지를 위해 등록한 Receiver 해제
        unregisterNotificationReceiver()
        super.onDestroyView()
        _binding = null
    }
}