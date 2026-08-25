package com.umc.mobile.my4cut.ui.myalbum

import android.content.Intent
import android.graphics.Color
import android.graphics.Rect
import android.graphics.RectF
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.view.View
import android.widget.FrameLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.umc.mobile.my4cut.databinding.ActivityCalendarPicker2Binding
import com.umc.mobile.my4cut.network.RetrofitClient
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * 날짜 선택 화면 튜토리얼(미니 달력 아이콘 강조)의 위치를 조정하는 값 모음.
 */
private object DateSelectTutorialLayout {
    const val TEXT_GAP_X_DP = 12        // 안내 텍스트 ↔ 배지 원 가로 간격 (원 왼쪽)
    const val TEXT_OFFSET_Y_DP = 8      // 안내 텍스트를 원 상단보다 얼마나 위로 올릴지
    const val ARROW_OFFSET_X_DP = -20     // 화살표의 원 중앙 기준 가로 오프셋
    const val ARROW_GAP_Y_DP = 2        // 배지 원 ↔ 화살표 세로 간격 (원 아래)
    const val ARROW_ROTATION = 0f       // 화살표 회전 각도

    const val CLOSE_MARGIN_END_DP = 20  // 닫기 버튼 ↔ 화면 오른쪽 여백
    const val CLOSE_MARGIN_BOTTOM_DP = 16 // 닫기 버튼 ↔ 화면 아래쪽 여백
}

class CalendarPickerActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCalendarPicker2Binding

    private var currentSelectedDateStr: String = ""

    // ✅ 등록된 날짜 저장
    private val registeredDates = mutableSetOf<LocalDate>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCalendarPicker2Binding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.myCalendar.setHeaderVisible(false)

        // Intent로 전달된 초기 날짜 (홈 화면에서 선택한 날짜)
        val year = intent.getIntExtra("YEAR", LocalDate.now().year)
        val month = intent.getIntExtra("MONTH", LocalDate.now().monthValue)
        val day = intent.getIntExtra("DAY", LocalDate.now().dayOfMonth)
        val initialDate = runCatching { LocalDate.of(year, month, day) }.getOrElse { LocalDate.now() }

        // 초기 선택 날짜를 홈에서 넘겨받은 날짜로 설정
        currentSelectedDateStr = initialDate.format(DateTimeFormatter.ofPattern("yyyy.MM.dd"))

        // 캘린더 날짜 클릭 리스너 추가
        binding.myCalendar.setOnDateSelectedListener { dateText ->
            // 사용자가 날짜를 누를 때마다 변수 갱신
            currentSelectedDateStr = dateText
            Log.d("CalendarPicker", "📅 Selected date updated: $currentSelectedDateStr")
        }

        setupCalendar(year, month)
        setupClickListeners()

        // 캘린더가 준비된 후 초기 날짜로 스크롤 & 선택 상태 반영
        binding.myCalendar.post {
            binding.myCalendar.scrollToDate(initialDate)
        }

        // 날짜 선택 화면 최초 진입 시 1회만 표시되는 튜토리얼 (미니 달력 아이콘 안내)
        binding.root.post {
            showDateSelectTutorialIfNeeded()
        }
    }

    /**
     * 날짜 선택 화면 최초 진입 시 1회만 표시되는 코치마크 튜토리얼.
     * 미니 달력 아이콘 영역은 딤에 투명 구멍을 뚫어(TutorialDimView) 어둡게 가려지지 않고
     * 그대로 보이게 한다. 홈 화면 튜토리얼(MainActivity)과 동일한 방식.
     */
    private fun showDateSelectTutorialIfNeeded() {
        if (DateSelectTutorialPrefs.hasSeenTutorial(this)) return

        val overlay = binding.includeDateSelectTutorial
        overlay.root.visibility = View.VISIBLE
        binding.vMinicalBadge.visibility = View.VISIBLE

        fun boundsOf(target: View): Rect {
            val rootLocation = IntArray(2)
            binding.root.getLocationInWindow(rootLocation)
            val loc = IntArray(2)
            target.getLocationInWindow(loc)
            val left = loc[0] - rootLocation[0]
            val top = loc[1] - rootLocation[1]
            return Rect(left, top, left + target.width, top + target.height)
        }

        fun positionOverlay() {
            val minicalBox = boundsOf(binding.vMinicalBadge)

            // 딤에 스포트라이트(완전 투명) 구멍을 뚫어 실제 미니 달력 배지 원이 어둡게 가려지지 않도록 함
            overlay.tutorialDimView.setHoles(
                listOf(RectF(minicalBox) to minicalBox.width() / 2f)
            )

            placeHighlight(overlay.vHighlightMinical, minicalBox)

            // 안내 텍스트: 배지 원 왼쪽, 살짝 위로 올려서 배치
            overlay.tvTutorialMinical.text = coralHighlightedText(
                "캘린더를 눌러\n직접 날짜를 설정해요.",
                "직접 날짜를 설정"
            )
            overlay.tvTutorialMinical.measure(
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            )
            val textWidth = overlay.tvTutorialMinical.measuredWidth
            val textLeft = minicalBox.left - dpToPx(DateSelectTutorialLayout.TEXT_GAP_X_DP) - textWidth
            val textTop = minicalBox.top - dpToPx(DateSelectTutorialLayout.TEXT_OFFSET_Y_DP)
            (overlay.tvTutorialMinical.layoutParams as FrameLayout.LayoutParams).apply {
                leftMargin = textLeft
                topMargin = textTop
            }
            overlay.tvTutorialMinical.requestLayout()

            // 화살표: 배지 원 아래쪽에 배치
            (overlay.ivTutorialArrowMinical.layoutParams as FrameLayout.LayoutParams).apply {
                leftMargin = minicalBox.centerX() - width / 2 + dpToPx(DateSelectTutorialLayout.ARROW_OFFSET_X_DP)
                topMargin = minicalBox.bottom + dpToPx(DateSelectTutorialLayout.ARROW_GAP_Y_DP)
            }
            overlay.ivTutorialArrowMinical.rotation = DateSelectTutorialLayout.ARROW_ROTATION
            overlay.ivTutorialArrowMinical.requestLayout()

            // 닫기 버튼: 화면 우측 맨 아래에 명시적 좌표로 배치
            overlay.llTutorialClose.bringToFront()
            overlay.llTutorialClose.measure(
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            )
            val closeWidth = overlay.llTutorialClose.measuredWidth
            val closeHeight = overlay.llTutorialClose.measuredHeight
            overlay.llTutorialClose.x =
                (overlay.root.width - closeWidth - dpToPx(DateSelectTutorialLayout.CLOSE_MARGIN_END_DP)).toFloat()
            overlay.llTutorialClose.y =
                (overlay.root.height - closeHeight - dpToPx(DateSelectTutorialLayout.CLOSE_MARGIN_BOTTOM_DP)).toFloat()
        }

        overlay.root.post { positionOverlay() }

        overlay.llTutorialClose.setOnClickListener {
            overlay.root.visibility = View.GONE
            binding.vMinicalBadge.visibility = View.GONE
            DateSelectTutorialPrefs.setTutorialSeen(this)
        }
    }

    private fun placeHighlight(target: View, rect: Rect) {
        (target.layoutParams as FrameLayout.LayoutParams).apply {
            width = rect.width()
            height = rect.height()
            leftMargin = rect.left
            topMargin = rect.top
        }
        target.requestLayout()
    }

    private fun coralHighlightedText(full: String, highlight: String): SpannableStringBuilder {
        val spannable = SpannableStringBuilder(full)
        val start = full.indexOf(highlight)
        if (start >= 0) {
            spannable.setSpan(
                ForegroundColorSpan(Color.parseColor("#FF7E67")),
                start,
                start + highlight.length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
        return spannable
    }

    private fun dpToPx(dp: Int): Int =
        (dp * resources.displayMetrics.density).toInt()

    private fun setupCalendar(year: Int, month: Int) {
        // ✅ API 호출하여 등록된 날짜 가져오기
        lifecycleScope.launch {
            try {
                Log.d("CalendarPicker", "📅 Loading calendar data: $year-$month")

                val response = RetrofitClient.day4CutService.getCalendarStatus(year, month)

                if (response.code == "C2001") {
                    registeredDates.clear()

                    val calendarDataList = response.data?.dates?.map { item ->
                        val date = LocalDate.of(year, month, item.day)
                        registeredDates.add(date)  // ✅ 등록된 날짜 저장

                        CalendarData(
                            date = date,
                            imageUris = if (item.thumbnailUrl != null) listOf(item.thumbnailUrl) else emptyList(),
                            memo = ""
                        )
                    } ?: emptyList()

                    Log.d("CalendarPicker", "✅ Registered dates: $registeredDates")

                    // 캘린더에 데이터 표시
                    binding.myCalendar.setDatesWithData(calendarDataList)
                } else {
                    Log.e("CalendarPicker", "❌ API failed: ${response.code}")
                    registeredDates.clear()
                    // 실패 시 빈 리스트
                    binding.myCalendar.setDatesWithData(emptyList())
                }
            } catch (e: Exception) {
                Log.e("CalendarPicker", "💥 Failed to load calendar", e)
                registeredDates.clear()
                binding.myCalendar.setDatesWithData(emptyList())
            }
        }
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener { finish() }

        // 미니 달력 아이콘 클릭 → 년/월 선택 바텀시트 표시
        binding.ivMiniCal.setOnClickListener {
            val currentMonth = java.time.YearMonth.now()
            // 현재 표시 중인 년/월을 초기값으로 넘겨 바텀시트 열기
            YearMonthPickerBottomSheet.newInstance(
                year = currentMonth.year,
                month = currentMonth.monthValue
            ) { selectedYear, selectedMonth ->
                // 선택한 년/월로 캘린더 이동 및 API 재조회
                val newDate = java.time.LocalDate.of(selectedYear, selectedMonth, 1)
                binding.myCalendar.scrollToDate(newDate)
                setupCalendar(selectedYear, selectedMonth)
            }.show(supportFragmentManager, "YearMonthPicker")
        }

        // ✅ 다음 버튼 클릭 시 체크
        binding.btnNext.setOnClickListener {
            val selectedDateStr = currentSelectedDateStr
            val selectedDate = parseDateFromFormatted(selectedDateStr)

            Log.d("CalendarPicker", "Checking: $selectedDate inside $registeredDates")

            // ✅ 1. 이미 등록된 날짜 체크
            if (registeredDates.contains(selectedDate)) {
                Toast.makeText(this, "이미 등록된 날짜입니다", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // ✅ 2. 미래 날짜 체크
            val isFutureDate = selectedDate.isAfter(LocalDate.now())
            if (isFutureDate) {
                Toast.makeText(this, "미래 날짜는 선택할 수 없습니다", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // ✅ 3. 등록 가능한 날짜 → EntryRegisterActivity로 이동
            val intent = Intent(this, EntryRegisterActivity::class.java)
            intent.putExtra("SELECTED_DATE", selectedDateStr)
            startActivityForResult(intent, REQUEST_REGISTER)
        }
    }

    companion object {
        private const val REQUEST_REGISTER = 1001
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_REGISTER && resultCode == RESULT_OK) {
            // ✅ EntryRegisterActivity에서 저장 완료 시 이 Activity도 종료
            finish()
        }
    }

    private fun parseDateFromFormatted(dateStr: String): LocalDate {
        return try {
            val parts = dateStr.split(".")
            if (parts.size == 3) {
                val year = parts[0].toInt()
                val month = parts[1].toInt()
                val day = parts[2].toInt()
                LocalDate.of(year, month, day)
            } else {
                LocalDate.now()
            }
        } catch (e: Exception) {
            LocalDate.now()
        }
    }
}