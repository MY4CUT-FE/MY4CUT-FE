package com.umc.mobile.my4cut.ui.space

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.umc.mobile.my4cut.data.network.RetrofitClient
import com.umc.mobile.my4cut.data.workspace.model.WorkspaceUpdateRequestDto
import com.umc.mobile.my4cut.databinding.DialogSpaceEditNameBinding
import kotlinx.coroutines.launch
import retrofit2.HttpException

class EditSpaceNameDialogFragment : DialogFragment() {

    private var _binding: DialogSpaceEditNameBinding? = null
    private val binding get() = _binding!!

    private var spaceId: Long = -1L
    private var originalSpaceName: String = ""

    private var onEditCompleteListener: (() -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let { bundle ->
            spaceId = bundle.getLong(ARG_SPACE_ID, -1L)
            originalSpaceName = bundle.getString(ARG_SPACE_NAME).orEmpty()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        dialog?.window?.setBackgroundDrawable(
            ColorDrawable(Color.TRANSPARENT)
        )

        _binding = DialogSpaceEditNameBinding.inflate(
            inflater,
            container,
            false
        )

        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        // 기존 스페이스 이름 표시
        binding.etSpaceName.setText(originalSpaceName)

        // 커서를 텍스트 끝으로 이동
        binding.etSpaceName.setSelection(
            binding.etSpaceName.text.length
        )

        // 닫기 버튼
        binding.layoutClose.setOnClickListener {
            dismiss()
        }

        // 확인 버튼
        binding.mainText.setOnClickListener {
            updateSpaceName()
        }
    }

    /**
     * 스페이스 이름 수정 API
     */
    private fun updateSpaceName() {
        val newName = binding.etSpaceName.text.toString().trim()

        // 빈 이름 방지
        if (newName.isEmpty()) {
            Toast.makeText(
                requireContext(),
                "스페이스 이름을 입력해 주세요.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        // 기존 이름과 같으면 API 호출 없이 닫기
        if (newName == originalSpaceName) {
            dismiss()
            return
        }

        // 중복 요청 방지
        binding.mainText.isEnabled = false

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                RetrofitClient.workspaceService.updateWorkspace(
                    workspaceId = spaceId,
                    request = WorkspaceUpdateRequestDto(
                        name = newName
                    )
                )

                Toast.makeText(
                    requireContext(),
                    "스페이스를 수정했어요.",
                    Toast.LENGTH_SHORT
                ).show()

                // 수정 완료 후 화면 갱신
                onEditCompleteListener?.invoke()

                dismiss()

            } catch (e: HttpException) {
                Log.e(
                    "EditSpaceName",
                    "스페이스 이름 수정 실패",
                    e
                )

                when (e.code()) {
                    400 -> {
                        Toast.makeText(
                            requireContext(),
                            "스페이스 이름은 영어, 한글, 숫자만 가능하며 15자까지 입력할 수 있어요.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                    else -> {
                        Toast.makeText(
                            requireContext(),
                            "스페이스 수정에 실패했습니다. 다시 시도해 주세요.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

            } catch (e: Exception) {
                Log.e(
                    "EditSpaceName",
                    "스페이스 이름 수정 실패",
                    e
                )

                Toast.makeText(
                    requireContext(),
                    "스페이스 수정에 실패했습니다. 다시 시도해 주세요.",
                    Toast.LENGTH_SHORT
                ).show()

            } finally {
                _binding?.mainText?.isEnabled = true
            }
        }
    }

    override fun onStart() {
        super.onStart()

        dialog?.window?.apply {
            setLayout(
                (resources.displayMetrics.widthPixels * 0.9).toInt(),
                ViewGroup.LayoutParams.WRAP_CONTENT
            )

            setBackgroundDrawable(
                ColorDrawable(Color.TRANSPARENT)
            )
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    fun setOnEditCompleteListener(listener: () -> Unit) {
        onEditCompleteListener = listener
    }

    companion object {
        private const val ARG_SPACE_ID = "arg_space_id"
        private const val ARG_SPACE_NAME = "arg_space_name"

        fun newInstance(
            spaceId: Long,
            spaceName: String
        ): EditSpaceNameDialogFragment {
            return EditSpaceNameDialogFragment().apply {
                arguments = Bundle().apply {
                    putLong(ARG_SPACE_ID, spaceId)
                    putString(ARG_SPACE_NAME, spaceName)
                }
            }
        }
    }
}