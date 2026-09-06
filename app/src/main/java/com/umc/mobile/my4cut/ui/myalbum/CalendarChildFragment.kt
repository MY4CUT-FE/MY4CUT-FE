package com.umc.mobile.my4cut.ui.myalbum

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.umc.mobile.my4cut.R
import com.umc.mobile.my4cut.data.network.RetrofitClient
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

        val savedYear = lastViewedYear
        val savedMonth = lastViewedMonth
        if (savedYear != null && savedMonth != null) {
            binding.myCalendar.scrollToMonth(savedYear, savedMonth)
        }

        val year = binding.myCalendar.getCurrentYear()
        val month = binding.myCalendar.getCurrentMonth()

        fetchCalendarData(year, month)

        setupClickListeners()

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
                val response = RetrofitClient.day4CutService.getCalendarStatus(year, month)

                if (response.code == "C2001") {
                    val calendarDataList = response.data?.dates?.map { item ->
                        CalendarData(
                            date = LocalDate.of(year, month, item.day),
                            imageUris = if (item.thumbnailUrl != null) listOf(item.thumbnailUrl) else emptyList(),
                            memo = ""
                        )
                    } ?: emptyList()

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
        binding.myCalendar.setOnUploadClickListener {
            val intent = Intent(requireContext(), CalendarPickerActivity::class.java)

            val year = binding.myCalendar.getCurrentYear()
            val month = binding.myCalendar.getCurrentMonth()

            intent.putExtra("YEAR", year)
            intent.putExtra("MONTH", month)

            startActivity(intent)
        }

        binding.myCalendar.setOnMonthChangeListener { year, month ->
            lastViewedYear = year
            lastViewedMonth = month
            fetchCalendarData(year, month)
        }

        binding.myCalendar.setOnDateSelectedListener { dateText, data ->
            if (data != null) {
                lastViewedYear = binding.myCalendar.getCurrentYear()
                lastViewedMonth = binding.myCalendar.getCurrentMonth()

                val entryDetailFragment = EntryDetailFragment().apply {
                    arguments = Bundle().apply {
                        putString("SELECTED_DATE", dateText)
                        putString("API_DATE", binding.myCalendar.getSelectedDateApiFormat())
                    }
                }

                requireActivity().supportFragmentManager.beginTransaction()
                    .replace(R.id.fcv_main, entryDetailFragment)
                    .addToBackStack(null)
                    .commit()
            }
        }
    }

    companion object {
        private var lastViewedYear: Int? = null
        private var lastViewedMonth: Int? = null
    }
}