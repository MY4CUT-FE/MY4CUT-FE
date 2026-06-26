package com.umc.mobile.my4cut.ui.retouch

import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

import android.content.Intent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import com.umc.mobile.my4cut.ui.home.HomeFragment

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

    // FCM 푸시가 도착하면 리터치 화면의 알림 아이콘도 즉시 갱신
    private val notificationReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == HomeFragment.ACTION_NOTIFICATION_RECEIVED) {
                // 푸시 도착 직후 사용자가 바로 알 수 있도록 먼저 ON 아이콘으로 변경
                binding.ivNotification.setImageResource(R.drawable.ic_noti_on)

                // 알림창에 시스템 알림이 남아있어도, 서버 기준으로 전부 읽음이면 OFF 처리되도록 동기화
                updateNotificationIcon()
            }
        }
    }

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

        binding.ivMypage.setOnClickListener {
            (requireActivity() as? com.umc.mobile.my4cut.MainActivity)
                ?.navigateToMyPage()
        }

        // 최초 로드
        loadChildFragments()

        // 아래로 당겨 새로고침
        binding.swipeRefresh.setOnRefreshListener {
            loadChildFragments()
            binding.swipeRefresh.isRefreshing = false
        }
        updateNotificationIcon()
        // RetouchFragment가 살아있는 동안 푸시 수신 이벤트를 감지
        registerNotificationReceiver()
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

    // FCM 수신 브로드캐스트 Receiver 등록
    private fun registerNotificationReceiver() {
        val filter = IntentFilter(HomeFragment.ACTION_NOTIFICATION_RECEIVED)

        ContextCompat.registerReceiver(
            requireContext(),
            notificationReceiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    // Fragment View가 파괴될 때 Receiver 해제
    private fun unregisterNotificationReceiver() {
        try {
            requireContext().unregisterReceiver(notificationReceiver)
        } catch (_: IllegalArgumentException) {
            // 이미 해제된 경우 앱이 죽지 않도록 무시
        }
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
        // 메모리 누수 방지를 위해 등록한 Receiver 해제
        unregisterNotificationReceiver()
        super.onDestroyView()
        _binding = null
    }
}