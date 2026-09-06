package com.umc.mobile.my4cut.ui.myalbum

import android.animation.ArgbEvaluator
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.exifinterface.media.ExifInterface
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.CompositePageTransformer
import androidx.viewpager2.widget.MarginPageTransformer
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.umc.mobile.my4cut.MainActivity
import com.umc.mobile.my4cut.R
import com.umc.mobile.my4cut.data.day4cut.remote.Day4CutImage
import com.umc.mobile.my4cut.data.day4cut.remote.UpdateDay4CutRequest
import com.umc.mobile.my4cut.data.network.RetrofitClient
import com.umc.mobile.my4cut.databinding.DialogExit2Binding
import com.umc.mobile.my4cut.databinding.FragmentEntryDetailBinding
import com.umc.mobile.my4cut.databinding.ItemPhotoAddBinding
import com.umc.mobile.my4cut.databinding.ItemPhotoSlider2Binding
import com.umc.mobile.my4cut.ui.theme.loadWithSkeleton
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

data class ImageItem(
    val uri: String,
    val isNew: Boolean
)

class EntryDetailFragment : Fragment() {
    private lateinit var binding: FragmentEntryDetailBinding

    private var apiDate: String? = null
    private var selectedDate: String? = null

    private var imageItems = mutableListOf<ImageItem>()
    private var isEditMode = false

    private var originalImageItems = mutableListOf<ImageItem>()
    private var originalContent: String = ""
    private var originalEmojiType: String? = null
    private var typicalImageIndex: Int = 0
    private var heightFixListener: ViewTreeObserver.OnGlobalLayoutListener? = null

    private val pickMultipleMedia = registerForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(3)
    ) { uris ->
        if (uris.isNotEmpty()) {
            val remaining = 3 - imageItems.size
            val toAdd = uris.take(remaining)
            toAdd.forEach { uri ->
                imageItems.add(ImageItem(
                    uri = uri.toString(),
                    isNew = true
                ))
            }
            if (uris.size > remaining) {
                Toast.makeText(requireContext(), "사진은 최대 3장까지 추가할 수 있어요.", Toast.LENGTH_SHORT).show()
            }
            updatePhotoState()
            binding.vpPhotoSlider.currentItem = imageItems.size - 1
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentEntryDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        apiDate = arguments?.getString("API_DATE")
        selectedDate = arguments?.getString("SELECTED_DATE") ?: "2026.01.01"
        binding.tvDateCapsule.text = selectedDate

        setEditMode(false)

        setupClickListeners()
        setupDiaryLogic()
        setupMoodSelection()
        setupKeyboardScroll()
        setupBackPressHandling()

        if (apiDate != null) {
            fetchDay4CutDetail()
        } else {
            hideLoadingSkeleton()
        }
    }

    private fun setupBackPressHandling() {
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    goBack()
                }
            }
        )
    }

    private fun goBack() {
        parentFragmentManager.popBackStack()
    }

    private fun setupKeyboardScroll() {
        binding.etDiary.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                binding.etDiary.post {
                    binding.nsvEntryDetail.smoothScrollTo(0, binding.clDiaryContent.bottom)
                }
            }
        }

        var isFixingHeight = false
        heightFixListener = ViewTreeObserver.OnGlobalLayoutListener {
            if (!isAdded) return@OnGlobalLayoutListener

            val nsv = binding.nsvEntryDetail
            val fcvMain = requireActivity().findViewById<View>(R.id.fcv_main)

            if (!isFixingHeight && fcvMain != null && fcvMain.height > 0) {
                val desiredHeight = fcvMain.height - nsv.top
                if (desiredHeight > 0 && desiredHeight != nsv.height) {
                    isFixingHeight = true
                    val params = nsv.layoutParams
                    params.height = desiredHeight
                    nsv.layoutParams = params
                    nsv.post { isFixingHeight = false }
                }
            }

            val child = nsv.getChildAt(0)
            if (child != null) {
                val maxScroll = (child.height - nsv.height).coerceAtLeast(0)
                if (nsv.scrollY > maxScroll) {
                    nsv.scrollTo(0, maxScroll)
                }
            }
        }
        binding.nsvEntryDetail.viewTreeObserver.addOnGlobalLayoutListener(heightFixListener)
    }

    override fun onDestroyView() {
        heightFixListener?.let {
            binding.nsvEntryDetail.viewTreeObserver.removeOnGlobalLayoutListener(it)
        }
        heightFixListener = null
        super.onDestroyView()
    }

    private fun fetchDay4CutDetail() {
        showLoadingSkeleton()

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = RetrofitClient.day4CutService.getDay4CutDetail(apiDate!!)

                if (response.code == "C2001") {
                    val data = response.data
                    if (data == null) {
                        hideLoadingSkeleton()
                        return@launch
                    }

                    binding.etDiary.setText(data.content ?: "")
                    originalContent = data.content ?: ""

                    originalEmojiType = data.emojiType
                    setEmojiByType(originalEmojiType)

                    imageItems.clear()
                    data.viewUrls?.forEach { url ->
                        imageItems.add(ImageItem(
                            uri = url,
                            isNew = false
                        ))
                    }

                    val dateObj = LocalDate.parse(apiDate)
                    val statusResponse = RetrofitClient.day4CutService.getCalendarStatus(
                        dateObj.year, dateObj.monthValue
                    )

                    if (statusResponse.code == "C2001") {
                        val dayStatus = statusResponse.data?.dates?.find { it.day == dateObj.dayOfMonth }
                        val serverThumbnailUrl = dayStatus?.thumbnailUrl

                        val foundIndex = data.viewUrls?.indexOf(serverThumbnailUrl) ?: 0
                        typicalImageIndex = if (foundIndex != -1) foundIndex else 0
                    }

                    revealAfterTypicalImageLoaded()
                } else {
                    hideLoadingSkeleton()
                }
            } catch (e: Exception) {
                hideLoadingSkeleton()
            }
        }
    }

    private fun revealAfterTypicalImageLoaded() {
        val typicalUrl = imageItems.getOrNull(typicalImageIndex)?.uri

        if (typicalUrl.isNullOrBlank()) {
            hideLoadingSkeleton()
            return
        }

        Glide.with(this)
            .load(typicalUrl)
            .listener(object : RequestListener<Drawable> {
                override fun onLoadFailed(
                    e: GlideException?,
                    model: Any?,
                    target: Target<Drawable>,
                    isFirstResource: Boolean
                ): Boolean {
                    if (isAdded) hideLoadingSkeleton()
                    return false
                }

                override fun onResourceReady(
                    resource: Drawable,
                    model: Any,
                    target: Target<Drawable>?,
                    dataSource: DataSource,
                    isFirstResource: Boolean
                ): Boolean {
                    if (isAdded) hideLoadingSkeleton()
                    return false
                }
            })
            .preload()
    }

    private fun showLoadingSkeleton() {
        binding.tvDateCapsule.text = ""
        binding.tvDateCapsule.setBackgroundResource(R.drawable.bg_skeleton_text_light)

        binding.clPhotoEmpty.visibility = View.GONE
        binding.vpPhotoSlider.visibility = View.GONE
        binding.clPhotoLoadingSkeleton.visibility = View.VISIBLE

        binding.clDiaryContent.visibility = View.GONE
        binding.clDiaryLoadingSkeleton.visibility = View.VISIBLE
    }

    private fun hideLoadingSkeleton() {
        binding.tvDateCapsule.text = selectedDate
        binding.tvDateCapsule.setBackgroundResource(R.drawable.bg_date_capsule2)

        binding.clPhotoLoadingSkeleton.visibility = View.GONE
        binding.clDiaryLoadingSkeleton.visibility = View.GONE
        binding.clDiaryContent.visibility = View.VISIBLE

        updatePhotoState()
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener {
            goBack()
        }

        binding.btnEdit.setOnClickListener {
            originalImageItems.clear()
            originalImageItems.addAll(imageItems.map { it.copy() })
            originalContent = binding.etDiary.text.toString()

            setEditMode(true)

            binding.vpPhotoSlider.setCurrentItem(imageItems.size, true)
        }

        binding.btnCancel.setOnClickListener {
            imageItems.clear()
            imageItems.addAll(originalImageItems.map { it.copy() })
            binding.etDiary.setText(originalContent)
            setEmojiByType(originalEmojiType)
            currentEmojiType = originalEmojiType

            setEditMode(false)
        }

        binding.btnComplete.setOnClickListener {
            if (imageItems.isEmpty()) {
                showDeleteConfirmDialog()
            } else {
                updateDay4Cut()
            }
        }
    }

    private fun setEditMode(isEditing: Boolean) {
        this.isEditMode = isEditing

        binding.btnEdit.visibility = if (isEditing) View.GONE else View.VISIBLE
        binding.btnCancel.visibility = if (isEditing) View.VISIBLE else View.GONE
        binding.btnComplete.visibility = if (isEditing) View.VISIBLE else View.GONE
        binding.tvTextCount.visibility = if (isEditing) View.VISIBLE else View.GONE

        binding.etDiary.isEnabled = isEditing
        binding.etDiary.isFocusable = isEditing
        binding.etDiary.isFocusableInTouchMode = isEditing

        binding.ivMoodDisplay.visibility = if (isEditing) View.GONE else View.VISIBLE
        binding.ivMoodEditIcon.visibility = if (isEditing) View.VISIBLE else View.GONE
        binding.llMoodContainer.visibility = if (isEditing) View.VISIBLE else View.GONE

        if (isEditing) {
            currentEmojiType = originalEmojiType
            updateMoodSelectionUI()
        }

        updatePhotoState()
    }

    private fun launchPhotoPicker() {
        if (imageItems.size >= 3) {
            Toast.makeText(requireContext(), "사진은 최대 3장까지 추가할 수 있어요.", Toast.LENGTH_SHORT).show()
            return
        }
        pickMultipleMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    private fun updatePhotoState() {
        if (imageItems.isNotEmpty() || isEditMode) {
            binding.clPhotoEmpty.visibility = View.GONE
            binding.vpPhotoSlider.visibility = View.VISIBLE
            binding.vpPhotoSlider.adapter = PhotoPagerAdapter(imageItems)

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
                    val scale = 0.85f + r * 0.15f
                    page.scaleX = scale
                    page.scaleY = scale

                    val photoCard = page.findViewById<MaterialCardView>(R.id.cv_photo_card)
                    val addCard = page.findViewById<MaterialCardView>(R.id.cv_add_card)
                    val targetCard = photoCard ?: addCard

                    if (targetCard != null) {
                        val colorFraction = abs(position).coerceIn(0f, 1f)
                        val color = argbEvaluator.evaluate(colorFraction, activeColor, inactiveColor) as Int
                        targetCard.strokeColor = color
                    }

                    val addIcon = page.findViewById<ImageView>(R.id.iv_add_icon)
                    if (addIcon != null && addCard != null && addCard.width > 0) {
                        val density = page.resources.displayMetrics.density
                        val peekCenterX = 20f * density
                        val cardCenterX = addCard.width / 2f
                        val clampedPosition = position.coerceIn(0f, 1f)
                        addIcon.translationX = clampedPosition * (peekCenterX - cardCenterX)
                    } else {
                        addIcon?.translationX = 0f
                    }
                }
                setPageTransformer(transform)
            }
        } else {
            binding.clPhotoEmpty.visibility = View.VISIBLE
            binding.vpPhotoSlider.visibility = View.GONE
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

    private fun setEmojiByType(type: String?) {
        val emojiRes = when (type) {
            "HAPPY" -> R.drawable.img_mood_happy
            "ANGRY" -> R.drawable.img_mood_angry
            "TIRED" -> R.drawable.img_mood_tired
            "SAD" -> R.drawable.img_mood_sad
            "CALM" -> R.drawable.img_mood_calm
            else -> null
        }
        if (emojiRes != null) {
            binding.ivMoodDisplay.setImageResource(emojiRes)
        } else {
            binding.ivMoodDisplay.setImageDrawable(null)
        }
    }

    private var currentEmojiType: String? = null
    private val moodOrder = listOf("CALM", "HAPPY", "TIRED", "ANGRY", "SAD")

    private fun moodSelectViews(): List<ImageView> = listOf(
        binding.ivMoodSelect1,
        binding.ivMoodSelect2,
        binding.ivMoodSelect3,
        binding.ivMoodSelect4,
        binding.ivMoodSelect5
    )

    private fun setupMoodSelection() {
        moodSelectViews().forEachIndexed { index, imageView ->
            imageView.setOnClickListener {
                val clickedType = moodOrder[index]
                currentEmojiType = if (currentEmojiType == clickedType) null else clickedType
                updateMoodSelectionUI()
            }
        }
    }

    private fun updateMoodSelectionUI() {
        val selectedIndex = moodOrder.indexOf(currentEmojiType)
        moodSelectViews().forEachIndexed { index, imageView ->
            if (index == selectedIndex) {
                imageView.setBackgroundResource(R.drawable.bg_mood_selected)
                imageView.alpha = 1.0f
            } else {
                imageView.background = null
                imageView.alpha = 0.4f
            }
        }
    }

    private fun getCurrentEmojiType(): String? {
        return if (isEditMode) currentEmojiType else originalEmojiType
    }

    private fun compressImage(uri: Uri): File? {
        return try {
            val inputStream = requireContext().contentResolver.openInputStream(uri) ?: return null
            val originalBitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()

            if (originalBitmap == null) return null

            val rotatedBitmap = rotateImageIfRequired(uri, originalBitmap)
            val resizedBitmap = resizeBitmap(rotatedBitmap, 1920)

            val outputStream = ByteArrayOutputStream()
            resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
            val compressedBytes = outputStream.toByteArray()

            val tempFile = File(requireContext().cacheDir, "compressed_${System.currentTimeMillis()}.jpg")
            FileOutputStream(tempFile).use { fos ->
                fos.write(compressedBytes)
            }

            if (rotatedBitmap != originalBitmap) {
                originalBitmap.recycle()
            }
            resizedBitmap.recycle()

            tempFile
        } catch (e: Exception) {
            null
        }
    }

    private suspend fun downloadAndCompressImage(url: String): File? {
        return withContext(Dispatchers.IO) {
            try {
                val connection = java.net.URL(url).openConnection()
                connection.connect()

                val inputStream = connection.getInputStream()
                val originalBitmap = BitmapFactory.decodeStream(inputStream)
                inputStream.close()

                if (originalBitmap == null) return@withContext null

                val resizedBitmap = resizeBitmap(originalBitmap, 1920)

                val outputStream = ByteArrayOutputStream()
                resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
                val compressedBytes = outputStream.toByteArray()

                val tempFile = File(requireContext().cacheDir, "downloaded_${System.currentTimeMillis()}.jpg")
                FileOutputStream(tempFile).use { fos ->
                    fos.write(compressedBytes)
                }

                originalBitmap.recycle()
                resizedBitmap.recycle()

                tempFile
            } catch (e: Exception) {
                null
            }
        }
    }

    private fun rotateImageIfRequired(uri: Uri, bitmap: Bitmap): Bitmap {
        return try {
            val inputStream = requireContext().contentResolver.openInputStream(uri) ?: return bitmap
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

    private fun updateDay4Cut() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val fileParts = mutableListOf<MultipartBody.Part>()

                for (item in imageItems) {
                    val compressedFile = if (item.isNew) {
                        val uri = Uri.parse(item.uri)
                        compressImage(uri)
                    } else {
                        downloadAndCompressImage(item.uri)
                    }

                    if (compressedFile == null) {
                        throw Exception("이미지 압축 실패")
                    }

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

                val uploadedFiles = uploadResponse.data ?: throw Exception("업로드 데이터 없음")

                val images = uploadedFiles.mapIndexed { index, file ->
                    Day4CutImage(
                        mediaId = file.mediaId,
                        isThumbnail = (index == typicalImageIndex)
                    )
                }

                val request = UpdateDay4CutRequest(
                    date = apiDate!!,
                    content = binding.etDiary.text.toString().ifBlank { null },
                    emojiType = getCurrentEmojiType(),
                    images = images
                )

                val updateResponse = withContext(Dispatchers.IO) {
                    RetrofitClient.day4CutService.updateDay4Cut(request)
                }

                if (updateResponse.code == "C2001") {
                    withContext(Dispatchers.Main) {
                        requireContext().cacheDir.listFiles()?.filter {
                            it.name.startsWith("compressed_")
                        }?.forEach { it.delete() }

                        originalImageItems.clear()
                        originalImageItems.addAll(imageItems.map { it.copy() })
                        originalContent = binding.etDiary.text.toString()
                        originalEmojiType = currentEmojiType
                        setEmojiByType(originalEmojiType)

                        setEditMode(false)
                    }
                } else {
                    throw Exception("수정 실패")
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    requireContext().cacheDir.listFiles()?.filter {
                        it.name.startsWith("compressed_")
                    }?.forEach { it.delete() }
                }
            }
        }
    }

    private fun showDeleteConfirmDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("기록 삭제")
            .setMessage("모든 사진을 삭제하면 이 날짜의 기록이 모두 삭제됩니다. 계속하시겠습니까?")
            .setPositiveButton("삭제") { _, _ ->
                deleteDay4Cut()
            }
            .setNegativeButton("취소", null)
            .show()
    }

    private fun deleteDay4Cut() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    RetrofitClient.day4CutService.deleteDay4Cut(apiDate!!)
                }

                if (response.code == "C2001") {
                    withContext(Dispatchers.Main) {
                        (requireActivity() as? MainActivity)?.changeFragment(CalendarMainFragment())
                    }
                } else {
                    throw Exception("삭제 실패")
                }
            } catch (e: Exception) {
            }
        }
    }

    private fun showPhotoDeleteDialog(position: Int) {
        val dialogBinding = DialogExit2Binding.inflate(layoutInflater)
        val builder = MaterialAlertDialogBuilder(requireContext())
            .setView(dialogBinding.root)
        val dialog = builder.create()

        dialog.setCanceledOnTouchOutside(true)

        dialogBinding.btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialogBinding.btnExit.setOnClickListener {
            if (position < imageItems.size) {
                imageItems.removeAt(position)

                if (position == typicalImageIndex) {
                    typicalImageIndex = 0
                } else if (position < typicalImageIndex) {
                    typicalImageIndex--
                }

                updatePhotoState()
            }

            dialog.dismiss()
        }

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()
    }

    inner class PhotoPagerAdapter(private val items: List<ImageItem>) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        private val TYPE_PHOTO = 0
        private val TYPE_ADD = 1
        private val MAX_PHOTO_COUNT = 3

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
                val item = items[position]

                if (isEditMode) {
                    photoHolder.binding.ivDelete.visibility = View.VISIBLE
                    photoHolder.binding.ivTypical.visibility = View.VISIBLE
                } else {
                    photoHolder.binding.ivDelete.visibility = View.GONE
                    photoHolder.binding.ivTypical.visibility = View.GONE
                }

                val isTypical = position == typicalImageIndex
                photoHolder.binding.ivTypical.setImageResource(
                    if (isTypical) R.drawable.ic_typical_on else R.drawable.ic_typical_off
                )

                photoHolder.binding.ivPhoto.loadWithSkeleton(item.uri)

                photoHolder.binding.ivTypical.setOnClickListener {
                    if (isEditMode) {
                        val oldIndex = typicalImageIndex
                        val newIndex = holder.bindingAdapterPosition

                        if (oldIndex != newIndex) {
                            typicalImageIndex = newIndex
                            notifyItemChanged(oldIndex)
                            notifyItemChanged(newIndex)
                        }
                    }
                }

                photoHolder.binding.ivDelete.setOnClickListener {
                    showPhotoDeleteDialog(holder.bindingAdapterPosition)
                }
            } else {
                val addHolder = holder as AddViewHolder
                addHolder.itemView.setOnClickListener {
                    launchPhotoPicker()
                }
            }
        }

        private fun hasAddPage(): Boolean = isEditMode && items.size < MAX_PHOTO_COUNT

        override fun getItemCount(): Int {
            return if (hasAddPage()) items.size + 1 else items.size
        }

        override fun getItemViewType(position: Int): Int {
            return if (hasAddPage() && position == items.size) TYPE_ADD else TYPE_PHOTO
        }
    }
}