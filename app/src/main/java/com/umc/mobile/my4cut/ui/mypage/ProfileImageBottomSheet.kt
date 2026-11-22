package com.umc.mobile.my4cut.ui.mypage

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.umc.mobile.my4cut.databinding.DialogProfileImageOptionBinding

/**
 * 프로필 이미지 수정 옵션 BottomSheet
 * - 갤러리에서 불러오기
 * - 현재 이미지 삭제
 */
class ProfileImageBottomSheet : BottomSheetDialogFragment() {

    private var _binding: DialogProfileImageOptionBinding? = null
    private val binding get() = _binding!!

    private var onSelectGallery: (() -> Unit)? = null
    private var onSelectDelete: (() -> Unit)? = null

    override fun onStart() {
        super.onStart()
        // 레이아웃의 상단 둥근 배경(bg_bottom_sheet_rounded)이 보이도록 기본 배경 제거
        dialog?.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            ?.setBackgroundResource(android.R.color.transparent)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogProfileImageOptionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.tvOptionGallery.setOnClickListener {
            onSelectGallery?.invoke()
            dismiss()
        }

        binding.tvOptionDelete.setOnClickListener {
            onSelectDelete?.invoke()
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance(
            onSelectGallery: () -> Unit,
            onSelectDelete: () -> Unit
        ): ProfileImageBottomSheet {
            return ProfileImageBottomSheet().apply {
                this.onSelectGallery = onSelectGallery
                this.onSelectDelete = onSelectDelete
            }
        }
    }
}
