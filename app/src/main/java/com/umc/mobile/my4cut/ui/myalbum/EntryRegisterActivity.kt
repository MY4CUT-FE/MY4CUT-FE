package com.umc.mobile.my4cut.ui.myalbum

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.RectF
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.TextWatcher
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.exifinterface.media.ExifInterface
import androidx.lifecycle.lifecycleScope
import com.umc.mobile.my4cut.MainActivity
import com.umc.mobile.my4cut.R
import com.umc.mobile.my4cut.data.auth.local.TokenManager
import com.umc.mobile.my4cut.data.day4cut.remote.CreateDay4CutRequest
import com.umc.mobile.my4cut.data.day4cut.remote.Day4CutImage
import com.umc.mobile.my4cut.data.tutorial.TutorialManager
import com.umc.mobile.my4cut.data.tutorial.model.TutorialType
import com.umc.mobile.my4cut.databinding.ActivityEntryRegister2Binding
import com.umc.mobile.my4cut.data.network.RetrofitClient
import com.umc.mobile.my4cut.ui.record.PhotoUploadPager
import com.umc.mobile.my4cut.util.applySystemBarInsets
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate

private object EntryRegisterTutorialLayout {
    const val SCROLL_TOP_PADDING_DP = 110

    const val TEXT_GAP_DP = 8
    const val ARROW_OFFSET_X_DP = -160
    const val ARROW_GAP_DP = 2
    const val ARROW_ROTATION = 0f

    const val CLOSE_MARGIN_END_DP = 20
    const val CLOSE_MARGIN_BOTTOM_DP = 16
}

class EntryRegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEntryRegister2Binding

    private val selectedImageUris = mutableStateListOf<Uri>()

    private var thumbnailIndex by mutableStateOf(0)

    private var isDiaryExpanded = true
    private var selectedMoodIndex = 1

    private val pickMultipleMedia = registerForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(3)
    ) { uris ->
        if (uris.isNotEmpty()) {
            if (selectedImageUris.size >= 3) {
                selectedImageUris.clear()
                selectedImageUris.addAll(uris.take(3))
            } else {
                val remaining = 3 - selectedImageUris.size
                val toAdd = uris.take(remaining)
                selectedImageUris.addAll(toAdd)
                if (uris.size > remaining) {
                    Toast.makeText(this, "사진은 최대 3장까지 추가할 수 있어요.", Toast.LENGTH_SHORT).show()
                }
            }
            updateButtonState()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEntryRegister2Binding.inflate(layoutInflater)
        setContentView(binding.root)
        applySystemBarInsets()

        binding.cvPhotoPager.apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                PhotoUploadPager(
                    photos = selectedImageUris,
                    onAddPhotoClick = ::launchPhotoPicker,
                    thumbnailIndex = thumbnailIndex,
                    onThumbnailClick = { index -> thumbnailIndex = index },
                    onDeleteClick = { index -> deletePhotoAt(index) }
                )
            }
        }

        setupDateData()
        setupCalendarData()
        setupClickListeners()
        updateButtonState()
        setupDiaryLogic()
        setupMoodSelection()

        applyDiaryExpandedState()

        binding.root.post {
            showEntryRegisterTutorialIfNeeded()
        }
    }

    private fun showEntryRegisterTutorialIfNeeded() {
        val userId = TokenManager.getUserId(this) ?: return

        lifecycleScope.launch {
            if (TutorialManager.isTutorialCompleted(this@EntryRegisterActivity, userId, TutorialType.UPLOAD_CONTENT)) return@launch

            showEntryRegisterTutorialOverlay(userId)
        }
    }

    private fun showEntryRegisterTutorialOverlay(userId: Long) {
        val overlay = binding.includeEntryRegisterTutorial

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
            val headerBox = boundsOf(binding.clDiaryHeader)
            val contentBox = boundsOf(binding.clDiaryContent)
            val diaryBox = Rect(
                minOf(headerBox.left, contentBox.left),
                headerBox.top,
                maxOf(headerBox.right, contentBox.right),
                contentBox.bottom
            ).apply { inset(-dpToPx(2), -dpToPx(2)) }

            overlay.tutorialDimView.setHoles(
                listOf(RectF(diaryBox) to dpToPx(12).toFloat())
            )

            placeHighlight(overlay.vHighlightDiary, diaryBox)

            (overlay.ivTutorialArrowDiary.layoutParams as FrameLayout.LayoutParams).apply {
                leftMargin = diaryBox.right - dpToPx(24) + dpToPx(EntryRegisterTutorialLayout.ARROW_OFFSET_X_DP)
                topMargin = diaryBox.top - dpToPx(EntryRegisterTutorialLayout.ARROW_GAP_DP) - height
            }
            overlay.ivTutorialArrowDiary.rotation = EntryRegisterTutorialLayout.ARROW_ROTATION
            overlay.ivTutorialArrowDiary.requestLayout()
            val arrowTop = (overlay.ivTutorialArrowDiary.layoutParams as FrameLayout.LayoutParams).topMargin

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
            binding.nsvContent.post {
                binding.nsvContent.smoothScrollTo(0, 0, 500)
            }
            lifecycleScope.launch {
                TutorialManager.completeTutorial(this@EntryRegisterActivity, userId, TutorialType.UPLOAD_CONTENT)
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

    private fun setupDateData() {
        val dateString = intent.getStringExtra("SELECTED_DATE") ?: "2026-01-01"
        binding.tvDateCapsule.text = dateString
    }

    private fun setupCalendarData() {
        val data = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getSerializableExtra("SELECTED_DATA", CalendarData::class.java)
        } else {
            intent.getSerializableExtra("SELECTED_DATA") as? CalendarData
        }

        data?.let {
            binding.etDiary.setText(it.memo)
            binding.etDiary.setSelection(binding.etDiary.text?.length ?: 0)

            val uris = it.imageUris.map { uriString -> Uri.parse(uriString) }
            selectedImageUris.addAll(uris)

            updateButtonState()
        }
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener { finish() }

        binding.btnComplete.setOnClickListener {
            saveDay4Cut()
        }
    }

    private fun deletePhotoAt(index: Int) {
        if (index < 0 || index >= selectedImageUris.size) return

        selectedImageUris.removeAt(index)

        if (index == thumbnailIndex) {
            thumbnailIndex = 0
        } else if (index < thumbnailIndex) {
            thumbnailIndex--
        }

        updateButtonState()
    }

    private fun launchPhotoPicker() {
        pickMultipleMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    private fun updateButtonState() {
        val hasPhotos = selectedImageUris.isNotEmpty()
        binding.btnComplete.isEnabled = hasPhotos
        binding.btnComplete.alpha = if (hasPhotos) 1.0f else 0.5f
    }

    private fun saveDay4Cut() {
        lifecycleScope.launch {
            try {
                val fileParts = mutableListOf<MultipartBody.Part>()

                for ((index, uri) in selectedImageUris.withIndex()) {
                    val compressedFile = withContext(Dispatchers.IO) { compressImage(uri) } ?: throw Exception("이미지 ${index + 1} 압축 실패")

                    val requestBody = compressedFile.asRequestBody("image/jpeg".toMediaTypeOrNull())
                    val part = MultipartBody.Part.createFormData("files", compressedFile.name, requestBody)
                    fileParts.add(part)
                }

                val uploadResponse = withContext(Dispatchers.IO) {
                    RetrofitClient.mediaService.uploadMediaBulk(fileParts)
                }

                if (uploadResponse.code != "C2001" && uploadResponse.code != "C2011") {
                    throw Exception("이미지 업로드 실패")
                }

                val uploadedFiles = uploadResponse.data ?: throw Exception("업로드 응답 데이터 없음")

                val images = uploadedFiles.mapIndexed { index, file ->
                    Day4CutImage(
                        mediaId = file.mediaId,
                        isThumbnail = (index == thumbnailIndex)
                    )
                }

                val dateString = intent.getStringExtra("SELECTED_DATE") ?: ""
                val apiDate = convertToApiDate(dateString)

                val content = binding.etDiary.text.toString().trim().takeIf { it.isNotBlank() }
                val emojiType = when (selectedMoodIndex) {
                    1 -> "CALM"
                    2 -> "HAPPY"
                    3 -> "TIRED"
                    4 -> "ANGRY"
                    5 -> "SAD"
                    else -> null
                }

                val request = CreateDay4CutRequest(
                    date = apiDate,
                    content = content,
                    emojiType = emojiType,
                    images = images
                )

                val createResponse = withContext(Dispatchers.IO) {
                    RetrofitClient.day4CutService.createDay4Cut(request)
                }

                when {
                    createResponse.code == "C2001" || createResponse.code == "C2011" -> {
                        withContext(Dispatchers.Main) {
                            cacheDir.listFiles()?.filter {
                                it.name.startsWith("compressed_")
                            }?.forEach { it.delete() }

                            val intent = Intent(this@EntryRegisterActivity, MainActivity::class.java).apply {
                                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                                putExtra("MOVE_TO_DETAIL", true)
                                putExtra("API_DATE", apiDate)
                                putExtra("SELECTED_DATE", dateString)
                            }

                            startActivity(intent)
                            finish()
                        }
                    }
                    createResponse.code == "D4003" -> {
                        withContext(Dispatchers.Main) {
                            if (!isDiaryExpanded) {
                                isDiaryExpanded = true
                                applyDiaryExpandedState()
                            }
                            binding.etDiary.requestFocus()
                        }
                    }
                    else -> {
                        throw Exception("저장 실패")
                    }
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    cacheDir.listFiles()?.filter {
                        it.name.startsWith("compressed_")
                    }?.forEach { it.delete() }
                }
            }
        }
    }

    /**
     * 네컷 업로드용 이미지 처리
     * - 5MB 이하 + EXIF 방향 정상: 원본 그대로 사용 (재인코딩 없음)
     * - EXIF 방향이 정상이 아닌 경우: 사용자가 보는 방향 그대로 픽셀에 반영하여 저장
     * - 5MB 초과: 긴 쪽 2560px, JPEG 90부터 품질을 단계적으로 낮춰 5MB 이하로 저장
     */
    private fun compressImage(uri: Uri): File? {
        return try {
            val maxBytes = 5L * 1024 * 1024
            val maxLongSide = 2560

            // 원본을 임시 파일로 복사 (파일명 중복 방지를 위해 createTempFile 사용)
            val file = File.createTempFile("compressed_", ".jpg", cacheDir)
            contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(file).use { output ->
                    input.copyTo(output)
                }
            } ?: return null

            val originalSize = file.length()
            val orientation = ExifInterface(file.absolutePath).getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_UNDEFINED
            )
            val needsRotation = orientation == ExifInterface.ORIENTATION_ROTATE_90 ||
                orientation == ExifInterface.ORIENTATION_ROTATE_180 ||
                orientation == ExifInterface.ORIENTATION_ROTATE_270
            val isOversized = originalSize > maxBytes

            // 5MB 이하 + 방향 정상 → 원본 그대로 업로드
            if (!isOversized && !needsRotation) {
                Log.d("EntryRegister", "원본 유지: ${originalSize / 1024}KB, exif=$orientation")
                return file
            }

            // 큰 사진은 메모리 절약을 위해 샘플링하여 디코딩 (긴 쪽이 2560px 이상 유지되는 범위)
            val sideLimit = if (isOversized) maxLongSide else Int.MAX_VALUE
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.absolutePath, bounds)
            val longSide = maxOf(bounds.outWidth, bounds.outHeight)
            var sampleSize = 1
            while (longSide / (sampleSize * 2) >= sideLimit) sampleSize *= 2

            val decodedBitmap = BitmapFactory.decodeFile(
                file.absolutePath,
                BitmapFactory.Options().apply { inSampleSize = sampleSize }
            )
            if (decodedBitmap == null) {
                file.delete()
                return null
            }

            // EXIF 방향을 픽셀에 반영 (사용자가 보는 방향 유지)
            val rotatedBitmap = rotateImageIfRequired(uri, decodedBitmap)
            var bitmap = resizeBitmap(rotatedBitmap, sideLimit)

            // 5MB 이하가 될 때까지 품질을 단계적으로 낮춤 (회전만 필요한 경우 최고 품질부터 시작)
            val qualities = if (isOversized) listOf(90, 85, 80, 75) else listOf(100, 95, 90, 85, 80, 75)
            val outputStream = ByteArrayOutputStream()
            var fits = false
            var usedQuality = qualities.last()
            while (!fits) {
                for (quality in qualities) {
                    outputStream.reset()
                    bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
                    usedQuality = quality
                    if (outputStream.size() <= maxBytes) {
                        fits = true
                        break
                    }
                }
                if (!fits) {
                    // 최저 품질에서도 5MB 초과 시 해상도를 단계적으로 축소
                    val smaller = Bitmap.createScaledBitmap(
                        bitmap,
                        (bitmap.width * 0.85f).toInt(),
                        (bitmap.height * 0.85f).toInt(),
                        true
                    )
                    bitmap.recycle()
                    bitmap = smaller
                }
            }

            FileOutputStream(file).use { fos ->
                outputStream.writeTo(fos)
            }

            Log.d(
                "EntryRegister",
                "압축/회전 완료: ${originalSize / 1024}KB → ${file.length() / 1024}KB, " +
                    "exif=$orientation, ${bitmap.width}x${bitmap.height}, quality=$usedQuality"
            )

            decodedBitmap.recycle()
            rotatedBitmap.recycle()
            bitmap.recycle()

            file
        } catch (e: Exception) {
            Log.e("EntryRegister", "이미지 처리 실패", e)
            null
        }
    }

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
            bitmap
        }
    }

    private fun rotateBitmap(bitmap: Bitmap, degrees: Float): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(degrees)
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

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

    private fun convertToApiDate(dateString: String): String {
        return try {
            val parts = dateString.split(".")
            if (parts.size == 3) {
                val year = parts[0].toInt()
                val month = parts[1].toInt()
                val day = parts[2].toInt()
                String.format("%04d-%02d-%02d", year, month, day)
            } else {
                LocalDate.now().toString()
            }
        } catch (e: Exception) {
            LocalDate.now().toString()
        }
    }

    private fun getEmojiType(): String {
        return when (selectedMoodIndex) {
            1 -> "HAPPY"
            2 -> "CALM"
            3 -> "TIRED"
            4 -> "ANGRY"
            5 -> "SAD"
            else -> "HAPPY"
        }
    }

    private fun setupDiaryLogic() {
        binding.clDiaryHeader.setOnClickListener {
            isDiaryExpanded = !isDiaryExpanded
            applyDiaryExpandedState()
        }

        binding.etDiary.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val length = s?.length ?: 0
                binding.tvTextCount.text = "${minOf(length, 100)}/100"
            }
            override fun afterTextChanged(s: Editable?) {
                if ((s?.length ?: 0) > 100) {
                    s?.delete(100, s.length)
                }
            }
        })
    }

    private fun applyDiaryExpandedState() {
        if (isDiaryExpanded) {
            binding.clDiaryContent.visibility = View.VISIBLE
            binding.ivDiaryArrow.setImageResource(R.drawable.ic_arrow_up_gray)
        } else {
            binding.clDiaryContent.visibility = View.GONE
            binding.ivDiaryArrow.setImageResource(R.drawable.ic_arrow_down_gray_filled)
        }
    }

    private fun setupMoodSelection() {
        val moodViews = listOf(
            binding.ivMood1, binding.ivMood2, binding.ivMood3, binding.ivMood4, binding.ivMood5
        )
        moodViews.forEachIndexed { index, imageView ->
            imageView.setOnClickListener {
                val clickedIndex = index + 1
                if (selectedMoodIndex == clickedIndex) {
                    selectedMoodIndex = 0
                    updateMoodUI(moodViews, -1)
                } else {
                    selectedMoodIndex = clickedIndex
                    updateMoodUI(moodViews, index)
                }
            }
        }
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