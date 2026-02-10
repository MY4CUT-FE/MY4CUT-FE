package com.umc.mobile.my4cut.ui.myalbum

import android.animation.ArgbEvaluator
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.exifinterface.media.ExifInterface
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.CompositePageTransformer
import androidx.viewpager2.widget.MarginPageTransformer
import com.google.android.material.card.MaterialCardView
import com.umc.mobile.my4cut.MainActivity
import com.umc.mobile.my4cut.R
import com.umc.mobile.my4cut.data.day4cut.remote.CreateDay4CutRequest
import com.umc.mobile.my4cut.data.day4cut.remote.Day4CutImage
import com.umc.mobile.my4cut.databinding.ActivityEntryRegister2Binding
import com.umc.mobile.my4cut.databinding.ItemPhotoAddBinding
import com.umc.mobile.my4cut.databinding.ItemPhotoSlider2Binding
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
import java.time.LocalDate
import kotlin.math.abs

class EntryRegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEntryRegister2Binding

    private var selectedImageUris = mutableListOf<Uri>()

    private var isDiaryExpanded = false
    private var selectedMoodIndex = 0

    // ✅ 썸네일 인덱스 추가
    private var typicalImageIndex = 0

    private val pickMultipleMedia = registerForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(50)
    ) { uris ->
        if (uris.isNotEmpty()) {
            selectedImageUris.addAll(uris)
            updatePhotoState()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEntryRegister2Binding.inflate(layoutInflater)
        setContentView(binding.root)

        setupDateData()
        setupCalendarData()
        setupClickListeners()
        setupPhotoPicker()
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

            updatePhotoState()
        }
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener { finish() }

        // ✅ 완료 버튼: API 호출 추가
        binding.btnComplete.setOnClickListener {
            saveDay4Cut()
        }
    }

    /**
     * ✅ 하루네컷 저장 (API 호출)
     */
    private fun saveDay4Cut() {
        lifecycleScope.launch {
            try {
                Log.d("EntryRegister", "")
                Log.d("EntryRegister", "🔄 SAVE PROCESS STARTED")
                Log.d("EntryRegister", "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

                // Step 1: 이미지 압축
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

                // Step 2: Bulk 업로드
                Log.d("EntryRegister", "📤 Uploading ${fileParts.size} images via /media/upload/bulk")

                val uploadResponse = withContext(Dispatchers.IO) {
                    RetrofitClient.mediaService.uploadMediaBulk(fileParts)
                }

                Log.d("EntryRegister", "📨 Upload response: code=${uploadResponse.code}")

                if (uploadResponse.code != "C2001" && uploadResponse.code != "C2011") {
                    throw Exception("이미지 업로드 실패: ${uploadResponse.message}")
                }

                val uploadedFiles = uploadResponse.data ?: throw Exception("업로드 응답 데이터 없음")

                // Step 3: Day4Cut 생성 요청 구성
                val images = uploadedFiles.mapIndexed { index, file ->
                    Day4CutImage(
                        mediaFileId = file.fileId,
                        isThumbnail = (index == typicalImageIndex)  // ✅ 선택된 썸네일
                    )
                }

                Log.d("EntryRegister", "📊 Uploaded fileIds: ${uploadedFiles.map { it.fileId }}")

                // 날짜 변환 ("2026.2.9" -> "2026-02-09")
                val dateString = intent.getStringExtra("SELECTED_DATE") ?: ""
                val apiDate = convertToApiDate(dateString)

                val request = CreateDay4CutRequest(
                    date = apiDate,
                    content = binding.etDiary.text.toString().ifBlank { null },
                    emojiType = getEmojiType(),
                    images = images
                )

                // Step 4: POST /day4cut
                Log.d("EntryRegister", "📝 Creating Day4Cut...")
                Log.d("EntryRegister", "Request: $request")

                val createResponse = withContext(Dispatchers.IO) {
                    RetrofitClient.day4CutService.createDay4Cut(request)
                }

                Log.d("EntryRegister", "📨 Create response: code=${createResponse.code}")

                if (createResponse.code == "C2001" || createResponse.code == "C2011") {
                    Log.d("EntryRegister", "")
                    Log.d("EntryRegister", "🎉 DAY4CUT CREATED SUCCESSFULLY")
                    Log.d("EntryRegister", "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

                    withContext(Dispatchers.Main) {
                        // 임시 파일 정리
                        cacheDir.listFiles()?.filter {
                            it.name.startsWith("compressed_")
                        }?.forEach { it.delete() }

                        Toast.makeText(this@EntryRegisterActivity, "저장되었습니다!", Toast.LENGTH_SHORT).show()

                        // FLAG_ACTIVITY_CLEAR_TOP을 쓰면 스택에 쌓인 이전 Activity들을 정리하며 MainActivity로 돌아갑니다.
                        val intent = Intent(this@EntryRegisterActivity, MainActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                            putExtra("MOVE_TO_DETAIL", true)
                            putExtra("API_DATE", apiDate)
                            putExtra("SELECTED_DATE", dateString)
                        }

                        startActivity(intent)

                        finish()
                    }
                } else {
                    throw Exception("저장 실패: ${createResponse.message}")
                }

            } catch (e: Exception) {
                Log.e("EntryRegister", "💥 SAVE FAILED", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@EntryRegisterActivity, "${e.message}", Toast.LENGTH_LONG).show()

                    // 임시 파일 정리
                    cacheDir.listFiles()?.filter {
                        it.name.startsWith("compressed_")
                    }?.forEach { it.delete() }
                }
            }
        }
    }

    /**
     * 이미지 압축
     */
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

    /**
     * "2026.2.9" -> "2026-02-09"
     */
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

    /**
     * 선택된 이모지 타입 반환
     */
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

    private fun setupPhotoPicker() {
        binding.clPhotoEmpty.setOnClickListener {
            launchPhotoPicker()
        }
    }

    private fun launchPhotoPicker() {
        pickMultipleMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    private fun updatePhotoState() {
        if (selectedImageUris.isNotEmpty()) {
            binding.clPhotoEmpty.visibility = View.GONE
            binding.vpPhotoSlider.visibility = View.VISIBLE
            binding.vpPhotoSlider.adapter = PhotoPagerAdapter(selectedImageUris)

            binding.vpPhotoSlider.apply {
                offscreenPageLimit = 1
                getChildAt(0).overScrollMode = RecyclerView.OVER_SCROLL_NEVER

                val transform = CompositePageTransformer()
                transform.addTransformer(MarginPageTransformer(0))

                val argbEvaluator = ArgbEvaluator()
                val activeColor = Color.parseColor("#FFD5CD")
                val inactiveColor = Color.parseColor("#D9D9D9")

                transform.addTransformer { page, position ->
                    val r = 1 - abs(position)
                    page.scaleY = 0.85f + r * 0.15f

                    val photoCard = page.findViewById<MaterialCardView>(R.id.cv_photo_card)
                    val addCard = page.findViewById<MaterialCardView>(R.id.cv_add_card)
                    val targetCard = photoCard ?: addCard

                    if (targetCard != null) {
                        val colorFraction = abs(position).coerceIn(0f, 1f)
                        val color = argbEvaluator.evaluate(colorFraction, activeColor, inactiveColor) as Int
                        targetCard.strokeColor = color
                    }

                    val addIcon = page.findViewById<ImageView>(R.id.iv_add_icon)

                    if (addIcon != null && addCard != null) {
                        if (position > 0) {
                            val cardWidth = if (addCard.width > 0) addCard.width.toFloat() else 1000f
                            val moveDistance = (cardWidth / 2f) - (addIcon.width / 2f) - 20f
                            addIcon.translationX = -position * moveDistance
                        } else {
                            addIcon.translationX = 0f
                        }
                    }
                }
                setPageTransformer(transform)
            }

            binding.btnComplete.isEnabled = true
            binding.btnComplete.alpha = 1.0f
        } else {
            binding.clPhotoEmpty.visibility = View.VISIBLE
            binding.vpPhotoSlider.visibility = View.GONE
            binding.btnComplete.isEnabled = false
            binding.btnComplete.alpha = 0.5f
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
                selectedMoodIndex = index + 1
                updateMoodUI(moodViews, index)
            }
        }
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

    inner class PhotoPagerAdapter(private val imageUris: List<Uri>) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        private val TYPE_PHOTO = 0
        private val TYPE_ADD = 1

        inner class PhotoViewHolder(val binding: ItemPhotoSlider2Binding) : RecyclerView.ViewHolder(binding.root)
        inner class AddViewHolder(val binding: ItemPhotoAddBinding) : RecyclerView.ViewHolder(binding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            return if (viewType == TYPE_PHOTO) {
                val binding = ItemPhotoSlider2Binding.inflate(LayoutInflater.from(parent.context), parent, false)
                PhotoViewHolder(binding)
            } else {
                val binding = ItemPhotoAddBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                AddViewHolder(binding)
            }
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            if (getItemViewType(position) == TYPE_PHOTO) {
                val photoHolder = holder as PhotoViewHolder
                photoHolder.binding.ivPhoto.setImageURI(imageUris[position])

                // ✅ 썸네일 표시
                val isTypical = position == typicalImageIndex
                photoHolder.binding.ivTypical.visibility = View.VISIBLE
                photoHolder.binding.ivTypical.setImageResource(
                    if (isTypical) R.drawable.ic_typical_on else R.drawable.ic_typical_off
                )

                // ✅ 썸네일 클릭 이벤트
                photoHolder.binding.ivTypical.setOnClickListener {
                    typicalImageIndex = holder.bindingAdapterPosition
                    notifyDataSetChanged()
                }
            } else {
                val addHolder = holder as AddViewHolder
                addHolder.itemView.setOnClickListener {
                    launchPhotoPicker()
                }
            }
        }

        override fun getItemCount(): Int {
            return imageUris.size + 1
        }

        override fun getItemViewType(position: Int): Int {
            return if (position == imageUris.size) TYPE_ADD else TYPE_PHOTO
        }
    }
}