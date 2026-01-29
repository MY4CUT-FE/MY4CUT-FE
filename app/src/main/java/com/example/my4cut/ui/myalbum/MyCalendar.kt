package com.example.my4cut.ui.myalbum

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
import androidx.cardview.widget.CardView
import androidx.core.view.children
import com.bumptech.glide.Glide
import com.example.my4cut.ui.myalbum.CalendarData
import com.example.my4cut.R
import com.example.my4cut.databinding.CalendarDayLayoutBinding
import com.example.my4cut.databinding.CalendarDayMainBinding
import com.example.my4cut.databinding.ViewCustomCalendarBinding
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
import android.view.MotionEvent

class MyCalendar @JvmOverloads constructor( // 날짜 선택 캘린더
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr) {

    private val TAG = this::class.simpleName
    private val binding: ViewCustomCalendarBinding =
        ViewCustomCalendarBinding.inflate(LayoutInflater.from(context), this, true)

    private var selectedDate: LocalDate? = null
    private var currentMonth = YearMonth.now()

    private var onDateSelectedListener: ((String) -> Unit)? = null

    // 데이터가 있는 날짜들을 저장할 세트 (중복 방지 및 빠른 검색을 위해 Set 권장)
    private var datesWithDataMap = mutableMapOf<LocalDate, CalendarData>()

    private var currentDayLayoutRes = R.layout.calendar_day_layout

    // 외부(Fragment 등)에서 데이터를 설정할 수 있는 함수
    fun setDatesWithData(dataList: List<CalendarData>) {
        Log.e("LOOP", "setDatesWithData() called")
        datesWithDataMap.clear()
        dataList.forEach { data ->
            datesWithDataMap[data.date] = data
        }

        if (binding.mcCustom.isComputingLayout.not()) {
            // 1. 현재 화면에 보이는 월(Month) 정보를 가져옵니다.
            val visibleMonth = binding.mcCustom.findFirstVisibleMonth()

            if (visibleMonth != null) {
                // 2. 전체가 아닌 현재 보이는 달만 갱신합니다.
                binding.mcCustom.notifyMonthChanged(visibleMonth.yearMonth)
                binding.mcCustom.notifyMonthChanged(visibleMonth.yearMonth.minusMonths(1))
                binding.mcCustom.notifyMonthChanged(visibleMonth.yearMonth.plusMonths(1))
            } else {
                // 보이는 달을 찾지 못한 초기 상태일 때만 전체 갱신
                binding.mcCustom.notifyCalendarChanged()
            }
        }
    }

    fun setOnDateSelectedListener(listener: (String) -> Unit) {
        onDateSelectedListener = listener
    }

    fun setHeaderVisible(isVisible: Boolean) {
        binding.btnUpload.visibility = if (isVisible) VISIBLE else GONE
    }

    fun setOnUploadClickListener(listener: () -> Unit) {
        binding.btnUpload.setOnClickListener {
            listener.invoke()
        }
    }

    fun setDayLayout(layoutRes: Int) {
        Log.e("LOOP", "setDayLayout() called")
        if (this.currentDayLayoutRes == layoutRes) return
        this.currentDayLayoutRes = layoutRes

        setupCalendar()
        binding.mcCustom.notifyCalendarChanged()
    }

    // 선택된 날짜를 "yyyy년 MM월 dd일" 형태로 반환
    // 달력 클릭 후 받는 연월일 형태를 수정하려면 이 함수를 수정
    fun getSelectedDateFormatted(): String {
        return selectedDate?.let {
            "${it.year}.${it.monthValue}.${it.dayOfMonth}"
        } ?: "${currentMonth.year}.${currentMonth.monthValue}"
    }

    init {
        setupCalendar()
    }

    private var didInitialScroll = false
    private fun setupCalendar() {
        Log.e("LOOP", "setupCalendar() called")
        with(binding) {
            // 현재 연월 표시
            updateYearMonthText()

            // 이전 달 버튼(<) 클릭 리스너
            binding.btnPrevMonth.setOnClickListener {
                currentMonth = currentMonth.minusMonths(1)
                mcCustom.smoothScrollToMonth(currentMonth)
                updateYearMonthText()
            }

            // 다음 달 버튼(>) 클릭 리스너
            binding.btnNextMonth.setOnClickListener {
                currentMonth = currentMonth.plusMonths(1)
                mcCustom.smoothScrollToMonth(currentMonth)
                updateYearMonthText()
            }

            binding.mcCustom.setOnTouchListener { _, event ->
                // 사용자가 좌우로 미는 동작(MOVE)을 시스템이 무시하게 만듭니다.
                // ACTION_MOVE일 때 true를 반환하면 달력이 스크롤되지 않습니다.
                event.action == MotionEvent.ACTION_MOVE
            }

            val today = LocalDate.now()
            selectedDate = today

            binding.mcCustom.post {
                binding.mcCustom.notifyDateChanged(today)
                onDateSelectedListener?.invoke(getSelectedDateFormatted())
            }

            // 오늘 날짜 이전, 이후 연월은 100개월 전까지 표시
            val startMonth = currentMonth.minusMonths(100)
            val endMonth = currentMonth.plusMonths(100)

            // 지정된 첫 번째 요일이 시작 위치에 오는 주간 요일 값
            // 실행 시 일요일이 먼저 표시됨
            val daysOfWeek: List<DayOfWeek> = daysOfWeek()

            mcCustom.setup(startMonth, endMonth, daysOfWeek.first())
            if (!didInitialScroll) {
                didInitialScroll = true
                mcCustom.scrollToMonth(currentMonth)   // ✅ 최초 1회만
            }
            // 달력 스크롤 시
            mcCustom.monthScrollListener = { month ->
                Log.d(TAG, "## [스크롤 리스너] mouthScrollListener: $month")
                currentMonth = month.yearMonth
                updateYearMonthText()
            }

            // 일~토 텍스트가 표시되는 상단 뷰
            mcCustom.monthHeaderBinder = object : MonthHeaderFooterBinder<MonthViewContainer> {
                override fun create(view: View) = MonthViewContainer(view)
                override fun bind(container: MonthViewContainer, data: CalendarMonth) {
                    if (container.titlesContainer.tag == null) {
                        container.titlesContainer.tag = data.yearMonth
                        container.titlesContainer.children.map { it as TextView }
                            .forEachIndexed { index, textView ->
                                if (index < daysOfWeek.size) {
                                    val dayOfWeek = daysOfWeek[index]
                                    val title =
                                        dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.KOREAN)
                                    textView.text = title

                                    // 일요일은 빨간색, 토요일은 파란색으로 한글 글자색 설정
                                    // 현재 코드에서 이렇게 설정해도 이번 달 외의 날짜들은 회색으로 표시된다
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

            // 날짜가 표시되는 뷰
            mcCustom.dayBinder = object : MonthDayBinder<DayViewContainer> {
                override fun create(view: View) = DayViewContainer(view, currentDayLayoutRes)
                override fun bind(container: DayViewContainer, data: CalendarDay) {
                    container.textView.text = data.date.dayOfMonth.toString()

                    val dayData = datesWithDataMap[data.date]

                    val isSelected = selectedDate == data.date
                    val firstImage = dayData?.imageUris?.firstOrNull()

                    if (currentDayLayoutRes == R.layout.calendar_day_main) {
                        when {
                            // 1. 사진 데이터가 있는 경우 (이미지 6번 스타일)
                            !firstImage.isNullOrEmpty() -> {
                                container.dayImage?.let { imageView ->
                                    Glide.with(container.view.context)
                                        .load(firstImage)
                                        .override(46, 69)
                                        .centerCrop()
                                        .into(imageView)
                                }
                                container.cardImage?.setBackgroundColor(Color.TRANSPARENT)
                            }

                            // 2. 데이터는 없는데 선택된 경우 (이미지 7번 스타일)
                            isSelected -> {
                                container.dayImage?.setImageDrawable(null)
                                container.cardImage?.setBackgroundColor(Color.parseColor("#FFD5CD")) // 연한 핑크 배경
                            }

                            // 3. 데이터도 없고 선택도 안 된 경우 (이미지 8번 스타일)
                            else -> {
                                container.dayImage?.setImageDrawable(null)
                                container.cardImage?.setBackgroundColor(Color.parseColor("#FFF4F2")) // 아주 연한 배경
                            }
                        }
                    } else {
                        container.dataDot?.visibility = if (dayData != null && data.position == DayPosition.MonthDate) VISIBLE else INVISIBLE
                    }


                    // 오늘 날짜 가져오기
                    val today = LocalDate.now()
                    val isFutureDate = data.date.isAfter(today)

                    // 텍스트 색상 설정
                    when {
                        isFutureDate -> {
                            // 미래 날짜는 항상 회색으로 표시
                            container.textView.setTextColor(Color.GRAY)
                        }
                        data.position == DayPosition.MonthDate -> {
                            container.textView.setTextColor(Color.BLACK)
                            // 현재 월에 속한 과거 또는 오늘 날짜는 요일에 따라 색상 설정
//                            when (data.date.dayOfWeek) {
//                                DayOfWeek.SUNDAY -> container.textView.setTextColor(Color.RED)
//                                DayOfWeek.SATURDAY -> container.textView.setTextColor(Color.BLUE)
//                                else -> container.textView.setTextColor(Color.BLACK)
//                            }
                        }
                        else -> {
                            // 이전/다음 달의 날짜는 회색
                            container.textView.setTextColor(Color.GRAY)
                        }
                    }

                    container.textView.background = null

                    // 선택된 날짜 스타일 적용 (미래 날짜가 아닌 경우만)
                    if (selectedDate == data.date && !isFutureDate) {
                        // 원형 배경 설정
                        container.textView.background = GradientDrawable().apply {
                            shape = GradientDrawable.OVAL
                            setColor(Color.parseColor("#FE927F"))
                            setSize(80, 80)
                        }

                        // 선택된 날짜는 흰색 텍스트
                        container.textView.setTextColor(Color.WHITE)
                        container.textView.gravity = Gravity.CENTER
                    }

                    // 날짜 클릭 리스너 - 미래 날짜는 선택 불가
                    container.textView.setOnClickListener {
                        // 현재 월에 속한 과거 또는 오늘 날짜만 선택 가능
                        if (data.position == DayPosition.MonthDate && !isFutureDate) {
                            if (selectedDate != data.date) {
                                val oldDate = selectedDate
                                selectedDate = data.date

                                // 이전 선택된 날짜 갱신
                                oldDate?.let { date ->
                                    mcCustom.notifyDateChanged(date)
                                }

                                // 새로 선택된 날짜 갱신 후 콜백에 전달
                                mcCustom.notifyDateChanged(data.date)
                                onDateSelectedListener?.invoke(getSelectedDateFormatted())
                            }
                        }
                    }
                }
            }
        }
    }

    private fun updateYearMonthText() {
        binding.tvYearMonth.text = "${currentMonth.monthValue}월"
    }
}

class DayViewContainer(view: View, layoutRes: Int) : ViewContainer(view) {
    // binding을 먼저 선언해서 내부 뷰들을 가져옵니다.
    val textView: TextView
    val dayImage: ImageView?
    val dataDot: View?
    val cardImage: CardView?

    init {
        if (layoutRes == R.layout.calendar_day_main) {
            val b = CalendarDayMainBinding.bind(view)
            textView = b.calendarDayText
            dayImage = b.dayImage
            dataDot = null
            cardImage = b.cardImage
        } else {
            val b = CalendarDayLayoutBinding.bind(view)
            textView = b.calendarDayText
            dayImage = null // 일반 레이아웃에는 이미지가 없을 경우
            dataDot = b.viewDataDot
            cardImage = null
        }
    }
}

class MonthViewContainer(view: View) : ViewContainer(view) {
    val titlesContainer = view as ViewGroup
}