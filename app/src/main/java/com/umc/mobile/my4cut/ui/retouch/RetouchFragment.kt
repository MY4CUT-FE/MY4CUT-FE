package com.umc.mobile.my4cut.ui.retouch

import android.content.Intent

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.umc.mobile.my4cut.R
import com.umc.mobile.my4cut.databinding.FragmentRetouchBinding
import com.umc.mobile.my4cut.ui.friend.FriendsFragment
import com.umc.mobile.my4cut.ui.space.MySpaceFragment
import com.umc.mobile.my4cut.ui.notification.NotificationActivity

class RetouchFragment : Fragment(R.layout.fragment_retouch) {

    private var _binding: FragmentRetouchBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentRetouchBinding.bind(view)

        binding.ivNotification.setOnClickListener {
            val intent = Intent(requireContext(), NotificationActivity::class.java)
            startActivity(intent)
        }

        // 자식 Fragment 삽입 (MySpace / Friends)
        childFragmentManager.beginTransaction()
            .replace(R.id.containerMySpace, MySpaceFragment())
            .replace(R.id.containerFriends, FriendsFragment())
            .commit()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}