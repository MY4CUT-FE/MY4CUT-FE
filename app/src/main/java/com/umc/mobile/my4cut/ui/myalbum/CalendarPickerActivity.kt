package com.umc.mobile.my4cut.ui.myalbum

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.umc.mobile.my4cut.databinding.ActivityCalendarPicker2Binding
import com.umc.mobile.my4cut.databinding.ActivityCalendarPickerBinding
import com.umc.mobile.my4cut.network.RetrofitClient
import kotlinx.coroutines.launch
import java.time.LocalDate

class CalendarPickerActivity : AppCompatActivity() {
    lateinit var binding: ActivityCalendarPicker2Binding
    private val TAG = this::class.simpleName

    private var dummyDates: ArrayList<CalendarData> = arrayListOf()

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

        setupCalendar()
        setupClickListeners()
    }

    private fun setupCalendar() {
        val today = LocalDate.now()
        val currentYear = today.year
        val currentMonth = today.monthValue

        lifecycleScope.launch {
            try {
                // 월별 기록 날짜 + 대표 이미지 조회 API 호출
                val response =
                    RetrofitClient.day4CutService.getCalendarStatus(currentYear, currentMonth)

                if (response.code == "D2000") {
                    val calendarStatusList = response.data?.dates ?: emptyList()

                    // 3. 받아온 데이터를 캘린더 뷰가 이해할 수 있는 CalendarData 형태로 변환
                    // (서버 응답 객체를 CalendarData 리스트로 매핑하는 과정)
                    val mappedDates = calendarStatusList.map { status ->
                        val formattedDate = LocalDate.of(currentYear, currentMonth, status.day)
                        CalendarData(
                            date = formattedDate, // "2026-02-09" -> LocalDate
                            imageUris = arrayListOf(status.thumbnailUrl ?: "")
                        )
                    }.toCollection(ArrayList())

                    dummyDates.clear()
                    dummyDates.addAll(mappedDates)

                    // 4. 캘린더에 데이터 설정 (점이 찍히거나 이미지가 표시됨)
                    binding.myCalendar.setDatesWithData(dummyDates)
                }
            } catch (e: Exception) {
                Log.e("API_ERROR", "캘린더 상태 조회 실패: ${e.message}")
            }
        }
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener { finish() }

        // 데이터가 들어있는 날짜면 데이터를 전달
        binding.btnNext.setOnClickListener {
            val selectedDateStr = binding.myCalendar.getSelectedDateFormatted() // "2026-02-09" 형태
            val intent = Intent(this, EntryRegisterActivity::class.java).apply {
                putExtra("SELECTED_DATE", selectedDateStr)
            }

            startActivity(intent)
        }
    }
}