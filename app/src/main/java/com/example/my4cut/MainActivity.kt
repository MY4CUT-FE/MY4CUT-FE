package com.example.my4cut

import android.os.Bundle
import android.content.Intent
import com.example.my4cut.databinding.ActivityMainBinding
import androidx.appcompat.app.AppCompatActivity
import android.widget.*
import android.app.Activity
import androidx.fragment.app.*
import androidx.activity.enableEdgeToEdge
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.my4cut.databinding.DialogChangeBinding
import com.example.my4cut.databinding.DialogExitBinding
import com.example.my4cut.databinding.DialogPhotoBinding
import com.example.my4cut.ui.theme.MY4CUTTheme
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class MainActivity : AppCompatActivity() {
    lateinit var binding: ActivityMainBinding
    private lateinit var photoAdapter: PhotoRVAdapter
    private var photoDatas = ArrayList<PhotoData>()

    private lateinit var chatAdapter: ChatRVAdapter
    private var chatDatas = ArrayList<ChatData>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        photoAdapter = PhotoRVAdapter(photoDatas)
        binding.rvPhotoList.adapter = photoAdapter
        binding.rvPhotoList.layoutManager = LinearLayoutManager(this)

        val chatBinding = DialogPhotoBinding.inflate(layoutInflater)

        chatAdapter = ChatRVAdapter(chatDatas)
        chatBinding.rvChatList.adapter = chatAdapter
        chatBinding.rvChatList.layoutManager = LinearLayoutManager(this)

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
        val builder = MaterialAlertDialogBuilder(this)
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
        val builder = MaterialAlertDialogBuilder(this)
            .setView(dialogBinding.root)
        val dialog = builder.create()

        dialog.setCanceledOnTouchOutside(true)

        dialogBinding.ivClose.setOnClickListener {
            dialog.dismiss()
        }

        dialogBinding.mainText.setOnClickListener {
            dialog.dismiss()
        }

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        dialog.show()
    }

    private fun showChangeDialog() {
        val dialogBinding = DialogChangeBinding.inflate(layoutInflater)
        val builder = MaterialAlertDialogBuilder(this)
            .setView(dialogBinding.root)
        val dialog = builder.create()

        dialog.setCanceledOnTouchOutside(true)

        dialogBinding.ivClose.setOnClickListener {
            dialog.dismiss()
        }

        dialogBinding.mainText.setOnClickListener {
            dialog.dismiss()
        }

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        dialog.show()
    }
}