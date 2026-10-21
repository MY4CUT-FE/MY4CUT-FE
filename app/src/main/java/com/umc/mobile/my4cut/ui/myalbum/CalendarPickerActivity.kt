package com.umc.mobile.my4cut.ui.myalbum

import android.content.Intent
import android.graphics.Color
import android.graphics.Rect
import android.graphics.RectF
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.view.View
import android.widget.FrameLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.umc.mobile.my4cut.data.auth.local.TokenManager
import com.umc.mobile.my4cut.data.network.RetrofitClient
import com.umc.mobile.my4cut.data.tutorial.TutorialManager
import com.umc.mobile.my4cut.data.tutorial.model.TutorialType
import com.umc.mobile.my4cut.databinding.ActivityCalendarPicker2Binding
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private object DateSelectTutorialLayout {
    const val TEXT_GAP_X_DP = 12
    const val TEXT_OFFSET_Y_DP = 8
    const val ARROW_OFFSET_X_DP = -20
    const val ARROW_GAP_Y_DP = 2
    const val ARROW_ROTATION = 0f

    const val CLOSE_MARGIN_END_DP = 20
    const val CLOSE_MARGIN_BOTTOM_DP = 16
}

class CalendarPickerActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCalendarPicker2Binding
    private var currentSelectedDateStr: String = ""
    private val registeredDates = mutableSetOf<LocalDate>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCalendarPicker2Binding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.myCalendar.setHeaderVisible(false)

        val year = intent.getIntExtra("YEAR", LocalDate.now().year)
        val month = intent.getIntExtra("MONTH", LocalDate.now().monthValue)
        val day = intent.getIntExtra("DAY", LocalDate.now().dayOfMonth)
        val initialDate = runCatching { LocalDate.of(year, month, day) }.getOrElse { LocalDate.now() }

        currentSelectedDateStr = initialDate.format(DateTimeFormatter.ofPattern("yyyy.MM.dd"))

        binding.myCalendar.setOnDateSelectedListener { dateText ->
            currentSelectedDateStr = dateText
        }

        setupCalendar(year, month)
        setupClickListeners()

        binding.myCalendar.post {
            binding.myCalendar.scrollToDate(initialDate)
        }

        binding.root.post {
            showDateSelectTutorialIfNeeded()
        }
    }

    private fun showDateSelectTutorialIfNeeded() {
        val userId = TokenManager.getUserId(this) ?: return

        lifecycleScope.launch {
            if (TutorialManager.isTutorialCompleted(this@CalendarPickerActivity, userId, TutorialType.UPLOAD_DATE)) return@launch
            showDateSelectTutorialOverlay(userId)
        }
    }

    private fun showDateSelectTutorialOverlay(userId: Long) {
        val overlay = binding.includeDateSelectTutorial
        overlay.root.visibility = View.VISIBLE
        binding.vMinicalBadge.visibility = View.VISIBLE

        fun boundsOf(target: View): Rect {
            val rootLocation = IntArray(2)
            binding.root.getLocationInWindow(rootLocation)
            val loc = IntArray(2)
            target.getLocationInWindow(loc)
            val left = loc[0] - rootLocation[0]
            val top = loc[1] - rootLocation[1]
            return Rect(left, top, left + target.width, top + target.height)
        }

        fun positionOverlay() {
            val minicalBox = boundsOf(binding.vMinicalBadge)

            overlay.tutorialDimView.setHoles(
                listOf(RectF(minicalBox) to minicalBox.width() / 2f)
            )

            placeHighlight(overlay.vHighlightMinical, minicalBox)

            overlay.tvTutorialMinical.text = coralHighlightedText(
                "캘린더를 눌러\n직접 날짜를 설정해요.",
                "직접 날짜를 설정"
            )
            overlay.tvTutorialMinical.measure(
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            )
            val textWidth = overlay.tvTutorialMinical.measuredWidth
            val textLeft = minicalBox.left - dpToPx(DateSelectTutorialLayout.TEXT_GAP_X_DP) - textWidth
            val textTop = minicalBox.top - dpToPx(DateSelectTutorialLayout.TEXT_OFFSET_Y_DP)
            (overlay.tvTutorialMinical.layoutParams as FrameLayout.LayoutParams).apply {
                leftMargin = textLeft
                topMargin = textTop
            }
            overlay.tvTutorialMinical.requestLayout()

            (overlay.ivTutorialArrowMinical.layoutParams as FrameLayout.LayoutParams).apply {
                leftMargin = minicalBox.centerX() - width / 2 + dpToPx(DateSelectTutorialLayout.ARROW_OFFSET_X_DP)
                topMargin = minicalBox.bottom + dpToPx(DateSelectTutorialLayout.ARROW_GAP_Y_DP)
            }
            overlay.ivTutorialArrowMinical.rotation = DateSelectTutorialLayout.ARROW_ROTATION
            overlay.ivTutorialArrowMinical.requestLayout()

            overlay.llTutorialClose.bringToFront()
            overlay.llTutorialClose.measure(
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            )
            val closeWidth = overlay.llTutorialClose.measuredWidth
            val closeHeight = overlay.llTutorialClose.measuredHeight
            overlay.llTutorialClose.x =
                (overlay.root.width - closeWidth - dpToPx(DateSelectTutorialLayout.CLOSE_MARGIN_END_DP)).toFloat()
            overlay.llTutorialClose.y =
                (overlay.root.height - closeHeight - dpToPx(DateSelectTutorialLayout.CLOSE_MARGIN_BOTTOM_DP)).toFloat()
        }

        overlay.root.post { positionOverlay() }

        overlay.llTutorialClose.setOnClickListener {
            overlay.root.visibility = View.GONE
            binding.vMinicalBadge.visibility = View.GONE
            lifecycleScope.launch {
                TutorialManager.completeTutorial(this@CalendarPickerActivity, userId, TutorialType.UPLOAD_DATE)
            }
        }
    }

    private fun placeHighlight(target: View, rect: Rect) {
        (target.layoutParams as FrameLayout.LayoutParams).apply {
            width = rect.width()
            height = rect.height()
            leftMargin = rect.left
            topMargin = rect.top
        }
        target.requestLayout()
    }

    private fun coralHighlightedText(full: String, highlight: String): SpannableStringBuilder {
        val spannable = SpannableStringBuilder(full)
        val start = full.indexOf(highlight)
        if (start >= 0) {
            spannable.setSpan(
                ForegroundColorSpan(Color.parseColor("#FF7E67")),
                start,
                start + highlight.length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
        return spannable
    }

    private fun dpToPx(dp: Int): Int =
        (dp * resources.displayMetrics.density).toInt()

    private fun setupCalendar(year: Int, month: Int) {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.day4CutService.getCalendarStatus(year, month)

                if (response.code == "C2001") {
                    registeredDates.clear()

                    val calendarDataList = response.data?.dates?.map { item ->
                        val date = LocalDate.of(year, month, item.day)
                        registeredDates.add(date)

                        CalendarData(
                            date = date,
                            imageUris = if (item.thumbnailUrl != null) listOf(item.thumbnailUrl) else emptyList(),
                            memo = ""
                        )
                    } ?: emptyList()

                    binding.myCalendar.setDatesWithData(calendarDataList)
                } else {
                    registeredDates.clear()
                    binding.myCalendar.setDatesWithData(emptyList())
                }
            } catch (e: Exception) {
                registeredDates.clear()
                binding.myCalendar.setDatesWithData(emptyList())
            }
        }
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener { finish() }

        binding.ivMiniCal.setOnClickListener {
            val currentMonth = java.time.YearMonth.now()
            YearMonthPickerBottomSheet.newInstance(
                year = currentMonth.year,
                month = currentMonth.monthValue
            ) { selectedYear, selectedMonth ->
                val newDate = java.time.LocalDate.of(selectedYear, selectedMonth, 1)
                binding.myCalendar.scrollToDate(newDate)
                setupCalendar(selectedYear, selectedMonth)
            }.show(supportFragmentManager, "YearMonthPicker")
        }

        binding.btnNext.setOnClickListener {
            val selectedDateStr = currentSelectedDateStr
            val selectedDate = parseDateFromFormatted(selectedDateStr)

            if (registeredDates.contains(selectedDate)) {
                Toast.makeText(this, "이미 등록된 날짜입니다", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val isFutureDate = selectedDate.isAfter(LocalDate.now())
            if (isFutureDate) {
                Toast.makeText(this, "미래 날짜는 선택할 수 없습니다", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val intent = Intent(this, EntryRegisterActivity::class.java)
            intent.putExtra("SELECTED_DATE", selectedDateStr)
            startActivityForResult(intent, REQUEST_REGISTER)
        }
    }

    companion object {
        private const val REQUEST_REGISTER = 1001
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_REGISTER && resultCode == RESULT_OK) {
            finish()
        }
    }

    private fun parseDateFromFormatted(dateStr: String): LocalDate {
        return try {
            val parts = dateStr.split(".")
            if (parts.size == 3) {
                val year = parts[0].toInt()
                val month = parts[1].toInt()
                val day = parts[2].toInt()
                LocalDate.of(year, month, day)
            } else {
                LocalDate.now()
            }
        } catch (e: Exception) {
            LocalDate.now()
        }
    }
}