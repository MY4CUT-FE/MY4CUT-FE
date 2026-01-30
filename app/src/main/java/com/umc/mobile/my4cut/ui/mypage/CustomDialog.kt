package com.umc.mobile.my4cut.ui.mypage

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Window
import com.umc.mobile.my4cut.databinding.DialogCustomBinding

class CustomDialog(
    context: Context,
    private val title: String,
    private val subtitle: String,
    private val actionText: String,
    private val onActionClick: () -> Unit
) : Dialog(context) {

    private lateinit var binding: DialogCustomBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        binding = DialogCustomBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 배경 투명하게
        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        // 텍스트 세팅
        binding.tvTitle.text = title
        binding.tvSubtitle.text = subtitle
        binding.btnAction.text = actionText

        // 클릭 리스너
        binding.btnCancel.setOnClickListener { dismiss() }
        binding.btnAction.setOnClickListener {
            onActionClick()
            dismiss()
        }
    }

    override fun onStart() {
        super.onStart()
        val window = window ?: return
        val params = window.attributes
        // 화면 너비의 90% 크기로 설정
        params.width = (context.resources.displayMetrics.widthPixels * 0.9).toInt()
        window.attributes = params
    }
}