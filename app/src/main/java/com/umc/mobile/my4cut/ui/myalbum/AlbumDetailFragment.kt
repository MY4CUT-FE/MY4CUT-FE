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
import com.umc.mobile.my4cut.data.album.model.AlbumRequest
import com.umc.mobile.my4cut.data.album.model.PhotoResponse
import com.umc.mobile.my4cut.data.auth.local.TokenManager
import com.umc.mobile.my4cut.data.network.RetrofitClient
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

        binding.btnBack.setOnClickListener {
//            val photoResIds = selectedImageUris.map { uri ->
//                uri.toString().toIntOrNull() ?: R.drawable.image1
//            }
//
//            val result = Bundle().apply {
//                putString("ALBUM_TITLE", binding.tvTitle.text.toString())
//                // Uri 리스트를 String 리스트로 변환하여 전달
//                putIntegerArrayList("SELECTED_IMAGES", ArrayList(photoResIds))
//            }
//
//            parentFragmentManager.setFragmentResult("album_complete", result)
            parentFragmentManager.popBackStack()
        }

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
                if (response.code == "A2003") {
                    updateUI(response.data?.photos)
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
                val uploadedMediaIds = mutableListOf<Int>()

                // 1. Presigned URL 발급 요청
                for (uri in uris) {
                    val urlRes = RetrofitClient.imageService.getUploadPresignedUrl(
                        PresignedUrlRequest(
                            type = "PROFILE",
                            fileName = "photo_${System.currentTimeMillis()}.jpg",
                            contentType = "image/jpeg"
                        )
                    )

                    // 2. 응답 데이터 안전하게 가져오기
                    val data = urlRes.data ?: continue
                    val uploadUrl = data.uploadUrl
                    val mediaId = data.mediaId

                    // 3. S3에 실제 이미지 바이트 전송 (PUT)
                    if (uploadToS3(uploadUrl, uri)) {
                        uploadedMediaIds.add(mediaId)
                        Log.d("ALBUM", "S3 업로드 성공: mediaId = $mediaId")
                    }
                }

                // 4. 앨범 사진 추가 API 호출
                if (uploadedMediaIds.isNotEmpty()) {
                    val addRes = RetrofitClient.albumService.addPhotosToAlbum(
                        albumId,
                        AlbumRequest(photoIds = uploadedMediaIds)
                    )

                    if (addRes.code == "A2006") {
                        Log.d("ALBUM", "앨범 등록 완료!")
                        // updateUI(addRes.data?.photos)

                        fetchAlbumDetail()
                    } else {
                        Log.e("ALBUM", "앨범 등록 실패: ${addRes.message}")
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

    private suspend fun uploadToS3(url: String, uri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            val inputStream = requireContext().contentResolver.openInputStream(uri)
            val requestBody = inputStream?.readBytes()?.toRequestBody("image/jpeg".toMediaTypeOrNull()) ?: return@withContext false
            val request = Request.Builder().url(url).put(requestBody).build()
            OkHttpClient().newCall(request).execute().isSuccessful
        } catch (e: Exception) { false }
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
                binding.tvTitle.text = newName

                val result = Bundle().apply {
                    putString("OLD_TITLE", oldTitle)
                    putString("NEW_TITLE", newName)
                }
                parentFragmentManager.setFragmentResult("album_update", result)

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
            val result = Bundle().apply {
                putString("DELETE_TITLE", binding.tvTitle.text.toString())
            }
            parentFragmentManager.setFragmentResult("album_delete", result)

            dialog.dismiss()
            parentFragmentManager.popBackStack()
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

                val cleanUrl = photo.fileUrl.substringBefore("?")

                val testS3Url = "https://my4cut-image-bucket.s3.ap-northeast-2.amazonaws.com/profile/a03d47f6-74de-4b52-88ae-761611a93cec_string?X-Amz-Security-Token=IQoJb3JpZ2luX2VjEJP%2F%2F%2F%2F%2F%2F%2F%2F%2F%2FwEaDmFwLW5vcnRoZWFzdC0yIkgwRgIhAJ2Sr3%2BvUjqnOvWqtJWvn4F6HLi2DJ9NS%2F45LN8oUVJdAiEAldOwjYiZODDlabsiVan235WfC6GKHZ%2F%2BNXxXqfgeP2YqywUIXBAAGgw5NjA5MDA4ODQ0NzQiDEqEqF2QJrOKG%2BmQIyqoBRsaZpiFAW%2BM01ScomWnh%2Ba0uxtRF5RqSqYYnO8mjfIS25EqXFTAlLoaxN%2B7zC9n0iQ1xMpkg58YsQCXTnPp5EUM1I2zVceRT7HEAsJV9G3iAt0J%2BUAw71Qtl%2Fk0V7eoXCgvcwO8uNX9SP4y7AcK8miPkdfWSmigYzjvghFIaqHDu5uKUPI%2FuoOK3a1GzvmacvrLKWFn2u%2FAkTQMrh6MLGLxg%2FJrSYLd%2BE3J5NyUycxyfPQCQXHazwDhUulV4Q8VOXr5E1M4MGL1OGcaeZieQjznpwKfphljDDHdoRMZa336FiB5Ww6z%2Bzoe5g1TCdm044Ydx7j1CJpC3VmPufUKDslILqETMPTqhugX330BzfPLXHNq7LfUOiIQCB%2BDY08TmmOfWnLyF2xmUZ0TCdpn%2FfqvnnbS%2F%2BTYOD0ebTB1sFWtup9Vk2MoVcrhq0ZsYsPdXOn0fh%2FqyB8u%2FBwSR3KaejzrrvxOiIMkyKtQ8g83deIUFKRdQMwN%2BkwabrA%2FqNGVi9tJMApqEFPz3m9eArcEgpVqYZYgytxM%2FBtDafs7LGJftCizu85MxuzSf%2FaGDzzubWRWAJ94N5erKvs2t0K6yPaiqxmzgseOoPMgnZaw9%2Bbs5cJechZ6yyIaRI2DL%2Fdbulmral%2BRKH2tlbqhFBh1eyaMs4vjqufbJUoJCj0PHa9jw%2Bs%2BPDR0jc5yFxaUKJUdDoF8MKJabIuJmy88Tnp6RvYJQ7C4BshnF4uN7aL4M0u%2BVipl%2BlgZ13c5fd8vz4Le61L4L41TH0x8Hfp3a%2Bdi41cKL002pecYO2qI3sTU5gmx1RxqXcTeZ2tXxPqr2pFy5yLpFAixUbeP4T3rWiua3LVLF3tA%2Fgm5VaARWOAxEsBoxDwVrfj6%2BMlXL3qMLE0Zq4Uq0Ka77LyxMK25nMwGOrABrrToR65%2FGlGbIcJU2VMUnAOYlM7OW61aZmads0yLIELSKO1xxtDoZ4PO%2Br5H90PhnYiAls5IsmncaGsuMS%2BV5rbvLrkOq0LSBlNT9kQucdYocI1NMyBsXvzYq%2F4CbJDtDMiS64EmWGo13TlY9%2Fft6GL5xeTLA6egR%2Fw6Hej247sXLNT%2FWHUFaADx2V0OUPktly4fX1L9AiGxRnjvW%2BMZWkAM1wdw6GyflyPEmtSuVBA%3D&X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Date=20260207T115819Z&X-Amz-SignedHeaders=host&X-Amz-Credential=ASIA57ORH475NK3MCPSZ%2F20260207%2Fap-northeast-2%2Fs3%2Faws4_request&X-Amz-Expires=300&X-Amz-Signature=14a02e39a06fc4949361b634e845bbd61935ecdbf1668106c14418345f20a38b"

                val test = "https://my4cut-image-bucket.s3.ap-northeast-2.amazonaws.com/"

                val BASE_URL = "https://cdn.my4cut.shop/"

                // 2. 만약 photo.fileUrl이 http로 시작하지 않는다면 도메인을 붙여줍니다.
                val finalUrl = if (photo.fileUrl.startsWith("http")) {
                    photo.fileUrl
                } else {
                    "${test}${photo.fileUrl}"
                }

                Log.d("ALBUM_DEBUG", "최종 로드 URL: $finalUrl")

                // 서버에서 받은 fileUrl을 사용하여 이미지 로드
                Glide.with(holder.binding.ivAlbumCover.context)
                    .load(finalUrl)
                    .placeholder(R.color.gray_300)
                    .centerCrop()
                    .into(holder.binding.ivAlbumCover)
            } else if (holder is AddViewHolder) {
                holder.itemView.setOnClickListener { onAddClick() }
            }
        }
    }
}