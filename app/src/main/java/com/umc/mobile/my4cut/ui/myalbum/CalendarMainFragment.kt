
package com.umc.mobile.my4cut.ui.myalbum
import androidx.lifecycle.lifecycleScope
import com.umc.mobile.my4cut.R
import kotlinx.coroutines.launch
import android.content.Intent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import com.umc.mobile.my4cut.ui.home.HomeFragment
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.umc.mobile.my4cut.databinding.FragmentCalendarMainBinding
import com.umc.mobile.my4cut.databinding.ViewTabCustomBinding
import com.google.android.material.tabs.TabLayoutMediator
import com.umc.mobile.my4cut.network.RetrofitClient
import com.umc.mobile.my4cut.ui.notification.NotificationActivity

class CalendarMainFragment : Fragment() {
    lateinit var binding: FragmentCalendarMainBinding

    // FCM 푸시가 도착하면 마이앨범 화면의 알림 아이콘도 즉시 갱신
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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentCalendarMainBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupTabs()

        updateNotificationIcon()
        // CalendarMainFragment가 살아있는 동안 푸시 수신 이벤트를 감지
        registerNotificationReceiver()

        binding.ivNotification.setOnClickListener {
            startActivity(Intent(requireContext(), NotificationActivity::class.java))
        }

        binding.ivMypage.setOnClickListener {
            (requireActivity() as? com.umc.mobile.my4cut.MainActivity)
                ?.navigateToMyPage()
        }
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

    override fun onDestroyView() {
        // 메모리 누수 방지를 위해 등록한 Receiver 해제
        unregisterNotificationReceiver()
        super.onDestroyView()
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

    private fun setupTabs() {
        // 1. 어댑터 연결
        val pagerAdapter = MyAlbumVPAdapter(this)
        binding.viewPager.adapter = pagerAdapter

        binding.viewPager.isUserInputEnabled = false

        // 2. TabLayoutMediator 연결 (탭 생성 + 뷰페이저 연동)
        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            // 탭 마다 커스텀 레이아웃 입히기
            val tabBinding = ViewTabCustomBinding.inflate(layoutInflater)
            if (position == 0) {
                tabBinding.tvTabSub.text = "CALENDAR"
                tabBinding.tvTabMain.text = "캘린더"
            } else {
                tabBinding.tvTabSub.text = "ALBUM"
                tabBinding.tvTabMain.text = "앨범"
            }
            tab.customView = tabBinding.root
        }.attach()
    }
}

class MyAlbumVPAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {
    override fun getItemCount(): Int = 2

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> CalendarChildFragment()
            1 -> AlbumFragment()
            else -> throw IllegalArgumentException("Invalid position")
        }
    }
}