package com.example.my4cut.ui.myalbum

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.my4cut.ui.myalbum.CalendarData
import com.example.my4cut.R
import com.example.my4cut.databinding.ActivityCalendarPickerBinding
import java.time.LocalDate

class CalendarPickerActivity : AppCompatActivity() {
    lateinit var binding: ActivityCalendarPickerBinding
    private val TAG = this::class.simpleName

    private var dummyDates: ArrayList<CalendarData> = arrayListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCalendarPickerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // binding.myCalendar.setDayLayout(R.layout.calendar_day_layout)
        binding.myCalendar.setHeaderVisible(false)

        setupCalendar()
        setupClickListeners()
    }

    private fun setupCalendar() {
        val today = LocalDate.now()

        val calendarDataList = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            intent.getSerializableExtra("CALENDAR_DATA", ArrayList::class.java) as? ArrayList<CalendarData>
        } else {
            intent.getSerializableExtra("CALENDAR_DATA") as? ArrayList<CalendarData>
        }

        calendarDataList?.let {
            dummyDates = it
            binding.myCalendar.setDatesWithData(dummyDates)
        }
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener { finish() }

        // 데이터가 들어있는 날짜면 데이터를 전달
        binding.btnNext.setOnClickListener {
            val intent = Intent(this, EntryRegisterActivity::class.java)
            val selectedDateStr = binding.myCalendar.getSelectedDateFormatted()
            val selectedData = dummyDates.find { data ->
                val dataDateStr = String.format("%d.%d.%d",
                    data.date.year, data.date.monthValue, data.date.dayOfMonth)

                dataDateStr == selectedDateStr
            }

            intent.putExtra("SELECTED_DATE", selectedDateStr)
            intent.putExtra("SELECTED_DATA", selectedData)

            startActivity(intent)
        }
    }
}