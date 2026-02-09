package com.umc.mobile.my4cut.ui.home

import android.app.Activity
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
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import coil.load
import com.umc.mobile.my4cut.R
import com.umc.mobile.my4cut.data.base.BaseResponse
import com.umc.mobile.my4cut.data.day4cut.model.CalendarStatusResponse
import com.umc.mobile.my4cut.data.day4cut.model.Day4CutDetailResponse
import com.umc.mobile.my4cut.databinding.FragmentHomeBinding
import com.umc.mobile.my4cut.databinding.ItemCalendarDayBinding
import com.umc.mobile.my4cut.network.RetrofitClient
import com.umc.mobile.my4cut.ui.calendar.CalendarFullActivity
import com.umc.mobile.my4cut.ui.calendar.CalendarPickerActivity
import com.umc.mobile.my4cut.ui.notification.NotificationActivity
import com.umc.mobile.my4cut.ui.pose.PoseRecommendActivity
import com.umc.mobile.my4cut.ui.record.EntryRegisterActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private var selectedDate: LocalDate = LocalDate.now()

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

        // ✅ 초기 UI 설정
        setupWeekCalendar() // 주간 캘린더 먼저 그리기
        updateContentState(selectedDate) // 날짜 표시

        // ✅ API 데이터 로드
        loadCalendarData()
        loadDay4CutData(selectedDate)
    }

    private fun setupDateBanner() {
        val today = LocalDate.now()
        val formatter = DateTimeFormatter.ofPattern("yyyy MMMM d", Locale.ENGLISH)
        binding.tvDateBanner.text = "${today.format(formatter)}${getDayNumberSuffix(today.dayOfMonth)}"
    }

    private fun getDayNumberSuffix(day: Int): String {
        if (day in 11..13) return "th"
        return when (day % 10) { 1 -> "st" 2 -> "nd" 3 -> "rd" else -> "th" }
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

    // ✅ 특정 날짜의 하루네컷 데이터 로드 (API) - suspend 함수로 변경
    private fun loadDay4CutData(date: LocalDate) {
        val dateString = date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))

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
                    Toast.makeText(requireContext(), "네트워크 오류", Toast.LENGTH_SHORT).show()
                    showEmptyState()
                }
            }
        }
    }

    // ✅ 기록이 있는 경우 표시
    private fun showFilledState(day4cut: Day4CutDetailResponse) {
        binding.clEmptyState.visibility = View.GONE
        binding.llFilledState.visibility = View.VISIBLE

        // 이미지 표시 - viewUrls의 첫 번째 이미지 (썸네일) 사용
        val imageUrl = day4cut.viewUrls?.firstOrNull()
        if (imageUrl != null) {
            Log.d("HomeFragment", "📸 Loading image with Coil: ${imageUrl.take(80)}")
            // Coil로 이미지 로드 (placeholder 제거)
            binding.ivHomePhoto.load(imageUrl) {
                crossfade(true)
                error(R.drawable.img_ex_photo)  // 로드 실패 시만 기본 이미지 표시
            }
        } else {
            // 이미지가 없으면 기본 이미지 표시
            Log.d("HomeFragment", "⚠️ No viewUrls for day ${selectedDate.dayOfMonth}")
            binding.ivHomePhoto.setImageResource(R.drawable.img_ex_photo)
        }

        // 일기 내용 표시 (줄바꿈으로 분리)
        val content = day4cut.content ?: ""
        val lines = content.split("\n")

        when {
            lines.isEmpty() || content.isBlank() -> {
                binding.tvDiaryLine1.text = ""
                binding.tvDiaryLine2.text = ""
            }
            lines.size == 1 -> {
                binding.tvDiaryLine1.text = lines[0]
                binding.tvDiaryLine2.text = ""
            }
            else -> {
                binding.tvDiaryLine1.text = lines[0]
                binding.tvDiaryLine2.text = lines.drop(1).joinToString("\n")
            }
        }

        // 이모지 아이콘 표시
        val moodIcon = when (day4cut.emojiType) {
            "HAPPY" -> R.drawable.img_mood_happy
            "ANGRY" -> R.drawable.img_mood_angry
            "TIRED" -> R.drawable.img_mood_tired
            "SAD" -> R.drawable.img_mood_sad
            "CALM" -> R.drawable.img_mood_calm
            else -> R.drawable.img_mood_happy // 기본값
        }
        binding.ivMoodIcon.setImageResource(moodIcon)

        Log.d("HomeFragment", "✅ Filled state displayed - content: ${content.take(30)}, emoji: ${day4cut.emojiType}, images: ${day4cut.viewUrls?.size ?: 0}")
    }

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

            if (date.isEqual(selectedDate)) {
                dayViewBinding.tvDayNumber.setBackgroundResource(R.drawable.bg_calendar_selected)
                dayViewBinding.tvDayNumber.backgroundTintList = null
                dayViewBinding.tvDayNumber.setTextColor(Color.WHITE)
            } else {
                dayViewBinding.tvDayNumber.background = null
                dayViewBinding.tvDayNumber.setTextColor(Color.parseColor("#6A6A6A"))
            }

            // ✅ API에서 받은 데이터로 점 표시
            if (recordedDates.contains(date.dayOfMonth) && date.month == selectedDate.month) {
                dayViewBinding.vRecordDot.visibility = View.VISIBLE
            } else {
                dayViewBinding.vRecordDot.visibility = View.GONE
            }

            dayViewBinding.root.setOnClickListener {
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
            selectedDate = selectedDate.plusWeeks(1)
            refreshCalendarData()
        }

        binding.clPoseRecommend.setOnClickListener {
            startActivity(Intent(requireContext(), PoseRecommendActivity::class.java))
        }

        binding.ivNotification.setOnClickListener {
            startActivity(Intent(requireContext(), NotificationActivity::class.java))
        }

        // ✅ 빈 화면 클릭 → 기록 등록 화면으로 이동
        binding.clEmptyState.setOnClickListener {
            val intent = Intent(requireContext(), EntryRegisterActivity::class.java)
            intent.putExtra("SELECTED_DATE", selectedDate.format(DateTimeFormatter.ofPattern("yyyy.MM.dd")))
            entryRegisterLauncher.launch(intent)
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}