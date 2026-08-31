package com.umc.mobile.my4cut.ui.space

import com.bumptech.glide.Glide

import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.view.setPadding
import androidx.recyclerview.widget.RecyclerView
import com.google.android.flexbox.AlignItems
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexWrap
import com.google.android.flexbox.FlexboxLayoutManager
import com.google.android.flexbox.JustifyContent

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.RectF
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
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
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
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.doOnNextLayout
import androidx.exifinterface.media.ExifInterface
import com.umc.mobile.my4cut.data.auth.local.TokenManager
import com.umc.mobile.my4cut.data.base.BaseResponse
import com.umc.mobile.my4cut.data.user.model.UserMeResponse
import com.umc.mobile.my4cut.data.network.RetrofitClient
import com.umc.mobile.my4cut.data.tutorial.TutorialManager
import com.umc.mobile.my4cut.data.tutorial.model.TutorialType
import com.umc.mobile.my4cut.ui.photo.PhotoDialogFragment
import com.umc.mobile.my4cut.ui.tutorial.TutorialDimView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.ByteArrayOutputStream
import java.io.FileOutputStream

import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.time.Duration
import java.time.LocalDateTime
import java.util.Locale

class SpaceFragment : Fragment(R.layout.fragment_space) {

    private lateinit var binding: FragmentSpaceBinding
    private lateinit var photoAdapter: PhotoRVAdapter
    private var photoDatas = ArrayList<PhotoData>()
    private var selectedFinalPhotoId: Long? = null

    private lateinit var memberAdapter: MemberAdapter
    private val memberItems = ArrayList<MemberItem>()

    private var retouchSpaceTutorialView: View? = null
    private var retouchPhotoTutorialView: View? = null

    private var spaceId: Long = -1L
    // 알림에서 특정 사진을 눌러 들어온 경우
    // 자동으로 열어야 할 mediaId
    private var targetPhotoId: Long? = null
    private var myUserId: Long = -1L
    private var memberCount: Int = 0
    private var myNickname: String = ""
    private var myProfileImageUrl: String? = null
    private val existingMemberIds = mutableListOf<Long>()

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

        // 열어야 할 워크스페이스 ID
        spaceId = arguments?.getLong(ARG_SPACE_ID) ?: -1L

        // 알림에서 사진을 눌러 진입했다면 mediaId도 전달받음
        targetPhotoId = arguments
            ?.takeIf { it.containsKey(ARG_PHOTO_ID) }
            ?.getLong(ARG_PHOTO_ID)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentSpaceBinding.bind(view)

        // API 호출 전
        binding.btnChange.visibility = View.GONE
        binding.tvExpire.text = ""
        binding.tvExpire.setBackgroundResource(R.drawable.bg_skeleton_text)

        val membersRecyclerView = view.findViewById<RecyclerView>(R.id.rvMembers)
        memberAdapter = MemberAdapter(memberItems)
        membersRecyclerView.adapter = memberAdapter
        membersRecyclerView.layoutManager = FlexboxLayoutManager(requireContext()).apply {
            flexDirection = FlexDirection.ROW
            flexWrap = FlexWrap.WRAP
            justifyContent = JustifyContent.CENTER
            alignItems = AlignItems.CENTER
        }
        membersRecyclerView.isNestedScrollingEnabled = false

        parentFragmentManager.setFragmentResultListener(
            PhotoDialogFragment.RESULT_PHOTO_DELETED,
            viewLifecycleOwner
        ) { _, bundle ->
            val deletedPhotoId = bundle.getLong(
                PhotoDialogFragment.BUNDLE_KEY_DELETED_PHOTO_ID,
                -1L
            )

            if (deletedPhotoId != -1L) {
                photoAdapter.removePhoto(deletedPhotoId)

                val isEmpty = photoAdapter.itemCount == 0

                binding.layoutEmptyPhotos.visibility =
                    if (isEmpty) View.VISIBLE else View.GONE

                binding.rvPhotoList.visibility =
                    if (isEmpty) View.GONE else View.VISIBLE
            }
        }

        photoAdapter = PhotoRVAdapter(photoDatas)
        binding.rvPhotoList.adapter = photoAdapter
        binding.rvPhotoList.layoutManager = GridLayoutManager(requireContext(), 2)

        loadSpaceFromApi()

        photoAdapter.showSkeleton()
        loadPhotosFromApi()

        binding.swipeRefreshLayout.setOnRefreshListener {
            loadSpaceFromApi()
            loadPhotosFromApi()
        }

        photoAdapter.onItemClickListener = { photo ->
            showPhotoDialog(photo, isCommentExpanded = true)
        }

        photoAdapter.onFinalToggleListener = { photo ->
            selectFinalPhoto(photo)
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

    override fun onDestroyView() {
        retouchSpaceTutorialView?.let { view ->
            (view.parent as? ViewGroup)?.removeView(view)
        }
        retouchSpaceTutorialView = null

        retouchPhotoTutorialView?.let { view ->
            (view.parent as? ViewGroup)?.removeView(view)
        }
        retouchPhotoTutorialView = null

        super.onDestroyView()
    }

    private fun loadSpaceFromApi() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = RetrofitClient.workspaceService.getWorkspaceDetail(spaceId)
                val data = response.data ?: return@launch

                // 스페이스 정보 UI 반영
                binding.tvTitle.text = data.name
                memberCount = data.memberCount ?: 0

                // API 응답 성공 후
                binding.tvExpire.setBackgroundResource(R.drawable.bg_label_pink)
                binding.tvExpire.text = formatExpireText(data.expiresAt)

                existingMemberIds.clear()
                existingMemberIds.addAll(data.alreadyInvitedFriendIds.orEmpty())

                updateMemberUi(data.memberProfiles)

                Log.d(
                    "SpaceFragment",
                    "edit dialog excludedUserIds=$existingMemberIds"
                )

                // 현재 로그인 사용자 정보 조회 → 방장 여부 판단
                RetrofitClient.userService.getMyPage().enqueue(object :
                    Callback<BaseResponse<UserMeResponse>> {

                    override fun onResponse(
                        call: Call<BaseResponse<UserMeResponse>>,
                        response: Response<BaseResponse<UserMeResponse>>
                    ) {
                        if (!response.isSuccessful) {
                            Log.e(
                                "SpaceFragment",
                                "내 정보 조회 응답 실패 code=${response.code()}"
                            )
                            return
                        }

                        val userData = response.body()?.data

                        myUserId = userData?.userId?.toLong() ?: -1L
                        myNickname = userData?.nickname.orEmpty()
                        myProfileImageUrl = userData?.profileImageViewUrl

                        binding.btnChange.visibility = View.VISIBLE

                        binding.btnChange.post {
                            checkRetouchSpaceTutorial()
                        }

                        updateMemberUi(data.memberProfiles)
                        updatePhotoUploaderProfiles()
                    }

                    override fun onFailure(
                        call: Call<BaseResponse<UserMeResponse>>,
                        t: Throwable
                    ) {
                        Log.e("SpaceFragment", "내 정보 조회 실패", t)
                    }
                })

            } catch (e: Exception) {
                Log.e("SpaceFragment", "스페이스 정보 API 실패", e)
            }
        }
    }

    private fun formatExpireText(expiresAt: String?): String {
        if (expiresAt.isNullOrBlank()) {
            return ""
        }

        return try {
            val expireDateTime = LocalDateTime.parse(
                expiresAt,
                DateTimeFormatter.ISO_LOCAL_DATE_TIME
            )

            val now = LocalDateTime.now()

            val remainDays = Duration.between(now, expireDateTime)
                .toDays()
                .coerceAtLeast(0)

            when {
                remainDays <= 0 -> "오늘 만료"
                else -> "${remainDays}일 뒤 만료"
            }
        } catch (e: Exception) {
            Log.e(
                "SpaceFragment",
                "만료 일시 파싱 실패 expiresAt=$expiresAt",
                e
            )
            ""
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

    /**
     * 이미지 압축
     */
    private fun compressImage(uri: Uri): File? {
        return try {
            val inputStream = requireContext().contentResolver.openInputStream(uri) ?: return null

            val originalBitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()

            if (originalBitmap == null) {
                Log.e("EntryRegister", "❌ Failed to decode bitmap from URI: $uri")
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

            Log.d("EntryRegister", "✅ Image compressed: ${tempFile.length() / 1024}KB")

            tempFile
        } catch (e: Exception) {
            Log.e("EntryRegister", "❌ Image compression failed", e)
            null
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
            Log.e("EntryRegister", "Failed to read EXIF", e)
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

    private fun loadPhotosFromApi() {
        workspacePhotoService.getPhotos(spaceId, "oldest")
            .enqueue(object : Callback<BaseResponse<List<com.umc.mobile.my4cut.data.photo.model.WorkspacePhotoResponseDto>>> {
                override fun onResponse(
                    call: Call<BaseResponse<List<com.umc.mobile.my4cut.data.photo.model.WorkspacePhotoResponseDto>>>,
                    response: Response<BaseResponse<List<com.umc.mobile.my4cut.data.photo.model.WorkspacePhotoResponseDto>>>
                ) {
                    val list = response.body()?.data ?: emptyList()
                    Log.d("PHOTO_DEBUG", "서버에서 받은 사진 개수 = ${list.size}")

                    // empty state
                    val isEmpty = list.isEmpty()

                    binding.layoutEmptyPhotos.visibility =
                        if (isEmpty) View.VISIBLE else View.GONE

                    binding.rvPhotoList.visibility =
                        if (isEmpty) View.GONE else View.VISIBLE

                    val newPhotos = ArrayList<PhotoData>()

                    for (photoResponse in list) {
                        val photoId = photoResponse.mediaId ?: 0L

                        newPhotos.add(
                            PhotoData(
                                photoId = photoId,
                                userProfileUrl = photoResponse.uploaderProfileImageUrl,
                                userName = photoResponse.uploaderNickname ?: "",
                                dateTime = formatDateTime(photoResponse.createdAt),
                                commentCount = 0,
                                photoImageRes = null,
                                photoUrl = photoResponse.viewUrl,
                                uploaderId = photoResponse.uploaderId,
                                isFinal = photoResponse.isFinal ?: false
                            )
                        )
                    }

                    selectedFinalPhotoId = newPhotos
                        .firstOrNull { it.isFinal }
                        ?.photoId

                    photoDatas.clear()
                    photoDatas.addAll(newPhotos)
                    photoAdapter.updatePhotos(newPhotos)
                    photoAdapter.hideSkeleton()

                    // RETOUCH_PHOTO는 실제 첫 번째 사진 ViewHolder가 만들어진 뒤에 표시
                    if (newPhotos.isNotEmpty()) {
                        binding.rvPhotoList.doOnNextLayout {
                            val firstViewHolder =
                                binding.rvPhotoList.findViewHolderForAdapterPosition(0)

                            if (firstViewHolder != null) {
                                checkRetouchPhotoTutorial()
                            }
                        }
                    }

                    binding.swipeRefreshLayout.isRefreshing = false

                    // 알림에서 특정 사진을 눌러 들어온 경우
                    // 서버에서 사진 목록을 다 받은 뒤 해당 사진 모달을 자동으로 연다.
                    targetPhotoId?.let { targetId ->

                        Log.d(
                            "NotificationNavigation",
                            "targetPhotoId=$targetId, photoIds=${newPhotos.map { it.photoId }}"
                        )

                        val targetPhoto = newPhotos.find { photo ->
                            photo.photoId == targetId
                        }

                        if (targetPhoto != null) {

                            Log.d(
                                "NotificationNavigation",
                                "target photo found: ${targetPhoto.photoId}"
                            )

                            showPhotoDialog(
                                photo = targetPhoto,
                                isCommentExpanded = true
                            )

                            targetPhotoId = null

                        } else {
                            Log.e(
                                "NotificationNavigation",
                                "target photo not found: $targetId"
                            )
                        }
                    }

                    for (photo in newPhotos) {
                        viewLifecycleOwner.lifecycleScope.launch {
                            try {
                                val commentResponse = workspacePhotoService.getComments(spaceId, photo.photoId)
                                val count = commentResponse.data?.size ?: 0
                                val index = photoDatas.indexOfFirst { it.photoId == photo.photoId }
                                if (index != -1) {
                                    photoDatas[index] = photoDatas[index].copy(commentCount = count)
                                    photoAdapter.notifyItemChanged(index)
                                }
                            } catch (e: Exception) {
                                Log.e("PHOTO_DEBUG", "댓글 개수 조회 실패 photoId=${photo.photoId}", e)
                            }
                        }
                    }
                }

                override fun onFailure(
                    call: Call<BaseResponse<List<com.umc.mobile.my4cut.data.photo.model.WorkspacePhotoResponseDto>>>,
                    t: Throwable
                ) {
                    photoAdapter.hideSkeleton()
                    binding.swipeRefreshLayout.isRefreshing = false

                    Log.e(
                        "SpaceFragment",
                        "사진 목록 API 실패",
                        t
                    )
                }
            })
    }

    private fun selectFinalPhoto(photo: PhotoData) {
        val clickedPhotoId = photo.photoId
        val isCurrentlyFinal = selectedFinalPhotoId == clickedPhotoId

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                if (isCurrentlyFinal) {
                    workspacePhotoService.deselectFinalPhoto(
                        spaceId,
                        clickedPhotoId
                    )

                    selectedFinalPhotoId = null
                } else {
                    workspacePhotoService.selectFinalPhoto(
                        spaceId,
                        clickedPhotoId
                    )

                    selectedFinalPhotoId = clickedPhotoId
                }

                val updatedPhotos = photoDatas.map { item ->
                    item.copy(
                        isFinal = item.photoId == selectedFinalPhotoId
                    )
                }

                photoDatas.clear()
                photoDatas.addAll(updatedPhotos)
                photoAdapter.updatePhotos(photoDatas.toList())

                Log.d(
                    "SpaceFragment",
                    if (isCurrentlyFinal) {
                        "최종 사진 선택 해제 성공 photoId=$clickedPhotoId"
                    } else {
                        "최종 사진 선택 성공 photoId=$clickedPhotoId"
                    }
                )

            } catch (e: Exception) {
                Log.e(
                    "SpaceFragment",
                    "최종 사진 선택/해제 API 실패 photoId=$clickedPhotoId",
                    e
                )
            }
        }
    }

    private fun showExitDialog() {
        val dialogBinding = DialogExitBinding.inflate(layoutInflater)

        dialogBinding.tvTitle.text = "정말 나가시겠어요?"

        dialogBinding.tvMessage.text = if (memberCount <= 1) {
            "마지막 멤버가 나가면 스페이스가 삭제되어 복구할 수 없어요."
        } else {
            "다시 초대받기 전까지 스페이스를 이용할 수 없어요."
        }

        dialogBinding.btnExit.text = "나가기"

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
                    workspaceMemberService.leaveWorkspace(spaceId)

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
            memberIds = existingMemberIds.toList()
        )

        // 수정 완료 후 자동 갱신
        dialog.setOnEditCompleteListener {
            loadSpaceFromApi()   // 제목 다시 불러오기
        }

        dialog.show(parentFragmentManager, "EditSpaceDialog")
    }

    private fun uploadImageToServer(uri: Uri) {
        lifecycleScope.launch {
            try {
                Log.d("SpaceFragment", "🔄 이미지 압축 및 업로드 시작")

                // 1. [수정] 아까 정의한 compressImage 함수를 사용하여 압축된 파일을 가져옴
                val compressedFile = compressImage(uri)

                if (compressedFile == null) {
                    Log.e("SpaceFragment", "❌ 이미지 압축 실패")
                    return@launch
                }

                Log.d("SpaceFragment", "📤 압축 완료: ${compressedFile.length() / 1024}KB")

                // 2. [수정] 압축된 파일을 RequestBody로 변환
                // 서버 용량 제한(413 에러)을 피하기 위해 image/jpeg 타입 명시
                val requestFile = compressedFile.asRequestBody("image/jpeg".toMediaTypeOrNull())
                val multipart = MultipartBody.Part.createFormData(
                    "files", // 서버 파라미터명 (EntryRegister와 동일하게 "files")
                    compressedFile.name,
                    requestFile
                )

                // 3. Media 업로드 (Bulk)
                val uploadResponse = withContext(Dispatchers.IO) {
                    RetrofitClient.mediaService.uploadMediaBulk(listOf(multipart))
                }

                val mediaIds = uploadResponse.data?.map { it.mediaId.toLong() } ?: emptyList()
                if (mediaIds.isEmpty()) {
                    Log.e("SpaceFragment", "❌ 업로드된 미디어 ID가 없습니다.")
                    return@launch
                }

                // 4. Workspace Photo 등록
                workspacePhotoService.uploadPhotos(
                    spaceId,
                    WorkspacePhotoUploadRequestDto(mediaIds = mediaIds)
                )

                // 5. 성공 시 목록 다시 불러오기 및 캐시 삭제
                withContext(Dispatchers.Main) {
                    loadPhotosFromApi()
                    compressedFile.delete()
                }

            } catch (e: Exception) {
                Log.e("SpaceFragment", "이미지 업로드 실패", e)
            }
        }
    }

    companion object {

        // 열어야 할 워크스페이스 ID
        private const val ARG_SPACE_ID = "arg_space_id"

        // 알림에서 특정 사진을 열어야 할 경우 전달하는 mediaId
        private const val ARG_PHOTO_ID = "arg_photo_id"

        /**
         * 일반적인 스페이스 진입:
         * SpaceFragment.newInstance(spaceId)
         *
         * 알림에서 특정 사진으로 진입:
         * SpaceFragment.newInstance(
         *     spaceId = workspaceId,
         *     photoId = mediaId
         * )
         */
        fun newInstance(
            spaceId: Long,
            photoId: Long? = null
        ): SpaceFragment {

            return SpaceFragment().apply {
                arguments = Bundle().apply {

                    putLong(ARG_SPACE_ID, spaceId)

                    if (photoId != null) {
                        putLong(ARG_PHOTO_ID, photoId)
                    }
                }
            }
        }
    }

    private fun updateMemberUi(memberProfiles: List<String>?) {
        memberItems.clear()

        memberProfiles.orEmpty().forEachIndexed { index, profilePath ->
            memberItems.add(
                MemberItem(
                    id = index.toLong(),
                    profileImageUrl = buildProfileUrl(profilePath)
                )
            )
        }

        memberAdapter.notifyDataSetChanged()
    }

    private fun buildProfileUrl(profilePath: String?): String? {
        if (profilePath.isNullOrBlank()) return null
        return if (profilePath.startsWith("http://") || profilePath.startsWith("https://")) {
            profilePath
        } else {
            null
        }
    }

    private fun updatePhotoUploaderProfiles() {
        if (myProfileImageUrl.isNullOrBlank()) return
        if (photoDatas.isEmpty()) return

        var changed = false
        for (index in photoDatas.indices) {
            val photo = photoDatas[index]
            if (photo.uploaderId == myUserId && photo.userProfileUrl != myProfileImageUrl) {
                photoDatas[index] = photo.copy(userProfileUrl = myProfileImageUrl)
                changed = true
            }
        }

        if (changed) {
            photoAdapter.updatePhotos(photoDatas.toList())
        }
    }

    private fun Int.toDp(): Int {
        return (this * resources.displayMetrics.density).toInt()
    }

    private data class MemberItem(
        val id: Long,
        val profileImageUrl: String?
    )

    private inner class MemberAdapter(
        private val items: List<MemberItem>
    ) : RecyclerView.Adapter<MemberAdapter.MemberViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MemberViewHolder {
            val imageView = AppCompatImageView(parent.context).apply {
                layoutParams = FlexboxLayoutManager.LayoutParams(27.toDp(), 27.toDp()).apply {
                    val horizontal = 3.toDp()
                    val vertical = 3.toDp()
                    setMargins(horizontal, vertical, horizontal, vertical)
                }
                setImageResource(R.drawable.img_profile_default)
                scaleType = ImageView.ScaleType.CENTER_CROP
                setPadding(0)
            }
            return MemberViewHolder(imageView)
        }

        override fun onBindViewHolder(holder: MemberViewHolder, position: Int) {
            holder.bind(items[position])
        }

        override fun getItemCount(): Int = items.size

        inner class MemberViewHolder(
            private val imageView: AppCompatImageView
        ) : RecyclerView.ViewHolder(imageView) {

            fun bind(item: MemberItem) {
                if (item.profileImageUrl.isNullOrBlank()) {
                    imageView.setImageResource(R.drawable.img_profile_default)
                } else {
                    Glide.with(imageView)
                        .load(item.profileImageUrl)
                        .placeholder(R.drawable.img_profile_default)
                        .error(R.drawable.img_profile_default)
                        .circleCrop()
                        .into(imageView)
                }
            }
        }
    }

    private fun checkRetouchPhotoTutorial() {
        val userId =
            TokenManager.getUserId(requireContext())
                ?: return

        viewLifecycleOwner.lifecycleScope.launch {

            // SPACE 튜토리얼이 아직 안 끝났으면
            // PHOTO는 띄우지 않음
            val spaceCompleted =
                TutorialManager.isCompleted(
                    requireContext(),
                    userId,
                    TutorialType.RETOUCH_SPACE
                )

            if (spaceCompleted != true) {
                return@launch
            }

            val photoCompleted =
                TutorialManager.isCompleted(
                    requireContext(),
                    userId,
                    TutorialType.RETOUCH_PHOTO
                )

            if (photoCompleted == false) {
                val firstViewHolder =
                    binding.rvPhotoList
                        .findViewHolderForAdapterPosition(0)

                if (firstViewHolder != null) {
                    showRetouchPhotoTutorial()
                }
            }
        }
    }

    private fun completeRetouchPhotoTutorial() {
        val userId =
            TokenManager.getUserId(requireContext())
                ?: return

        hideRetouchPhotoTutorial()

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                RetrofitClient.tutorialService
                    .completeTutorial(
                        TutorialType.RETOUCH_PHOTO
                    )

                TutorialManager.setCompleted(
                    requireContext(),
                    userId,
                    TutorialType.RETOUCH_PHOTO
                )

            } catch (e: Exception) {
                Log.e(
                    "Tutorial",
                    "RETOUCH_PHOTO 튜토리얼 완료 처리 실패",
                    e
                )
            }
        }
    }

    private fun showRetouchSpaceTutorial() {
        if (retouchSpaceTutorialView != null) return

        val root = requireActivity()
            .findViewById<ViewGroup>(android.R.id.content)

        val overlay = layoutInflater.inflate(
            R.layout.view_tutorial_space,
            root,
            false
        )

        retouchSpaceTutorialView = overlay
        root.addView(overlay)

        overlay.findViewById<View>(
            R.id.ll_tutorial_close
        ).setOnClickListener {
            completeRetouchSpaceTutorial()
        }

        overlay.post {
            setupRetouchSpaceTutorial()
        }
    }

    private fun checkRetouchSpaceTutorial() {
        val userId =
            TokenManager.getUserId(requireContext())
                ?: return

        viewLifecycleOwner.lifecycleScope.launch {
            val completed =
                TutorialManager.isCompleted(
                    requireContext(),
                    userId,
                    TutorialType.RETOUCH_SPACE
                )

            if (completed == false) {
                showRetouchSpaceTutorial()
            }
        }
    }

    private fun completeRetouchSpaceTutorial() {
        val userId =
            TokenManager.getUserId(requireContext())
                ?: return

        hideRetouchSpaceTutorial()

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                RetrofitClient.tutorialService
                    .completeTutorial(
                        TutorialType.RETOUCH_SPACE
                    )

                TutorialManager.setCompleted(
                    requireContext(),
                    userId,
                    TutorialType.RETOUCH_SPACE
                )

                // SPACE가 끝났으면 PHOTO 튜토리얼 확인
                checkRetouchPhotoTutorial()

            } catch (e: Exception) {
                Log.e(
                    "Tutorial",
                    "RETOUCH_SPACE 튜토리얼 완료 처리 실패",
                    e
                )
            }
        }
    }

    private fun hideRetouchSpaceTutorial() {
        retouchSpaceTutorialView?.let { view ->
            (view.parent as? ViewGroup)?.removeView(view)
        }

        retouchSpaceTutorialView = null
    }

    private fun dp(value: Float): Float {
        return value * resources.displayMetrics.density
    }

    private fun getRectInOverlay(
        target: View,
        overlay: View,
        padding: Float = 0f
    ): RectF {

        val targetLocation = IntArray(2)
        val overlayLocation = IntArray(2)

        target.getLocationOnScreen(targetLocation)
        overlay.getLocationOnScreen(overlayLocation)

        val left =
            targetLocation[0] -
                    overlayLocation[0] -
                    padding

        val top =
            targetLocation[1] -
                    overlayLocation[1] -
                    padding

        return RectF(
            left,
            top,
            left + target.width + padding * 2,
            top + target.height + padding * 2
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
            view.layoutParams as FrameLayout.LayoutParams

        params.width = rect.width().toInt()
        params.height = rect.height().toInt()

        view.layoutParams = params
        view.x = rect.left
        view.y = rect.top
        view.visibility = View.VISIBLE
    }

    private fun setupRetouchSpaceTutorial() {
        val overlay =
            retouchSpaceTutorialView ?: return

        val dimView =
            overlay.findViewById<TutorialDimView>(
                R.id.tutorial_dim_view
            )

        // 실제 SpaceFragment View
        val changeView = binding.btnChange
        val exitView = binding.btnExitMenu
        val uploadView = binding.btnUpload

        if (
            changeView.width == 0 ||
            exitView.width == 0 ||
            uploadView.width == 0
        ) {
            return
        }

        val baseChangeRect =
            getRectInOverlay(
                changeView,
                overlay
            )

        val changeRect = RectF(
            baseChangeRect.left - dp(9f),
            baseChangeRect.top - dp(10f),
            baseChangeRect.right + dp(9f),
            baseChangeRect.bottom + dp(10f)
        )

        val baseExitRect =
            getRectInOverlay(
                exitView,
                overlay
            )

        val exitRect = RectF(
            baseExitRect.left - dp(10f),
            baseExitRect.top - dp(10f),
            baseExitRect.right + dp(9f),
            baseExitRect.bottom + dp(10f)
        )

        val baseUploadRect =
            getRectInOverlay(
                uploadView,
                overlay
            )

        val uploadDiameter =
            maxOf(
                baseUploadRect.width(),
                baseUploadRect.height()
            ) + dp(10f)

        val uploadRect = RectF(
            baseUploadRect.centerX() - uploadDiameter / 2f + dp(6f),
            baseUploadRect.centerY() - uploadDiameter / 2f + dp(6f),
            baseUploadRect.centerX() + uploadDiameter / 2f - dp(6f),
            baseUploadRect.centerY() + uploadDiameter / 2f - dp(6f)
        )

        dimView.clearHighlights()

        dimView.addHighlight(
            changeRect,
            changeRect.width() / 2f
        )

        dimView.addHighlight(
            exitRect,
            exitRect.width() / 2f
        )

        dimView.addHighlight(
            uploadRect,
            uploadRect.width() / 2f
        )

        positionTutorialHighlight(
            overlay.findViewById(
                R.id.v_highlight_change
            ),
            changeRect
        )

        positionTutorialHighlight(
            overlay.findViewById(
                R.id.v_highlight_exit
            ),
            exitRect
        )

        positionTutorialHighlight(
            overlay.findViewById(
                R.id.v_highlight_upload
            ),
            uploadRect
        )

        setupRetouchSpacePositions(
            overlay,
            changeRect,
            exitRect,
            uploadRect
        )

        setupRetouchSpaceTexts(overlay)
    }

    private fun setupRetouchSpacePositions(
        overlay: View,
        changeRect: RectF,
        exitRect: RectF,
        uploadRect: RectF
    ) {
        val changeText =
            overlay.findViewById<TextView>(
                R.id.tv_tutorial_change
            )

        val changeArrow =
            overlay.findViewById<ImageView>(
                R.id.iv_arrow_change
            )

        val exitText =
            overlay.findViewById<TextView>(
                R.id.tv_tutorial_exit
            )

        val exitArrow =
            overlay.findViewById<ImageView>(
                R.id.iv_arrow_exit
            )

        val uploadText =
            overlay.findViewById<TextView>(
                R.id.tv_tutorial_upload
            )

        val uploadArrow =
            overlay.findViewById<ImageView>(
                R.id.iv_arrow_upload
            )

        // 수정 설명
        positionTutorialView(
            changeText,
            changeRect.right -
                    changeText.width -
                    dp(50f),
            changeRect.bottom + dp(5f)
        )

        // 수정 화살표
        positionTutorialView(
            changeArrow,
            changeRect.left -
                    changeArrow.width * 0.45f + dp(1f),
            changeRect.bottom -
                    changeArrow.height * 0.15f + dp(3f)
        )

        // 나가기 설명
        positionTutorialView(
            exitText,
            exitRect.centerX() -
                    exitText.width / 2f -
                    dp(195f),
            exitRect.bottom + dp(65f)
        )

        // 나가기 화살표
        positionTutorialView(
            exitArrow,
            exitRect.centerX() -
                    exitArrow.width / 2f - dp(13f),
            exitRect.bottom
        )

        // 업로드 설명
        positionTutorialView(
            uploadText,
            uploadRect.left -
                    uploadText.width -
                    dp(12f),
            uploadRect.centerY() -
                    uploadText.height / 2f -
                    dp (12f)
        )

        // 업로드 화살표
        positionTutorialView(
            uploadArrow,
            uploadRect.left -
                    uploadArrow.width * 0.7f + dp(1f),
            uploadRect.centerY() -
                    uploadArrow.height / 2f + dp(27f)
        )
    }

    private fun setupRetouchSpaceTexts(
        overlay: View
    ) {
        setRetouchSpaceTutorialText(
            overlay.findViewById(
                R.id.tv_tutorial_change
            ),
            "스페이스 이름을 수정하고,\n새로운 친구를 초대해요.",
            "이름을 수정"
        )

        setRetouchSpaceTutorialText(
            overlay.findViewById(
                R.id.tv_tutorial_exit
            ),
            "버튼을 누르면 스페이스에서 나갈 수 있어요.\n혼자 이용 중인 스페이스를 나갈 경우 사라져요.",
            "스페이스에서 나갈 수 있어요"
        )

        setRetouchSpaceTutorialText(
            overlay.findViewById(
                R.id.tv_tutorial_upload
            ),
            "네컷 업로드 버튼을 눌러 \n함께 보정할 네컷을 업로드해보세요.",
            "네컷을 업로드"
        )
    }

    private fun setRetouchSpaceTutorialText(
        textView: TextView,
        text: String,
        highlight: String
    ) {
        val spannable =
            SpannableString(text)

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

        textView.text = spannable
    }

    private fun showRetouchPhotoTutorial() {
        if (retouchPhotoTutorialView != null) return

        val root = requireActivity()
            .findViewById<ViewGroup>(android.R.id.content)

        val overlay = layoutInflater.inflate(
            R.layout.view_tutorial_photo,
            root,
            false
        )

        retouchPhotoTutorialView = overlay
        root.addView(overlay)

        overlay.findViewById<View>(
            R.id.ll_tutorial_close
        ).setOnClickListener {
            completeRetouchPhotoTutorial()
        }

        overlay.post {
            setupRetouchPhotoTutorial()
        }
    }

    private fun hideRetouchPhotoTutorial() {
        retouchPhotoTutorialView?.let { view ->
            (view.parent as? ViewGroup)?.removeView(view)
        }

        retouchPhotoTutorialView = null
    }

    private fun setupRetouchPhotoTutorial() {
        val overlay =
            retouchPhotoTutorialView ?: return

        val dimView =
            overlay.findViewById<TutorialDimView>(
                R.id.tutorial_dim_view
            )

        val firstPhotoItem =
            binding.rvPhotoList
                .findViewHolderForAdapterPosition(0)
                ?.itemView
                ?: return

        val finalToggle =
            firstPhotoItem.findViewById<View>(
                R.id.ivFinalToggle
            )

        if (
            finalToggle.width == 0 ||
            finalToggle.height == 0
        ) {
            return
        }

        val baseFinalRect =
            getRectInOverlay(
                finalToggle,
                overlay
            )

        val finalRect = RectF(
            baseFinalRect.left - dp(3f),
            baseFinalRect.top + dp(1f),
            baseFinalRect.right + dp(3f),
            baseFinalRect.bottom - dp(1f)
        )

        dimView.clearHighlights()

        // pill 형태
        dimView.addHighlight(
            finalRect,
            finalRect.height() / 2f
        )

        positionTutorialHighlight(
            overlay.findViewById(
                R.id.v_highlight_final
            ),
            finalRect
        )

        setupRetouchPhotoPosition(
            overlay,
            finalRect
        )

        setupRetouchPhotoText(
            overlay
        )
    }

    private fun setupRetouchPhotoPosition(
        overlay: View,
        finalRect: RectF
    ) {
        val finalText =
            overlay.findViewById<TextView>(
                R.id.tv_tutorial_final
            )

        val finalArrow =
            overlay.findViewById<ImageView>(
                R.id.iv_arrow_final
            )

        // 설명: 최종본 버튼 오른쪽 아래
        val finalTextX =
            (finalRect.right + dp(8f))
                .coerceAtMost(
                    overlay.width.toFloat() -
                            finalText.width -
                            dp(8f)
                )

        positionTutorialView(
            finalText,
            finalTextX + dp(20f),
            finalRect.bottom + dp(2f)
        )

        // 화살표: 버튼과 설명 사이
        positionTutorialView(
            finalArrow,
            finalRect.right -
                    finalArrow.width * 0.25f - dp(47f),
            finalRect.bottom -
                    finalArrow.height * 0.15f - dp(3f)
        )
    }

    private fun setupRetouchPhotoText(
        overlay: View
    ) {
        setRetouchPhotoTutorialText(
            overlay.findViewById(
                R.id.tv_tutorial_final
            ),
            "다음 사람이 어떤 사진을 보정하면\n되는지 표시할 수 있어요.",
            "어떤 사진을 보정하면\n되는지"
        )
    }

    private fun setRetouchPhotoTutorialText(
        textView: TextView,
        text: String,
        highlight: String
    ) {
        val spannable =
            SpannableString(text)

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

        textView.text = spannable
    }
}