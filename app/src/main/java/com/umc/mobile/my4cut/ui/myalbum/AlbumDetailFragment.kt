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
import com.bumptech.glide.Glide
import com.umc.mobile.my4cut.databinding.DialogChangeBinding
import com.umc.mobile.my4cut.databinding.FragmentAlbumDetailBinding
import com.umc.mobile.my4cut.databinding.ItemAlbumAddBinding
import com.umc.mobile.my4cut.databinding.ItemAlbumDetailBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.umc.mobile.my4cut.R
import com.umc.mobile.my4cut.data.album.model.AlbumNameRequest
import com.umc.mobile.my4cut.data.album.model.AlbumRequest
import com.umc.mobile.my4cut.data.album.model.PhotoResponse
import com.umc.mobile.my4cut.data.network.RetrofitClient
import com.umc.mobile.my4cut.databinding.DialogExit2Binding
import com.umc.mobile.my4cut.ui.theme.loadWithSkeleton
import kotlinx.coroutines.Dispatchers
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

    // 편집 모드 상태
    private var isEditMode = false
    private val pendingDeleteMediaIds = mutableSetOf<Int>()
    private var pendingAlbumName: String? = null
    private var originalAlbumTitle: String = ""

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
        pendingAlbumName = null
        originalAlbumTitle = binding.tvTitle.text.toString()

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

        // 취소 시 삭제 표시했던 항목 + 변경했던 제목 모두 원래대로 복구
        if (discardChanges) {
            binding.tvTitle.text = originalAlbumTitle
            pendingAlbumName = null
            fetchAlbumDetail()
        }
    }

    // 편집 모드에서 저장 눌렀을 때 대기 중이던 변경사항(사진 삭제, 이름 변경)을 실제 서버에 반영
    private fun saveEditChanges() {
        if (pendingDeleteMediaIds.isEmpty() && pendingAlbumName == null) {
            exitEditMode(discardChanges = false)
            return
        }

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                pendingAlbumName?.let { newName ->
                    val response = RetrofitClient.albumService.updateAlbumName(
                        albumId,
                        AlbumNameRequest(newName)
                    )
                    if (response.isSuccessful) {
                        parentFragmentManager.setFragmentResult("album_changed", Bundle())
                    } else {
                        Log.e("API_ERROR", "이름 변경 실패: ${response.code()}")
                    }
                }

                if (pendingDeleteMediaIds.isNotEmpty()) {
                    RetrofitClient.albumService.deletePhotosFromAlbum(
                        albumId,
                        AlbumRequest(mediaIds = pendingDeleteMediaIds.toList())
                    )
                }

                // 응답에 담긴 데이터를 그대로 쓰지 않고, 성공 후 목록을 다시 새로 조회해서 반영
                pendingDeleteMediaIds.clear()
                pendingAlbumName = null
                exitEditMode(discardChanges = false)
                fetchAlbumDetail()
            } catch (e: Exception) {
                Log.e("API_ERROR", "저장 중 오류: ${e.message}")
            }
        }
    }

    // [GET] 앨범 상세 정보 조회
    private fun fetchAlbumDetail() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = RetrofitClient.albumService.getAlbumDetail(albumId)
                if (response.code == "A2003") { // 서버 응답 코드 확인
                    updateUI(response.data?.mediaList)
                }
            } catch (e: Exception) {
                Log.e("API_ERROR", "상세 데이터 로드 실패: ${e.message}")
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
        detailAdapter.notifyDataSetChanged()
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

        // 수정한 제목을 로컬(대기 상태)로만 반영. 실제 서버 저장은 편집 화면의 "저장" 버튼에서 처리
        dialogBinding.btnNext.setOnClickListener {
            val newName = dialogBinding.etSpaceName.text.toString()
            if (newName.isNotEmpty()) {
                pendingAlbumName = newName
                binding.tvTitle.text = newName
                dialog.dismiss()
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
        // 기본 흰 배경 테마로 만들어졌다가 나중에 투명하게 바꾸면 그 사이 한 프레임 깜빡이므로,
        // 처음부터 투명 테마로 Dialog를 생성함
        val dialog = android.app.Dialog(requireContext(), android.R.style.Theme_Translucent_NoTitleBar)

        val dialogBinding = com.umc.mobile.my4cut.databinding.DialogPhotoDetailBinding.inflate(layoutInflater)
        dialog.setContentView(dialogBinding.root)

        Glide.with(dialogBinding.ivFullPhoto)
            .load(imageUrl)
            .placeholder(R.drawable.ic_skeleton_img)
            .into(dialogBinding.ivFullPhoto)
        dialogBinding.btnClose.setOnClickListener { dialog.dismiss() }

        // 배경(다크 스크림) 눌렀을 때만 닫히게, 카드 자체를 눌렀을 때는 안 닫히도록
        dialogBinding.root.setOnClickListener { dialog.dismiss() }
        dialogBinding.cvPhotoCard.setOnClickListener { /* 카드 클릭은 전파 막기용, 아무 동작 없음 */ }

        // 다크 스크림이 화면 전체를 채우도록 전체화면 크기로 설정 (카드는 XML 안에서 중앙 정렬됨)
        dialog.window?.let { window ->
            window.setBackgroundDrawableResource(android.R.color.transparent)
            window.setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
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

        private var editMode = false

        fun setEditMode(enabled: Boolean) {
            editMode = enabled
            notifyDataSetChanged()
        }

        inner class PhotoViewHolder(val binding: ItemAlbumDetailBinding) : RecyclerView.ViewHolder(binding.root)
        inner class AddViewHolder(val binding: ItemAlbumAddBinding) : RecyclerView.ViewHolder(binding.root)

        override fun getItemViewType(position: Int): Int {
            return if (position == photos.size) TYPE_ADD else TYPE_PHOTO
        }

        override fun getItemCount(): Int = photos.size + 1

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val inflater = LayoutInflater.from(parent.context)
            return if (viewType == TYPE_ADD) {
                val binding = ItemAlbumAddBinding.inflate(inflater, parent, false)
                AddViewHolder(binding)
            } else {
                val binding = ItemAlbumDetailBinding.inflate(inflater, parent, false)
                PhotoViewHolder(binding)
            }
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            if (holder is PhotoViewHolder) {
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