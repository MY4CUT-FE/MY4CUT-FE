package com.umc.mobile.my4cut.ui.photo

import android.os.Bundle
import android.view.*
import android.widget.ImageView
import android.widget.TextView
import android.widget.EditText
import android.widget.Button
import android.util.Log
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.appcompat.app.AlertDialog
import com.umc.mobile.my4cut.R
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.umc.mobile.my4cut.network.RetrofitClient
import com.umc.mobile.my4cut.data.photo.model.CommentCreateRequest
import com.umc.mobile.my4cut.data.photo.model.CommentDto
import com.bumptech.glide.Glide
import java.time.Duration
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

class PhotoDialogFragment : DialogFragment() {

    private lateinit var ivClose: ImageView
    private lateinit var ivDelete: ImageView
    private lateinit var ivSend: ImageView
    private lateinit var ivProfile: ImageView
    private lateinit var ivMainPhoto: ImageView
    private lateinit var tvUserName: TextView
    private lateinit var tvDate: TextView
    private lateinit var tvChat: TextView
    private lateinit var rvChatList: RecyclerView
    private lateinit var ivToggleComment: ImageView

    private lateinit var etComment: EditText

    // 전달받을 값
    private var workspaceId: Long = -1L
    private var photoId: Long = -1L
    private var uploaderId: Long? = null
    private var myUserId: Long? = null

    /** 내 정보 조회해서 userId 가져오기 */
    private fun loadMyInfo() {
        lifecycleScope.launch {
            try {
                val res = withContext(kotlinx.coroutines.Dispatchers.IO) {
                    RetrofitClient.userService.getMyPage().execute().body()
                }

                myUserId = res?.data?.userId?.toLong()
                Log.d("PhotoDialog", "내 정보 조회 성공 myUserId=$myUserId")

                // 내 정보 로드 후 삭제 버튼 상태 다시 반영
                updateDeleteButtonVisibility()

                // 댓글 다시 로드해서 isMine 반영
                loadComments()
            } catch (e: Exception) {
                Log.e("PhotoDialog", "내 정보 조회 실패", e)
            }
        }
    }
    private var uploaderNickname: String? = null
    private var uploaderProfileUrl: String? = null
    private var createdAt: String? = null
    private var photoUrl: String? = null

    // 댓글 어댑터
    private lateinit var commentAdapter: CommentAdapter
    private var isCommentExpanded: Boolean = true

    /** 댓글 삭제 API 연결 */
    fun deleteComment(commentId: Long) {
        if (workspaceId == -1L || photoId == -1L) {
            Log.e("PhotoDialog", "workspaceId 또는 photoId가 없음")
            return
        }

        lifecycleScope.launch {
            try {
                RetrofitClient.workspacePhotoService
                    .deleteComment(workspaceId, photoId, commentId)

                Log.d("PhotoDialog", "댓글 삭제 성공: $commentId")
                loadComments() // 삭제 후 자동 갱신
            } catch (e: Exception) {
                Log.e("PhotoDialog", "댓글 삭제 실패", e)
            }
        }
    }

    /** 공통 삭제 확인 모달 (사진/댓글 공용) */
    private fun showDeleteConfirmDialog(
        title: String,
        message: String,
        confirmText: String = "삭제",
        onConfirm: () -> Unit
    ) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_exit, null)

        val tvTitle = dialogView.findViewById<TextView>(R.id.tvTitle)
        val tvMessage = dialogView.findViewById<TextView>(R.id.tvMessage)
        val btnCancel = dialogView.findViewById<Button>(R.id.btnCancel)
        val btnExit = dialogView.findViewById<Button>(R.id.btnExit)
        btnExit.text = confirmText

        tvTitle.text = title
        tvMessage.text = message

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        btnExit.setOnClickListener {
            dialog.dismiss()
            onConfirm()
        }

        dialog.show()
    }

    /** 댓글 삭제 확인 모달 */
    private fun showDeleteCommentDialog(commentId: Long) {
        showDeleteConfirmDialog(
            title = "정말 삭제하시겠어요?",
            message = "삭제한 댓글은 다시 복구할 수 없어요.",
            confirmText = "삭제"
        ) {
            deleteComment(commentId)
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?) =
        super.onCreateDialog(savedInstanceState)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.dialog_photo, container, false)

        workspaceId = arguments?.getLong("workspaceId") ?: -1L
        photoId = arguments?.getLong("photoId") ?: -1L
        myUserId = arguments?.getLong("myUserId")
        uploaderId = arguments?.getLong("uploaderId")
        Log.d("PhotoDialog", "arguments 전달값 -> uploaderId=$uploaderId, photoId=$photoId, workspaceId=$workspaceId")
        uploaderNickname = arguments?.getString("uploaderNickname")
        uploaderProfileUrl = arguments?.getString("uploaderProfileUrl")
        createdAt = arguments?.getString("createdAt")
        photoUrl = arguments?.getString("photoUrl")

        initViews(view)
        bindUploaderInfo()
        initRecyclerView()
        initListeners()
        loadMyInfo()   // 내 userId API로 조회 후 댓글/삭제버튼 반영

        return view
    }

    private fun initViews(view: View) {
        ivClose = view.findViewById(R.id.ivClose)
        ivDelete = view.findViewById(R.id.ivDelete)
        ivSend = view.findViewById(R.id.ivSend)
        ivProfile = view.findViewById(R.id.ivProfile)
        ivMainPhoto = view.findViewById(R.id.ivMainPhoto)

        tvUserName = view.findViewById(R.id.tvUserName)
        tvDate = view.findViewById(R.id.tvDate)
        tvChat = view.findViewById(R.id.tvChat)

        rvChatList = view.findViewById(R.id.rvChatList)
        ivToggleComment = view.findViewById(R.id.ivToggleComment)
        etComment = view.findViewById(R.id.text)

        rvChatList.visibility = View.VISIBLE
        etComment.visibility = View.VISIBLE
        ivSend.visibility = View.VISIBLE
    }

    private fun bindUploaderInfo() {
        // 닉네임 표시
        tvUserName.text = uploaderNickname ?: ""

        // 시간 포맷 yyyy/MM/dd HH:mm
        tvDate.text = createdAt?.let { formatAbsoluteDateTime(it) } ?: ""

        // 프로필 이미지 표시 (없으면 기본 이미지)
        if (!uploaderProfileUrl.isNullOrEmpty()) {
            Glide.with(this)
                .load(uploaderProfileUrl)
                .placeholder(R.drawable.ic_profile_cat)
                .error(R.drawable.ic_profile_cat)
                .circleCrop()
                .into(ivProfile)
        } else {
            ivProfile.setImageResource(R.drawable.ic_profile_cat)
        }

        // 메인 사진 표시
        if (!photoUrl.isNullOrEmpty()) {
            Glide.with(this)
                .load(photoUrl)
                .placeholder(R.drawable.image1)
                .error(R.drawable.image1)
                .into(ivMainPhoto)
        }

        // 디버그 로그
        Log.d("PhotoDialog", "bindUploaderInfo -> uploaderId=$uploaderId myUserId=$myUserId uploaderNickname=$uploaderNickname profileUrl=$uploaderProfileUrl createdAt=$createdAt photoUrl=$photoUrl")
        updateDeleteButtonVisibility()
    }

    private fun formatDateTimeSafe(serverTime: String): String {
        return try {
            val time = OffsetDateTime.parse(serverTime)
            val now = OffsetDateTime.now()
            val diff = Duration.between(time, now)

            when {
                diff.toMinutes() < 1 -> "방금 전"
                diff.toMinutes() < 60 -> "${diff.toMinutes()}분 전"
                diff.toHours() < 24 -> "${diff.toHours()}시간 전"
                else -> time.format(DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm"))
            }
        } catch (e: Exception) {
            try {
                // Offset 없는 LocalDateTime 형식 대응
                val time = java.time.LocalDateTime.parse(serverTime.replace("Z", ""))
                val now = java.time.LocalDateTime.now()
                val diff = Duration.between(time, now)

                when {
                    diff.toMinutes() < 1 -> "방금 전"
                    diff.toMinutes() < 60 -> "${diff.toMinutes()}분 전"
                    diff.toHours() < 24 -> "${diff.toHours()}시간 전"
                    else -> time.format(DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm"))
                }
            } catch (e2: Exception) {
                serverTime
            }
        }
    }

    private fun formatAbsoluteDateTime(serverTime: String): String {
        return try {
            val time = OffsetDateTime.parse(serverTime)
            time.format(DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm"))
        } catch (e: Exception) {
            serverTime
        }
    }

    private fun initRecyclerView() {
        commentAdapter = CommentAdapter(
            emptyList(),
            onDeleteClick = { comment -> showDeleteCommentDialog(comment.commentId) }
        )

        rvChatList.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = commentAdapter
        }
    }

    private fun initListeners() {
        // 닫기
        ivClose.setOnClickListener {
            dismiss()
        }

        // 삭제 버튼 표시 여부 (내가 올린 사진만 삭제 가능)
        updateDeleteButtonVisibility()

        ivDelete.setOnClickListener {
            showDeletePhotoDialog()
        }

        // 댓글 전송
        ivSend.setOnClickListener {
            sendComment()
        }

        // 댓글 영역 펼치기/접기
        ivToggleComment.setOnClickListener {
            isCommentExpanded = !isCommentExpanded

            if (isCommentExpanded) {
                rvChatList.visibility = View.VISIBLE
                etComment.visibility = View.VISIBLE
                ivSend.visibility = View.VISIBLE
                ivToggleComment.setImageResource(R.drawable.ic_down)
            } else {
                rvChatList.visibility = View.GONE
                etComment.visibility = View.GONE
                ivSend.visibility = View.GONE
                ivToggleComment.setImageResource(R.drawable.ic_up)
            }
        }
    }

    private fun updateDeleteButtonVisibility() {
        // View가 아직 초기화되지 않은 경우 방어
        if (!::ivDelete.isInitialized) return

        val isMine = uploaderId != null && myUserId != null && uploaderId == myUserId

        Log.d(
            "PhotoDialog",
            "삭제버튼 체크 -> uploaderId=$uploaderId myUserId=$myUserId isMine=$isMine"
        )

        ivDelete.visibility = if (isMine) View.VISIBLE else View.GONE
    }

    /** 사진 삭제 확인 모달 */
    private fun showDeletePhotoDialog() {
        showDeleteConfirmDialog(
            title = "정말 삭제하시겠어요?",
            message = "삭제한 사진은 다시 복구할 수 없어요.",
            confirmText = "삭제"
        ) {
            deletePhoto()
        }
    }

    /** 사진 삭제 API 연결 */
    private fun deletePhoto() {
        if (workspaceId == -1L || photoId == -1L) {
            Log.e("PhotoDialog", "workspaceId 또는 photoId가 없음")
            return
        }

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.workspacePhotoService
                    .deletePhoto(workspaceId, photoId)

                Log.d("PhotoDialog", "사진 삭제 성공: ${response.code}")
                dismiss()
            } catch (e: Exception) {
                Log.e("PhotoDialog", "사진 삭제 실패", e)
            }
        }
    }

    /** 댓글 작성 API 연결 */
    private fun sendComment() {
        val content = etComment.text.toString().trim()
        if (content.isEmpty()) return

        if (workspaceId == -1L || photoId == -1L) {
            Log.e("PhotoDialog", "workspaceId 또는 photoId가 없음")
            return
        }

        lifecycleScope.launch {
            try {
                RetrofitClient.workspacePhotoService.createComment(
                    workspaceId,
                    photoId,
                    CommentCreateRequest(content = content)
                )

                Log.d("PhotoDialog", "댓글 작성 성공")
                etComment.setText("")
                etComment.requestFocus()
                loadComments() // 작성 후 새로고침
            } catch (e: Exception) {
                Log.e("PhotoDialog", "댓글 작성 실패", e)
            }
        }
    }

    private fun loadComments() {
        if (workspaceId == -1L || photoId == -1L) {
            Log.e("PhotoDialog", "workspaceId 또는 photoId가 없음")
            return
        }

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.workspacePhotoService
                    .getComments(workspaceId, photoId)

                Log.d("PhotoDialog", "댓글 API 응답 code=${response.code}, message=${response.message}")
                Log.d("PhotoDialog", "댓글 raw data=${response.data}")

                val list: List<CommentDto> = response.data ?: emptyList()

                // CommentData로 변환
                val mapped = list.map { dto ->
                    // 작성자 여부 판단 (서버 dto에 userId 필드가 있다고 가정)
                    val isMine = myUserId != null && dto.userId?.toLong() == myUserId
                    Log.d(
                        "PhotoDialog",
                        "댓글 매핑 -> commentId=${dto.id}, userId=${dto.userId}, myUserId=$myUserId, isMine=$isMine"
                    )

                    CommentData(
                        commentId = dto.id,
                        profileImgUrl = dto.profileImageUrl,
                        userName = dto.nickname,
                        // 댓글 시간은 항상 n분 전 형식으로 표시
                        time = formatDateTimeSafe(dto.createdAt),
                        content = dto.content,
                        isMine = isMine
                    )
                }

                updateComments(mapped)
                Log.d("PhotoDialog", "댓글 로드 성공: ${mapped.size}")
                Log.d("PhotoDialog", "현재 로그인 사용자 myUserId=$myUserId, 댓글 개수=${mapped.size}")

            } catch (e: Exception) {
                Log.e("PhotoDialog", "댓글 목록 조회 실패", e)
            }
        }
    }

    /** 댓글 목록 갱신 */
    fun updateComments(list: List<CommentData>) {
        commentAdapter.updateData(list)
        tvChat.text = "댓글 ${list.size}"

        // 마지막 댓글로 자동 스크롤
        if (list.isNotEmpty()) {
            rvChatList.post {
                rvChatList.scrollToPosition(list.size - 1)
            }
        }

        // 입력창 포커스 유지
        etComment.requestFocus()
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    companion object {
        private const val ARG_PHOTO_URL = "photoUrl"

        fun newInstance(
            workspaceId: Long,
            photoId: Long,
            photoUrl: String,
            uploaderId: Long?,
            uploaderNickname: String,
            uploaderProfileUrl: String?,
            createdAt: String,
            myUserId: Long
        ): PhotoDialogFragment {
            return PhotoDialogFragment().apply {
                arguments = Bundle().apply {
                    putLong("workspaceId", workspaceId)
                    putLong("photoId", photoId)
                    putString("photoUrl", photoUrl)
                    putLong("uploaderId", uploaderId ?: -1L)
                    putString("uploaderNickname", uploaderNickname)
                    putString("uploaderProfileUrl", uploaderProfileUrl)
                    putString("createdAt", createdAt)
                    putLong("myUserId", myUserId)
                }
            }
        }
    }
}