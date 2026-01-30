package com.umc.mobile.my4cut.ui.calendar

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.umc.mobile.my4cut.databinding.ActivityCalendarPickerBinding
import com.umc.mobile.my4cut.ui.record.EntryRegisterActivity
import java.time.LocalDate

class CalendarPickerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCalendarPickerBinding

    // 기본 선택 날짜: 오늘
    private var selectedDate: LocalDate = LocalDate.now()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCalendarPickerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.myCalendar.setHeaderVisible(false) // 상단 메뉴 숨김

        setupCalendar()
        setupListeners()
    }

    private fun setupCalendar() {
        // 더미 데이터 없이 날짜만 선택하는 모드
        // 빈 리스트를 넣어서 초기화만 시켜줌.
        binding.myCalendar.setDatesWithData(emptyList())
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener { finish() }

        // 1. 날짜 클릭 시: 변수에만 저장 (화면 이동 X)
        binding.myCalendar.setOnDateSelectedListener { date ->
            selectedDate = date
        }

        // 2. 다음 버튼 클릭 시: 등록 화면으로 이동
        binding.btnNext.setOnClickListener {
            val intent = Intent(this, EntryRegisterActivity::class.java)
            // 선택한 날짜를 문자열로 넘겨줌 (EX. "2026-01-25")
            intent.putExtra("SELECTED_DATE", selectedDate.toString())
            startActivity(intent)
        }
    }
}