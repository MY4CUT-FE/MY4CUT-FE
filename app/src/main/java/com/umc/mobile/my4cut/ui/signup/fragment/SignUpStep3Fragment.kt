package com.umc.mobile.my4cut.ui.signup.fragment

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.umc.mobile.my4cut.MainActivity
import com.umc.mobile.my4cut.databinding.FragmentSignUpStep3Binding

class SignUpStep3Fragment : Fragment() {

    private var _binding: FragmentSignUpStep3Binding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSignUpStep3Binding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initClickListener()
        initTextWatcher()
    }

    private fun initClickListener() {
        binding.btnBack.setOnClickListener { parentFragmentManager.popBackStack() }

        // [완료] 버튼 클릭 시 -> 홈 화면(MainActivity)으로 이동
        binding.btnNext.setOnClickListener {
            val intent = Intent(requireContext(), MainActivity::class.java)
            startActivity(intent)

            // 회원가입 액티비티 종료 (뒤로가기 눌러도 다시 못 돌아오게)
            requireActivity().finish()
        }
    }

    private fun initTextWatcher() {
        binding.etNickname.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                // 닉네임이 비어있지 않으면 버튼 활성화
                binding.btnNext.isEnabled = s.toString().isNotEmpty()
            }
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}