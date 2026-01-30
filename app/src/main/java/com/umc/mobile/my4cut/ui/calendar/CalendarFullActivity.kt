package com.umc.mobile.my4cut.ui.calendar

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.umc.mobile.my4cut.databinding.ActivityCalendarFullBinding
import java.time.LocalDate

class CalendarFullActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCalendarFullBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityCalendarFullBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 상단 [네컷 업로드] 버튼 숨기기
        binding.myCalendar.setHeaderVisible(false)

        setupCalendarData()
        setupListeners()
    }

    private fun setupCalendarData() {
        // 더미 데이터 (홈 화면과 동일하게 맞춤 - 테스트용)
        val today = LocalDate.now()
        val dummyDates = listOf(
            CalendarData(today, null, "오늘"),
            CalendarData(today.minusDays(2), null, "그저께")
        )
        binding.myCalendar.setDatesWithData(dummyDates)
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener {
            finish() // 그냥 닫기
        }

        // 날짜 선택 시 홈 화면으로 데이터 전달
        binding.myCalendar.setOnDateSelectedListener { selectedDate ->
            // 1. 결과 데이터 -> (Intent)
            val resultIntent = Intent()
            resultIntent.putExtra("SELECTED_DATE", selectedDate.toString()) // "2026-01-25" 형태

            // 2. 성공 신호(RESULT_OK)와 함께 데이터 세팅
            setResult(Activity.RESULT_OK, resultIntent)

            // 3. 액티비티 종료 -> 자동으로 홈 화면으로 돌아감
            finish()
        }
    }
}