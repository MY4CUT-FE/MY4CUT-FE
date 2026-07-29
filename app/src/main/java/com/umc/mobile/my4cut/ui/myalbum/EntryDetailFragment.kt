package com.umc.mobile.my4cut.ui.myalbum

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
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.umc.mobile.my4cut.MainActivity
import com.umc.mobile.my4cut.R
import com.umc.mobile.my4cut.data.day4cut.remote.Day4CutImage
import com.umc.mobile.my4cut.data.day4cut.remote.UpdateDay4CutRequest
import com.umc.mobile.my4cut.databinding.DialogExit2Binding
import com.umc.mobile.my4cut.databinding.FragmentEntryDetailBinding
import com.umc.mobile.my4cut.databinding.ItemPhotoAddBinding
import com.umc.mobile.my4cut.databinding.ItemPhotoSlider2Binding
import com.umc.mobile.my4cut.network.RetrofitClient
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

// ✅ ImageItem을 Fragment 외부로 이동
data class ImageItem(
    val uri: String,       // URI 또는 URL
    val isNew: Boolean     // 새로 추가된 이미지인지
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

        // ✅ 초기 상태: 읽기 모드
        setEditMode(false)

        setupClickListeners()
        setupDiaryLogic()
        setupKeyboardScroll()
        setupBackPressHandling()

        if (apiDate != null) {
            fetchDay4CutDetail()
        }
    }

    // 뒤로가기 화살표 클릭과 하드웨어/제스처 뒤로가기를 모두 여기서 처리 (둘 다 popBackStack으로 통일)
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

    // 일기 EditText에 포커스가 갈 때(키보드가 올라올 때) 해당 영역이 가려지지 않도록 아래로 스크롤
    private fun setupKeyboardScroll() {
        // 이 앱은 엣지투엣지 모드가 아니라 windowSoftInputMode="adjustResize"로 동작하므로
        // 창이 자동으로 리사이즈된다. 여기서는 리사이즈된 화면 안에서 다이어리 입력창이
        // 가려지지 않도록 스크롤 위치만 보정해준다.
        binding.etDiary.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                binding.etDiary.post {
                    binding.nsvEntryDetail.smoothScrollTo(0, binding.clDiaryContent.bottom)
                }
            }
        }

        // 타이핑 중 커서 위치 추적은 NestedScrollView의 기본 동작에 맡긴다.
        // (매 글자마다 강제로 맨 아래까지 스크롤시키면 커서가 위쪽에 있어도 화면 밖으로 밀려남)

        // fcv_main(부모)은 리사이즈됐는데 nsvEntryDetail(자식)이 이전 크기에 멈춰있는 경우가 있어서,
        // 어긋나면 실제 높이값을 직접 계산해서 강제로 덮어쓰고, 스크롤 위치도 새 범위로 당겨온다.
        var isFixingHeight = false
        heightFixListener = ViewTreeObserver.OnGlobalLayoutListener {
            // Fragment가 이미 화면에서 내려간(백스택 pop 등) 뒤에 콜백이 늦게 들어오는 경우 방어
            if (!isAdded) return@OnGlobalLayoutListener

            val nsv = binding.nsvEntryDetail
            val fcvMain = requireActivity().findViewById<View>(R.id.fcv_main)

            Log.d(
                "HeightDebug",
                "nsvEntryDetail height=${nsv.height}, top=${nsv.top}, scrollY=${nsv.scrollY}, " +
                        "childHeight=${nsv.getChildAt(0)?.height}, fcvMainHeight=${fcvMain?.height}"
            )

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
        // 뷰가 사라질 때 리스너를 확실히 제거해서 크래시/누수 방지
        heightFixListener?.let {
            binding.nsvEntryDetail.viewTreeObserver.removeOnGlobalLayoutListener(it)
        }
        heightFixListener = null
        super.onDestroyView()
    }

    private fun fetchDay4CutDetail() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                Log.d("EntryDetail", "📖 Fetching detail for date: $apiDate")

                val response = RetrofitClient.day4CutService.getDay4CutDetail(apiDate!!)

                Log.d("EntryDetail", "📨 Response: code=${response.code}, message=${response.message}")

                // ✅ isSuccess 제거, code 체크만 사용
                if (response.code == "C2001") {
                    response.data?.let { data ->
                        Log.d("EntryDetail", "✅ Data loaded:")
                        Log.d("EntryDetail", "   ├─ content: ${data.content}")
                        Log.d("EntryDetail", "   ├─ emojiType: ${data.emojiType}")
                        Log.d("EntryDetail", "   └─ images: ${data.viewUrls?.size ?: 0}")

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
                            // 해당 날짜의 데이터를 찾음
                            val dayStatus = statusResponse.data?.dates?.find { it.day == dateObj.dayOfMonth }
                            val serverThumbnailUrl = dayStatus?.thumbnailUrl

                            // 리스트 중 서버 썸네일 URL과 일치하는 인덱스 찾기
                            val foundIndex = data.viewUrls?.indexOf(serverThumbnailUrl) ?: 0
                            typicalImageIndex = if (foundIndex != -1) foundIndex else 0

                            updatePhotoState()
                        }
                    }
                } else {
                    Log.e("EntryDetail", "❌ Failed to load: ${response.code} - ${response.message}")
                    Toast.makeText(requireContext(), "데이터를 불러올 수 없습니다", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("EntryDetail", "💥 Failed to fetch detail", e)
                Toast.makeText(requireContext(), "조회 실패: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
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

        // ✅ EditText 활성화/비활성화
        binding.etDiary.isEnabled = isEditing
        binding.etDiary.isFocusable = isEditing
        binding.etDiary.isFocusableInTouchMode = isEditing

        // 편집 모드에 따라 어댑터 재생성 (추가 버튼 노출 여부 때문)
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
                    page.scaleY = 0.85f + r * 0.15f

                    val photoCard = page.findViewById<MaterialCardView>(R.id.cv_photo_card)
                    val addCard = page.findViewById<MaterialCardView>(R.id.cv_add_card)
                    val targetCard = photoCard ?: addCard

                    if (targetCard != null) {
                        val colorFraction = abs(position).coerceIn(0f, 1f)
                        val color = argbEvaluator.evaluate(colorFraction, activeColor, inactiveColor) as Int
                        targetCard.strokeColor = color
                    }

                    // position=0(완전히 현재 페이지) → 카드 정중앙
                    // position=1(오른쪽에 40dp만 peek로 보이는 상태) → peek 영역(0~40dp)의 중앙(20dp 지점)
                    val addIcon = page.findViewById<ImageView>(R.id.iv_add_icon)
                    if (addIcon != null && addCard != null && addCard.width > 0) {
                        val density = page.resources.displayMetrics.density
                        val peekCenterX = 20f * density // ViewPager2 paddingHorizontal(40dp)의 절반
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
            binding.ivMood1.setImageResource(emojiRes)
        } else {
            binding.ivMood1.setImageDrawable(null)
        }
    }

    private fun getCurrentEmojiType(): String? {
        return originalEmojiType
    }

    private fun compressImage(uri: Uri): File? {
        return try {
            val inputStream = requireContext().contentResolver.openInputStream(uri) ?: return null

            val originalBitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()

            if (originalBitmap == null) {
                Log.e("EntryDetail", "❌ Failed to decode bitmap from URI: $uri")
                return null
            }

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

            Log.d("EntryDetail", "✅ Image compressed: ${tempFile.length() / 1024}KB")

            tempFile
        } catch (e: Exception) {
            Log.e("EntryDetail", "❌ Image compression failed", e)
            null
        }
    }

    /**
     * ✅ 서버 URL에서 이미지 다운로드 후 압축
     */
    private suspend fun downloadAndCompressImage(url: String): File? {
        return withContext(Dispatchers.IO) {
            try {
                val connection = java.net.URL(url).openConnection()
                connection.connect()

                val inputStream = connection.getInputStream()
                val originalBitmap = BitmapFactory.decodeStream(inputStream)
                inputStream.close()

                if (originalBitmap == null) {
                    Log.e("EntryDetail", "❌ Failed to decode bitmap from URL: $url")
                    return@withContext null
                }

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

                Log.d("EntryDetail", "✅ Image downloaded and compressed: ${tempFile.length() / 1024}KB")

                tempFile
            } catch (e: Exception) {
                Log.e("EntryDetail", "❌ Image download failed", e)
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
            Log.e("EntryDetail", "Failed to read EXIF", e)
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
                Log.d("EntryDetail", "")
                Log.d("EntryDetail", "🔄 UPDATE PROCESS STARTED")
                Log.d("EntryDetail", "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

                val fileParts = mutableListOf<MultipartBody.Part>()

                for ((index, item) in imageItems.withIndex()) {
                    Log.d("EntryDetail", "📤 Processing image ${index + 1}/${imageItems.size}")

                    val compressedFile = if (item.isNew) {
                        // ✅ 새 이미지: URI에서 압축
                        Log.d("EntryDetail", "   ├─ New image (local URI)")
                        val uri = Uri.parse(item.uri)
                        compressImage(uri)
                    } else {
                        // ✅ 기존 이미지: URL에서 다운로드 후 압축
                        Log.d("EntryDetail", "   ├─ Existing image (server URL)")
                        downloadAndCompressImage(item.uri)
                    }

                    if (compressedFile == null) {
                        Log.e("EntryDetail", "❌ Image ${index + 1} compression failed")
                        throw Exception("이미지 ${index + 1} 압축 실패")
                    }

                    val requestBody = compressedFile.asRequestBody("image/jpeg".toMediaTypeOrNull())
                    val part = MultipartBody.Part.createFormData("files", compressedFile.name, requestBody)
                    fileParts.add(part)
                }

                Log.d("EntryDetail", "📤 Uploading ${fileParts.size} images via /media/upload/bulk")

                val uploadResponse = withContext(Dispatchers.IO) {
                    RetrofitClient.mediaService.uploadMediaBulk(fileParts)
                }

                Log.d("EntryDetail", "📨 Upload response: code=${uploadResponse.code}")

                // ✅ C2001 또는 C2011 모두 성공
                if (uploadResponse.code != "C2001" && uploadResponse.code != "C2011") {
                    throw Exception("이미지 업로드 실패: ${uploadResponse.message}")
                }

                val uploadedFiles = uploadResponse.data ?: throw Exception("업로드 응답 데이터 없음")

                val images = uploadedFiles.mapIndexed { index, file ->
                    Day4CutImage(
                        mediaId = file.mediaId,
                        isThumbnail = (index == typicalImageIndex)
                    )
                }

                Log.d("EntryDetail", "📊 Uploaded fileIds: ${uploadedFiles.map { it.mediaId }}")

                val request = UpdateDay4CutRequest(
                    date = apiDate!!,
                    content = binding.etDiary.text.toString().ifBlank { null },
                    emojiType = getCurrentEmojiType(),
                    images = images
                )

                Log.d("EntryDetail", "📝 Updating Day4Cut...")
                Log.d("EntryDetail", "Request: $request")

                val updateResponse = withContext(Dispatchers.IO) {
                    RetrofitClient.day4CutService.updateDay4Cut(request)
                }

                Log.d("EntryDetail", "📨 Update response: code=${updateResponse.code}")

                // ✅ isSuccess 제거
                if (updateResponse.code == "C2001") {
                    Log.d("EntryDetail", "")
                    Log.d("EntryDetail", "🎉 DAY4CUT UPDATED SUCCESSFULLY")
                    Log.d("EntryDetail", "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

                    withContext(Dispatchers.Main) {
                        requireContext().cacheDir.listFiles()?.filter {
                            it.name.startsWith("compressed_")
                        }?.forEach { it.delete() }

                        Toast.makeText(requireContext(), "수정되었습니다!", Toast.LENGTH_SHORT).show()

                        originalImageItems.clear()
                        originalImageItems.addAll(imageItems.map { it.copy() })
                        originalContent = binding.etDiary.text.toString()

                        setEditMode(false)
                    }
                } else {
                    throw Exception("수정 실패: ${updateResponse.message}")
                }

            } catch (e: Exception) {
                Log.e("EntryDetail", "💥 UPDATE FAILED", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "수정 실패: ${e.message}", Toast.LENGTH_LONG).show()

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
                Log.d("EntryDetail", "🗑️ Deleting Day4Cut for date: $apiDate")

                val response = withContext(Dispatchers.IO) {
                    RetrofitClient.day4CutService.deleteDay4Cut(apiDate!!)
                }

                Log.d("EntryDetail", "📨 Delete response: code=${response.code}")

                // ✅ isSuccess 제거
                if (response.code == "C2001") {
                    Log.d("EntryDetail", "✅ Day4Cut deleted successfully")

                    withContext(Dispatchers.Main) {
                        Toast.makeText(requireContext(), "기록이 삭제되었습니다", Toast.LENGTH_SHORT).show()
                        (requireActivity() as? MainActivity)?.changeFragment(CalendarChildFragment())
                    }
                } else {
                    throw Exception("삭제 실패: ${response.message}")
                }
            } catch (e: Exception) {
                Log.e("EntryDetail", "💥 DELETE FAILED", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "삭제 실패: ${e.message}", Toast.LENGTH_LONG).show()
                }
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

                // ✅ 편집 모드일 때만 삭제/썸네일 버튼 표시
                if (isEditMode) {
                    photoHolder.binding.ivDelete.visibility = View.VISIBLE
                    photoHolder.binding.ivTypical.visibility = View.VISIBLE
                } else {
                    photoHolder.binding.ivDelete.visibility = View.GONE
                    photoHolder.binding.ivTypical.visibility = View.GONE  // ✅ 읽기 모드에서 숨김
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
                            // 전체를 갱신하지 말고, 이전 대표와 현재 대표 사진의 아이콘만 갱신
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

        override fun getItemCount(): Int {
            return if (isEditMode) items.size + 1 else items.size
        }

        override fun getItemViewType(position: Int): Int {
            return if (isEditMode && position == items.size) TYPE_ADD else TYPE_PHOTO
        }
    }
}