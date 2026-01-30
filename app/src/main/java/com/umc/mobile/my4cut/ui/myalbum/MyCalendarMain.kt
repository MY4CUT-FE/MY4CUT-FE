package com.umc.mobile.my4cut.ui.myalbum

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.MotionEvent
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.ImageView
import androidx.core.view.children
import com.bumptech.glide.Glide
import com.google.android.material.card.MaterialCardView
import com.umc.mobile.my4cut.R
import com.umc.mobile.my4cut.databinding.CalendarDayMainBinding
import com.umc.mobile.my4cut.databinding.ViewCustomCalendarMainBinding
import com.kizitonwose.calendar.core.CalendarDay
import com.kizitonwose.calendar.core.CalendarMonth
import com.kizitonwose.calendar.core.DayPosition
import com.kizitonwose.calendar.core.daysOfWeek
import com.kizitonwose.calendar.view.MonthDayBinder
import com.kizitonwose.calendar.view.MonthHeaderFooterBinder
import com.kizitonwose.calendar.view.ViewContainer
import com.kizitonwose.calendar.view.DaySize
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

class MyCalendarMain @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr) {

    private val TAG = this::class.simpleName
    private val binding: ViewCustomCalendarMainBinding =
        ViewCustomCalendarMainBinding.inflate(LayoutInflater.from(context), this, true)

    private var selectedDate: LocalDate? = null
    private var currentMonth = YearMonth.now()
    private var onDateSelectedListener: ((String, CalendarData?) -> Unit)? = null
    private var datesWithDataMap = mutableMapOf<LocalDate, CalendarData>()

    fun setDatesWithData(dataList: List<CalendarData>) {
        datesWithDataMap.clear()
        dataList.forEach { data ->
            datesWithDataMap[data.date] = data
        }

        if (binding.mcCustom.isComputingLayout.not()) {
            val visibleMonth = binding.mcCustom.findFirstVisibleMonth()
            if (visibleMonth != null) {
                binding.mcCustom.notifyMonthChanged(visibleMonth.yearMonth)
            } else {
                binding.mcCustom.notifyCalendarChanged()
            }
        }
    }

    fun setOnDateSelectedListener(listener: (String, CalendarData?) -> Unit) {
        onDateSelectedListener = listener
    }

    fun setHeaderVisible(isVisible: Boolean) {
        binding.btnUpload.visibility = if (isVisible) VISIBLE else GONE
    }

    fun setOnUploadClickListener(listener: () -> Unit) {
        binding.btnUpload.setOnClickListener { listener.invoke() }
    }

    fun setDayLayout(layoutRes: Int) {}

    fun getSelectedDateFormatted(): String {
        return selectedDate?.let {
            "${it.year}.${it.monthValue}.${it.dayOfMonth}"
        } ?: "${currentMonth.year}.${currentMonth.monthValue}"
    }

    init {
        setupCalendar()
    }

    private fun dpToPx(dp: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp.toFloat(),
            resources.displayMetrics
        ).toInt()
    }

    private var didInitialScroll = false
    private fun setupCalendar() {
        with(binding) {
            mcCustom.daySize = DaySize.Rectangle

            updateYearMonthText()

            binding.btnPrevMonth.setOnClickListener {
                currentMonth = currentMonth.minusMonths(1)
                mcCustom.smoothScrollToMonth(currentMonth)
                updateYearMonthText()
            }

            binding.btnNextMonth.setOnClickListener {
                currentMonth = currentMonth.plusMonths(1)
                mcCustom.smoothScrollToMonth(currentMonth)
                updateYearMonthText()
            }

            binding.mcCustom.setOnTouchListener { _, event ->
                event.action == MotionEvent.ACTION_MOVE
            }

            val today = LocalDate.now()
            selectedDate = today
            val todayData = datesWithDataMap[today]

            binding.mcCustom.post {
                binding.mcCustom.notifyDateChanged(today)
                onDateSelectedListener?.invoke(getSelectedDateFormatted(), todayData)
            }

            val startMonth = currentMonth.minusMonths(100)
            val endMonth = currentMonth.plusMonths(100)
            val daysOfWeek: List<DayOfWeek> = daysOfWeek()

            mcCustom.setup(startMonth, endMonth, daysOfWeek.first())
            if (!didInitialScroll) {
                didInitialScroll = true
                mcCustom.scrollToMonth(currentMonth)
            }

            mcCustom.monthScrollListener = { month ->
                currentMonth = month.yearMonth
                updateYearMonthText()
            }

            mcCustom.monthHeaderBinder = object : MonthHeaderFooterBinder<MainMonthViewContainer> {
                override fun create(view: View) = MainMonthViewContainer(view)
                override fun bind(container: MainMonthViewContainer, data: CalendarMonth) {
                    if (container.titlesContainer.tag == null) {
                        container.titlesContainer.tag = data.yearMonth
                        container.titlesContainer.children.map { it as TextView }
                            .forEachIndexed { index, textView ->
                                if (index < daysOfWeek.size) {
                                    val dayOfWeek = daysOfWeek[index]
                                    val title = dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.KOREAN)
                                    textView.text = title
                                    when (dayOfWeek) {
                                        DayOfWeek.SUNDAY -> textView.setTextColor(Color.RED)
                                        DayOfWeek.SATURDAY -> textView.setTextColor(Color.BLUE)
                                        else -> textView.setTextColor(Color.GRAY)
                                    }
                                }
                            }
                    }
                }
            }

            mcCustom.dayBinder = object : MonthDayBinder<MainDayViewContainer> {
                override fun create(view: View): MainDayViewContainer {
                    val layoutParams = view.layoutParams
                    // [수정] 높이를 100dp로 고정 (모든 월 비율 통일)
                    layoutParams.height = dpToPx(100)
                    view.layoutParams = layoutParams
                    return MainDayViewContainer(view)
                }

                override fun bind(container: MainDayViewContainer, data: CalendarDay) {
                    if (data.position != DayPosition.MonthDate) {
                        container.textView.visibility = INVISIBLE
                        container.cardImage.visibility = INVISIBLE
                        container.dayImage.visibility = INVISIBLE

                        container.cardImage.setCardBackgroundColor(Color.TRANSPARENT)
                        container.cardImage.strokeWidth = 0
                        return
                    } else {
                        container.textView.visibility = VISIBLE
                        container.cardImage.visibility = VISIBLE
                    }

                    container.textView.text = data.date.dayOfMonth.toString()

                    val dayData = datesWithDataMap[data.date]
                    val isSelected = selectedDate == data.date
                    val firstImage = dayData?.imageUris?.firstOrNull()
                    val isToday = data.date == LocalDate.now()
                    val isFutureDate = data.date.isAfter(LocalDate.now())

                    // 이미지 처리
                    if (!firstImage.isNullOrEmpty()) {
                        container.dayImage.visibility = VISIBLE
                        Glide.with(container.view.context)
                            .load(firstImage)
                            .centerCrop()
                            .into(container.dayImage)
                        container.cardImage.setCardBackgroundColor(Color.TRANSPARENT)
                    } else {
                        container.dayImage.visibility = INVISIBLE
                        container.dayImage.setImageDrawable(null)

                        if (isSelected) {
                            container.cardImage.setCardBackgroundColor(Color.parseColor("#FFD5CD"))
                        } else {
                            container.cardImage.setCardBackgroundColor(Color.parseColor("#FFF4F2"))
                        }
                    }

                    // 테두리 처리
                    if (isToday) {
                        container.cardImage.strokeWidth = 4
                        container.cardImage.strokeColor = Color.parseColor("#FE927F")
                    } else {
                        container.cardImage.strokeWidth = 0
                    }

                    // 텍스트 스타일
                    container.textView.background = null
                    when {
                        isToday -> {
                            container.textView.setTextColor(Color.parseColor("#FE927F"))
                            container.textView.typeface = android.graphics.Typeface.DEFAULT_BOLD
                        }
                        isFutureDate -> container.textView.setTextColor(Color.GRAY)
                        data.position == DayPosition.MonthDate -> container.textView.setTextColor(Color.BLACK)
                        else -> container.textView.setTextColor(Color.GRAY)
                    }

                    if (selectedDate == data.date && !isFutureDate) {
                        container.textView.background = GradientDrawable().apply {
                            shape = GradientDrawable.OVAL
                            setColor(Color.parseColor("#FE927F"))
                        }
                        container.textView.setTextColor(Color.WHITE)
                        container.textView.gravity = Gravity.CENTER
                    }

                    val onClickListener = View.OnClickListener {
                        if (data.position == DayPosition.MonthDate && !isFutureDate) {
                            if (selectedDate != data.date) {
                                val oldDate = selectedDate
                                selectedDate = data.date
                                oldDate?.let { mcCustom.notifyDateChanged(it) }
                                mcCustom.notifyDateChanged(data.date)
                                onDateSelectedListener?.invoke(getSelectedDateFormatted(), dayData)
                            }
                        }
                    }
                    container.textView.setOnClickListener(onClickListener)
                    container.dayImage.setOnClickListener(onClickListener)
                    container.cardImage.setOnClickListener(onClickListener)
                }
            }
        }
    }

    private fun updateYearMonthText() {
        binding.tvYearMonth.text = "${currentMonth.monthValue}월"
    }
}

class MainDayViewContainer(view: View) : ViewContainer(view) {
    private val binding = CalendarDayMainBinding.bind(view)
    val textView: TextView = binding.calendarDayText
    val dayImage: ImageView = binding.dayImage
    val cardImage: MaterialCardView = binding.cardImage as MaterialCardView
}

class MainMonthViewContainer(view: View) : ViewContainer(view) {
    val titlesContainer = view as ViewGroup
}