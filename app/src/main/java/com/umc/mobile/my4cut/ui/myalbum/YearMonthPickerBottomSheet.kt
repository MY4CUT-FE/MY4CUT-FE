package com.umc.mobile.my4cut.ui.myalbum

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import java.time.LocalDate
import kotlin.math.abs

class YearMonthPickerBottomSheet : BottomSheetDialogFragment() {

    private var onConfirm: ((year: Int, month: Int) -> Unit)? = null
    private var initialYear: Int = LocalDate.now().year
    private var initialMonth: Int = LocalDate.now().monthValue

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isCancelable = false
    }

    override fun onStart() {
        super.onStart()
        dialog?.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)?.let { sheet ->
            sheet.setBackgroundResource(android.R.color.transparent)
            com.google.android.material.bottomsheet.BottomSheetBehavior.from(sheet).apply {
                isDraggable = false
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = ComposeView(requireContext()).apply {
        setContent {
            YearMonthPickerContent(
                initialYear = initialYear,
                initialMonth = initialMonth,
                onConfirm = { year, month ->
                    onConfirm?.invoke(year, month)
                    dismiss()
                },
                onDismiss = { dismiss() }
            )
        }
    }

    companion object {
        fun newInstance(
            year: Int,
            month: Int,
            onConfirm: (year: Int, month: Int) -> Unit
        ): YearMonthPickerBottomSheet {
            return YearMonthPickerBottomSheet().apply {
                this.initialYear = year
                this.initialMonth = month
                this.onConfirm = onConfirm
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun YearMonthPickerContent(
    initialYear: Int,
    initialMonth: Int,
    onConfirm: (year: Int, month: Int) -> Unit,
    onDismiss: () -> Unit
) {
    val currentYear = LocalDate.now().year
    val years = (2000..currentYear).toList()
    val initialYearIndex = years.indexOf(initialYear).coerceAtLeast(0)
    val initialMonthIndex = (initialMonth - 1).coerceIn(0, 11)

    val yearListState = rememberLazyListState()
    val monthListState = rememberLazyListState()

    LaunchedEffect(Unit) {
        yearListState.scrollToItem(initialYearIndex)
        monthListState.scrollToItem(initialMonthIndex)
    }

    val selectedYearIndex by remember {
        derivedStateOf {
            val visible = yearListState.layoutInfo.visibleItemsInfo
            val mid = (yearListState.layoutInfo.viewportStartOffset + yearListState.layoutInfo.viewportEndOffset) / 2
            visible.minByOrNull { abs(it.offset + it.size / 2 - mid) }?.index ?: initialYearIndex
        }
    }

    val selectedMonthIndex by remember {
        derivedStateOf {
            val visible = monthListState.layoutInfo.visibleItemsInfo
            val mid = (monthListState.layoutInfo.viewportStartOffset + monthListState.layoutInfo.viewportEndOffset) / 2
            visible.minByOrNull { abs(it.offset + it.size / 2 - mid) }?.index ?: initialMonthIndex
        }
    }

    val selectedYear = years.getOrElse(selectedYearIndex) { currentYear }
    val selectedMonth = selectedMonthIndex + 1

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
            .padding(horizontal = 24.dp)
            .padding(top = 16.dp, bottom = 24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "날짜 선택",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1A1A1A)
            )
            Text(
                text = "✕",
                fontSize = 18.sp,
                color = Color(0xFF888888),
                modifier = Modifier
                    .padding(4.dp)
                    .clickable { onDismiss() }
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        val itemHeight = 40.dp

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(itemHeight * 5)
        ) {
            // 년도 컬럼 (독립 캡슐 배경)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .fillMaxWidth()
                        .height(itemHeight)
                        .padding(horizontal = 8.dp)
                        .background(Color(0xFFFFF0EE), RoundedCornerShape(12.dp))
                )
                WheelColumn(
                    items = years.map { it.toString() },
                    listState = yearListState,
                    selectedIndex = selectedYearIndex,
                    itemHeight = itemHeight,
                    modifier = Modifier.fillMaxSize()
                )
            }
            // 월 컬럼 (독립 캡슐 배경)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .fillMaxWidth()
                        .height(itemHeight)
                        .padding(horizontal = 8.dp)
                        .background(Color(0xFFFFF0EE), RoundedCornerShape(12.dp))
                )
                WheelColumn(
                    items = (1..12).map { it.toString() },
                    listState = monthListState,
                    selectedIndex = selectedMonthIndex,
                    itemHeight = itemHeight,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .background(Color(0xFFFFAD9D), RoundedCornerShape(12.dp))
                .clickable { onConfirm(selectedYear, selectedMonth) },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "확인",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun WheelColumn(
    items: List<String>,
    listState: LazyListState,
    selectedIndex: Int,
    itemHeight: Dp,
    modifier: Modifier = Modifier
) {
    val snapFlingBehavior = rememberSnapFlingBehavior(lazyListState = listState)
    val contentPaddingVertical = itemHeight * 2

    LazyColumn(
        state = listState,
        flingBehavior = snapFlingBehavior,
        contentPadding = PaddingValues(vertical = contentPaddingVertical),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.fillMaxHeight()
    ) {
        items(items.size) { index ->
            val distance = abs(index - selectedIndex)
            val textColor = when (distance) {
                0 -> Color(0xFFFE927F)
                1 -> Color(0xFFB0B0B0)
                else -> Color(0xFFD8D8D8)
            }
            val fontSize = when (distance) {
                0 -> 20.sp
                1 -> 16.sp
                else -> 13.sp
            }
            val fontWeight = if (distance == 0) FontWeight.Bold else FontWeight.Normal
            val alpha = when (distance) {
                0 -> 1f
                1 -> 0.75f
                else -> 0.4f
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(itemHeight),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = items[index],
                    fontSize = fontSize,
                    fontWeight = fontWeight,
                    color = textColor.copy(alpha = alpha)
                )
            }
        }
    }
}
