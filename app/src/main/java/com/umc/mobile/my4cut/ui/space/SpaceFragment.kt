package com.umc.mobile.my4cut.ui.space

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
import com.umc.mobile.my4cut.databinding.DialogChangeSpaceBinding
import com.umc.mobile.my4cut.databinding.DialogExitBinding
import com.umc.mobile.my4cut.databinding.DialogPhotoBinding
import com.umc.mobile.my4cut.databinding.FragmentSpaceBinding
import com.umc.mobile.my4cut.ui.photo.ChatRVAdapter
import com.umc.mobile.my4cut.ui.home.HomeFragment

class SpaceFragment : Fragment(R.layout.fragment_space) {
    data class Space(
        val id: Int,
        val name: String,
        val createdAt: Long,
        val expiredAt: Long,
        val maxMember: Int = 10,
        val currentMember: Int
    )

    private val spaces = mutableListOf<Space>()

    private lateinit var binding: FragmentSpaceBinding
    private lateinit var photoAdapter: PhotoRVAdapter
    private var photoDatas = ArrayList<PhotoData>()

    // private lateinit var chatAdapter: ChatRVAdapter
    private var chatAdapter: ChatRVAdapter? = null
    private var chatDatas = ArrayList<ChatData>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentSpaceBinding.bind(view)

        photoAdapter = PhotoRVAdapter(photoDatas)
        binding.rvPhotoList.adapter = photoAdapter
        binding.rvPhotoList.layoutManager = GridLayoutManager(requireContext(), 2)

        initDummyPhotos()


        photoAdapter.onItemClickListener = { photo ->
            showPhotoDialog(photo, isCommentExpanded = true)
        }

        binding.btnExitMenu.setOnClickListener {
            showExitDialog()  //혼자일 때 -> tvMessage.text = 나가면 스페이스가 삭제되어 복구할 수 없어요.
        }


        binding.btnChange.setOnClickListener {
            showChangeDialog()
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
            dialog.dismiss()

            // 홈 Fragment로 전환
            parentFragmentManager.beginTransaction()
                .replace(R.id.fcv_main, HomeFragment())
                .commit()

            // BottomNavigationView 선택 상태를 홈으로 변경
            requireActivity()
                .findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bnv_main)
                .selectedItemId = R.id.menu_home
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
        initDummyChats()

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

    private fun showChangeDialog() {
        val dialogBinding = DialogChangeSpaceBinding.inflate(layoutInflater)
        val builder = MaterialAlertDialogBuilder(requireContext())
            .setView(dialogBinding.root)
        val dialog = builder.create()

        dialog.setCanceledOnTouchOutside(true)

        dialogBinding.ivClose.setOnClickListener {
            dialog.dismiss()
        }

        dialogBinding.mainText.setOnClickListener {
            dialog.dismiss()
        }

        dialog.window?.setBackgroundDrawableResource(R.color.transparent)

        dialog.show()
    }

    private fun initDummyPhotos() {
        photoDatas.clear()

        photoDatas.add(
            PhotoData(
                userImageRes = R.drawable.ic_profile,
                userName = "에블린",
                dateTime = "2025/11/04 20:45",
                commentCount = 2,
                photoImageRes = R.drawable.image1
            )
        )

        photoDatas.add(
            PhotoData(
                userImageRes = R.drawable.ic_profile,
                userName = "유복치",
                dateTime = "2025/11/04 16:21",
                commentCount = 0,
                photoImageRes = R.drawable.image1
            )
        )

        photoAdapter.notifyDataSetChanged()
    }

    // 더미 댓글 데이터 생성 예시
    private fun initDummyChats() {
        chatDatas.clear()

        chatDatas.add(
            ChatData(
                profileImg = R.drawable.ic_profile,
                userName = "에블린",
                time = "20분 전",
                content = "올릴 때 2번째 가리고 ㄱㄱ"
            )
        )

        chatDatas.add(
            ChatData(
                profileImg = R.drawable.ic_profile,
                userName = "유복치",
                time = "1분 전",
                content = "왜 예쁜데"
            )
        )

        chatAdapter?.notifyDataSetChanged()
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

    // Space는 CreateSpaceDialogFragment 결과를 통해서만 생성됨
    // - name: 모달에서 입력한 스페이스 이름
    // - currentMember: 선택한 인원 수 (+ 본인)
    // - maxMember: 항상 10
    // 이 함수는 외부(Fragment/Dialog)에서 전달받은 값만 사용해야 함
    private fun addNewSpace(
        name: String,
        currentMember: Int,
        maxMember: Int = 10
    ) {
        val now = System.currentTimeMillis()
        val sevenDays = 7L * 24 * 60 * 60 * 1000

        val newSpace = Space(
            id = spaces.size + 1,
            name = name,
            createdAt = now,
            expiredAt = now + sevenDays,
            maxMember = maxMember,
            currentMember = currentMember
        )

        spaces.add(newSpace)

        // TODO: SpaceCircleView / Adapter 반영
        // spaceCircleView.setSpaces(spaces)
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

    companion object {
        private const val ARG_SPACE_ID = "space_id"

        fun newInstance(spaceId: Int): SpaceFragment {
            return SpaceFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_SPACE_ID, spaceId)
                }
            }
        }
    }
}