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

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentCalendarChildBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.myCalendar.setHeaderVisible(true)

        // 마지막으로 보던 달이 기억되어 있으면 그 달로, 없으면(최초 진입) 기본 현재 달 그대로 사용
        val savedYear = lastViewedYear
        val savedMonth = lastViewedMonth
        if (savedYear != null && savedMonth != null) {
            binding.myCalendar.scrollToMonth(savedYear, savedMonth)
        }

        val year = binding.myCalendar.getCurrentYear()
        val month = binding.myCalendar.getCurrentMonth()

        fetchCalendarData(year, month)

        setupClickListeners()

        // 당겨서 새로고침
        binding.swipeRefresh.setOnRefreshListener {
            val currentYear = binding.myCalendar.getCurrentYear()
            val currentMonth = binding.myCalendar.getCurrentMonth()
            fetchCalendarData(currentYear, currentMonth)
        }
    }

    override fun onResume() {
        super.onResume()
        val year = binding.myCalendar.getCurrentYear()
        val month = binding.myCalendar.getCurrentMonth()
        fetchCalendarData(year, month)
    }

    private fun fetchCalendarData(year: Int, month: Int) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                Log.d("CalendarChild", "Fetching calendar data: $year-$month")

                val response = RetrofitClient.day4CutService.getCalendarStatus(year, month)

                Log.d("CalendarChild", "Response: code=${response.code}, message=${response.message}")

                if (response.code == "C2001") {
                    val calendarDataList = response.data?.dates?.map { item ->
                        CalendarData(
                            date = LocalDate.of(year, month, item.day),
                            imageUris = if (item.thumbnailUrl != null) listOf(item.thumbnailUrl) else emptyList(),
                            memo = ""
                        )
                    } ?: emptyList()

                    Log.d("CalendarChild", "Calendar data loaded: ${calendarDataList.size} days with records")

                    binding.myCalendar.setDatesWithData(calendarDataList)
                } else {
                    Log.e("CalendarChild", "API failed: ${response.code} - ${response.message}")
                }
            } catch (e: Exception) {
                Log.e("CalendarChild", "Calendar load failed", e)
            } finally {
                binding.swipeRefresh.isRefreshing = false
            }
        }
    }

    private fun setupClickListeners() {
        // Upload button click
        binding.myCalendar.setOnUploadClickListener {
            val intent = Intent(requireContext(), CalendarPickerActivity::class.java)

            // Pass current year and month to CalendarPickerActivity
            val year = binding.myCalendar.getCurrentYear()
            val month = binding.myCalendar.getCurrentMonth()

            intent.putExtra("YEAR", year)
            intent.putExtra("MONTH", month)

            startActivity(intent)
        }

        // Month change listener
        binding.myCalendar.setOnMonthChangeListener { year, month ->
            // 사용자가 달을 넘길 때마다 마지막으로 보던 달을 기억해둠
            lastViewedYear = year
            lastViewedMonth = month
            fetchCalendarData(year, month)
        }

        // Date selection listener
        binding.myCalendar.setOnDateSelectedListener { dateText, data ->
            if (data != null) {
                // 상세 화면으로 넘어가는 시점의 달도 기억해둠 (달을 안 넘기고 바로 클릭한 경우 대비)
                lastViewedYear = binding.myCalendar.getCurrentYear()
                lastViewedMonth = binding.myCalendar.getCurrentMonth()

                val entryDetailFragment = EntryDetailFragment().apply {
                    arguments = Bundle().apply {
                        putString("SELECTED_DATE", dateText)
                        putString("API_DATE", binding.myCalendar.getSelectedDateApiFormat())
                    }
                }

                // ✅ 백스택에 추가하여 뒤로가기 가능하도록
                requireActivity().supportFragmentManager.beginTransaction()
                    .replace(R.id.fcv_main, entryDetailFragment)
                    .addToBackStack(null)
                    .commit()
            }
        }
    }

    companion object {
        // 프래그먼트가 통째로 재생성돼도(뒤로가기 등) 유지되도록 companion object에 저장
        private var lastViewedYear: Int? = null
        private var lastViewedMonth: Int? = null
    }
}