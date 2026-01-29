package com.example.my4cut.ui.myalbum

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.my4cut.MainActivity
import com.example.my4cut.ui.myalbum.CalendarData
import com.example.my4cut.R
import com.example.my4cut.databinding.FragmentCalendarChildBinding
import java.time.LocalDate

class CalendarChildFragment : Fragment() {
    private lateinit var binding: FragmentCalendarChildBinding

    private var dummyDates: ArrayList<CalendarData> = arrayListOf()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentCalendarChildBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // binding.myCalendar.setDayLayout(R.layout.calendar_day_main)
        binding.myCalendar.setHeaderVisible(true)

        setupCalendar()
        setupClickListeners()
    }

    private fun setupCalendar() {
        // 1. 테스트용 임의 데이터 생성 (오늘, 어제, 그저께 등)
        val today = LocalDate.now()
        val path1 = "android.resource://${requireContext().packageName}/${R.drawable.image1}"
        val path2 = "android.resource://${requireContext().packageName}/${R.drawable.image2}"
        val path3 = "android.resource://${requireContext().packageName}/${R.drawable.image3}"

        dummyDates = arrayListOf(
            CalendarData(today, listOf(path1, path2), "오늘 찍은 네컷사진"),
            CalendarData(today.minusDays(2), listOf(path2, path3), "사진도 찍고 기록한 날")
        )

        // 2. 달력에 데이터 전달
        binding.myCalendar.setDatesWithData(dummyDates)
    }

    private fun setupClickListeners() {
        binding.myCalendar.setOnUploadClickListener {
            val intent = Intent(requireContext(), CalendarPickerActivity::class.java)
            intent.putExtra("CALENDAR_DATA", dummyDates)

            startActivity(intent)
        }

        binding.myCalendar.setOnDateSelectedListener { dateText, data ->
            if (data == null) return@setOnDateSelectedListener

            val entryDetailFragment = EntryDetailFragment().apply {
                arguments = Bundle().apply {
                    putString("selected_date", dateText)
                    putSerializable("calendar_data", data)
                }
            }

            (requireActivity() as? MainActivity)?.changeFragment(entryDetailFragment)
        }
    }
}