package com.umc.mobile.my4cut.ui.myalbum

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.umc.mobile.my4cut.MainActivity
import com.umc.mobile.my4cut.R
import com.umc.mobile.my4cut.network.RetrofitClient
import com.umc.mobile.my4cut.databinding.FragmentCalendarChildBinding
import kotlinx.coroutines.launch
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
        binding.myCalendar.setHeaderVisible(true)

        val year = binding.myCalendar.getCurrentYear()
        val month = binding.myCalendar.getCurrentMonth()

        // fetchCalendarData(year, month)

        setupClickListeners()
    }

//    private fun fetchCalendarData(year: Int, month: Int) {
//        viewLifecycleOwner.lifecycleScope.launch {
//            try {
//                val response = RetrofitClient.day4CutService.getCalendarStatus(year, month)
//                if (response.code == "SUCCESS") {
//                    // 서버 응답 데이터를 CalendarData 리스트로 변환 (대표사진 포함)
//                    val calendarDataList = response.data?.dates?.map { item ->
//                        CalendarData(
//                            date = LocalDate.of(year, month, item.day),
//                            imageUris = if (item.thumbnailUrl != null) listOf(item.thumbnailUrl) else emptyList(),
//                            memo = ""
//                        )
//                    } ?: emptyList()
//
//                    // 달력에 데이터 전달 -> 이미지 렌더링
//                    binding.myCalendar.setDatesWithData(calendarDataList)
//                }
//            } catch (e: Exception) {
//                Log.e("API_ERROR", "달력 로드 실패: ${e.message}")
//            }
//        }
//    }

//    private fun setupCalendar() {
//        // 1. 테스트용 임의 데이터 생성 (오늘, 어제, 그저께 등)
//        val today = LocalDate.now()
//        val path1 = "android.resource://${requireContext().packageName}/${R.drawable.image1}"
//        val path2 = "android.resource://${requireContext().packageName}/${R.drawable.image2}"
//        val path3 = "android.resource://${requireContext().packageName}/${R.drawable.image3}"
//
//        dummyDates = arrayListOf(
//            CalendarData(today, listOf(path1, path2), "오늘 찍은 네컷사진"),
//            CalendarData(today.minusDays(2), listOf(path2, path3), "사진도 찍고 기록한 날")
//        )
//
//        // 2. 달력에 데이터 전달
//        binding.myCalendar.setDatesWithData(dummyDates)
//    }

    private fun setupClickListeners() {
        binding.myCalendar.setOnUploadClickListener {
            val intent = Intent(requireContext(), CalendarPickerActivity::class.java)
            // 현재 달력에서 선택된 날짜 문자열 (예: "2026.2.7")
            val selectedDateText = binding.myCalendar.getSelectedDateFormatted()

            intent.putExtra("SELECTED_DATE", selectedDateText)

            startActivity(intent)
        }

        binding.myCalendar.setOnMonthChangeListener { year, month ->
            // fetchCalendarData(year, month)
        }

        binding.myCalendar.setOnDateSelectedListener { dateText, data ->
            if (data != null) {
                val entryDetailFragment = EntryDetailFragment().apply {
                    arguments = Bundle().apply {
                        // 화면 상단 표시용 (예: 2026.2.7)
                        putString("SELECTED_DATE", dateText)
                        // API 상세 조회용 (예: 2026-02-07)
                        putString("API_DATE", binding.myCalendar.getSelectedDateApiFormat())
                    }
                }
                (requireActivity() as? MainActivity)?.changeFragment(entryDetailFragment)
            }
        }
    }
}