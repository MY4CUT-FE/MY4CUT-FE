package com.example.my4cut.fragment

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.my4cut.ChatData
import com.example.my4cut.ChatRVAdapter
import com.example.my4cut.PhotoData
import com.example.my4cut.PhotoRVAdapter
import com.example.my4cut.R
import com.example.my4cut.databinding.DialogChangeSpaceBinding
import com.example.my4cut.databinding.DialogExitBinding
import com.example.my4cut.databinding.DialogPhotoBinding
import com.example.my4cut.databinding.FragmentSpaceBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlin.text.clear


class SpaceFragment : Fragment(R.layout.fragment_space) {
    private lateinit var binding: FragmentSpaceBinding
    private lateinit var photoAdapter: PhotoRVAdapter
    private var photoDatas = ArrayList<PhotoData>()

    private lateinit var chatAdapter: ChatRVAdapter
    private var chatDatas = ArrayList<ChatData>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentSpaceBinding.bind(view)

        photoAdapter = PhotoRVAdapter(photoDatas)
        binding.rvPhotoList.adapter = photoAdapter
        binding.rvPhotoList.layoutManager = GridLayoutManager(requireContext(), 2)

        initDummyPhotos()

        val chatBinding = DialogPhotoBinding.inflate(layoutInflater)

        chatAdapter = ChatRVAdapter(chatDatas)
        chatBinding.rvChatList.adapter = chatAdapter
        chatBinding.rvChatList.layoutManager = LinearLayoutManager(requireContext())

        photoAdapter.onItemClickListener = { photo ->
            showPhotoDialog(photo)
        }

        chatAdapter.onItemClickListener = { chat ->
            showExitDialog()  //tvTitle.text = 정말 삭제하시겠어요?, tvMessage.text = 삭제한 댓글은 다시 복구할 수 없어요.
        }

        binding.btnExitMenu.setOnClickListener {
            showExitDialog()  //혼자일 때 -> tvMessage.text = 나가면 스페이스가 삭제되어 복구할 수 없어요.
        }

        chatBinding.ivClose.setOnClickListener {
            showExitDialog()  //tvTitle.text = 정말 삭제하시겠어요?, tvMessage.text = 삭제한 사진은 다시 복구할 수 없어요.
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
        }

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        dialog.show()
    }

    private fun showPhotoDialog(photo: PhotoData) {
        val dialogBinding = DialogPhotoBinding.inflate(layoutInflater)
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

        chatAdapter.notifyDataSetChanged()
    }
}
