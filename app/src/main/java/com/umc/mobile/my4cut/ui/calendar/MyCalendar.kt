package com.umc.mobile.my4cut.ui.calendar

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.children
import com.umc.mobile.my4cut.R
import com.umc.mobile.my4cut.databinding.CalendarDayLayoutBinding
import com.umc.mobile.my4cut.databinding.ViewCustomCalendarBinding
import com.kizitonwose.calendar.core.CalendarDay
import com.kizitonwose.calendar.core.CalendarMonth
import com.kizitonwose.calendar.core.DayPosition
import com.kizitonwose.calendar.core.daysOfWeek
import com.kizitonwose.calendar.view.MonthDayBinder
import com.kizitonwose.calendar.view.MonthHeaderFooterBinder
import com.kizitonwose.calendar.view.ViewContainer
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

class MyCalendar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val binding: ViewCustomCalendarBinding =
        ViewCustomCalendarBinding.inflate(LayoutInflater.from(context), this, true)

    private var selectedDate: LocalDate? = null
    private var currentMonth = YearMonth.now()
    private var onDateSelectedListener: ((LocalDate) -> Unit)? = null
    private var datesWithDataMap = mutableMapOf<LocalDate, CalendarData>()

    fun setDatesWithData(dataList: List<CalendarData>) {
        datesWithDataMap.clear()
        dataList.forEach { data -> datesWithDataMap[data.date] = data }
        binding.mcCustom.notifyCalendarChanged()
    }

    fun setOnDateSelectedListener(listener: (LocalDate) -> Unit) {
        onDateSelectedListener = listener
    }

    fun setHeaderVisible(isVisible: Boolean) {
        binding.btnUpload.visibility = if (isVisible) View.VISIBLE else View.GONE
    }

    init {
        setupCalendar()
    }

    private fun setupCalendar() {
        with(binding) {
            updateYearMonthText()

            btnPrevMonth.setOnClickListener {
                currentMonth = currentMonth.minusMonths(1)
                mcCustom.smoothScrollToMonth(currentMonth)
                updateYearMonthText()
            }

            btnNextMonth.setOnClickListener {
                currentMonth = currentMonth.plusMonths(1)
                mcCustom.smoothScrollToMonth(currentMonth)
                updateYearMonthText()
            }

            val startMonth = currentMonth.minusMonths(100)
            val endMonth = currentMonth.plusMonths(100)
            val daysOfWeek = daysOfWeek()

            mcCustom.setup(startMonth, endMonth, daysOfWeek.first())
            mcCustom.scrollToMonth(currentMonth)

            mcCustom.monthScrollListener = { month ->
                currentMonth = month.yearMonth
                updateYearMonthText()
            }

            // 요일 헤더 (일, 월, 화...)
            mcCustom.monthHeaderBinder = object : MonthHeaderFooterBinder<MonthViewContainer> {
                override fun create(view: View) = MonthViewContainer(view)
                override fun bind(container: MonthViewContainer, data: CalendarMonth) {
                    if (container.titlesContainer.tag == null) {
                        container.titlesContainer.tag = data.yearMonth
                        container.titlesContainer.children.map { it as TextView }
                            .forEachIndexed { index, textView ->
                                val dayOfWeek = daysOfWeek[index]
                                val title = dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.KOREAN)
                                textView.text = title
                                when (dayOfWeek) {
                                    DayOfWeek.SUNDAY -> textView.setTextColor(Color.parseColor("#FF7E67"))
                                    DayOfWeek.SATURDAY -> textView.setTextColor(Color.parseColor("#4B7EFF"))
                                    else -> textView.setTextColor(Color.parseColor("#D1D1D1"))
                                }
                            }
                    }
                }
            }

            // 날짜 바인더
            mcCustom.dayBinder = object : MonthDayBinder<DayViewContainer> {
                override fun create(view: View) = DayViewContainer(view)
                override fun bind(container: DayViewContainer, data: CalendarDay) {
                    container.textView.text = data.date.dayOfMonth.toString()
                    val dayData = datesWithDataMap[data.date]

                    // 데이터 점 표시
                    if (dayData != null && data.position == DayPosition.MonthDate) {
                        container.dataDot.visibility = View.VISIBLE
                    } else {
                        container.dataDot.visibility = View.INVISIBLE
                    }

                    // 초기화
                    container.textView.background = null
                    container.textView.gravity = Gravity.CENTER

                    val isSelected = selectedDate == data.date
                    val isToday = data.date == LocalDate.now()
                    val isFutureDate = data.date.isAfter(LocalDate.now())

                    // 텍스트 색상 및 클릭 리스너 처리
                    if (data.position == DayPosition.MonthDate) {
                        if (isFutureDate) {
                            container.textView.setTextColor(Color.parseColor("#D1D1D1")) // 미래
                        } else {
                            container.textView.setTextColor(Color.parseColor("#6A6A6A")) // 평소
                        }

                        // 선택된 날짜 디자인
                        if (isSelected) {
                            container.textView.background = GradientDrawable().apply {
                                shape = GradientDrawable.OVAL
                                setColor(Color.parseColor("#FF7E67")) // 주황색 선택 원
                                setSize(80, 80)
                            }
                            container.textView.setTextColor(Color.WHITE)
                        }

                        container.rootView.setOnClickListener {
                            if (!isFutureDate) { // 미래가 아니면 선택 가능
                                if (selectedDate != data.date) {
                                    val oldDate = selectedDate
                                    selectedDate = data.date
                                    oldDate?.let { mcCustom.notifyDateChanged(it) }
                                    mcCustom.notifyDateChanged(data.date)
                                    onDateSelectedListener?.invoke(data.date)
                                }
                            }
                        }
                    } else {
                        container.textView.setTextColor(Color.TRANSPARENT) // 이번달 아니면 안보이게
                    }
                }
            }
        }
    }

    private fun updateYearMonthText() {
        binding.tvYearMonth.text = "${currentMonth.year}년 ${currentMonth.monthValue}월"
    }
}

class DayViewContainer(view: View) : ViewContainer(view) {
    val rootView = view
    val textView: TextView
    val dataDot: View

    init {
        val b = CalendarDayLayoutBinding.bind(view)
        textView = b.calendarDayText
        dataDot = b.viewDataDot
    }
}

class MonthViewContainer(view: View) : ViewContainer(view) {
    val titlesContainer = view as ViewGroup
}