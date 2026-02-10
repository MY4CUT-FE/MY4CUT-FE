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

class CalendarPickerActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCalendarPicker2Binding

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

        // ✅ Intent로 받은 년/월 정보
        val year = intent.getIntExtra("YEAR", LocalDate.now().year)
        val month = intent.getIntExtra("MONTH", LocalDate.now().monthValue)

        setupCalendar(year, month)
        setupClickListeners()
    }

    private fun setupCalendar(year: Int, month: Int) {
        // ✅ API 호출하여 등록된 날짜 가져오기
        lifecycleScope.launch {
            try {
                Log.d("CalendarPicker", "📅 Loading calendar data: $year-$month")

                val response = RetrofitClient.day4CutService.getCalendarStatus(year, month)

                if (response.code == "C2001") {
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
                    // 실패 시 빈 리스트
                    binding.myCalendar.setDatesWithData(emptyList())
                }
            } catch (e: Exception) {
                Log.e("CalendarPicker", "💥 Failed to load calendar", e)
                binding.myCalendar.setDatesWithData(emptyList())
            }
        }
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener { finish() }

        // ✅ 다음 버튼 클릭 시 체크
        binding.btnNext.setOnClickListener {
            val selectedDateStr = binding.myCalendar.getSelectedDateFormatted()
            val selectedDate = parseDateFromFormatted(selectedDateStr)

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