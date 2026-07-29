package com.umc.mobile.my4cut.ui.myalbum

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.umc.mobile.my4cut.databinding.DialogChangeBinding
import com.umc.mobile.my4cut.databinding.FragmentAlbumDetailBinding
import com.umc.mobile.my4cut.databinding.ItemAlbumAddBinding
import com.umc.mobile.my4cut.databinding.ItemAlbumDetailBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.umc.mobile.my4cut.R
import com.umc.mobile.my4cut.data.album.model.AlbumNameRequest
import com.umc.mobile.my4cut.data.album.model.AlbumRequest
import com.umc.mobile.my4cut.data.album.model.PhotoResponse
import com.umc.mobile.my4cut.network.RetrofitClient
import com.umc.mobile.my4cut.databinding.DialogExit2Binding
import com.umc.mobile.my4cut.ui.theme.loadWithSkeleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

class AlbumDetailFragment : Fragment() {
    private lateinit var binding: FragmentAlbumDetailBinding

    private var albumId: Int = -1
    private val photoList = mutableListOf<PhotoResponse>()
    private lateinit var detailAdapter: AlbumDetailAdapter

    private val MIN_SKELETON_DURATION_MS = 400L

    // 편집 모드 상태
    private var isEditMode = false
    private val pendingDeleteMediaIds = mutableSetOf<Int>()

    private val galleryPickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val viewUrls = result.data?.getStringArrayListExtra(
                GalleryPickerActivity.EXTRA_SELECTED_VIEW_URLS
            )
            if (!viewUrls.isNullOrEmpty()) {
                addSelectedPhotosToAlbum(viewUrls)
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentAlbumDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val albumTitle = arguments?.getString("ALBUM_TITLE") ?: "앨범 상세"
        albumId = arguments?.getInt("ALBUM_ID") ?: -1
        binding.tvTitle.text = albumTitle

        if (albumTitle == "ALL") {
            binding.btnEdit.visibility = View.GONE
            binding.btnDelete.visibility = View.GONE
        }

        binding.btnBack.setOnClickListener { parentFragmentManager.popBackStack() }

        // 수정 아이콘 → 다이얼로그 대신 편집 모드 진입
        binding.btnEdit.setOnClickListener { enterEditMode() }
        binding.btnDelete.setOnClickListener { showDeleteDialog() }

        binding.tvCancel.setOnClickListener { exitEditMode(discardChanges = true) }
        binding.tvSave.setOnClickListener { saveEditChanges() }

        // 편집 모드에서만 제목 클릭 시 이름 수정 모달
        binding.tvTitle.setOnClickListener {
            if (isEditMode) showChangeDialog()
        }

        setupRecyclerView()
        fetchAlbumDetail()
    }

    private fun enterEditMode() {
        isEditMode = true
        pendingDeleteMediaIds.clear()

        binding.btnEdit.visibility = View.GONE
        binding.btnDelete.visibility = View.GONE
        binding.tvCancel.visibility = View.VISIBLE
        binding.tvSave.visibility = View.VISIBLE
        binding.lineTitleDotted.visibility = View.VISIBLE
        binding.tvTitle.setTextColor(android.graphics.Color.parseColor("#AAAAAA"))

        detailAdapter.setEditMode(true)
    }

    private fun exitEditMode(discardChanges: Boolean) {
        isEditMode = false
        pendingDeleteMediaIds.clear()

        binding.btnEdit.visibility = View.VISIBLE
        binding.btnDelete.visibility = View.VISIBLE
        binding.tvCancel.visibility = View.GONE
        binding.tvSave.visibility = View.GONE
        binding.lineTitleDotted.visibility = View.GONE
        binding.tvTitle.setTextColor(android.graphics.Color.parseColor("#000000"))

        detailAdapter.setEditMode(false)

        // 취소 시 삭제 표시했던 항목 복구 위해 서버에서 다시 불러옴
        if (discardChanges) {
            fetchAlbumDetail()
        }
    }

    // 편집 모드에서 X 눌러 삭제 표시된 사진들을 실제 서버에 반영
    private fun saveEditChanges() {
        if (pendingDeleteMediaIds.isEmpty()) {
            exitEditMode(discardChanges = false)
            return
        }

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = RetrofitClient.albumService.deletePhotosFromAlbum(
                    albumId,
                    AlbumRequest(mediaIds = pendingDeleteMediaIds.toList())
                )

                // 서버가 삭제 후 최신 mediaList를 바로 반환해주므로 재조회 없이 그대로 반영
                updateUI(response.data?.mediaList)
                exitEditMode(discardChanges = false)
            } catch (e: Exception) {
                Log.e("API_ERROR", "사진 삭제 중 오류: ${e.message}")
            }
        }
    }

    // [GET] 앨범 상세 정보 조회
    private fun fetchAlbumDetail() {
        detailAdapter.showSkeleton()
        val startTime = android.os.SystemClock.elapsedRealtime()

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = RetrofitClient.albumService.getAlbumDetail(albumId)

                // 로딩이 너무 빨리 끝나면 스켈레톤이 한 프레임도 안 보이고 지나가버리므로
                // 최소 MIN_SKELETON_DURATION_MS만큼은 스켈레톤이 보이도록 지연 처리
                val elapsed = android.os.SystemClock.elapsedRealtime() - startTime
                val remaining = MIN_SKELETON_DURATION_MS - elapsed
                if (remaining > 0) delay(remaining)

                if (response.code == "A2003") { // 서버 응답 코드 확인
                    updateUI(response.data?.mediaList)
                } else {
                    detailAdapter.hideSkeleton()
                }
            } catch (e: Exception) {
                Log.e("API_ERROR", "상세 데이터 로드 실패: ${e.message}")
                detailAdapter.hideSkeleton()
            }
        }
    }

    // [POST] 선택한 Day4cut 사진을 다시 업로드하여 앨범에 추가 (Day4cut 조회 응답엔 mediaId가 없어 재업로드로 mediaId를 새로 발급받음)
    private fun addSelectedPhotosToAlbum(viewUrls: List<String>) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val multipartFiles = withContext(Dispatchers.IO) {
                    viewUrls.mapNotNull { url ->
                        downloadAndCompressImage(url)?.let { file ->
                            MultipartBody.Part.createFormData(
                                "files",
                                file.name,
                                file.asRequestBody("image/jpeg".toMediaTypeOrNull())
                            )
                        }
                    }
                }

                if (multipartFiles.isEmpty()) return@launch

                val uploadResponse = RetrofitClient.imageService.uploadImagesMedia(multipartFiles)
                if (uploadResponse.isSuccessful) {
                    val uploadedMediaIds = uploadResponse.body()?.data?.map { it.mediaId }

                    if (!uploadedMediaIds.isNullOrEmpty()) {
                        val addRes = RetrofitClient.albumService.addPhotosToAlbum(
                            albumId,
                            AlbumRequest(mediaIds = uploadedMediaIds)
                        )

                        if (addRes.code == "A2006") {
                            Log.d("ALBUM", "앨범에 사진 추가 성공!")
                            fetchAlbumDetail()
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("API_ERROR", "앨범에 사진 추가 중 오류: ${e.message}")
            }
        }
    }

    private fun downloadAndCompressImage(url: String): File? {
        return try {
            val client = OkHttpClient()
            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()
            val inputStream = response.body?.byteStream() ?: return null

            val originalBitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()

            if (originalBitmap == null) {
                Log.e("ALBUM", "❌ Failed to decode bitmap from URL: $url")
                return null
            }

            val resizedBitmap = resizeBitmap(originalBitmap, 1920)

            val outputStream = ByteArrayOutputStream()
            resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)

            val tempFile = File(requireContext().cacheDir, "day4cut_${System.currentTimeMillis()}.jpg")
            FileOutputStream(tempFile).use { fos ->
                fos.write(outputStream.toByteArray())
            }

            if (resizedBitmap != originalBitmap) {
                originalBitmap.recycle()
            }
            resizedBitmap.recycle()

            tempFile
        } catch (e: Exception) {
            Log.e("API_ERROR", "❌ 이미지 다운로드/압축 실패: ${e.message}")
            null
        }
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

    // 서버가 오래된 순으로 반환하므로 reversed()로 최신순(새로 추가된 게 앞) 정렬
    private fun updateUI(newList: List<PhotoResponse>?) {
        Log.d("ALBUM_DEBUG", "받아온 사진 개수: ${newList?.size ?: 0}")
        photoList.clear()
        newList?.let { photoList.addAll(it.reversed()) }
        detailAdapter.hideSkeleton()
    }

    private fun setupRecyclerView() {
        detailAdapter = AlbumDetailAdapter(
            photos = photoList,
            onAddClick = {
                val intent = Intent(requireContext(), GalleryPickerActivity::class.java).apply {
                    putStringArrayListExtra(
                        GalleryPickerActivity.EXTRA_EXISTING_VIEW_URLS,
                        ArrayList(photoList.map { it.viewUrl })
                    )
                }
                galleryPickerLauncher.launch(intent)
            },
            onPhotoClick = { viewUrl ->
                if (!isEditMode) showSimplePhotoModal(viewUrl)
            },
            onDeleteClick = { photo, position ->
                // TODO: PhotoResponse에 mediaId 필드명이 다르면 photo.mediaId 부분 수정 필요
                pendingDeleteMediaIds.add(photo.mediaId)
                photoList.removeAt(position)
                detailAdapter.notifyItemRemoved(position)
            }
        )

        binding.rvAlbums.adapter = detailAdapter
    }

    private fun showChangeDialog() {
        val dialogBinding = DialogChangeBinding.inflate(layoutInflater)
        val builder = MaterialAlertDialogBuilder(requireContext())
            .setView(dialogBinding.root)
        val dialog = builder.create()

        dialog.setCanceledOnTouchOutside(true)

        dialogBinding.tvTitle.text = "앨범 이름 수정"

        val oldTitle = binding.tvTitle.text.toString()
        dialogBinding.etSpaceName.setText(oldTitle)

        // 수정한 제목을 가져와 바꾸는 로직
        dialogBinding.btnNext.setOnClickListener {
            val newName = dialogBinding.etSpaceName.text.toString()
            if (newName.isNotEmpty()) {
                viewLifecycleOwner.lifecycleScope.launch {
                    try {
                        val response = RetrofitClient.albumService.updateAlbumName(
                            albumId,
                            AlbumNameRequest(newName)
                        )

                        if (response.isSuccessful) { // 수정 성공 코드 (명세 확인 필요)
                            binding.tvTitle.text = newName

                            parentFragmentManager.setFragmentResult("album_changed", Bundle())

                            dialog.dismiss()
                        }
                    } catch (e: Exception) {
                        Log.e("API_ERROR", "수정 실패: ${e.message}")
                    }
                }
            }
        }

        dialogBinding.ivClose.setOnClickListener { dialog.dismiss() }
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()
    }

    private fun showDeleteDialog() {
        val dialogBinding = DialogExit2Binding.inflate(layoutInflater)
        val builder = MaterialAlertDialogBuilder(requireContext())
            .setView(dialogBinding.root)
        val dialog = builder.create()

        dialog.setCanceledOnTouchOutside(true)

        dialogBinding.tvMessage.text = "삭제한 앨범은 다시 복구할 수 없어요."

        dialogBinding.btnExit.setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    val response = RetrofitClient.albumService.deleteAlbum(albumId)

                    if (response.isSuccessful) {
                        parentFragmentManager.setFragmentResult("album_changed", Bundle())

                        dialog.dismiss()
                        parentFragmentManager.popBackStack() // 이전 화면으로 이동
                    }
                } catch (e: Exception) {
                    Log.e("API_ERROR", "삭제 실패: ${e.message}")
                }
            }
        }

        dialogBinding.btnCancel.setOnClickListener { dialog.dismiss() }
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()
    }

    private fun showSimplePhotoModal(imageUrl: String) {
        val dialog = android.app.Dialog(requireContext())

        val dialogBinding = com.umc.mobile.my4cut.databinding.DialogPhotoDetailBinding.inflate(layoutInflater)
        dialog.setContentView(dialogBinding.root)

        dialogBinding.ivFullPhoto.load(imageUrl) { crossfade(true) }
        dialogBinding.btnClose.setOnClickListener { dialog.dismiss() }
        dialogBinding.root.setOnClickListener { dialog.dismiss() }
        dialogBinding.root.setBackgroundColor(android.graphics.Color.WHITE)

        dialog.window?.let { window ->
            val metrics = resources.displayMetrics
            val width = (metrics.widthPixels * 0.8).toInt()
            val height = (metrics.heightPixels * 0.6).toInt()

            window.setLayout(width, height)
        }

        dialog.show()
    }

    inner class AlbumDetailAdapter(  // 앨범 상세 프래그먼트 어댑터
        private val photos: MutableList<PhotoResponse>,
        private val onAddClick: () -> Unit,
        private val onPhotoClick: (String) -> Unit,
        private val onDeleteClick: (PhotoResponse, Int) -> Unit
    ) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        private val TYPE_PHOTO = 0
        private val TYPE_ADD = 1
        private val TYPE_SKELETON = 2

        private var editMode = false
        private var isLoading = false
        private val SKELETON_COUNT = 12

        fun setEditMode(enabled: Boolean) {
            editMode = enabled
            notifyDataSetChanged()
        }

        // 데이터 로딩 시작 전 호출 - 진짜 사진 대신 스켈레톤 아이템들을 먼저 보여줌
        fun showSkeleton() {
            isLoading = true
            notifyDataSetChanged()
        }

        // 데이터 로딩이 끝났을 때 호출 - 실제 photos 리스트 내용으로 전환
        fun hideSkeleton() {
            isLoading = false
            notifyDataSetChanged()
        }

        inner class PhotoViewHolder(val binding: ItemAlbumDetailBinding) : RecyclerView.ViewHolder(binding.root) {

            // 로딩 중일 때 표시할 스켈레톤 상태
            fun bindSkeleton() {
                itemView.isClickable = false
                itemView.setOnClickListener(null)

                binding.ivDeletePhoto.visibility = View.GONE
                binding.ivDeletePhoto.setOnClickListener(null)

                binding.cvAlbumCover.strokeColor = androidx.core.content.ContextCompat.getColor(
                    itemView.context, R.color.transparent
                )

                binding.ivAlbumCover.setImageResource(R.drawable.ic_skeleton_img)
                binding.ivAlbumCover.scaleType = android.widget.ImageView.ScaleType.CENTER
                binding.ivAlbumCover.setBackgroundResource(R.drawable.bg_skeleton_img)
            }
        }
        inner class AddViewHolder(val binding: ItemAlbumAddBinding) : RecyclerView.ViewHolder(binding.root)

        override fun getItemViewType(position: Int): Int {
            return when {
                isLoading -> TYPE_SKELETON
                position == photos.size -> TYPE_ADD
                else -> TYPE_PHOTO
            }
        }

        override fun getItemCount(): Int = if (isLoading) SKELETON_COUNT else photos.size + 1

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val inflater = LayoutInflater.from(parent.context)
            return if (viewType == TYPE_ADD) {
                val binding = ItemAlbumAddBinding.inflate(inflater, parent, false)
                AddViewHolder(binding)
            } else {
                // TYPE_PHOTO, TYPE_SKELETON 둘 다 같은 레이아웃 재사용
                val binding = ItemAlbumDetailBinding.inflate(inflater, parent, false)
                PhotoViewHolder(binding)
            }
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            if (holder is PhotoViewHolder) {
                if (isLoading) {
                    holder.bindSkeleton()
                    return
                }

                val photo = photos[position]

                holder.binding.ivAlbumCover.loadWithSkeleton(photo.viewUrl)

                // 편집 모드일 때만 회색 테두리 + 삭제 아이콘 노출
                holder.binding.cvAlbumCover.strokeColor = if (editMode) {
                    android.graphics.Color.parseColor("#999999")
                } else {
                    androidx.core.content.ContextCompat.getColor(holder.itemView.context, R.color.transparent)
                }
                holder.binding.ivDeletePhoto.visibility = if (editMode) View.VISIBLE else View.GONE

                holder.itemView.isClickable = true

                holder.binding.ivDeletePhoto.setOnClickListener {
                    val currentPos = holder.bindingAdapterPosition
                    if (currentPos != RecyclerView.NO_POSITION) {
                        onDeleteClick(photos[currentPos], currentPos)
                    }
                }

                holder.itemView.setOnClickListener {
                    if (!editMode) onPhotoClick(photo.viewUrl)
                }
            } else if (holder is AddViewHolder) {
                holder.itemView.setOnClickListener { onAddClick() }
            }
        }
    }
}