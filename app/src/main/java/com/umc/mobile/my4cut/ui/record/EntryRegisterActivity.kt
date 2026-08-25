package com.umc.mobile.my4cut.ui.record

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.RectF
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.TextWatcher
import android.text.InputFilter
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.exifinterface.media.ExifInterface
import androidx.lifecycle.lifecycleScope
import com.umc.mobile.my4cut.R
import com.umc.mobile.my4cut.data.day4cut.remote.CreateDay4CutRequest
import com.umc.mobile.my4cut.data.day4cut.remote.Day4CutImage
import com.umc.mobile.my4cut.databinding.ActivityEntryRegister2Binding
import com.umc.mobile.my4cut.network.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

/**
 * 네컷 업로드 화면 튜토리얼(일기 작성 카드 강조)의 위치를 조정하는 값 모음.
 * 다른 화면 요소의 런타임 좌표를 읽어서 위치를 계산하기 때문에 Android Studio 미리보기에는
 * 나타나지 않습니다. 값을 바꾼 뒤 에뮬레이터/실기기에서 앱을 다시 실행해서 확인하세요.
 */
private object EntryRegisterTutorialLayout {
    const val SCROLL_TOP_PADDING_DP = 110  // 일기 카드를 스크롤로 보여줄 때 위쪽에 남길 여백(안내 텍스트 공간)

    const val TEXT_GAP_DP = 8           // 안내 텍스트 ↔ 화살표 간격
    const val ARROW_OFFSET_X_DP = 0     // 화살표의 카드 기준 가로 오프셋
    const val ARROW_GAP_DP = 4          // 화살표 ↔ 카드 간격
    const val ARROW_ROTATION = 0f       // 화살표 회전 각도

    const val CLOSE_MARGIN_END_DP = 20  // 닫기 버튼 ↔ 화면 오른쪽 여백
    const val CLOSE_MARGIN_BOTTOM_DP = 16 // 닫기 버튼 ↔ 화면 아래쪽 여백
}

class EntryRegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEntryRegister2Binding

    // Compose 관찰 가능 리스트 — 변경 즉시 ComposeView 리컴포지션 트리거
    private val selectedImageUris = mutableStateListOf<Uri>()
    private var isDiaryExpanded = false

    // 💡 Calm 이모지의 값(1)을 기본값으로 선언합니다.
    private var selectedMoodIndex = 1

    private val uploadedMediaIds = mutableListOf<Int>()

    private val pickMultipleMedia = registerForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(3)) { uris ->
        if (uris.isNotEmpty()) {
            val remaining = 3 - selectedImageUris.size
            if (remaining <= 0) return@registerForActivityResult
            val toAdd = uris.take(remaining)
            selectedImageUris.addAll(toAdd)
            if (uris.size > remaining) {
                Toast.makeText(this, "사진은 최대 3장까지 추가할 수 있어요.", Toast.LENGTH_SHORT).show()
            }
            updateButtonState()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEntryRegister2Binding.inflate(layoutInflater)
        setContentView(binding.root)

        // 사진 업로드 ComposeView 연결
        binding.cvPhotoPager.apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                PhotoUploadPager(
                    photos = selectedImageUris,
                    onAddPhotoClick = ::launchPhotoPicker
                )
            }
        }

        setupDateData()
        setupClickListeners()
        updateButtonState()
        setupDiaryLogic()
        setupMoodSelection()

        // 네컷 업로드 화면 최초 진입 시 1회만 표시되는 튜토리얼 (일기 작성 카드 안내)
        binding.root.post {
            showEntryRegisterTutorialIfNeeded()
        }
    }

    /**
     * 네컷 업로드 화면 최초 진입 시 1회만 표시되는 코치마크 튜토리얼.
     * 일기 작성 카드는 스크롤해야 보이는 위치에 있어서, 튜토리얼을 띄우기 전에 먼저
     * NestedScrollView를 해당 위치까지 스크롤한 뒤 그 자리에 딤 구멍을 뚫는다.
     */
    private fun showEntryRegisterTutorialIfNeeded() {
        if (EntryRegisterTutorialPrefs.hasSeenTutorial(this)) return

        val overlay = binding.includeEntryRegisterTutorial

        // 일기 카드가 화면에 들어오도록 미리 스크롤 (안내 텍스트 공간만큼 위쪽 여백을 남김)
        val scrollTargetY = (binding.clDiaryHeader.top - dpToPx(EntryRegisterTutorialLayout.SCROLL_TOP_PADDING_DP))
            .coerceAtLeast(0)
        binding.nsvContent.scrollTo(0, scrollTargetY)

        overlay.root.visibility = View.VISIBLE

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
            // 일기 헤더("하루 일기" 접기/펼치기 행) + 일기 내용(입력창·이모지) 카드를 한 덩어리로 강조
            val headerBox = boundsOf(binding.clDiaryHeader)
            val contentBox = boundsOf(binding.clDiaryContent)
            val diaryBox = Rect(
                minOf(headerBox.left, contentBox.left),
                headerBox.top,
                maxOf(headerBox.right, contentBox.right),
                contentBox.bottom
            ).apply { inset(-dpToPx(2), -dpToPx(2)) }

            // 딤에 스포트라이트(완전 투명) 구멍을 뚫어 실제 일기 카드가 어둡게 가려지지 않도록 함
            overlay.tutorialDimView.setHoles(
                listOf(RectF(diaryBox) to dpToPx(12).toFloat())
            )

            placeHighlight(overlay.vHighlightDiary, diaryBox)

            // 화살표(카드 바로 위)를 먼저 배치한 뒤, 그 위에 안내 텍스트를 배치
            (overlay.ivTutorialArrowDiary.layoutParams as FrameLayout.LayoutParams).apply {
                leftMargin = diaryBox.right - dpToPx(24) + dpToPx(EntryRegisterTutorialLayout.ARROW_OFFSET_X_DP)
                topMargin = diaryBox.top - dpToPx(EntryRegisterTutorialLayout.ARROW_GAP_DP) - height
            }
            overlay.ivTutorialArrowDiary.rotation = EntryRegisterTutorialLayout.ARROW_ROTATION
            overlay.ivTutorialArrowDiary.requestLayout()
            val arrowTop = (overlay.ivTutorialArrowDiary.layoutParams as FrameLayout.LayoutParams).topMargin

            // 안내 텍스트: 화살표 바로 위, 오른쪽 정렬
            overlay.tvTutorialDiary.text = coralHighlightedText(
                "포토리 이모티콘과 함께\n100자 이내로 하루를 기록해요.",
                "하루를 기록"
            )
            val textWidth = (overlay.tvTutorialDiary.layoutParams as FrameLayout.LayoutParams).width
            overlay.tvTutorialDiary.measure(
                View.MeasureSpec.makeMeasureSpec(textWidth, View.MeasureSpec.AT_MOST),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            )
            val textHeight = overlay.tvTutorialDiary.measuredHeight
            (overlay.tvTutorialDiary.layoutParams as FrameLayout.LayoutParams).apply {
                leftMargin = diaryBox.right - textWidth
                topMargin = arrowTop - dpToPx(EntryRegisterTutorialLayout.TEXT_GAP_DP) - textHeight
            }
            overlay.tvTutorialDiary.requestLayout()

            // 닫기 버튼: 화면 우측 맨 아래에 명시적 좌표로 배치
            overlay.llTutorialClose.bringToFront()
            overlay.llTutorialClose.measure(
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            )
            val closeWidth = overlay.llTutorialClose.measuredWidth
            val closeHeight = overlay.llTutorialClose.measuredHeight
            overlay.llTutorialClose.x =
                (overlay.root.width - closeWidth - dpToPx(EntryRegisterTutorialLayout.CLOSE_MARGIN_END_DP)).toFloat()
            overlay.llTutorialClose.y =
                (overlay.root.height - closeHeight - dpToPx(EntryRegisterTutorialLayout.CLOSE_MARGIN_BOTTOM_DP)).toFloat()
        }

        overlay.root.post { positionOverlay() }

        overlay.llTutorialClose.setOnClickListener {
            overlay.root.visibility = View.GONE
            EntryRegisterTutorialPrefs.setTutorialSeen(this)
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

    private fun setupDateData() {
        val dateString = intent.getStringExtra("SELECTED_DATE") ?: "2026.01.01"
        binding.tvDateCapsule.text = dateString
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener { finish() }

        binding.btnComplete.setOnClickListener {
            if (selectedImageUris.isNotEmpty()) {
                Log.d("EntryRegister", "====================================")
                Log.d("EntryRegister", "🚀 UPLOAD PROCESS STARTED")
                Log.d("EntryRegister", "📸 Total images: ${selectedImageUris.size}")
                Log.d("EntryRegister", "====================================")
                uploadImagesAndCreateDay4Cut()
            } else {
                Toast.makeText(this, "이미지를 추가해주세요.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun launchPhotoPicker() {
        // 이미 3장이면 토스트만 표시하고 picker 미실행
        if (selectedImageUris.size >= 3) {
            Toast.makeText(this, "사진은 최대 3장까지 추가할 수 있어요.", Toast.LENGTH_SHORT).show()
            return
        }
        pickMultipleMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    private fun updateButtonState() {
        val hasPhotos = selectedImageUris.isNotEmpty()
        binding.btnComplete.isEnabled = hasPhotos
        binding.btnComplete.alpha = if (hasPhotos) 1.0f else 0.5f
    }

    /**
     * 이미지를 압축하여 파일 크기를 줄입니다
     * @param uri 원본 이미지 URI
     * @return 압축된 이미지 파일, 실패 시 null
     */
    private fun compressImage(uri: Uri): File? {
        return try {
            val inputStream = contentResolver.openInputStream(uri) ?: return null

            // 1. Bitmap으로 변환
            val originalBitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()

            if (originalBitmap == null) {
                Log.e("EntryRegister", "❌ Failed to decode bitmap from URI: $uri")
                return null
            }

            // 2. EXIF 정보로 회전 처리
            val rotatedBitmap = rotateImageIfRequired(uri, originalBitmap)

            // 3. 리사이징 (최대 1920px)
            val resizedBitmap = resizeBitmap(rotatedBitmap, 1920)

            // 4. JPEG로 압축 (품질 80%)
            val outputStream = ByteArrayOutputStream()
            resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
            val compressedBytes = outputStream.toByteArray()

            // 5. 임시 파일로 저장
            val tempFile = File(cacheDir, "compressed_${System.currentTimeMillis()}.jpg")
            FileOutputStream(tempFile).use { fos ->
                fos.write(compressedBytes)
            }

            // 메모리 정리
            if (rotatedBitmap != originalBitmap) {
                originalBitmap.recycle()
            }
            resizedBitmap.recycle()

            val originalSize = contentResolver.openInputStream(uri)?.available() ?: 0
            val compressedSize = tempFile.length()
            val reductionPercent = if (originalSize > 0) {
                ((originalSize - compressedSize) * 100.0 / originalSize).toInt()
            } else 0

            Log.d("EntryRegister", "✅ Image compressed: ${originalSize / 1024}KB → ${compressedSize / 1024}KB (${reductionPercent}% reduction)")

            tempFile
        } catch (e: Exception) {
            Log.e("EntryRegister", "❌ Image compression failed", e)
            null
        }
    }

    /**
     * EXIF 정보를 읽어 이미지 회전 처리
     */
    private fun rotateImageIfRequired(uri: Uri, bitmap: Bitmap): Bitmap {
        return try {
            val inputStream = contentResolver.openInputStream(uri) ?: return bitmap
            val exif = ExifInterface(inputStream)
            inputStream.close()

            val orientation = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_UNDEFINED
            )

            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> rotateBitmap(bitmap, 90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> rotateBitmap(bitmap, 180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> rotateBitmap(bitmap, 270f)
                else -> bitmap
            }
        } catch (e: Exception) {
            Log.e("EntryRegister", "Failed to read EXIF", e)
            bitmap
        }
    }

    /**
     * Bitmap 회전
     */
    private fun rotateBitmap(bitmap: Bitmap, degrees: Float): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(degrees)
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    /**
     * Bitmap 리사이징
     */
    private fun resizeBitmap(bitmap: Bitmap, maxSize: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height

        if (width <= maxSize && height <= maxSize) {
            return bitmap
        }

        val ratio = minOf(
            maxSize.toFloat() / width,
            maxSize.toFloat() / height
        )

        val newWidth = (width * ratio).toInt()
        val newHeight = (height * ratio).toInt()

        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    private fun uploadImagesAndCreateDay4Cut() {
        binding.btnComplete.isEnabled = false
        Toast.makeText(this, "업로드 중...", Toast.LENGTH_SHORT).show()

        lifecycleScope.launch {
            try {
                uploadedMediaIds.clear()
                Log.d("EntryRegister", "")
                Log.d("EntryRegister", "📂 IMAGE UPLOAD PHASE")
                Log.d("EntryRegister", "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

                // Step 1: 이미지 압축 및 MultipartBody.Part 리스트 생성
                val fileParts = mutableListOf<MultipartBody.Part>()

                for ((index, uri) in selectedImageUris.withIndex()) {
                    Log.d("EntryRegister", "")
                    Log.d("EntryRegister", "📤 Processing image ${index + 1}/${selectedImageUris.size}")

                    // 이미지 압축
                    val compressedFile = compressImage(uri)
                    if (compressedFile == null) {
                        Log.e("EntryRegister", "❌ Image ${index + 1} compression failed")
                        throw Exception("이미지 ${index + 1} 압축 실패")
                    }

                    // MultipartBody.Part 생성
                    val requestBody = compressedFile.asRequestBody("image/jpeg".toMediaTypeOrNull())
                    val part = MultipartBody.Part.createFormData("files", compressedFile.name, requestBody)
                    fileParts.add(part)

                    // 임시 파일 삭제는 업로드 후에 수행
                }

                Log.d("EntryRegister", "📤 Uploading ${fileParts.size} compressed images via /media/upload/bulk")

                // Step 2: Bulk 업로드 API 호출
                val response = withContext(Dispatchers.IO) {
                    RetrofitClient.mediaService.uploadMediaBulk(fileParts)
                }

                withContext(Dispatchers.Main) {
                    Log.d("EntryRegister", "📨 Response received:")
                    Log.d("EntryRegister", "   ├─ code: ${response.code}")
                    Log.d("EntryRegister", "   ├─ message: ${response.message}")
                    Log.d("EntryRegister", "   └─ data: ${response.data}")

                    val uploadedFiles = response.data
                    if (uploadedFiles != null && uploadedFiles.isNotEmpty()) {
                        Log.d("EntryRegister", "✅ All images uploaded successfully")

                        uploadedMediaIds.clear()
                        uploadedFiles.forEach { file ->
                            uploadedMediaIds.add(file.mediaId)
                            Log.d("EntryRegister", "   ├─ fileId: ${file.mediaId}, viewUrl: ${file.viewUrl}")
                        }

                        Log.d("EntryRegister", "📊 Uploaded mediaIds: $uploadedMediaIds")

                        // 임시 파일 정리
                        cacheDir.listFiles()?.filter { it.name.startsWith("compressed_") }?.forEach { it.delete() }

                        // Step 3: 하루네컷 생성
                        createDay4Cut()
                    } else {
                        Log.e("EntryRegister", "❌ Upload response data is null or empty")
                        throw Exception("이미지 업로드 실패")
                    }
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Log.e("EntryRegister", "💥 UPLOAD PROCESS FAILED", e)
                    Toast.makeText(this@EntryRegisterActivity, "업로드 실패: ${e.message}", Toast.LENGTH_LONG).show()
                    binding.btnComplete.isEnabled = true

                    // 임시 파일 정리
                    cacheDir.listFiles()?.filter { it.name.startsWith("compressed_") }?.forEach { it.delete() }
                }
            }
        }
    }

    private suspend fun createDay4Cut() {
        val dateString = binding.tvDateCapsule.text.toString()
        val apiDate = dateString.replace(".", "-")

        // blank면 null → GsonConverterFactory 기본 설정에서 null 필드는 JSON 생략됨
        // 서버는 빈 문자열("")을 D4003으로 거부하므로 반드시 null 전송
        val content = binding.etDiary.text.toString().trim().takeIf { it.isNotBlank() }

        val emojiType = when (selectedMoodIndex) {
            1 -> "CALM"
            2 -> "HAPPY"
            3 -> "TIRED"
            4 -> "ANGRY"
            5 -> "SAD"
            else -> null  // 선택 해제 상태
        }

        val images = uploadedMediaIds.mapIndexed { index, mediaId ->
            Day4CutImage(
                mediaId = mediaId,
                isThumbnail = (index == 0)  // 첫 번째 사진을 썸네일로 기본 지정
            )
        }

        val request = CreateDay4CutRequest(
            date = apiDate,
            content = content,
            emojiType = emojiType,
            images = images
        )

        Log.d("EntryRegister", "")
        Log.d("EntryRegister", "📝 DAY4CUT CREATION PHASE")
        Log.d("EntryRegister", "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        Log.d("EntryRegister", "🔹 API: POST /day4cut")
        Log.d("EntryRegister", "🔹 Request Body:")
        Log.d("EntryRegister", "   ├─ date: $apiDate")
        Log.d("EntryRegister", "   ├─ content: ${content ?: "(null)"}")
        Log.d("EntryRegister", "   ├─ emojiType: $emojiType")
        Log.d("EntryRegister", "   └─ images: ${images.size} items")
        images.forEachIndexed { index, image ->
            Log.d("EntryRegister", "       ├─ [$index] mediaFileId: ${image.mediaId}, isThumbnail: ${image.isThumbnail}")
        }

        try {
            val response = withContext(Dispatchers.IO) {
                RetrofitClient.day4CutService.createDay4Cut(request)
            }

            withContext(Dispatchers.Main) {
                Log.d("EntryRegister", "")
                Log.d("EntryRegister", "📨 Day4Cut Response received:")
                Log.d("EntryRegister", "   ├─ code: ${response.code}")
                Log.d("EntryRegister", "   ├─ message: ${response.message}")
                Log.d("EntryRegister", "   └─ data: ${response.data}")

                when {
                    response.data != null || response.message.contains("성공") -> {
                        Log.d("EntryRegister", "🎉 DAY4CUT CREATED SUCCESSFULLY")
                        Toast.makeText(this@EntryRegisterActivity, "기록이 저장되었습니다!", Toast.LENGTH_SHORT).show()
                        setResult(RESULT_OK)
                        finish()
                    }
                    response.code == "D4003" -> {
                        // 서버가 content 필수로 요구 → 일기 섹션 자동 펼침 후 텍스트 필드 포커스
                        Log.e("EntryRegister", "❌ D4003: 서버에서 content 필수 응답")
                        Toast.makeText(this@EntryRegisterActivity, "일기 내용을 입력해주세요.", Toast.LENGTH_SHORT).show()
                        if (!isDiaryExpanded) {
                            isDiaryExpanded = true
                            binding.clDiaryContent.visibility = View.VISIBLE
                            binding.ivDiaryArrow.setImageResource(R.drawable.ic_arrow_up_gray)
                        }
                        binding.etDiary.requestFocus()
                        binding.btnComplete.isEnabled = true
                    }
                    else -> {
                        Log.e("EntryRegister", "❌ Day4Cut creation failed: ${response.message}")
                        Toast.makeText(this@EntryRegisterActivity, "저장 실패: ${response.message}", Toast.LENGTH_LONG).show()
                        binding.btnComplete.isEnabled = true
                    }
                }
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Log.e("EntryRegister", "💥 NETWORK ERROR", e)
                Toast.makeText(this@EntryRegisterActivity, "네트워크 오류: ${e.message}", Toast.LENGTH_LONG).show()
                binding.btnComplete.isEnabled = true
            }
        }
    }

    private fun setupDiaryLogic() {
        binding.clDiaryHeader.setOnClickListener {
            isDiaryExpanded = !isDiaryExpanded
            if (isDiaryExpanded) {
                binding.clDiaryContent.visibility = View.VISIBLE
                binding.ivDiaryArrow.setImageResource(R.drawable.ic_arrow_up_gray)
            } else {
                binding.clDiaryContent.visibility = View.GONE
                binding.ivDiaryArrow.setImageResource(R.drawable.ic_arrow_down_gray)
            }
        }

        // [추가] 100자 입력 제한 필터
        binding.etDiary.filters = arrayOf(InputFilter.LengthFilter(100))

        binding.etDiary.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val length = s?.length ?: 0
                binding.tvTextCount.text = "$length/100"
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun setupMoodSelection() {
        val moodViews = listOf(
            binding.ivMood1, binding.ivMood2, binding.ivMood3, binding.ivMood4, binding.ivMood5
        )
        moodViews.forEachIndexed { index, imageView ->
            imageView.setOnClickListener {
                val clickedIndex = index + 1
                if (selectedMoodIndex == clickedIndex) {
                    // [수정] 같은 이모지 재클릭 → 선택 해제 (Toggle)
                    selectedMoodIndex = 0
                    updateMoodUI(moodViews, -1)
                } else {
                    selectedMoodIndex = clickedIndex
                    updateMoodUI(moodViews, index)
                }
            }
        }

        selectedMoodIndex = 1
        updateMoodUI(moodViews, 0)
    }

    private fun updateMoodUI(views: List<ImageView>, selectedIndex: Int) {
        views.forEachIndexed { index, imageView ->
            if (index == selectedIndex) {
                imageView.setBackgroundResource(R.drawable.bg_mood_selected)
                imageView.alpha = 1.0f
            } else {
                imageView.background = null
                imageView.alpha = 0.4f
            }
        }
    }

}