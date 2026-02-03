package com.umc.mobile.my4cut.ui.home

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.umc.mobile.my4cut.R
import com.umc.mobile.my4cut.databinding.FragmentHomeBinding
import com.umc.mobile.my4cut.databinding.ItemCalendarDayBinding
import com.umc.mobile.my4cut.ui.calendar.CalendarFullActivity
import com.umc.mobile.my4cut.ui.calendar.CalendarPickerActivity
import com.umc.mobile.my4cut.ui.notification.NotificationActivity
import com.umc.mobile.my4cut.ui.pose.PoseRecommendActivity
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    // 현재 선택된 날짜 (기본값: 오늘)
    private var selectedDate: LocalDate = LocalDate.now()

    // 캘린더 화면(전체 달력)에서 날짜를 받아오기 위한 런처
    private val startCalendarForResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val dateString = result.data?.getStringExtra("SELECTED_DATE")
            if (dateString != null) {
                selectedDate = LocalDate.parse(dateString)
                refreshCalendarData()
            }
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
        setupWeekCalendar()
        setupClickListeners()
        updateContentState(selectedDate)
    }

    private fun setupDateBanner() {
        // 상단 배너 '오늘' 날짜를 보여주는 것으로 유지
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

    // 주간 캘린더 생성 (selectedDate 기준)
    private fun setupWeekCalendar() {
        val calendarContainer = binding.llWeekCalendar
        calendarContainer.removeAllViews()

        // selectedDate가 포함된 주(일요일 시작) 계산
        // dayOfWeek.value: 월(1) ~ 일(7)
        // 일요일(7) -> % 7 = 0 (일요일이 시작)
        // 월요일(1) -> % 7 = 1
        val daysFromSunday = selectedDate.dayOfWeek.value % 7
        val startOfWeek = selectedDate.minusDays(daysFromSunday.toLong())

        for (i in 0 until 7) {
            val date = startOfWeek.plusDays(i.toLong())
            val dayViewBinding = ItemCalendarDayBinding.inflate(layoutInflater, calendarContainer, false)

            // 요일 표시 (일, 월, 화...)
            dayViewBinding.tvDayOfWeek.text = date.format(DateTimeFormatter.ofPattern("E", Locale.KOREAN))

            // 요일별 색상 지정
            val dayColor = when (date.dayOfWeek) {
                DayOfWeek.SUNDAY -> Color.parseColor("#FF7E67")
                DayOfWeek.SATURDAY -> Color.parseColor("#4B7EFF")
                else -> Color.parseColor("#D1D1D1")
            }
            dayViewBinding.tvDayOfWeek.setTextColor(dayColor)

            // 날짜 숫자 표시
            dayViewBinding.tvDayNumber.text = date.dayOfMonth.toString()

            // 선택된 날짜 UI 처리
            if (date.isEqual(selectedDate)) {
                dayViewBinding.tvDayNumber.setBackgroundResource(R.drawable.bg_calendar_selected)
                dayViewBinding.tvDayNumber.backgroundTintList = null
                dayViewBinding.tvDayNumber.setTextColor(Color.WHITE)
            } else {
                dayViewBinding.tvDayNumber.background = null
                dayViewBinding.tvDayNumber.setTextColor(Color.parseColor("#6A6A6A"))
            }

            // 점 표시 로직 (테스트용: 오늘 & 짝수 날짜)
            if (date.isEqual(LocalDate.now()) || date.dayOfMonth % 2 == 0) {
                dayViewBinding.vRecordDot.visibility = View.VISIBLE
            } else {
                dayViewBinding.vRecordDot.visibility = View.GONE
            }

            // 날짜 클릭 시 해당 날짜 선택
            dayViewBinding.root.setOnClickListener {
                selectedDate = date
                refreshCalendarData()
            }

            // 가중치(weight) 적용하여 균등 분할
            val params = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT)
            params.weight = 1f
            dayViewBinding.root.layoutParams = params
            calendarContainer.addView(dayViewBinding.root)
        }
    }

    private fun setupClickListeners() {
        // 왼쪽 화살표: 1주 전으로 이동
        binding.ivCalendarBack.setOnClickListener {
            selectedDate = selectedDate.minusWeeks(1)
            refreshCalendarData()
        }

        // 오른쪽 화살표: 1주 후로 이동
        binding.ivCalendarNext.setOnClickListener {
            selectedDate = selectedDate.plusWeeks(1)
            refreshCalendarData()
        }

        // 포즈 추천 클릭
        binding.clPoseRecommend.setOnClickListener {
            startActivity(Intent(requireContext(), PoseRecommendActivity::class.java))
        }

        // 알림 클릭
        binding.ivNotification.setOnClickListener {
            startActivity(Intent(requireContext(), NotificationActivity::class.java))
        }

        // 빈 화면 클릭 -> 날짜 선택(Picker) 화면으로 이동
        // (필요 시 수정: EntryRegisterActivity로 이동할지, Picker로 갈지 결정)
        binding.clEmptyState.setOnClickListener {
            // 예: 날짜를 선택해서 업로드 화면으로 이동하고 싶다면
            /*
            val intent = Intent(requireContext(), com.umc.mobile.my4cut.ui.record.EntryRegisterActivity::class.java)
            intent.putExtra("SELECTED_DATE", selectedDate.format(DateTimeFormatter.ofPattern("yyyy.MM.dd")))
            startActivity(intent)
            */
            // 기존 로직 유지 (Picker로 이동)
            startActivity(Intent(requireContext(), CalendarPickerActivity::class.java))
        }
    }

    // 날짜 변경 시 화면 갱신을 담당하는 헬퍼 함수
    private fun refreshCalendarData() {
        setupWeekCalendar()       // 주간 달력 다시 그리기
        updateContentState(selectedDate) // 하단 컨텐츠(일기/빈화면) 갱신
    }

    private fun updateContentState(date: LocalDate) {
        val formatter = DateTimeFormatter.ofPattern("yyyy.MM.dd")
        binding.tvContentDate.text = date.format(formatter)

        // 테스트용: 오늘이거나 짝수 날짜면 기록 있음
        val hasRecord = date.isEqual(LocalDate.now()) || date.dayOfMonth % 2 == 0

        if (hasRecord) {
            binding.clEmptyState.visibility = View.GONE
            binding.llFilledState.visibility = View.VISIBLE
        } else {
            binding.clEmptyState.visibility = View.VISIBLE
            binding.llFilledState.visibility = View.GONE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}