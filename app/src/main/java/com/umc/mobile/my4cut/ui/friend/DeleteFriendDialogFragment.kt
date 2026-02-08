package com.umc.mobile.my4cut.ui.friend

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.umc.mobile.my4cut.databinding.DialogDeleteFriendBinding
import com.umc.mobile.my4cut.network.RetrofitClient
import kotlinx.coroutines.launch

class DeleteFriendDialogFragment(
    private val friendId: Long,
    private val onConfirm: () -> Unit
) : DialogFragment() {

    private var _binding: DialogDeleteFriendBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        dialog?.window?.setBackgroundDrawable(ColorDrawable(0))
        _binding = DialogDeleteFriendBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnCancel.setOnClickListener {
            dismiss()
        }

        binding.btnExit.setOnClickListener {
            lifecycleScope.launch {
                try {
                    val response = RetrofitClient.friendService.deleteFriend(friendId)

                    if (response.code.startsWith("C2")) {
                        Toast.makeText(requireContext(), "친구가 삭제되었어요", Toast.LENGTH_SHORT).show()
                        onConfirm()
                        dismiss()
                    } else {
                        Toast.makeText(requireContext(), response.message, Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), "네트워크 오류가 발생했어요", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onStart() {
        super.onStart()

        dialog?.window?.apply {
            setLayout(
                (resources.displayMetrics.widthPixels * 0.9).toInt(),
                ViewGroup.LayoutParams.WRAP_CONTENT,
            )
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }
    }
}