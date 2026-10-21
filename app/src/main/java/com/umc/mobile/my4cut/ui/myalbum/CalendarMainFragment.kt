package com.umc.mobile.my4cut.ui.myalbum

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.tabs.TabLayoutMediator
import com.umc.mobile.my4cut.R
import com.umc.mobile.my4cut.data.network.RetrofitClient
import com.umc.mobile.my4cut.databinding.FragmentCalendarMainBinding
import com.umc.mobile.my4cut.databinding.ViewTabCustomBinding
import com.umc.mobile.my4cut.ui.home.HomeFragment
import com.umc.mobile.my4cut.ui.notification.NotificationActivity
import kotlinx.coroutines.launch

class CalendarMainFragment : Fragment() {
    lateinit var binding: FragmentCalendarMainBinding

    private val notificationReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == HomeFragment.ACTION_NOTIFICATION_RECEIVED) {
                binding.ivNotification.setImageResource(R.drawable.ic_noti_on)
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
        refreshAlbumTabIfPresent()
    }

    private fun refreshAlbumTabIfPresent() {
        childFragmentManager.fragments.forEach { fragment ->
            if (fragment is AlbumFragment) {
                fragment.refresh()
            }
        }
    }

    private fun registerNotificationReceiver() {
        val filter = IntentFilter(HomeFragment.ACTION_NOTIFICATION_RECEIVED)
        ContextCompat.registerReceiver(
            requireContext(),
            notificationReceiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    private fun unregisterNotificationReceiver() {
        try {
            requireContext().unregisterReceiver(notificationReceiver)
        } catch (_: IllegalArgumentException) {
        }
    }

    override fun onDestroyView() {
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
        val pagerAdapter = MyAlbumVPAdapter(this)
        binding.viewPager.adapter = pagerAdapter
        binding.viewPager.isUserInputEnabled = false

        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
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