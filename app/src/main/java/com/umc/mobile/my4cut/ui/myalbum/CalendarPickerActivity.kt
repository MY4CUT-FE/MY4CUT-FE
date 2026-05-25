package com.umc.mobile.my4cut.ui.myalbum

import android.content.Intent
import android.os.Bundle
import android.util.Log
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
    }

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