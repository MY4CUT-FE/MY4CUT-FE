package com.umc.mobile.my4cut.ui.photo

import android.app.Dialog
import android.app.DownloadManager
import android.content.Context
import android.graphics.Color
import android.graphics.RectF
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Environment
import android.webkit.URLUtil
import android.widget.Toast
import android.os.Bundle
import android.view.*
import android.widget.ImageView
import android.widget.TextView
import android.widget.EditText
import android.widget.Button
import android.util.Log
import android.util.TypedValue
import android.graphics.drawable.Drawable
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.widget.FrameLayout

import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import androidx.core.graphics.toColorInt

import com.umc.mobile.my4cut.R
import com.umc.mobile.my4cut.network.RetrofitClient
import com.umc.mobile.my4cut.data.photo.model.CommentCreateRequest
import com.umc.mobile.my4cut.data.photo.model.CommentDto
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.umc.mobile.my4cut.ui.tutorial.TutorialDimView

import java.time.Duration
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PhotoDialogFragment : DialogFragment() {

    private var retouchDetailTutorialView: View? = null
    private var retouchDetailTutorialDialog: Dialog? = null

    private lateinit var ivClose: ImageView
    private lateinit var ivDelete: ImageView
    private lateinit var ivSave: ImageView
    private lateinit var ivSaveTouchArea: View
    private lateinit var ivDeleteTouchArea: View
    private lateinit var ivSend: ImageView
    private lateinit var ivProfile: ImageView
    private lateinit var ivMainPhoto: ImageView
    private lateinit var tvUserName: TextView
    private lateinit var tvDate: TextView
    private lateinit var tvChat: TextView
    private lateinit var rvChatList: RecyclerView
    private lateinit var ivToggleComment: ImageView
    private lateinit var ivToggleCommentTouchArea: View
    private lateinit var etComment: EditText
    private lateinit var tvEmptyComments: TextView
    private lateinit var ivChat: ImageView

    // 전달받을 값
    private var workspaceId: Long = -1L
    private var photoId: Long = -1L
    private var uploaderId: Long? = null
    private var myUserId: Long? = null
    private var myNickname: String? = null

    /** 내 정보 조회해서 userId 가져오기 */
    private fun loadMyInfo() {
        lifecycleScope.launch {
            try {
                val res = withContext(kotlinx.coroutines.Dispatchers.IO) {
                    RetrofitClient.userService.getMyPage().execute().body()
                }

                myUserId = res?.data?.userId?.toLong()
                myNickname = res?.data?.nickname
                Log.d("PhotoDialog", "내 정보 조회 성공 myUserId=$myUserId myNickname=$myNickname")

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

    override fun onDestroyView() {
        retouchDetailTutorialDialog?.dismiss()
        retouchDetailTutorialDialog = null
        retouchDetailTutorialView = null

        super.onDestroyView()
    }

    private fun initViews(view: View) {
        ivClose = view.findViewById(R.id.ivClose)
        ivDelete = view.findViewById(R.id.ivDelete)
        ivSave = view.findViewById(R.id.ivSave)
        ivSaveTouchArea = view.findViewById(R.id.ivSaveTouchArea)
        ivDeleteTouchArea = view.findViewById(R.id.ivDeleteTouchArea)
        ivSend = view.findViewById(R.id.ivSend)
        ivProfile = view.findViewById(R.id.ivProfile)
        ivMainPhoto = view.findViewById(R.id.ivMainPhoto)

        tvUserName = view.findViewById(R.id.tvUserName)
        tvDate = view.findViewById(R.id.tvDate)
        tvChat = view.findViewById(R.id.tvChat)
        tvEmptyComments = view.findViewById(R.id.tvEmptyComments)

        rvChatList = view.findViewById(R.id.rvChatList)
        ivChat = view.findViewById(R.id.ivChat)
        ivToggleComment = view.findViewById(R.id.ivToggleComment)
        ivToggleCommentTouchArea = view.findViewById(R.id.ivToggleCommentTouchArea)
        etComment = view.findViewById(R.id.etText)
        etComment.setHintTextColor("#8F8F8F".toColorInt())
        etComment.setTextColor("#1A1A1A".toColorInt())

        rvChatList.visibility = View.VISIBLE
        etComment.visibility = View.VISIBLE
        ivSend.visibility = View.VISIBLE
    }

    private fun showSkeleton() {
        tvUserName.text = ""
        tvUserName.layoutParams = tvUserName.layoutParams.apply {
            width = dpToPx(70)
            height = dpToPx(12)
        }
        tvUserName.setBackgroundResource(
            R.drawable.bg_skeleton_text_light
        )

        tvDate.text = ""
        tvDate.layoutParams = tvDate.layoutParams.apply {
            width = dpToPx(110)
            height = dpToPx(10)
        }
        tvDate.setBackgroundResource(
            R.drawable.bg_skeleton_text_light
        )

        tvChat.text = ""
        tvChat.layoutParams = tvChat.layoutParams.apply {
            width = dpToPx(64)
            height = dpToPx(12)
        }
        tvChat.setBackgroundResource(
            R.drawable.bg_skeleton_text_light
        )

        ivProfile.setImageResource(R.drawable.ic_profile_cat)

        ivMainPhoto.setBackgroundResource(
            R.drawable.bg_skeleton_img
        )
        ivMainPhoto.setImageResource(
            R.drawable.ic_skeleton_img
        )
        ivMainPhoto.scaleType = ImageView.ScaleType.CENTER
    }

    private fun showCommentSkeleton() {
        tvChat.text = ""

        tvChat.layoutParams = tvChat.layoutParams.apply {
            width = dpToPx(64)
            height = dpToPx(12)
        }

        tvChat.setBackgroundResource(
            R.drawable.bg_skeleton_text_light
        )
    }

    private fun showCommentResult(commentCount: Int) {
        tvChat.background = null

        tvChat.layoutParams = tvChat.layoutParams.apply {
            width = ViewGroup.LayoutParams.WRAP_CONTENT
            height = ViewGroup.LayoutParams.WRAP_CONTENT
        }

        tvChat.text = "댓글  $commentCount"
    }

    private fun dpToPx(dp: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp.toFloat(),
            resources.displayMetrics
        ).toInt()
    }

    private fun bindUploaderInfo() {
        // 닉네임과 날짜는 즉시 실제 데이터 표시
        tvUserName.background = null
        tvDate.background = null

        tvUserName.layoutParams = tvUserName.layoutParams.apply {
            width = ViewGroup.LayoutParams.WRAP_CONTENT
            height = ViewGroup.LayoutParams.WRAP_CONTENT
        }

        tvDate.layoutParams = tvDate.layoutParams.apply {
            width = ViewGroup.LayoutParams.WRAP_CONTENT
            height = ViewGroup.LayoutParams.WRAP_CONTENT
        }

        tvUserName.text = uploaderNickname.orEmpty()
        tvDate.text = createdAt
            ?.let { formatAbsoluteDateTime(it) }
            .orEmpty()

        // 프로필도 기존 방식 유지
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

        // 메인 사진만 로딩 스켈레톤 적용
        ivMainPhoto.setBackgroundResource(
            R.drawable.bg_skeleton_img
        )
        ivMainPhoto.setImageResource(
            R.drawable.ic_skeleton_img
        )
        ivMainPhoto.scaleType = ImageView.ScaleType.CENTER

        if (!photoUrl.isNullOrEmpty()) {
            Glide.with(this)
                .load(photoUrl)
                .placeholder(R.drawable.ic_skeleton_img)
                .error(R.drawable.ic_skeleton_img)
                .listener(object : RequestListener<Drawable> {

                    override fun onLoadFailed(
                        e: GlideException?,
                        model: Any?,
                        target: Target<Drawable>,
                        isFirstResource: Boolean
                    ): Boolean {
                        ivMainPhoto.setBackgroundResource(
                            R.drawable.bg_skeleton_img
                        )
                        ivMainPhoto.scaleType =
                            ImageView.ScaleType.CENTER

                        return false
                    }

                    override fun onResourceReady(
                        resource: Drawable,
                        model: Any,
                        target: Target<Drawable>?,
                        dataSource: DataSource,
                        isFirstResource: Boolean
                    ): Boolean {
                        ivMainPhoto.background = null
                        ivMainPhoto.scaleType =
                            ImageView.ScaleType.CENTER_CROP

                        return false
                    }
                })
                .into(ivMainPhoto)
        }

        Log.d(
            "PhotoDialog",
            "bindUploaderInfo -> uploaderId=$uploaderId " +
                    "myUserId=$myUserId " +
                    "uploaderNickname=$uploaderNickname " +
                    "profileUrl=$uploaderProfileUrl " +
                    "createdAt=$createdAt " +
                    "photoUrl=$photoUrl"
        )

        updateDeleteButtonVisibility()
    }

    private fun formatDateTimeSafe(serverTime: String): String {
        return try {
            val time = parseServerDateTime(serverTime)
            val now = ZonedDateTime.now(ZoneId.of("Asia/Seoul"))
            val diff = Duration.between(time, now)

            when {
                diff.toMinutes() < 1 -> "방금 전"
                diff.toMinutes() < 60 -> "${diff.toMinutes()}분 전"
                diff.toHours() < 24 -> "${diff.toHours()}시간 전"
                else -> time.format(DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm"))
            }
        } catch (e: Exception) {
            serverTime
        }
    }

    private fun formatAbsoluteDateTime(serverTime: String): String {
        return try {
            parseServerDateTime(serverTime).format(DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm"))
        } catch (e: Exception) {
            serverTime
        }
    }

    private fun parseServerDateTime(serverTime: String): ZonedDateTime {
        val seoulZone = ZoneId.of("Asia/Seoul")

        return try {
            OffsetDateTime.parse(serverTime).atZoneSameInstant(seoulZone)
        } catch (_: Exception) {
            val normalized = serverTime.removeSuffix("Z")

            val localDateTime = try {
                LocalDateTime.parse(normalized, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            } catch (_: Exception) {
                try {
                    LocalDateTime.parse(normalized, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                } catch (_: Exception) {
                    try {
                        LocalDateTime.parse(normalized, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
                    } catch (_: Exception) {
                        try {
                            LocalDateTime.parse(normalized, DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss"))
                        } catch (_: Exception) {
                            LocalDateTime.parse(normalized, DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm"))
                        }
                    }
                }
            }

            localDateTime.atZone(ZoneOffset.UTC).withZoneSameInstant(seoulZone)
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

        ivDeleteTouchArea.setOnClickListener {
            showDeletePhotoDialog()
        }

        ivSaveTouchArea.setOnClickListener {
            downloadPhoto()
        }

        // 댓글 전송
        ivSend.setOnClickListener {
            sendComment()
        }

        // 댓글 영역 펼치기/접기
        val toggleCommentAction = {
            isCommentExpanded = !isCommentExpanded

            if (isCommentExpanded) {
                val hasComments = commentAdapter.itemCount > 0

                rvChatList.visibility =
                    if (hasComments) View.VISIBLE else View.GONE

                tvEmptyComments.visibility =
                    if (hasComments) View.GONE else View.VISIBLE

                etComment.visibility = View.VISIBLE
                ivSend.visibility = View.VISIBLE
                ivToggleComment.setImageResource(R.drawable.ic_space_down)
            } else {
                rvChatList.visibility = View.GONE
                tvEmptyComments.visibility = View.GONE
                etComment.visibility = View.GONE
                ivSend.visibility = View.GONE
                ivToggleComment.setImageResource(R.drawable.ic_space_up)
            }
        }

        ivToggleComment.setOnClickListener {
            toggleCommentAction()
        }

        ivToggleCommentTouchArea.setOnClickListener {
            toggleCommentAction()
        }
    }



    private fun updateDeleteButtonVisibility() {
        // View가 아직 초기화되지 않은 경우 방어
        if (!::ivDelete.isInitialized) return

        val isMineById = uploaderId != null && uploaderId != -1L && myUserId != null && uploaderId == myUserId
        val isMineByNickname = !uploaderNickname.isNullOrBlank() && !myNickname.isNullOrBlank() && uploaderNickname == myNickname
        val isMine = isMineById || isMineByNickname

        Log.d(
            "PhotoDialog",
            "삭제버튼 체크 -> uploaderId=$uploaderId myUserId=$myUserId uploaderNickname=$uploaderNickname myNickname=$myNickname isMineById=$isMineById isMineByNickname=$isMineByNickname isMine=$isMine"
        )

        ivDelete.visibility = if (isMine) View.VISIBLE else View.GONE

        if (::ivDeleteTouchArea.isInitialized) {
            ivDeleteTouchArea.visibility = if (isMine) View.VISIBLE else View.GONE
        }
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

                parentFragmentManager.setFragmentResult(
                    RESULT_PHOTO_DELETED,
                    Bundle().apply {
                        putLong(BUNDLE_KEY_DELETED_PHOTO_ID, photoId)
                    }
                )

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

    private fun downloadPhoto() {
        val url = photoUrl
        if (url.isNullOrBlank()) {
            Toast.makeText(requireContext(), "다운로드할 사진이 없어요.", Toast.LENGTH_SHORT).show()
            return
        }

        runCatching {
            val request = DownloadManager.Request(Uri.parse(url)).apply {
                setTitle("MY4CUT 사진 다운로드")
                setDescription(uploaderNickname?.ifBlank { "사진을 다운로드하고 있어요." } ?: "사진을 다운로드하고 있어요.")
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                setAllowedOverMetered(true)
                setAllowedOverRoaming(true)

                val token = requireContext()
                    .getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
                    .getString("access_token", null)
                if (!token.isNullOrBlank()) {
                    addRequestHeader("Authorization", "Bearer $token")
                }

                val fileName = buildDownloadFileName(url)
                setDestinationInExternalPublicDir(
                    Environment.DIRECTORY_PICTURES,
                    "MY4CUT/$fileName"
                )
            }

            val downloadManager = requireContext().getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            downloadManager.enqueue(request)
            Toast.makeText(requireContext(), "사진 다운로드를 시작했어요.", Toast.LENGTH_SHORT).show()
        }.onFailure { e ->
            Log.e("PhotoDialog", "사진 다운로드 실패", e)
            Toast.makeText(requireContext(), "사진 다운로드에 실패했어요.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun buildDownloadFileName(url: String): String {
        val guessedName = URLUtil.guessFileName(url, null, "image/jpeg")
        return if (guessedName.contains('.')) guessedName else "my4cut_$photoId.jpg"
    }

    private fun loadComments() {
        showCommentSkeleton()

        if (workspaceId == -1L || photoId == -1L) {
            showCommentResult(0)

            Log.e(
                "PhotoDialog",
                "workspaceId 또는 photoId가 없음"
            )
            return
        }

        lifecycleScope.launch {
            try {
                val response =
                    RetrofitClient.workspacePhotoService
                        .getComments(workspaceId, photoId)

                Log.d(
                    "PhotoDialog",
                    "댓글 API 응답 code=${response.code}, " +
                            "message=${response.message}"
                )

                val list: List<CommentDto> =
                    response.data ?: emptyList()

                val mapped = list.map { dto ->
                    val isMine =
                        myUserId != null &&
                                dto.userId?.toLong() == myUserId

                    CommentData(
                        commentId = dto.id,
                        profileImgUrl = dto.profileImageUrl,
                        userName = dto.nickname,
                        time = dto.createdAt,
                        content = dto.content,
                        isMine = isMine
                    )
                }

                updateComments(mapped)

                Log.d(
                    "PhotoDialog",
                    "댓글 로드 성공: ${mapped.size}"
                )
            } catch (e: Exception) {
                showCommentResult(0)

                Log.e(
                    "PhotoDialog",
                    "댓글 목록 조회 실패",
                    e
                )
            }
        }
    }

    /** 댓글 목록 갱신 */
    fun updateComments(list: List<CommentData>) {
        commentAdapter.updateData(list)
        showCommentResult(list.size)

        if (isCommentExpanded && list.isEmpty()) {
            rvChatList.visibility = View.GONE
            tvEmptyComments.visibility = View.VISIBLE
        } else {
            rvChatList.visibility =
                if (isCommentExpanded) View.VISIBLE else View.GONE

            tvEmptyComments.visibility = View.GONE
        }

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

        dialog?.window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

            setLayout(
                (resources.displayMetrics.widthPixels * 0.9).toInt(),
                (resources.displayMetrics.heightPixels * 0.85).toInt()
            )
        }

        view?.post {
            showRetouchDetailTutorial()
        }
    }

    companion object {
        const val RESULT_PHOTO_DELETED = "result_photo_deleted"
        const val BUNDLE_KEY_DELETED_PHOTO_ID = "bundle_key_deleted_photo_id"

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

    private fun showRetouchDetailTutorial() {
        if (retouchDetailTutorialDialog != null) return

        val overlay =
            layoutInflater.inflate(
                R.layout.view_tutorial_detail,
                null,
                false
            )

        val tutorialDialog =
            Dialog(requireContext()).apply {
                requestWindowFeature(Window.FEATURE_NO_TITLE)
                setContentView(overlay)

                window?.apply {
                    setBackgroundDrawable(
                        ColorDrawable(Color.TRANSPARENT)
                    )

                    setLayout(
                        WindowManager.LayoutParams.MATCH_PARENT,
                        WindowManager.LayoutParams.MATCH_PARENT
                    )

                    clearFlags(
                        WindowManager.LayoutParams.FLAG_DIM_BEHIND
                    )
                }

                setCancelable(false)
            }

        retouchDetailTutorialView = overlay
        retouchDetailTutorialDialog = tutorialDialog

        overlay.findViewById<View>(
            R.id.ll_tutorial_close
        ).setOnClickListener {
            // API 연결 후
            // completeRetouchDetailTutorial()

            hideRetouchDetailTutorial()
        }

        tutorialDialog.setOnShowListener {
            tutorialDialog.window?.setLayout(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT
            )

            overlay.post {
                setupRetouchDetailTutorial()
            }
        }

        tutorialDialog.show()
    }

    private fun hideRetouchDetailTutorial() {
        retouchDetailTutorialDialog?.dismiss()
        retouchDetailTutorialDialog = null
        retouchDetailTutorialView = null
    }

    private fun tutorialDp(
        value: Float
    ): Float {
        return value *
                resources.displayMetrics.density
    }

    private fun getTutorialRect(
        target: View,
        overlay: View
    ): RectF {

        val targetLocation = IntArray(2)
        val overlayLocation = IntArray(2)

        target.getLocationOnScreen(
            targetLocation
        )

        overlay.getLocationOnScreen(
            overlayLocation
        )

        val left =
            targetLocation[0] -
                    overlayLocation[0]

        val top =
            targetLocation[1] -
                    overlayLocation[1]

        return RectF(
            left.toFloat(),
            top.toFloat(),
            left + target.width.toFloat(),
            top + target.height.toFloat()
        )
    }

    private fun positionTutorialView(
        view: View,
        x: Float,
        y: Float
    ) {
        view.x = x
        view.y = y
    }

    private fun positionTutorialHighlight(
        view: View,
        rect: RectF
    ) {
        val params =
            view.layoutParams as
                    FrameLayout.LayoutParams

        params.width =
            rect.width().toInt()

        params.height =
            rect.height().toInt()

        view.layoutParams = params

        view.x = rect.left
        view.y = rect.top

        view.visibility = View.VISIBLE
    }

    private fun setupRetouchDetailTutorial() {
        val overlay =
            retouchDetailTutorialView ?: return

        val dimView =
            overlay.findViewById<TutorialDimView>(
                R.id.tutorial_dim_view
            )

        if (
            ivSaveTouchArea.width == 0 ||
            tvChat.width == 0
        ) {
            return
        }

        val baseSaveRect =
            getTutorialRect(
                ivSaveTouchArea,
                overlay
            )

        val saveRect = RectF(
            baseSaveRect.left + tutorialDp(6f),
            baseSaveRect.top + tutorialDp(6f),
            baseSaveRect.right - tutorialDp(6f),
            baseSaveRect.bottom - tutorialDp(6f)
        )

        val deleteRect =
            if (
                ivDeleteTouchArea.visibility == View.VISIBLE &&
                ivDeleteTouchArea.width > 0
            ) {
                val baseDeleteRect =
                    getTutorialRect(
                        ivDeleteTouchArea,
                        overlay
                    )

                RectF(
                    baseDeleteRect.left + tutorialDp(6f),
                    baseDeleteRect.top + tutorialDp(6f),
                    baseDeleteRect.right - tutorialDp(6f),
                    baseDeleteRect.bottom - tutorialDp(6f)
                )
            } else {
                null
            }

        val baseCommentIconRect =
            getTutorialRect(
                ivChat,
                overlay
            )

        val baseCommentTextRect =
            getTutorialRect(
                tvChat,
                overlay
            )

        val baseCommentRect =
            getTutorialRect(
                tvChat,
                overlay
            )

        val commentRect = RectF(
            minOf(baseCommentIconRect.left, baseCommentTextRect.left) - tutorialDp(7f),
            minOf(baseCommentIconRect.top, baseCommentTextRect.top) - tutorialDp(2f),
            maxOf(baseCommentRect.right, baseCommentTextRect.right) - tutorialDp(24f),
            maxOf(baseCommentIconRect.bottom, baseCommentTextRect.bottom) + tutorialDp(2f)
        )

        dimView.clearHighlights()

        dimView.addHighlight(
            saveRect,
            saveRect.height() / 2f
        )

        deleteRect?.let {
            dimView.addHighlight(
                it,
                it.height() / 2f
            )
        }

        dimView.addHighlight(
            commentRect,
            21f
        )

        positionTutorialHighlight(
            overlay.findViewById(
                R.id.v_highlight_save
            ),
            saveRect
        )

        val deleteHighlight =
            overlay.findViewById<View>(
                R.id.v_highlight_delete
            )

        if (deleteRect != null) {
            positionTutorialHighlight(
                deleteHighlight,
                deleteRect
            )
        } else {
            deleteHighlight.visibility = View.GONE
        }

        positionTutorialHighlight(
            overlay.findViewById(
                R.id.v_highlight_comment
            ),
            commentRect
        )

        setupRetouchDetailPositions(
            overlay,
            saveRect,
            deleteRect,
            commentRect
        )

        setupRetouchDetailTexts(
            overlay
        )
    }

    private fun setupRetouchDetailPositions(
        overlay: View,
        saveRect: RectF,
        deleteRect: RectF?,
        commentRect: RectF
    ) {
        val saveText =
            overlay.findViewById<TextView>(
                R.id.tv_tutorial_save
            )

        val saveArrow =
            overlay.findViewById<ImageView>(
                R.id.iv_arrow_save
            )

        val deleteText =
            overlay.findViewById<TextView>(
                R.id.tv_tutorial_delete
            )

        val deleteArrow =
            overlay.findViewById<ImageView>(
                R.id.iv_arrow_delete
            )

        val commentText =
            overlay.findViewById<TextView>(
                R.id.tv_tutorial_comment
            )

        val commentArrow =
            overlay.findViewById<ImageView>(
                R.id.iv_arrow_comment
            )

        // 다운로드 설명
        positionTutorialView(
            saveText,
            saveRect.centerX() -
                    saveText.width / 2f -
                    tutorialDp(100f),
            saveRect.top -
                    saveText.height -
                    tutorialDp(49f)
        )

        positionTutorialView(
            saveArrow,
            saveRect.centerX() -
                    saveArrow.width / 2f,
            saveRect.top -
                    saveArrow.height -
                    tutorialDp(2f)
        )

        // 삭제 설명
        if (deleteRect != null) {
            deleteText.visibility = View.VISIBLE
            deleteArrow.visibility = View.VISIBLE

            positionTutorialView(
                deleteText,
                deleteRect.centerX() -
                        deleteText.width / 2f -
                        tutorialDp(103f),
                deleteRect.bottom +
                        tutorialDp(23f)
            )

            positionTutorialView(
                deleteArrow,
                deleteRect.centerX() -
                        deleteArrow.width / 2f,
                deleteRect.bottom +
                        tutorialDp(4f)
            )
        } else {
            deleteText.visibility = View.GONE
            deleteArrow.visibility = View.GONE
        }

        // 댓글 설명
        positionTutorialView(
            commentText,
            commentRect.right -
                    tutorialDp(29f),
            commentRect.bottom +
                    tutorialDp(6f)
        )

        positionTutorialView(
            commentArrow,
            commentRect.left +
                    tutorialDp(4f),
            commentRect.bottom +
                    tutorialDp(2f)
        )
    }

    private fun setupRetouchDetailTexts(
        overlay: View
    ) {
        setRetouchDetailText(
            overlay.findViewById(
                R.id.tv_tutorial_save
            ),
            "친구가 올린 사진을 다운로드 받아\n이어서 보정할 수 있어요.",
            "이어서 보정"
        )

        setRetouchDetailText(
            overlay.findViewById(
                R.id.tv_tutorial_delete
            ),
            "내가 올린 사진은 언제든 삭제할 수 있어요.",
            "언제든 삭제"
        )

        setRetouchDetailText(
            overlay.findViewById(
                R.id.tv_tutorial_comment
            ),
            "댓글로 스페이스 친구와 소통해요.\n포토리의 TIP: 댓글에 SNS 업로드 시\n가리고 싶은 것을 말하거나 보정 순서를 정해보세요:)",
            "포토리의 TIP:"
        )
    }

    private fun setRetouchDetailText(
        textView: TextView,
        text: String,
        vararg highlights: String
    ) {
        val spannable =
            SpannableString(text)

        highlights.forEach { highlight ->
            val start =
                text.indexOf(highlight)

            if (start >= 0) {
                spannable.setSpan(
                    ForegroundColorSpan(
                        Color.parseColor("#FF7E67")
                    ),
                    start,
                    start + highlight.length,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
        }

        textView.text = spannable
    }
}