package com.umc.mobile.my4cut.ui.record

import android.animation.ArgbEvaluator
import android.graphics.Color
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
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.umc.mobile.my4cut.R
import com.umc.mobile.my4cut.data.base.BaseResponse
import com.umc.mobile.my4cut.data.day4cut.remote.CreateDay4CutRequest
import com.umc.mobile.my4cut.data.day4cut.remote.Day4CutImage
import com.umc.mobile.my4cut.data.image.remote.PresignedUrlRequest
import com.umc.mobile.my4cut.databinding.ActivityEntryRegisterBinding
import com.umc.mobile.my4cut.databinding.ItemPhotoAddBinding
import com.umc.mobile.my4cut.databinding.ItemPhotoSliderBinding
import com.umc.mobile.my4cut.network.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.io.FileOutputStream

class EntryRegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEntryRegisterBinding

    private var selectedImageUris = mutableListOf<Uri>()
    private var isDiaryExpanded = false
    private var selectedMoodIndex = 1

    private val uploadedMediaIds = mutableListOf<Int>()

    private val pickMultipleMedia = registerForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(50)) { uris ->
        if (uris.isNotEmpty()) {
            selectedImageUris.addAll(uris)
            updatePhotoState()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEntryRegisterBinding.inflate(layoutInflater)
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

    private fun uploadImagesAndCreateDay4Cut() {
        binding.btnComplete.isEnabled = false
        Toast.makeText(this, "업로드 중...", Toast.LENGTH_SHORT).show()

        lifecycleScope.launch {
            try {
                uploadedMediaIds.clear()
                Log.d("EntryRegister", "")
                Log.d("EntryRegister", "📂 Step 1-2: IMAGE UPLOAD PHASE")
                Log.d("EntryRegister", "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

                for ((index, uri) in selectedImageUris.withIndex()) {
                    Log.d("EntryRegister", "")
                    Log.d("EntryRegister", "📤 Uploading image ${index + 1}/${selectedImageUris.size}")
                    Log.d("EntryRegister", "URI: $uri")

                    val mediaId = uploadImageWithPresignedUrl(uri, index)

                    if (mediaId != null) {
                        uploadedMediaIds.add(mediaId)
                        Log.d("EntryRegister", "✅ Image ${index + 1} uploaded successfully")
                        Log.d("EntryRegister", "   └─ mediaId: $mediaId")
                    } else {
                        Log.e("EntryRegister", "❌ Image ${index + 1} upload FAILED")
                        throw Exception("이미지 ${index + 1} 업로드 실패")
                    }
                }

                Log.d("EntryRegister", "")
                Log.d("EntryRegister", "✅ ALL IMAGES UPLOADED SUCCESSFULLY")
                Log.d("EntryRegister", "📊 Uploaded mediaIds: $uploadedMediaIds")
                Log.d("EntryRegister", "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

                withContext(Dispatchers.Main) {
                    createDay4Cut()
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Log.e("EntryRegister", "")
                    Log.e("EntryRegister", "💥 UPLOAD PROCESS FAILED")
                    Log.e("EntryRegister", "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                    Log.e("EntryRegister", "Error: ${e.message}", e)
                    Toast.makeText(this@EntryRegisterActivity, "업로드 실패: ${e.message}", Toast.LENGTH_LONG).show()
                    binding.btnComplete.isEnabled = true
                }
            }
        }
    }

    private suspend fun uploadImageWithPresignedUrl(uri: Uri, imageIndex: Int): Int? = withContext(Dispatchers.IO) {
        try {
            // ==================== Step 1: Presigned URL 발급 ====================
            val fileName = "photo_${System.currentTimeMillis()}.jpg"
            val request = PresignedUrlRequest(
                type = "CALENDAR",
                fileName = fileName,
                contentType = "image/jpeg"
            )

            Log.d("EntryRegister", "")
            Log.d("EntryRegister", "   [Step 1] 🔑 Requesting Presigned URL")
            Log.d("EntryRegister", "   ├─ API: POST /images/presigned-url")
            Log.d("EntryRegister", "   ├─ type: CALENDAR")
            Log.d("EntryRegister", "   ├─ fileName: $fileName")
            Log.d("EntryRegister", "   └─ contentType: image/jpeg")

            val presignedResponse = RetrofitClient.imageService.getPresignedUrl(request).execute()

            Log.d("EntryRegister", "   ├─ Response Code: ${presignedResponse.code()}")

            if (!presignedResponse.isSuccessful) {
                val errorBody = presignedResponse.errorBody()?.string()
                Log.e("EntryRegister", "")
                Log.e("EntryRegister", "   ❌ API ERROR: POST /images/presigned-url")
                Log.e("EntryRegister", "   ├─ Status Code: ${presignedResponse.code()}")
                Log.e("EntryRegister", "   ├─ Error Message: ${presignedResponse.message()}")
                Log.e("EntryRegister", "   └─ Error Body: $errorBody")
                return@withContext null
            }

            if (presignedResponse.body()?.data == null) {
                Log.e("EntryRegister", "   ❌ Response body or data is null")
                return@withContext null
            }

            val presignedData = presignedResponse.body()!!.data!!
            val mediaId = presignedData.mediaId
            val uploadUrl = presignedData.uploadUrl

            Log.d("EntryRegister", "   ✅ Presigned URL received")
            Log.d("EntryRegister", "   ├─ mediaId: $mediaId")
            Log.d("EntryRegister", "   ├─ fileKey: ${presignedData.fileKey}")
            Log.d("EntryRegister", "   └─ uploadUrl: ${uploadUrl.take(100)}...")

            // ==================== Step 2: S3 업로드 ====================
            val file = uriToFile(uri)
            val fileSize = file.length() / 1024 // KB
            val requestBody = file.asRequestBody("image/jpeg".toMediaTypeOrNull())

            Log.d("EntryRegister", "")
            Log.d("EntryRegister", "   [Step 2] ☁️ Uploading to S3")
            Log.d("EntryRegister", "   ├─ Method: PUT")
            Log.d("EntryRegister", "   ├─ File size: ${fileSize}KB")
            Log.d("EntryRegister", "   ├─ Content-Type: image/jpeg")
            Log.d("EntryRegister", "   └─ Destination: S3 Bucket")

            val uploadResponse = RetrofitClient.s3UploadService.uploadToS3(uploadUrl, requestBody).execute()

            Log.d("EntryRegister", "   ├─ Response Code: ${uploadResponse.code()}")

            if (!uploadResponse.isSuccessful) {
                val errorBody = uploadResponse.errorBody()?.string()
                Log.e("EntryRegister", "")
                Log.e("EntryRegister", "   ❌ S3 UPLOAD FAILED")
                Log.e("EntryRegister", "   ├─ Status Code: ${uploadResponse.code()}")
                Log.e("EntryRegister", "   ├─ Error Message: ${uploadResponse.message()}")
                Log.e("EntryRegister", "   └─ Error Body: $errorBody")
                return@withContext null
            }

            Log.d("EntryRegister", "   ✅ S3 upload successful")
            Log.d("EntryRegister", "   └─ Returning mediaId: $mediaId")

            mediaId

        } catch (e: Exception) {
            Log.e("EntryRegister", "")
            Log.e("EntryRegister", "   💥 EXCEPTION during image upload")
            Log.e("EntryRegister", "   ├─ Image index: $imageIndex")
            Log.e("EntryRegister", "   ├─ Exception type: ${e.javaClass.simpleName}")
            Log.e("EntryRegister", "   └─ Message: ${e.message}", e)
            null
        }
    }

    private fun uriToFile(uri: Uri): File {
        val inputStream = contentResolver.openInputStream(uri)
        val file = File(cacheDir, "temp_image_${System.currentTimeMillis()}.jpg")
        val outputStream = FileOutputStream(file)
        inputStream?.copyTo(outputStream)
        inputStream?.close()
        outputStream.close()
        return file
    }

    private fun createDay4Cut() {
        val dateString = intent.getStringExtra("SELECTED_DATE") ?: "2026.01.01"
        val apiDate = dateString.replace(".", "-")

        val content = binding.etDiary.text.toString().takeIf { it.isNotBlank() }

        // ✅ 변경된 이모지 타입
        val emojiType = when (selectedMoodIndex) {
            1 -> "HAPPY"
            2 -> "ANGRY"
            3 -> "TIRED"
            4 -> "SAD"
            5 -> "CALM"
            else -> "HAPPY"
        }

        val images = uploadedMediaIds.mapIndexed { index, mediaId ->
            Day4CutImage(
                mediaFileId = mediaId,
                isThumbnail = (index == 0)
            )
        }

        val request = CreateDay4CutRequest(
            date = apiDate,
            content = content,
            emojiType = emojiType,
            images = images
        )

        Log.d("EntryRegister", "")
        Log.d("EntryRegister", "📝 Step 3: DAY4CUT CREATION PHASE")
        Log.d("EntryRegister", "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        Log.d("EntryRegister", "🔹 API: POST /day4cut")
        Log.d("EntryRegister", "🔹 Request Body:")
        Log.d("EntryRegister", "   ├─ date: $apiDate")
        Log.d("EntryRegister", "   ├─ content: ${content ?: "(null)"}")
        Log.d("EntryRegister", "   ├─ emojiType: $emojiType")
        Log.d("EntryRegister", "   └─ images: ${images.size} items")
        images.forEachIndexed { index, image ->
            Log.d("EntryRegister", "       ├─ [$index] mediaFileId: ${image.mediaFileId}, isThumbnail: ${image.isThumbnail}")
        }

        RetrofitClient.day4CutService.createDay4Cut(request)
            .enqueue(object : Callback<BaseResponse<String>> {
                override fun onResponse(
                    call: Call<BaseResponse<String>>,
                    response: Response<BaseResponse<String>>
                ) {
                    Log.d("EntryRegister", "")
                    Log.d("EntryRegister", "📨 Response received from POST /day4cut")
                    Log.d("EntryRegister", "   ├─ Status Code: ${response.code()}")
                    Log.d("EntryRegister", "   ├─ Status Message: ${response.message()}")

                    if (response.isSuccessful) {
                        val responseBody = response.body()
                        Log.d("EntryRegister", "   ├─ Response Body: $responseBody")
                        Log.d("EntryRegister", "   └─ Result: SUCCESS ✅")
                        Log.d("EntryRegister", "")
                        Log.d("EntryRegister", "🎉 DAY4CUT CREATED SUCCESSFULLY")
                        Log.d("EntryRegister", "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                        Log.d("EntryRegister", "")

                        Toast.makeText(this@EntryRegisterActivity, "기록이 저장되었습니다!", Toast.LENGTH_SHORT).show()

                        setResult(RESULT_OK)
                        finish()
                    } else {
                        val errorBody = response.errorBody()?.string()
                        Log.e("EntryRegister", "   └─ Result: FAILED ❌")
                        Log.e("EntryRegister", "")
                        Log.e("EntryRegister", "❌ API ERROR: POST /day4cut")
                        Log.e("EntryRegister", "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                        Log.e("EntryRegister", "Status Code: ${response.code()}")
                        Log.e("EntryRegister", "Error Message: ${response.message()}")
                        Log.e("EntryRegister", "Error Body: $errorBody")
                        Log.e("EntryRegister", "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                        Log.e("EntryRegister", "")

                        Toast.makeText(this@EntryRegisterActivity, "저장 실패: ${response.code()}", Toast.LENGTH_LONG).show()
                        binding.btnComplete.isEnabled = true
                    }
                }

                override fun onFailure(call: Call<BaseResponse<String>>, t: Throwable) {
                    Log.e("EntryRegister", "")
                    Log.e("EntryRegister", "💥 NETWORK ERROR: POST /day4cut")
                    Log.e("EntryRegister", "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                    Log.e("EntryRegister", "Exception type: ${t.javaClass.simpleName}")
                    Log.e("EntryRegister", "Message: ${t.message}", t)
                    Log.e("EntryRegister", "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                    Log.e("EntryRegister", "")

                    Toast.makeText(this@EntryRegisterActivity, "네트워크 오류: ${t.message}", Toast.LENGTH_LONG).show()
                    binding.btnComplete.isEnabled = true
                }
            })
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
                imageView.alpha = 1.0f
                imageView.scaleX = 1.2f
                imageView.scaleY = 1.2f
            } else {
                imageView.alpha = 0.4f
                imageView.scaleX = 1.0f
                imageView.scaleY = 1.0f
            }
        }
    }

    inner class PhotoPagerAdapter(private val imageUris: List<Uri>) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        private val TYPE_PHOTO = 0
        private val TYPE_ADD = 1

        inner class PhotoViewHolder(val binding: ItemPhotoSliderBinding) : RecyclerView.ViewHolder(binding.root)
        inner class AddViewHolder(val binding: ItemPhotoAddBinding) : RecyclerView.ViewHolder(binding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            return if (viewType == TYPE_PHOTO) {
                val binding = ItemPhotoSliderBinding.inflate(LayoutInflater.from(parent.context), parent, false)
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
