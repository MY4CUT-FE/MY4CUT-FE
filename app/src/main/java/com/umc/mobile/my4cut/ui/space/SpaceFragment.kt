package com.umc.mobile.my4cut.ui.space

import com.umc.mobile.my4cut.data.photo.model.WorkspacePhotoUploadRequestDto

import com.umc.mobile.my4cut.data.photo.remote.WorkspacePhotoService
import com.umc.mobile.my4cut.data.workspace.remote.WorkspaceMemberService

import android.net.Uri
import androidx.activity.result.contract.ActivityResultContracts
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import com.umc.mobile.my4cut.ui.photo.PhotoData
import com.umc.mobile.my4cut.ui.photo.PhotoRVAdapter
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.umc.mobile.my4cut.R
import com.umc.mobile.my4cut.databinding.DialogExitBinding
import com.umc.mobile.my4cut.databinding.FragmentSpaceBinding

import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import android.util.Log
import com.umc.mobile.my4cut.data.base.BaseResponse
import com.umc.mobile.my4cut.data.user.model.UserMeResponse
import com.umc.mobile.my4cut.network.RetrofitClient
import com.umc.mobile.my4cut.ui.photo.PhotoDialogFragment
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

class SpaceFragment : Fragment(R.layout.fragment_space) {

    private lateinit var binding: FragmentSpaceBinding
    private lateinit var photoAdapter: PhotoRVAdapter
    private var photoDatas = ArrayList<PhotoData>()

    private var spaceId: Long = -1L
    private var isOwner: Boolean = false
    private var myUserId: Long = -1L
    private var myNickname: String = ""

    private val workspacePhotoService: WorkspacePhotoService by lazy {
        RetrofitClient.workspacePhotoService
    }

    private val workspaceMemberService: WorkspaceMemberService by lazy {
        RetrofitClient.workspaceMemberService
    }

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            uploadImageToServer(it)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        spaceId = arguments?.getLong(ARG_SPACE_ID) ?: -1L
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentSpaceBinding.bind(view)

        photoAdapter = PhotoRVAdapter(photoDatas)
        binding.rvPhotoList.adapter = photoAdapter
        binding.rvPhotoList.layoutManager = GridLayoutManager(requireContext(), 2)

        // initDummyPhotos() // 더미 데이터 제거, 실제 API로 대체
        loadSpaceFromApi()
        loadPhotosFromApi()

        photoAdapter.onItemClickListener = { photo ->
            showPhotoDialog(photo, isCommentExpanded = true)
        }

        binding.btnExitMenu.setOnClickListener {
            showExitDialog()  //혼자일 때 -> tvMessage.text = 나가면 스페이스가 삭제되어 복구할 수 없어요.
        }

        binding.btnChange.setOnClickListener {
            showChangeDialog(spaceId)
        }

        binding.btnUpload.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        // 뒤로가기 버튼: 이전(리터치 스페이스) 화면으로 돌아가기
        binding.back.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    private fun loadSpaceFromApi() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = RetrofitClient.workspaceService.getWorkspaceDetail(spaceId)
                val data = response.data ?: return@launch

                // 스페이스 정보 UI 반영
                binding.tvTitle.text = data.name

                // 현재 로그인 사용자 정보 조회 → 방장 여부 판단
                RetrofitClient.userService.getMyPage().enqueue(object :
                    Callback<BaseResponse<UserMeResponse>> {

                    override fun onResponse(
                        call: Call<BaseResponse<UserMeResponse>>,
                        response: Response<BaseResponse<UserMeResponse>>
                    ) {
                        val body = response.body()
                        myUserId = body?.data?.userId?.toLong() ?: -1L
                        myNickname = body?.data?.nickname ?: ""
                        isOwner = (data.ownerId?.toLong() == myUserId)

                        Log.d("SpaceFragment", "isOwner=$isOwner ownerId=${data.ownerId} myUserId=$myUserId")
                    }

                    override fun onFailure(
                        call: Call<BaseResponse<UserMeResponse>>,
                        t: Throwable
                    ) {
                        Log.e("SpaceFragment", "내 정보 조회 실패", t)
                    }
                })

                // TODO: 사진 / 댓글 API 결과로 교체
            } catch (e: Exception) {
                Log.e("SpaceFragment", "스페이스 정보 API 실패", e)
            }
        }
    }

    private fun formatDateTime(dateTime: String?): String {
        if (dateTime.isNullOrEmpty()) return ""
        return try {
            // ISO 문자열에서 밀리초 등 불필요한 부분 제거 (예: 2026-02-11T07:03:00.306203 → 2026-02-11T07:03:00)
            val cleaned = dateTime.substringBeforeLast(".")
            val parsed = OffsetDateTime.parse(cleaned)
            val formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm", Locale.getDefault())
            parsed.format(formatter)
        } catch (e: Exception) {
            // 그래도 실패하면 T 기준으로 잘라서 최소한 날짜/시간만 표시
            dateTime.substringBefore(".").replace("T", " ")
        }
    }

    private fun loadPhotosFromApi() {
        lifecycleScope.launch {
            try {
                val response = workspacePhotoService.getPhotos(spaceId)

                val list = response.data ?: emptyList()
                Log.d("PHOTO_DEBUG", "서버에서 받은 사진 개수 = ${list.size}")

                photoDatas.clear()

                photoDatas.addAll(
                    list.map {
                        val photoId = it.mediaId ?: 0L

                        // 댓글 개수 조회 (기본값 0)
                        var commentCount = 0
                        try {
                            val commentResponse =
                                workspacePhotoService.getComments(spaceId, photoId)
                            commentCount = commentResponse.data?.size ?: 0
                        } catch (e: Exception) {
                            Log.e("PHOTO_DEBUG", "댓글 개수 조회 실패 photoId=$photoId", e)
                        }

                        PhotoData(
                            photoId = photoId,
                            userName = it.uploaderNickname ?: "",
                            userProfileUrl = null,
                            photoUrl = it.viewUrl ?: "",
                            dateTime = formatDateTime(it.createdAt),
                            commentCount = commentCount,
                            uploaderId = if (it.uploaderNickname == myNickname) myUserId else null
                        )
                    }
                )

                photoAdapter.notifyDataSetChanged()

            } catch (e: Exception) {
                Log.e("SpaceFragment", "사진 목록 API 실패", e)
            }
        }
    }

    private fun showExitDialog() {
        val dialogBinding = DialogExitBinding.inflate(layoutInflater)

        // 방장 여부에 따라 문구 변경
        if (isOwner) {
            dialogBinding.tvTitle.text = "정말 나가시겠어요?"
            dialogBinding.tvMessage.text = "나가면 스페이스가 삭제되어 복구할 수 없어요."
            dialogBinding.btnExit.text = "나가기"
        } else {
            dialogBinding.tvTitle.text = "정말 나가시겠어요?"
            dialogBinding.tvMessage.text = "다시 초대받기 전까지 스페이스를 이용할 수 없어요."
            dialogBinding.btnExit.text = "나가기"
        }

        val builder = MaterialAlertDialogBuilder(requireContext())
            .setView(dialogBinding.root)
        val dialog = builder.create()

        dialog.setCanceledOnTouchOutside(true)

        dialogBinding.btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialogBinding.btnExit.setOnClickListener {
            lifecycleScope.launch {
                try {
                    if (isOwner) {
                        // 방장 → 스페이스 삭제 API
                        RetrofitClient.workspaceService.deleteWorkspace(spaceId)
                    } else {
                        // 일반 멤버 → 나가기 API
                        workspaceMemberService.leaveWorkspace(spaceId)
                    }

                    dialog.dismiss()

                    // 이전 화면(리터치 스페이스)으로 돌아가기
                    if (isAdded) {
                        parentFragmentManager.popBackStack()
                    }

                } catch (e: Exception) {
                    Log.e("SpaceFragment", "나가기/삭제 API 실패", e)
                }
            }
        }

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        dialog.show()
    }

    private fun showPhotoDialog(
        photo: PhotoData,
        isCommentExpanded: Boolean = true
    ) {
        val dialog = PhotoDialogFragment.newInstance(
            workspaceId = spaceId,
            photoId = photo.photoId,
            photo.photoUrl ?: "",
            uploaderId = photo.uploaderId,
            uploaderNickname = photo.userName,
            uploaderProfileUrl = photo.userProfileUrl,
            createdAt = photo.dateTime,
            myUserId = myUserId
        )
        dialog.show(parentFragmentManager, "PhotoDialog")
    }


    private fun showChangeDialog(spaceId: Long) {
        Log.d("SpaceFragment", "수정할 스페이스 ID: $spaceId")

        val dialog = EditSpaceDialogFragment.newInstance(
            spaceId = spaceId,
            spaceName = binding.tvTitle.text.toString(),
            memberIds = emptyList()
        )

        // 수정 완료 후 자동 갱신
        dialog.setOnEditCompleteListener {
            loadSpaceFromApi()   // 제목 다시 불러오기
        }

        dialog.show(parentFragmentManager, "EditSpaceDialog")
    }


    private fun showMaxSpaceDialog() {
        val dialogBinding = DialogExitBinding.inflate(layoutInflater)

        dialogBinding.tvTitle.text = "스페이스를 더 만들 수 없어요"
        dialogBinding.tvMessage.text = "스페이스는 최대 4개까지 만들 수 있어요."
        dialogBinding.btnExit.text = "확인"

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(dialogBinding.root)
            .create()

        dialogBinding.btnCancel.visibility = View.GONE

        dialogBinding.btnExit.setOnClickListener {
            dialog.dismiss()
        }

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()
    }

    private fun uploadImageToServer(uri: Uri) {
        lifecycleScope.launch {
            try {
                // URI → File 변환 (간단한 방식)
                val inputStream = requireContext().contentResolver.openInputStream(uri)
                val tempFile = File.createTempFile("upload", ".jpg", requireContext().cacheDir)
                tempFile.outputStream().use { fileOut ->
                    inputStream?.copyTo(fileOut)
                }

                val requestFile = tempFile.asRequestBody("image/*".toMediaTypeOrNull())
                val multipart = MultipartBody.Part.createFormData(
                    "files",
                    tempFile.name,
                    requestFile
                )

                // 1. Media 업로드
                val uploadResponse = RetrofitClient.mediaService.uploadMediaBulk(
                    listOf(multipart)
                )

                val mediaIds = uploadResponse.data?.map { it.fileId.toLong() } ?: emptyList()
                if (mediaIds.isEmpty()) return@launch

                // 2. Workspace Photo 등록
                workspacePhotoService.uploadPhotos(
                    spaceId,
                    WorkspacePhotoUploadRequestDto(mediaIds = mediaIds)
                )

                // 3. 목록 다시 불러오기
                loadPhotosFromApi()

            } catch (e: Exception) {
                Log.e("SpaceFragment", "이미지 업로드 실패", e)
            }
        }
    }

    companion object {
        private const val ARG_SPACE_ID = "arg_space_id"

        fun newInstance(spaceId: Long): SpaceFragment {
            return SpaceFragment().apply {
                arguments = Bundle().apply {
                    putLong(ARG_SPACE_ID, spaceId)
                }
            }
        }
    }
}