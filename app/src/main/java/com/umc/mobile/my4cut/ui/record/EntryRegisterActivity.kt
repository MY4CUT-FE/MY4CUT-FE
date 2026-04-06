package com.umc.mobile.my4cut.ui.record

import android.animation.ArgbEvaluator
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Matrix
import android.net.Uri
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
import com.google.android.material.card.MaterialCardView
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
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

class EntryRegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEntryRegister2Binding

    private var selectedImageUris = mutableListOf<Uri>()
    private var isDiaryExpanded = false
    private var selectedMoodIndex = 1

    // ✅ 썸네일로 지정할 이미지 인덱스 (기본값: 0)
    private var typicalImageIndex = 0

    private val uploadedMediaIds = mutableListOf<Int>()

    private val pickMultipleMedia = registerForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(50)) { uris ->
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
        setupClickListeners()
        setupPhotoPicker()
        setupDiaryLogic()
        setupMoodSelection()
    }

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

                val transform = androidx.viewpager2.widget.CompositePageTransformer()
                transform.addTransformer(androidx.viewpager2.widget.MarginPageTransformer(0))

                val argbEvaluator = ArgbEvaluator()
                val activeColor = Color.parseColor("#FFD5CD")
                val inactiveColor = Color.parseColor("#D9D9D9")

                transform.addTransformer { page, position ->
                    val r = 1 - kotlin.math.abs(position)
                    page.scaleY = 0.85f + r * 0.15f

                    val photoCard = page.findViewById<MaterialCardView>(R.id.cv_photo_card)
                    val addCard = page.findViewById<MaterialCardView>(R.id.cv_add_card)
                    val targetCard = photoCard ?: addCard

                    if (targetCard != null) {
                        val colorFraction = kotlin.math.abs(position).coerceIn(0f, 1f)
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

        val content = binding.etDiary.text.toString().takeIf { it.isNotBlank() }

        val emojiType = when (selectedMoodIndex) {
            1 -> "HAPPY"
            2 -> "CALM"
            3 -> "TIRED"
            4 -> "ANGRY"
            5 -> "SAD"
            else -> "HAPPY"
        }

        val images = uploadedMediaIds.mapIndexed { index, mediaId ->
            Day4CutImage(
                mediaId = mediaId,
                isThumbnail = (index == typicalImageIndex)  // ✅ 사용자가 선택한 썸네일 인덱스 사용
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

                if (response.data != null || response.message.contains("성공")) {
                    Log.d("EntryRegister", "🎉 DAY4CUT CREATED SUCCESSFULLY")
                    Toast.makeText(this@EntryRegisterActivity, "기록이 저장되었습니다!", Toast.LENGTH_SHORT).show()
                    setResult(RESULT_OK)
                    finish()
                } else {
                    Log.e("EntryRegister", "❌ Day4Cut creation failed: ${response.message}")
                    Toast.makeText(this@EntryRegisterActivity, "저장 실패: ${response.message}", Toast.LENGTH_LONG).show()
                    binding.btnComplete.isEnabled = true
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

        updateMoodUI(moodViews, 0)
    }

    private fun updateMoodUI(views: List<ImageView>, selectedIndex: Int) {
        views.forEachIndexed { index, imageView ->
            if (index == selectedIndex) {
                imageView.setBackgroundResource(R.drawable.bg_mood_selected)
                imageView.alpha = 1.0f
                // imageView.scaleX = 1.2f
                // mageView.scaleY = 1.2f
            } else {
                imageView.background = null
                imageView.alpha = 0.4f
                // imageView.scaleX = 1.0f
                // imageView.scaleY = 1.0f
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

                // ✅ 썸네일 아이콘 표시
                val isTypical = position == typicalImageIndex
                photoHolder.binding.ivTypical.setImageResource(
                    if (isTypical) R.drawable.ic_typical_on else R.drawable.ic_typical_off
                )

                // ✅ 썸네일 아이콘 클릭 이벤트
                photoHolder.binding.ivTypical.setOnClickListener {
                    val oldIndex = typicalImageIndex
                    val newIndex = holder.bindingAdapterPosition

                    if (oldIndex != newIndex) {
                        typicalImageIndex = newIndex
                        notifyItemChanged(oldIndex)
                        notifyItemChanged(newIndex)
                    }
                }
            } else {
                val addHolder = holder as AddViewHolder
                addHolder.itemView.setOnClickListener {
                    launchPhotoPicker()
                }
            }
        }

        override fun getItemCount(): Int = imageUris.size + 1

        override fun getItemViewType(position: Int): Int {
            return if (position == imageUris.size) TYPE_ADD else TYPE_PHOTO
        }
    }
}