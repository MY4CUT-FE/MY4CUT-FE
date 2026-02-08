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
                    if (response.code == "SUCCESS" || response.code == "200") {
                        val calendarData = response.data
                        if (calendarData != null) {
                            Log.d("HomeFragment", "✅ Calendar loaded: ${calendarData.dates.size} dates")

                            // 기록이 있는 날짜 저장
                            recordedDates.clear()
                            calendarData.dates.forEach { date ->
                                recordedDates.add(date.day)
                            }

                            setupWeekCalendar() // 주간 캘린더 다시 그리기
                        } else {
                            Log.e("HomeFragment", "❌ Calendar data is null")
                        }
                    } else {
                        Log.e("HomeFragment", "❌ Calendar load failed: ${response.message}")
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
                    if (response.code == "SUCCESS" || response.code == "200") {
                        val day4cut = response.data
                        if (day4cut != null) {
                            Log.d("HomeFragment", "✅ Day4cut loaded: ${day4cut.id}")
                            showFilledState(day4cut)
                        } else {
                            Log.d("HomeFragment", "⚠️ No data for this date")
                            showEmptyState()
                        }
                    } else {
                        Log.d("HomeFragment", "⚠️ No record for this date: ${response.message}")
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

        // TODO: 실제 이미지와 내용 표시 (추후 구현)
        // binding.ivDay4CutImage.load(day4cut.fileUrl.firstOrNull())
        // binding.tvContent.text = day4cut.content
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