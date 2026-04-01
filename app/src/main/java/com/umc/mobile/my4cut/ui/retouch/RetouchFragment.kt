package com.umc.mobile.my4cut.ui.retouch

import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

import android.content.Intent

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.umc.mobile.my4cut.R
import com.umc.mobile.my4cut.databinding.FragmentRetouchBinding
import com.umc.mobile.my4cut.ui.friend.FriendsFragment
import com.umc.mobile.my4cut.ui.space.MySpaceFragment
import com.umc.mobile.my4cut.ui.notification.NotificationActivity
import com.umc.mobile.my4cut.network.RetrofitClient
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.ActivityResultLauncher

class RetouchFragment : Fragment(R.layout.fragment_retouch) {

    private var _binding: FragmentRetouchBinding? = null
    private val binding get() = _binding!!

    private lateinit var notificationLauncher: ActivityResultLauncher<Intent>

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentRetouchBinding.bind(view)

        notificationLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) {
            updateNotificationIcon()
        }

        binding.ivNotification.setOnClickListener {
            val intent = Intent(requireContext(), NotificationActivity::class.java)
            notificationLauncher.launch(intent)
        }

        // 최초 로드
        loadChildFragments()

        // 아래로 당겨 새로고침
        binding.swipeRefresh.setOnRefreshListener {
            loadChildFragments()
            binding.swipeRefresh.isRefreshing = false
        }
        updateNotificationIcon()
    }

    private fun loadChildFragments() {
        childFragmentManager.beginTransaction()
            .replace(R.id.containerMySpace, MySpaceFragment())
            .replace(R.id.containerFriends, FriendsFragment())
            .commit()
    }

    override fun onResume() {
        super.onResume()
        updateNotificationIcon()
    }

    private fun updateNotificationIcon() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = RetrofitClient.notificationService.getUnreadStatus()
                val hasUnread = response.data?.hasUnread == true

                binding.ivNotification.setImageResource(
                    if (hasUnread) R.drawable.ic_noti_on
                    else R.drawable.ic_noti_off
                )
            } catch (e: Exception) {
                binding.ivNotification.setImageResource(R.drawable.ic_noti_off)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}