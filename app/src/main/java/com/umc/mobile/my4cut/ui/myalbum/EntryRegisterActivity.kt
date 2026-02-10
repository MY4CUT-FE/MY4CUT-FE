package com.umc.mobile.my4cut.ui.myalbum

import android.animation.ArgbEvaluator
import android.content.Intent
import android.graphics.Color
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
import androidx.lifecycle.lifecycleScope
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.CompositePageTransformer
import androidx.viewpager2.widget.MarginPageTransformer
import com.umc.mobile.my4cut.MainActivity
import com.umc.mobile.my4cut.R
import com.umc.mobile.my4cut.databinding.ActivityEntryRegisterBinding
import com.umc.mobile.my4cut.databinding.ItemPhotoAddBinding
import com.umc.mobile.my4cut.databinding.ItemPhotoSliderBinding
import com.google.android.material.card.MaterialCardView
import com.umc.mobile.my4cut.data.day4cut.remote.CreateDay4CutRequest
import com.umc.mobile.my4cut.data.day4cut.remote.Day4CutImage
import com.umc.mobile.my4cut.databinding.ActivityEntryRegister2Binding
import com.umc.mobile.my4cut.databinding.ItemPhotoSlider2Binding
import com.umc.mobile.my4cut.network.RetrofitClient
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import kotlin.math.abs

class EntryRegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEntryRegister2Binding

    // 이미지 무한대 추가 리스트
    private var selectedImageUris = mutableListOf<Uri>()

    private var isDiaryExpanded = false
    private var selectedMoodIndex = 0

    private var selectedDate: String = ""
    private var isEditMode = false // 수정 모드 여부

    private val pickMultipleMedia = registerForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(50)) { uris ->
        if (uris.isNotEmpty()) {
            // 기존 리스트에 추가
            selectedImageUris.addAll(uris)
            updatePhotoState()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEntryRegister2Binding.inflate(layoutInflater)
        setContentView(binding.root)

        selectedDate = intent.getStringExtra("SELECTED_DATE") ?: "2026-01-01"
        val rawDate = selectedDate.replace("-", ".")
        binding.tvDateCapsule.text = rawDate

        fetchExistingDay4Cut()

        setupClickListeners()
        setupPhotoPicker()
        setupDiaryLogic()
        setupMoodSelection()
    }

    // [GET] 기존 기록 조회
    private fun fetchExistingDay4Cut() {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.day4CutService.getDay4CutDetail(selectedDate)
                if (response.code == "D2001") {
                    isEditMode = true
                    val data = response.data

                    // 내용 채우기
                    binding.etDiary.setText(data?.content)

                    // 이모지 복원
                    val emojiMapInv = mapOf("CALM" to 1, "TIRED" to 2, "HAPPY" to 3, "ANGRY" to 4, "SAD" to 5)
                    selectedMoodIndex = emojiMapInv[data?.emojiType] ?: 0
                    if (selectedMoodIndex > 0) {
                        updateMoodUI(listOf(binding.ivMood1, binding.ivMood2, binding.ivMood3, binding.ivMood4, binding.ivMood5), selectedMoodIndex - 1)
                    }

                    // 사진 처리: 수정 시 사진은 '전체 교체' 방식이므로
                    // 기존 사진 정보를 어떻게 관리할지(그대로 둘지, 새로 갤러리를 열지) 결정해야 합니다.
                    // 만약 기존 사진을 수정 없이 유지하고 싶다면 해당 URL을 Uri로 변환하거나
                    // 사용자가 사진을 새로 추가하도록 유도해야 합니다.
                }
            } catch (e: Exception) {
                isEditMode = false
                Log.d("DEBUG", "기존 기록 없음 (신규 생성 모드)")
            }
        }
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener { finish() }
        binding.btnComplete.setOnClickListener {
            saveDay4Cut()
        }
    }

    private fun saveDay4Cut() {
        lifecycleScope.launch {
            try {
                // 0. 로딩 바 표시 (필요시)
                // binding.btnComplete.isEnabled = false

                // 1. 이미지 업로드 (기존에 작성하신 prepareMultipartList 활용)
                val multipartFiles = prepareMultipartList(selectedImageUris)
                val uploadResponse = RetrofitClient.imageService.uploadImagesMedia(multipartFiles)

                if (uploadResponse.isSuccessful) {
                    // 2. Request Body 조립
                    val uploadedImages = uploadResponse.body()?.data ?: emptyList()

                    val imageList = uploadedImages.mapIndexed { index, data ->
                        Day4CutImage(
                            mediaFileId = data.fileId,
                            isThumbnail = (index == 0) // 첫 번째 사진을 썸네일로 지정
                        )
                    }

                    val emojiMap = mapOf(1 to "CALM", 2 to "TIRED", 3 to "HAPPY", 4 to "ANGRY", 5 to "SAD")
                    val request = CreateDay4CutRequest(
                        date = selectedDate,
                        content = binding.etDiary.text.toString(),
                        emojiType = emojiMap[selectedMoodIndex] ?: "HAPPY",
                        images = imageList
                    )

                    // 3. 모드에 따라 API 분기
                    val response = if (isEditMode) {
                        RetrofitClient.day4CutService.updateDay4Cut(request) // PATCH
                    } else {
                        RetrofitClient.day4CutService.createDay4Cut(request) // POST
                    }

                    if (response.code == "D1001" || response.code == "D1002") {
                        navigateToDetail(selectedDate)
                    }
                }
            } catch (e: Exception) {
                Log.e("API_ERROR", "저장 실패: ${e.message}")
                binding.btnComplete.isEnabled = true
            }
        }
    }

    private fun navigateToDetail(date: String) {
        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra("TARGET_FRAGMENT", "ENTRY_DETAIL")
            putExtra("selected_date", date)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        startActivity(intent)
        finish()
    }

    private fun prepareMultipartList(uris: List<Uri>): List<MultipartBody.Part> {
        val multipartList = mutableListOf<MultipartBody.Part>()
        val contentResolver = this.contentResolver

        uris.forEach { uri ->
            // 1. Uri에서 파일을 읽어 임시 파일 생성
            val inputStream = contentResolver.openInputStream(uri)
            val file = File(this.cacheDir, "upload_${System.currentTimeMillis()}.jpg")
            file.outputStream().use { inputStream?.copyTo(it) }

            // 2. RequestBody 생성 (MediaType은 이미지 타입에 맞게)
            val requestFile = file.asRequestBody(contentResolver.getType(uri)?.toMediaTypeOrNull())

            // 3. 서버 파라미터명인 "files"로 Part 생성
            val part = MultipartBody.Part.createFormData("files", file.name, requestFile)
            multipartList.add(part)
        }
        return multipartList
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

                    // 테두리 색상 변경
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

    // 어댑터 수정: 멀티 뷰 타입 지원
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
                // 사진 바인딩
                val photoHolder = holder as PhotoViewHolder
                photoHolder.binding.ivPhoto.setImageURI(imageUris[position])
            } else {
                // 추가 버튼 바인딩
                val addHolder = holder as AddViewHolder
                addHolder.itemView.setOnClickListener {
                    // 추가 버튼 클릭 시 다시 갤러리 열기
                    launchPhotoPicker()
                }
            }
        }

        override fun getItemCount(): Int {
            // 사진 개수 + 1 (마지막 플러스 버튼)
            return imageUris.size + 1
        }

        override fun getItemViewType(position: Int): Int {
            // 마지막 아이템은 '추가 버튼', 나머지는 '사진'
            return if (position == imageUris.size) TYPE_ADD else TYPE_PHOTO
        }
    }
}