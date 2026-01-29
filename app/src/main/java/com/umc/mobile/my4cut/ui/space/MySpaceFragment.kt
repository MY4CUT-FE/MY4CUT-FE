package com.umc.mobile.my4cut.ui.space

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.umc.mobile.my4cut.R
import com.umc.mobile.my4cut.databinding.FragmentMySpaceBinding

class MySpaceFragment : Fragment() {

    private var _binding: FragmentMySpaceBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMySpaceBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 스페이스 생성하기 버튼 클릭 -> 모달 표시
        binding.tvAddSpace.setOnClickListener {
            val dialog = CreateSpaceDialogFragment()
            dialog.show(parentFragmentManager, "CreateSpaceDialog")
        }

        binding.viewOrangeCircle.setOnClickListener {
            requireActivity().supportFragmentManager.beginTransaction()
                .replace(R.id.fcv_main, SpaceFragment())
                .addToBackStack(null)
                .commit()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}