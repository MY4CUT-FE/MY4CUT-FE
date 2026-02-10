package com.umc.mobile.my4cut.ui.myalbum

import android.content.Intent
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
import com.umc.mobile.my4cut.ui.notification.NotificationActivity

class CalendarMainFragment : Fragment() {
    lateinit var binding: FragmentCalendarMainBinding

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

        binding.ivNotification.setOnClickListener {
            startActivity(Intent(requireContext(), NotificationActivity::class.java))
        }
    }

    private fun setupTabs() {
        // 1. 어댑터 연결
        val pagerAdapter = MyAlbumVPAdapter(this)
        binding.viewPager.adapter = pagerAdapter

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