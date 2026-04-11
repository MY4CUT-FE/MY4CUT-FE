package com.umc.mobile.my4cut.ui.booth

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.umc.mobile.my4cut.databinding.FragmentBoothBinding

class BoothFragment : Fragment() {

    private lateinit var binding: FragmentBoothBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentBoothBinding.inflate(inflater, container, false)
        return binding.root
    }
}