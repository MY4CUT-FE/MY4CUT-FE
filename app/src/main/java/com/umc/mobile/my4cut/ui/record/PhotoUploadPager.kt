package com.umc.mobile.my4cut.ui.record

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import com.umc.mobile.my4cut.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.absoluteValue

private val CoralColor = Color(0xFFFF7E67)

/**
 * 네컷 업로드 페이저
 * - 사진 없음: EmptyCard(page 0) + AddCard(page 1) → 우측에 + 카드뷰 peek
 * - 사진 있음: PhotoCards + AddCard(마지막) + 인디케이터 내부 오버레이
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PhotoUploadPager(
    photos: List<Uri>,
    onAddPhotoClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 사진 없음: EmptyCard + AddCard 2페이지 (우측 peek)
        // 사진 있음: PhotoCards + AddCard (photos.size + 1)
        val pageCount = if (photos.isEmpty()) 2 else photos.size + 1
        val pagerState = rememberPagerState(pageCount = { pageCount })

        // Box: 인디케이터를 카드뷰 내부 하단에 오버레이
        Box {
            HorizontalPager(
                state = pagerState,
                // 60dp: 양옆 카드뷰가 더 많이 peek되어 슬라이더블 느낌 강조
                contentPadding = PaddingValues(horizontal = 60.dp),
                pageSpacing = 12.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(370.dp)
            ) { page ->
                // 비중앙 카드 85% 축소, 중앙 카드 100% 원본 크기
                val pageOffset = ((pagerState.currentPage - page) +
                        pagerState.currentPageOffsetFraction).absoluteValue
                val scale = lerp(
                    start = 0.85f,
                    stop = 1f,
                    fraction = 1f - pageOffset.coerceIn(0f, 1f)
                )
                val isCurrent = pagerState.currentPage == page

                val scaledModifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    }

                when {
                    // [빈 상태] page 0 → EmptyCard
                    photos.isEmpty() && page == 0 -> EmptyPhotoCard(
                        onClick = onAddPhotoClick,
                        modifier = scaledModifier
                    )
                    // [빈 상태] page 1 → AddCard (우측 peek, 아이콘 숨김)
                    photos.isEmpty() -> AddPhotoCard(
                        onClick = onAddPhotoClick,
                        pageOffset = pageOffset,
                        showIcon = false,
                        modifier = scaledModifier
                    )
                    // [사진 있음] 사진 페이지
                    page < photos.size -> PhotoCard(
                        uri = photos[page],
                        isCurrent = isCurrent,
                        modifier = scaledModifier
                    )
                    // [사진 있음] 마지막 페이지 → + 추가 슬롯 (사진 추가 직접 연결)
                    else -> AddPhotoCard(
                        onClick = onAddPhotoClick,
                        pageOffset = pageOffset,
                        modifier = scaledModifier
                    )
                }
            }

            // 인디케이터: 사진이 있을 때만 카드뷰 내부 하단 정중앙 오버레이
            if (photos.isNotEmpty()) {
                PhotoPageIndicator(
                    currentPage = pagerState.currentPage,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 14.dp)
                )
            }
        }
    }
}

// ─── 빈 상태 카드 ──────────────────────────────────────────────────────────────

@Composable
private fun EmptyPhotoCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(16.dp))
            .background(Color.White)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
                .dashedBorder(
                    width = 1.5.dp,
                    color = Color(0xFFD0D0D0),
                    cornerRadius = 12.dp
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_upload_placeholder_icon),
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    contentScale = ContentScale.Fit
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "오늘의 네컷을\n업로드 해주세요",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1A1A1A),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                // 숫자 3·1만 코랄 색상
                Text(
                    text = buildAnnotatedString {
                        append("하루 최대 ")
                        withStyle(SpanStyle(color = CoralColor)) { append("3") }
                        append("장의 네컷을 등록하고\n일기는 ")
                        withStyle(SpanStyle(color = CoralColor)) { append("1") }
                        append("개만 작성할 수 있어요.")
                    },
                    fontSize = 11.sp,
                    color = Color(0xFF888888),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

// ─── 사진 카드 ─────────────────────────────────────────────────────────────────

@Composable
private fun PhotoCard(uri: Uri, isCurrent: Boolean, modifier: Modifier = Modifier) {
    val context = LocalContext.current

    val bitmap by produceState<ImageBitmap?>(initialValue = null, key1 = uri) {
        value = withContext(Dispatchers.IO) {
            try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    BitmapFactory.decodeStream(stream)?.asImageBitmap()
                }
            } catch (e: Exception) {
                null
            }
        }
    }

    // 대표(현재 중앙) 카드: 코랄색 2dp 실선 / 비선택 카드: 기본 회색 1dp
    val borderColor = if (isCurrent) CoralColor else Color(0xFFE8E8E8)
    val borderWidth = if (isCurrent) 2.dp else 1.dp

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .border(borderWidth, borderColor, RoundedCornerShape(16.dp))
            .background(Color(0xFFF5F5F5))
    ) {
        bitmap?.let {
            Image(
                bitmap = it,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

// ─── 사진 추가 슬롯 ────────────────────────────────────────────────────────────

/**
 * pageOffset: 현재 페이지 기준 이 슬롯의 거리 (0f=중앙, 1f=완전 peek)
 * showIcon: false이면 아이콘 표시 안 함 (빈 상태 우측 peek 슬롯에서 숨김)
 * - peek 상태에서 아이콘이 카드 정중앙에 있으면 보이지 않으므로
 *   pageOffset에 따라 visible 영역 쪽으로 이동시켜 항상 보이도록 처리
 */
@Composable
private fun AddPhotoCard(
    onClick: () -> Unit,
    pageOffset: Float = 0f,
    showIcon: Boolean = true,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .border(1.5.dp, Color(0xFFE6E6E6), RoundedCornerShape(16.dp))
            .background(Color(0xFFFAFAFA))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (showIcon) {
            // BoxWithConstraints로 카드 실제 너비를 측정하여 offset 계산
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val cardWidth = maxWidth
                // contentPadding(60dp) - pageSpacing/2(6dp) = 54dp: peek시 보이는 너비
                val peekWidth = 54.dp
                // pageOffset=0(중앙): 이동 없음 / pageOffset=1(완전 peek): 아이콘을 peek 영역 중앙으로 이동
                val translationX = -(cardWidth / 2 - peekWidth / 2) * pageOffset.coerceIn(0f, 1f)

                Image(
                    painter = painterResource(R.drawable.btn_upload_photo),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(30.dp)
                        .offset(x = translationX)
                )
            }
        }
    }
}

// ─── 페이지 인디케이터 ─────────────────────────────────────────────────────────

/**
 * 최대 등록 장수(3장)에 맞춘 3개 점 인디케이터
 * - currentPage에 맞춰 해당 인덱스의 점이 활성화되어 스와이프 시 함께 움직임
 */
@Composable
private fun PhotoPageIndicator(
    currentPage: Int,
    modifier: Modifier = Modifier
) {
    val maxPhotoCount = 3
    val activeIndex = currentPage.coerceIn(0, maxPhotoCount - 1)

    Box(
        modifier = modifier
            .background(Color(0xFFE6E6E6), RoundedCornerShape(50))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(maxPhotoCount) { index ->
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(
                            color = if (index == activeIndex) Color(0xFF1A1A1A) else Color(0xFFB3B3B3),
                            shape = CircleShape
                        )
                )
            }
        }
    }
}

// ─── Modifier 확장: 점선 테두리 ────────────────────────────────────────────────

private fun Modifier.dashedBorder(
    width: Dp,
    color: Color,
    cornerRadius: Dp,
    dashLength: Float = 10f,
    gapLength: Float = 8f
): Modifier = this.drawBehind {
    drawRoundRect(
        color = color,
        style = Stroke(
            width = width.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(dashLength, gapLength), 0f)
        ),
        cornerRadius = CornerRadius(cornerRadius.toPx())
    )
}
