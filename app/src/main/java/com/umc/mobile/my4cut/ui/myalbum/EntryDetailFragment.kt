package com.umc.mobile.my4cut.ui.myalbum

import android.animation.ArgbEvaluator
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
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.CompositePageTransformer
import androidx.viewpager2.widget.MarginPageTransformer
import com.umc.mobile.my4cut.R
import com.umc.mobile.my4cut.databinding.ItemPhotoAddBinding
import com.umc.mobile.my4cut.databinding.ItemPhotoSliderBinding
import com.google.android.material.card.MaterialCardView
import kotlin.math.abs
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.umc.mobile.my4cut.MainActivity
import com.umc.mobile.my4cut.databinding.DialogExitBinding
import com.umc.mobile.my4cut.databinding.FragmentEntryDetailBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.umc.mobile.my4cut.databinding.DialogExit2Binding
import com.umc.mobile.my4cut.databinding.ItemPhotoSlider2Binding

class EntryDetailFragment : Fragment() {
    private lateinit var binding: FragmentEntryDetailBinding

    private var selectedImageUris = mutableListOf<Uri>()
    private var isDiaryExpanded = false
    private var selectedMoodIndex = 0

    private var calendarData: CalendarData? = null
    private var selectedDate: String? = null

    private var isEditMode = false

    private var originalImageUris = mutableListOf<Uri>()
    private var typicalImageUri: Uri? = null

    // 갤러리 호출
    private val pickMultipleMedia = registerForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(50)) { uris ->
        if (uris.isNotEmpty()) {
            selectedImageUris.addAll(uris)
            updatePhotoState()
            // 사진이 추가되면 가장 마지막에 추가된 사진 쪽으로 이동
            binding.vpPhotoSlider.currentItem = selectedImageUris.size - 1
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentEntryDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupCalendarData()
        setupClickListeners()
        setupPhotoPicker()
        setupDiaryLogic()
    }

    private fun setupCalendarData() {
        arguments?.let {
            selectedDate = it.getString("selected_date") ?: "2026.01.01"

            calendarData = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                it.getSerializable("calendar_data", CalendarData::class.java)
            } else {
                @Suppress("DEPRECATION")
                it.getSerializable("calendar_data") as? CalendarData
            }
        }

        binding.tvDateCapsule.text = selectedDate

        calendarData?.let { data ->
            binding.etDiary.setText(data.memo)

            val uris = data.imageUris.map { Uri.parse(it) }
            selectedImageUris.clear()
            selectedImageUris.addAll(uris)

            if (selectedImageUris.isNotEmpty()) {
                typicalImageUri = selectedImageUris[0]
            }

            updatePhotoState()
        }
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener {
            parentFragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)

            val mainFragment = CalendarMainFragment()

            (requireActivity() as MainActivity).changeFragment(mainFragment)
        }

        binding.btnEdit.setOnClickListener {
            setEditMode(true)

            binding.vpPhotoSlider.setCurrentItem(selectedImageUris.size, true)
        }

        binding.btnCancel.setOnClickListener {
            selectedImageUris.clear()
            selectedImageUris.addAll(originalImageUris)

            updatePhotoState()
            setEditMode(false)
        }

        binding.btnComplete.setOnClickListener {
            originalImageUris.clear()
            setEditMode(false)
            updatePhotoState()
        }
    }

    private fun setEditMode(isEditing: Boolean) {
        if (isEditing) {
            // 편집 모드 활성화
            originalImageUris.clear()
            originalImageUris.addAll(selectedImageUris)

            binding.btnEdit.visibility = View.GONE
            binding.btnCancel.visibility = View.VISIBLE
            binding.btnComplete.visibility = View.VISIBLE
            binding.tvTextCount.visibility = View.VISIBLE

            isEditMode = true
        } else {
            // 편집 모드 종료
            binding.btnEdit.visibility = View.VISIBLE
            binding.btnCancel.visibility = View.GONE
            binding.btnComplete.visibility = View.GONE
            binding.tvTextCount.visibility = View.GONE

            isEditMode = false
        }

        binding.vpPhotoSlider.adapter?.notifyDataSetChanged()
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
        binding.etDiary.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val length = s?.length ?: 0
                binding.tvTextCount.text = "$length/100"
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun showExitDialog(position: Int) {
        val dialogBinding = DialogExit2Binding.inflate(layoutInflater)
        val builder = MaterialAlertDialogBuilder(requireContext())
            .setView(dialogBinding.root)
        val dialog = builder.create()

        dialog.setCanceledOnTouchOutside(true)

        dialogBinding.btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialogBinding.btnExit.setOnClickListener {
            if (position < selectedImageUris.size) {
                val removedUri = selectedImageUris[position]
                selectedImageUris.removeAt(position)

                // 대표 사진 관리 로직
                if (removedUri == typicalImageUri && selectedImageUris.isNotEmpty()) {
                    typicalImageUri = selectedImageUris[0]
                } else if (selectedImageUris.isEmpty()) {
                    typicalImageUri = null
                }

                updatePhotoState()
            }

            dialog.dismiss()
        }

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        dialog.show()
    }

    // 어댑터는 내부 클래스로 유지하되, LayoutInflater 호출 시 context 주의
    inner class PhotoPagerAdapter(private val imageUris: List<Uri>) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        private val TYPE_PHOTO = 0
        private val TYPE_ADD = 1

        inner class PhotoViewHolder(val binding: ItemPhotoSlider2Binding) : RecyclerView.ViewHolder(binding.root)
        inner class AddViewHolder(val binding: ItemPhotoAddBinding) : RecyclerView.ViewHolder(binding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val inflater = LayoutInflater.from(parent.context)

            return if (viewType == TYPE_PHOTO) {
                PhotoViewHolder(ItemPhotoSlider2Binding.inflate(inflater, parent, false))
            } else {
                AddViewHolder(ItemPhotoAddBinding.inflate(inflater, parent, false))
            }
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            if (getItemViewType(position) == TYPE_PHOTO) {
                val photoHolder = holder as PhotoViewHolder
                val currentUri = imageUris[position]

                if (isEditMode) {
                    photoHolder.binding.ivDelete.visibility = View.VISIBLE
                } else {
                    photoHolder.binding.ivDelete.visibility = View.GONE
                }

                if (typicalImageUri == null && position == 0) typicalImageUri = currentUri
                val isTypical = currentUri == typicalImageUri
                photoHolder.binding.ivTypical.setImageResource(
                    if (isTypical) R.drawable.ic_typical_on else R.drawable.ic_typical_off
                )

                photoHolder.binding.ivPhoto.setImageURI(imageUris[position])

                photoHolder.binding.ivTypical.setOnClickListener {
                    typicalImageUri = currentUri
                    notifyDataSetChanged()
                }

                photoHolder.binding.ivDelete.setOnClickListener {
                    showExitDialog(holder.bindingAdapterPosition)
                }
            } else {
                val addHolder = holder as AddViewHolder
                addHolder.itemView.setOnClickListener {
                    launchPhotoPicker()
                }
            }
        }

        override fun getItemCount(): Int {
            return if (isEditMode) imageUris.size + 1 else imageUris.size
        }

        override fun getItemViewType(position: Int): Int {
            return if (isEditMode && position == imageUris.size) TYPE_ADD else TYPE_PHOTO
        }
    }
}