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
import androidx.recyclerview.widget.LinearLayoutManager
import com.umc.mobile.my4cut.ui.photo.ChatData
import com.umc.mobile.my4cut.ui.photo.PhotoData
import com.umc.mobile.my4cut.ui.photo.PhotoRVAdapter
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.umc.mobile.my4cut.R
import com.umc.mobile.my4cut.databinding.DialogExitBinding
import com.umc.mobile.my4cut.databinding.DialogPhotoBinding
import com.umc.mobile.my4cut.databinding.FragmentSpaceBinding
import com.umc.mobile.my4cut.ui.photo.ChatRVAdapter

import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import android.util.Log
import com.umc.mobile.my4cut.data.base.BaseResponse
import com.umc.mobile.my4cut.data.user.model.UserMeResponse
import com.umc.mobile.my4cut.network.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.time.OffsetDateTime
import java.util.Date
import java.util.Locale

class SpaceFragment : Fragment(R.layout.fragment_space) {

    private lateinit var binding: FragmentSpaceBinding
    private lateinit var photoAdapter: PhotoRVAdapter
    private var photoDatas = ArrayList<PhotoData>()

    // private lateinit var chatAdapter: ChatRVAdapter
    private var chatAdapter: ChatRVAdapter? = null
    private var chatDatas = ArrayList<ChatData>()

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

    private fun loadPhotosFromApi() {
        lifecycleScope.launch {
            try {
                val response = workspacePhotoService.getPhotos(spaceId)
                if (response.code == "SUCCESS" && response.data != null) {
                    photoDatas.clear()
                    photoDatas.addAll(
                        response.data.map {
                            PhotoData(
                                photoId = it.mediaId ?: 0L,
                                userName = it.uploaderNickname ?: "",
                                userImageRes = R.drawable.ic_profile_cat,
                                photoImageRes = R.drawable.ic_profile_cat, // 실제 URL 이미지는 Glide 등으로 교체 가능
                                dateTime = it.createdAt ?: "",
                                commentCount = 0
                            )
                        }
                    )
                    photoAdapter.notifyDataSetChanged()
                }
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

    private fun showDeleteCommentDialog(onConfirm: () -> Unit) {
        val dialogBinding = DialogExitBinding.inflate(layoutInflater)

        // 문구 변경 (댓글 삭제용)
        dialogBinding.tvTitle.text = "정말 삭제하시겠어요?"
        dialogBinding.tvMessage.text = "삭제한 댓글은 다시 복구할 수 없어요."
        dialogBinding.btnExit.text = "삭제"

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(dialogBinding.root)
            .create()

        dialog.setCanceledOnTouchOutside(true)

        dialogBinding.btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialogBinding.btnExit.setOnClickListener {
            dialog.dismiss()
            onConfirm()
        }

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()
    }

    private fun showPhotoDialog(
        photo: PhotoData,
        isCommentExpanded: Boolean = true
    ) {
        val photoPosition = photoDatas.indexOf(photo)
        val dialogBinding = DialogPhotoBinding.inflate(layoutInflater)

        // 다이얼로그를 먼저 생성
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(dialogBinding.root)
            .create()

        // 내가 올린 사진 여부
        val isMine = photo.userName == myNickname

        // 기본 데이터 바인딩
        photo.userImageRes?.let { dialogBinding.ivProfile.setImageResource(it) }
        photo.photoImageRes?.let { dialogBinding.ivMainPhoto.setImageResource(it) }
        dialogBinding.tvUserName.text = photo.userName
        dialogBinding.tvDate.text = photo.dateTime
        dialogBinding.tvChat.text = "댓글 ${photo.commentCount}"

        // 내가 올린 사진일 때만 저장 아이콘 노출
        dialogBinding.ivSave.visibility =
            if (isMine) View.VISIBLE else View.GONE

        // 내가 올린 사진일 때만 삭제 아이콘 노출
        dialogBinding.ivDelete.visibility =
            if (isMine) View.VISIBLE else View.GONE

        // 댓글 데이터는 API에서 불러옴
        chatDatas.clear()
        loadCommentsFromApi(photo)

        // 로그인 사용자 닉네임 기준으로 내 댓글 여부 판단
        chatAdapter = ChatRVAdapter(chatDatas, myNickname)
        dialogBinding.rvChatList.adapter = chatAdapter
        dialogBinding.rvChatList.layoutManager = LinearLayoutManager(requireContext())

        // 댓글 삭제 확인 모달 적용
        chatAdapter?.onDeleteClickListener = { position ->
            if (position in chatDatas.indices) {
                val comment = chatDatas[position]

                showDeleteCommentDialog {
                    lifecycleScope.launch {
                        try {
                            // 서버 삭제 호출 (commentId 필요)
                            workspacePhotoService.deleteComment(
                                spaceId,
                                photo.photoId,
                                comment.commentId
                            )

                            loadCommentsFromApi(photo) // 다시 불러오기

                        } catch (e: Exception) {
                            Log.e("SpaceFragment", "댓글 삭제 실패", e)
                        }
                    }
                }
            }
        }

        // 사진 삭제 버튼 클릭 시 사진 삭제 모달 표시
        dialogBinding.ivDelete.setOnClickListener {
            showDeletePhotoDialog {
                if (photoPosition != -1) {
                    photoDatas.removeAt(photoPosition)
                    photoAdapter.notifyItemRemoved(photoPosition)
                    photoAdapter.notifyItemRangeChanged(photoPosition, photoDatas.size)
                }
                dialog.dismiss()
            }
        }

        // 댓글 펼침 여부 (댓글 없으면 기본 접힘)
        val expanded = isCommentExpanded && photo.commentCount > 0

        dialogBinding.rvChatList.visibility =
            if (expanded) View.VISIBLE else View.GONE
        dialogBinding.text.visibility =
            if (expanded) View.VISIBLE else View.GONE
        dialogBinding.ivSend.visibility =
            if (expanded) View.VISIBLE else View.GONE

        // 댓글 등록 버튼
        dialogBinding.ivSend.setOnClickListener {
            val content = dialogBinding.text.text.toString().trim()
            if (content.isEmpty()) return@setOnClickListener

            lifecycleScope.launch {
                try {
                    workspacePhotoService.createComment(
                        spaceId,
                        photo.photoId,
                        com.umc.mobile.my4cut.data.photo.model.CommentCreateRequest(content)
                    )

                    dialogBinding.text.text?.clear()
                    loadCommentsFromApi(photo) // 다시 불러오기

                } catch (e: Exception) {
                    Log.e("SpaceFragment", "댓글 등록 실패", e)
                }
            }
        }

        // 댓글 열림/닫힘 토글 상태
        var isExpanded = expanded

        fun updateCommentUi() {
            if (isExpanded) {
                // 댓글 펼침: 리스트 + 입력 영역 모두 보임
                dialogBinding.rvChatList.visibility = View.VISIBLE
                dialogBinding.text.visibility = View.VISIBLE
                dialogBinding.ivSend.visibility = View.VISIBLE
                dialogBinding.ivToggleComment.setImageResource(R.drawable.ic_up)
            } else {
                // 댓글 접힘: 리스트만 숨기고 입력 영역은 유지
                dialogBinding.rvChatList.visibility = View.GONE
                dialogBinding.text.visibility = View.VISIBLE
                dialogBinding.ivSend.visibility = View.VISIBLE
                dialogBinding.ivToggleComment.setImageResource(R.drawable.ic_down)
            }
        }

        // 초기 UI 반영
        updateCommentUi()

        // 화살표 클릭 시 댓글 열기/닫기
        dialogBinding.ivToggleComment.setOnClickListener {
            isExpanded = !isExpanded
            updateCommentUi()
        }

        dialog.setCanceledOnTouchOutside(true)

        dialogBinding.ivClose.setOnClickListener {
            dialog.dismiss()
        }

        dialogBinding.mainText.setOnClickListener {
            // SpaceFragment에서는 더 이상 생성하지 않음
            dialog.dismiss()
        }

        dialog.window?.setBackgroundDrawableResource(R.color.transparent)

        dialog.show()
    }

    private fun loadCommentsFromApi(photo: PhotoData) {
        lifecycleScope.launch {
            try {
                val response = workspacePhotoService
                    .getComments(spaceId, photo.photoId.toLong())

                if (response.code == "SUCCESS" && response.data != null) {
                    chatDatas.clear()
                    chatDatas.addAll(
                        response.data.map {
                            ChatData(
                                commentId = it.id ?: 0L,
                                profileImg = R.drawable.ic_profile,
                                userName = it.writerNickname ?: "",
                                content = it.content ?: "",
                                time = getRelativeTime(it.createdAt ?: "")
                            )
                        }
                    )
                    chatAdapter?.notifyDataSetChanged()
                }
            } catch (e: Exception) {
                Log.e("SpaceFragment", "댓글 API 실패", e)
            }
        }
    }

    private fun formatTime(timestamp: String): String {
        return try {
            val instant = OffsetDateTime.parse(timestamp).toInstant()
            val sdf = SimpleDateFormat(
                "MM/dd HH:mm",
                Locale.getDefault()
            )
            sdf.format(Date.from(instant))
        } catch (e: Exception) {
            timestamp
        }
    }

    private fun getRelativeTime(createdAt: String): String {
        return try {
            val instant = OffsetDateTime.parse(createdAt).toInstant()
            val now = java.time.Instant.now()

            val diffSeconds = java.time.Duration.between(instant, now).seconds
            val diffMinutes = diffSeconds / 60
            val diffHours = diffMinutes / 60
            val diffDays = diffHours / 24

            when {
                diffMinutes < 1 -> "방금 전"
                diffMinutes < 60 -> "${diffMinutes}분 전"
                diffHours < 24 -> "${diffHours}시간 전"
                diffDays < 7 -> "${diffDays}일 전"
                else -> {
                    val sdf = SimpleDateFormat("MM/dd", Locale.getDefault())
                    sdf.format(Date.from(instant))
                }
            }
        } catch (e: Exception) {
            createdAt
        }
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

    // 사진 삭제 모달 함수
    private fun showDeletePhotoDialog(onConfirm: () -> Unit) {
        val dialogBinding = DialogExitBinding.inflate(layoutInflater)

        dialogBinding.tvTitle.text = "정말 삭제하시겠어요?"
        dialogBinding.tvMessage.text = "삭제한 사진은 다시 복구할 수 없어요."
        dialogBinding.btnExit.text = "삭제"

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(dialogBinding.root)
            .create()

        dialog.setCanceledOnTouchOutside(true)

        dialogBinding.btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialogBinding.btnExit.setOnClickListener {
            dialog.dismiss()
            onConfirm()
        }

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()
    }

    // addNewSpace() // 더 이상 SpaceFragment 내부에서 사용하지 않으므로 제거

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