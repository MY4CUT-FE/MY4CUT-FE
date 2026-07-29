package com.umc.mobile.my4cut.ui.myalbum

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.exifinterface.media.ExifInterface
import androidx.lifecycle.lifecycleScope
import com.umc.mobile.my4cut.MainActivity
import com.umc.mobile.my4cut.R
import com.umc.mobile.my4cut.data.day4cut.remote.CreateDay4CutRequest
import com.umc.mobile.my4cut.data.day4cut.remote.Day4CutImage
import com.umc.mobile.my4cut.databinding.ActivityEntryRegister2Binding
import com.umc.mobile.my4cut.network.RetrofitClient
import com.umc.mobile.my4cut.ui.record.PhotoUploadPager
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

class EntryRegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEntryRegister2Binding

    private val selectedImageUris = mutableStateListOf<Uri>()

    private var isDiaryExpanded = false
    private var selectedMoodIndex = 1  // 기본값: CALM (첫 번째 이모지)

    private val pickMultipleMedia = registerForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(50)
    ) { uris ->
        if (uris.isNotEmpty()) {
            selectedImageUris.addAll(uris)
            updateButtonState()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEntryRegister2Binding.inflate(layoutInflater)
        setContentView(binding.root)

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
        setupCalendarData()
        setupClickListeners()
        updateButtonState()
        setupDiaryLogic()
        setupMoodSelection()
    }

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
                Log.d("EntryRegister", "")
                Log.d("EntryRegister", "🔄 SAVE PROCESS STARTED")
                Log.d("EntryRegister", "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

                val fileParts = mutableListOf<MultipartBody.Part>()

                for ((index, uri) in selectedImageUris.withIndex()) {
                    Log.d("EntryRegister", "📤 Processing image ${index + 1}/${selectedImageUris.size}")

                    val compressedFile = compressImage(uri)

                    if (compressedFile == null) {
                        Log.e("EntryRegister", "❌ Image ${index + 1} compression failed")
                        throw Exception("이미지 ${index + 1} 압축 실패")
                    }

                    val requestBody = compressedFile.asRequestBody("image/jpeg".toMediaTypeOrNull())
                    val part = MultipartBody.Part.createFormData("files", compressedFile.name, requestBody)
                    fileParts.add(part)
                }

                Log.d("EntryRegister", "📤 Uploading ${fileParts.size} images via /media/upload/bulk")

                val uploadResponse = withContext(Dispatchers.IO) {
                    RetrofitClient.mediaService.uploadMediaBulk(fileParts)
                }

                Log.d("EntryRegister", "📨 Upload response: code=${uploadResponse.code}")

                if (uploadResponse.code != "C2001" && uploadResponse.code != "C2011") {
                    throw Exception("이미지 업로드 실패: ${uploadResponse.message}")
                }

                val uploadedFiles = uploadResponse.data ?: throw Exception("업로드 응답 데이터 없음")

                val images = uploadedFiles.mapIndexed { index, file ->
                    Day4CutImage(
                        mediaId = file.mediaId,
                        isThumbnail = (index == 0)
                    )
                }

                Log.d("EntryRegister", "📊 Uploaded fileIds: ${uploadedFiles.map { it.mediaId }}")

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

                Log.d("EntryRegister", "📝 Creating Day4Cut...")
                Log.d("EntryRegister", "Request: $request")

                val createResponse = withContext(Dispatchers.IO) {
                    RetrofitClient.day4CutService.createDay4Cut(request)
                }

                Log.d("EntryRegister", "📨 Create response: code=${createResponse.code}")

                when {
                    createResponse.code == "C2001" || createResponse.code == "C2011" -> {
                        Log.d("EntryRegister", "")
                        Log.d("EntryRegister", "🎉 DAY4CUT CREATED SUCCESSFULLY")
                        Log.d("EntryRegister", "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

                        withContext(Dispatchers.Main) {
                            cacheDir.listFiles()?.filter {
                                it.name.startsWith("compressed_")
                            }?.forEach { it.delete() }

                            Toast.makeText(this@EntryRegisterActivity, "저장되었습니다!", Toast.LENGTH_SHORT).show()

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
                            Toast.makeText(this@EntryRegisterActivity, "일기 내용을 입력해주세요.", Toast.LENGTH_SHORT).show()
                            if (!isDiaryExpanded) {
                                isDiaryExpanded = true
                                binding.clDiaryContent.visibility = View.VISIBLE
                                binding.ivDiaryArrow.setImageResource(R.drawable.ic_arrow_up_gray)
                            }
                            binding.etDiary.requestFocus()
                        }
                    }
                    else -> {
                        throw Exception("저장 실패: ${createResponse.message}")
                    }
                }

            } catch (e: Exception) {
                Log.e("EntryRegister", "💥 SAVE FAILED", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@EntryRegisterActivity, "${e.message}", Toast.LENGTH_LONG).show()

                    cacheDir.listFiles()?.filter {
                        it.name.startsWith("compressed_")
                    }?.forEach { it.delete() }
                }
            }
        }
    }

    private fun compressImage(uri: Uri): File? {
        return try {
            val inputStream = contentResolver.openInputStream(uri) ?: return null

            val originalBitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()

            if (originalBitmap == null) {
                Log.e("EntryRegister", "❌ Failed to decode bitmap from URI: $uri")
                return null
            }

            val rotatedBitmap = rotateImageIfRequired(uri, originalBitmap)
            val resizedBitmap = resizeBitmap(rotatedBitmap, 1920)

            val outputStream = ByteArrayOutputStream()
            resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
            val compressedBytes = outputStream.toByteArray()

            val tempFile = File(cacheDir, "compressed_${System.currentTimeMillis()}.jpg")
            FileOutputStream(tempFile).use { fos ->
                fos.write(compressedBytes)
            }

            if (rotatedBitmap != originalBitmap) {
                originalBitmap.recycle()
            }
            resizedBitmap.recycle()

            Log.d("EntryRegister", "✅ Image compressed: ${tempFile.length() / 1024}KB")

            tempFile
        } catch (e: Exception) {
            Log.e("EntryRegister", "❌ Image compression failed", e)
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
            Log.e("EntryRegister", "Failed to read EXIF", e)
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
            if (isDiaryExpanded) {
                binding.clDiaryContent.visibility = View.VISIBLE
                binding.ivDiaryArrow.setImageResource(R.drawable.ic_arrow_up_gray)
            } else {
                binding.clDiaryContent.visibility = View.GONE
                binding.ivDiaryArrow.setImageResource(R.drawable.ic_arrow_down_gray)
            }
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

    private fun setupMoodSelection() {
        val moodViews = listOf(
            binding.ivMood1, binding.ivMood2, binding.ivMood3, binding.ivMood4, binding.ivMood5
        )
        moodViews.forEachIndexed { index, imageView ->
            imageView.setOnClickListener {
                val clickedIndex = index + 1
                if (selectedMoodIndex == clickedIndex) {
                    // 선택된 이모지 재클릭 → 해제 (Toggle)
                    selectedMoodIndex = 0
                    updateMoodUI(moodViews, -1)
                } else {
                    selectedMoodIndex = clickedIndex
                    updateMoodUI(moodViews, index)
                }
            }
        }
        // 기본 선택: Calm (첫 번째 이모지, index=0)
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
