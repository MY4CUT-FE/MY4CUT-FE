package com.umc.mobile.my4cut.ui.myalbum

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.load.model.LazyHeaders
import com.umc.mobile.my4cut.databinding.DialogChangeBinding
import com.umc.mobile.my4cut.databinding.DialogExitBinding
import com.umc.mobile.my4cut.databinding.FragmentAlbumDetailBinding
import com.umc.mobile.my4cut.databinding.ItemAlbumAddBinding
import com.umc.mobile.my4cut.databinding.ItemAlbumDetailBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.umc.mobile.my4cut.R
import com.umc.mobile.my4cut.data.album.model.AlbumNameRequest
import com.umc.mobile.my4cut.data.album.model.AlbumRequest
import com.umc.mobile.my4cut.data.album.model.PhotoResponse
import com.umc.mobile.my4cut.data.auth.local.TokenManager
import com.umc.mobile.my4cut.network.RetrofitClient
import com.umc.mobile.my4cut.databinding.DialogExit2Binding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

class AlbumDetailFragment : Fragment() {
    private lateinit var binding: FragmentAlbumDetailBinding

    private var albumId: Int = -1
    private val photoList = mutableListOf<PhotoResponse>()
    private lateinit var detailAdapter: AlbumDetailAdapter

    private val pickMultipleMedia = registerForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(50)) { uris ->
        if (uris.isNotEmpty()) {
            uploadImagesAndAddToAlbum(uris)
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

        binding.btnEdit.setOnClickListener { showChangeDialog() }
        binding.btnDelete.setOnClickListener { showDeleteDialog() }

        setupRecyclerView()
        fetchAlbumDetail()
    }

    private fun prepareMultipartList(uris: List<Uri>): List<MultipartBody.Part> {
        val multipartList = mutableListOf<MultipartBody.Part>()
        val contentResolver = requireContext().contentResolver

        uris.forEach { uri ->
            // 1. Uri에서 파일을 읽어 임시 파일 생성
            val inputStream = contentResolver.openInputStream(uri)
            val file = File(requireContext().cacheDir, "upload_${System.currentTimeMillis()}.jpg")
            file.outputStream().use { inputStream?.copyTo(it) }

            // 2. RequestBody 생성 (MediaType은 이미지 타입에 맞게)
            val requestFile = file.asRequestBody(contentResolver.getType(uri)?.toMediaTypeOrNull())

            // 3. 서버 파라미터명인 "files"로 Part 생성
            val part = MultipartBody.Part.createFormData("files", file.name, requestFile)
            multipartList.add(part)
        }
        return multipartList
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

    // [POST] 이미지 업로드 및 앨범 등록 프로세스
    private fun uploadImagesAndAddToAlbum(uris: List<Uri>) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                // 1. Multipart 리스트 준비
                val multipartFiles = prepareMultipartList(uris)

                // 2. 미디어 업로드 API 호출
                val response = RetrofitClient.imageService.uploadImagesMedia(multipartFiles)

                if (response.isSuccessful) {
                    val baseResponse = response.body()

                    // 3. baseResponse.data는 List<UploadMediaData> 형태임
                    val uploadedMediaList = baseResponse?.data

                    if (!uploadedMediaList.isNullOrEmpty()) {
                        val uploadedMediaIds = uploadedMediaList.map { it.fileId }

                        // 3. 앨범 사진 추가 API 호출
                        val addRes = RetrofitClient.albumService.addPhotosToAlbum(
                            albumId,
                            AlbumRequest(mediaIds = uploadedMediaIds)
                        )

                        if (addRes.code == "A2006") {
                            Log.d("ALBUM", "미디어 업로드 및 앨범 추가 성공!")
                            fetchAlbumDetail()
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("API_ERROR", "업로드 중 오류: ${e.message}")
            }
        }
    }

    private fun updateUI(newList: List<PhotoResponse>?) {
        Log.d("ALBUM_DEBUG", "받아온 사진 개수: ${newList?.size ?: 0}")
        photoList.clear()
        newList?.let { photoList.addAll(it) }
        detailAdapter.notifyDataSetChanged()
    }

    private fun setupRecyclerView() {
        detailAdapter = AlbumDetailAdapter(photoList) {
            pickMultipleMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }

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

    inner class AlbumDetailAdapter(  // 앨범 상세 프래그먼트 어댑터
        private val photos: List<PhotoResponse>,
        private val onAddClick: () -> Unit
    ) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        private val TYPE_PHOTO = 0
        private val TYPE_ADD = 1

        inner class PhotoViewHolder(val binding: ItemAlbumDetailBinding) : RecyclerView.ViewHolder(binding.root)
        inner class AddViewHolder(val binding: ItemAlbumAddBinding) : RecyclerView.ViewHolder(binding.root)

        override fun getItemViewType(position: Int): Int {
            return if (position == photos.size) TYPE_ADD else TYPE_PHOTO
        }

        override fun getItemCount(): Int = photos.size + 1

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val inflater = LayoutInflater.from(parent.context)
            return if (viewType == TYPE_PHOTO) {
                val binding = ItemAlbumDetailBinding.inflate(inflater, parent, false)
                PhotoViewHolder(binding)
            } else {
                val binding = ItemAlbumAddBinding.inflate(inflater, parent, false)
                AddViewHolder(binding)
            }
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            if (holder is PhotoViewHolder) {
                val photo = photos[position]

                // 서버에서 받은 fileUrl을 사용하여 이미지 로드
                Glide.with(holder.binding.ivAlbumCover.context)
                    .load(photo.viewUrl)
                    .placeholder(R.color.gray_300)
                    .centerCrop()
                    .into(holder.binding.ivAlbumCover)
            } else if (holder is AddViewHolder) {
                holder.itemView.setOnClickListener { onAddClick() }
            }
        }
    }
}