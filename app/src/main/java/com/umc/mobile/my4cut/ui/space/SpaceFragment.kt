package com.umc.mobile.my4cut.ui.space

import com.umc.mobile.my4cut.data.photo.remote.WorkspacePhotoService
import com.umc.mobile.my4cut.data.workspace.remote.WorkspaceMemberService

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
import com.umc.mobile.my4cut.ui.space.EditSpaceDialogFragment
import com.umc.mobile.my4cut.databinding.DialogExitBinding
import com.umc.mobile.my4cut.databinding.DialogPhotoBinding
import com.umc.mobile.my4cut.databinding.FragmentSpaceBinding
import com.umc.mobile.my4cut.ui.photo.ChatRVAdapter
import com.umc.mobile.my4cut.ui.home.HomeFragment

import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import android.util.Log
import com.umc.mobile.my4cut.data.base.BaseResponse
import com.umc.mobile.my4cut.data.user.model.UserMeResponse
import com.umc.mobile.my4cut.network.RetrofitClient

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

    private val workspacePhotoService: WorkspacePhotoService by lazy {
        RetrofitClient.workspacePhotoService
    }

    private val workspaceMemberService: WorkspaceMemberService by lazy {
        RetrofitClient.workspaceMemberService
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

        photoAdapter.onItemClickListener = { photo ->
            showPhotoDialog(photo, isCommentExpanded = true)
        }

        binding.btnExitMenu.setOnClickListener {
            showExitDialog()  //혼자일 때 -> tvMessage.text = 나가면 스페이스가 삭제되어 복구할 수 없어요.
        }

        binding.btnChange.setOnClickListener {
            showChangeDialog(spaceId)
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
                    retrofit2.Callback<BaseResponse<UserMeResponse>> {

                    override fun onResponse(
                        call: retrofit2.Call<BaseResponse<UserMeResponse>>,
                        response: retrofit2.Response<BaseResponse<UserMeResponse>>
                    ) {
                        val body = response.body()
                        myUserId = body?.data?.userId?.toLong() ?: -1L
                        isOwner = (data.ownerId?.toLong() == myUserId)

                        Log.d("SpaceFragment", "isOwner=$isOwner ownerId=${data.ownerId} myUserId=$myUserId")
                    }

                    override fun onFailure(
                        call: retrofit2.Call<BaseResponse<UserMeResponse>>,
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

    private fun showExitDialog() {
        val dialogBinding = DialogExitBinding.inflate(layoutInflater)
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
        val isMine = photo.userName == "유복치"

        // 기본 데이터 바인딩
        dialogBinding.ivProfile.setImageResource(photo.userImageRes)
        dialogBinding.ivMainPhoto.setImageResource(photo.photoImageRes)
        dialogBinding.tvUserName.text = photo.userName
        dialogBinding.tvDate.text = photo.dateTime
        dialogBinding.tvChat.text = "댓글 ${photo.commentCount}"

        // 내가 올린 사진일 때만 저장 아이콘 노출
        dialogBinding.ivSave.visibility =
            if (isMine) View.VISIBLE else View.GONE

        // 내가 올린 사진일 때만 삭제 아이콘 노출
        dialogBinding.ivDelete.visibility =
            if (isMine) View.VISIBLE else View.GONE

        // 더미 댓글 데이터 사용 (파일 추가 없이 SpaceFragment 내부 데이터 사용)
        val currentUserName = "유복치" // TODO: 로그인 유저 닉네임으로 교체
        chatDatas.clear()
        // initDummyChats()
        loadCommentsFromApi(photo)

        chatAdapter = ChatRVAdapter(chatDatas, currentUserName)
        dialogBinding.rvChatList.adapter = chatAdapter
        dialogBinding.rvChatList.layoutManager = LinearLayoutManager(requireContext())

        // 댓글 삭제 확인 모달 적용
        chatAdapter?.onDeleteClickListener = { position ->
            if (position in chatDatas.indices) {
                showDeleteCommentDialog {
                    chatDatas.removeAt(position)
                    chatAdapter?.notifyItemRemoved(position)
                    chatAdapter?.notifyItemRangeChanged(position, chatDatas.size)
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
                                profileImg = R.drawable.ic_profile,
                                userName = it.writerNickname,
                                content = it.content,
                                time = formatTime(it.createdAt)
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
            val instant = java.time.OffsetDateTime.parse(timestamp).toInstant()
            val sdf = java.text.SimpleDateFormat(
                "MM/dd HH:mm",
                java.util.Locale.getDefault()
            )
            sdf.format(java.util.Date.from(instant))
        } catch (e: Exception) {
            timestamp
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