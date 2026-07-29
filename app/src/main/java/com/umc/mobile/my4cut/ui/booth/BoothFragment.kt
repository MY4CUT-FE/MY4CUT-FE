package com.umc.mobile.my4cut.ui.booth

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.umc.mobile.my4cut.MainActivity
import com.umc.mobile.my4cut.databinding.FragmentBoothBinding

class BoothFragment : Fragment() {

    private var _binding: FragmentBoothBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBoothBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.btnBack.setOnClickListener {
            (activity as? MainActivity)?.selectHomeTab()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
