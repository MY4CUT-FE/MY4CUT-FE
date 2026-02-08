package com.umc.mobile.my4cut.ui.myalbum

import android.animation.ArgbEvaluator
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.CompositePageTransformer
import androidx.viewpager2.widget.MarginPageTransformer
import com.umc.mobile.my4cut.MainActivity
import com.umc.mobile.my4cut.R
import com.umc.mobile.my4cut.databinding.ActivityEntryRegisterBinding
import com.umc.mobile.my4cut.databinding.ItemPhotoAddBinding
import com.umc.mobile.my4cut.databinding.ItemPhotoSliderBinding
import com.google.android.material.card.MaterialCardView
import com.umc.mobile.my4cut.databinding.ActivityEntryRegister2Binding
import com.umc.mobile.my4cut.databinding.ItemPhotoSlider2Binding
import kotlin.math.abs

class EntryRegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEntryRegister2Binding

    // 이미지 무한대 추가 리스트
    private var selectedImageUris = mutableListOf<Uri>()

    private var isDiaryExpanded = false
    private var selectedMoodIndex = 0

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

        setupDateData()
        setupCalendarData()
        setupClickListeners()
        setupPhotoPicker()
        setupDiaryLogic()
        setupMoodSelection()
    }

    private fun setupDateData() {
        val dateString = intent.getStringExtra("SELECTED_DATE") ?: "2026.01.01"
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
        binding.btnComplete.setOnClickListener {
            val dateString = intent.getStringExtra("SELECTED_DATE")
            val calendarData = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getSerializableExtra("SELECTED_DATA", CalendarData::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getSerializableExtra("SELECTED_DATA") as? CalendarData
            }

            val intent = Intent(this, MainActivity::class.java).apply {
                // MainActivity에게 프래그먼트 전환이 필요함을 알리는 신호
                putExtra("TARGET_FRAGMENT", "ENTRY_DETAIL")
                putExtra("selected_date", dateString)
                putExtra("calendar_data", calendarData)

                // 중요: MainActivity가 새로 생성되는 게 아니라 기존 것을 재사용하도록 설정
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }

            startActivity(intent)
            finish()
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