package com.umc.mobile.my4cut

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.umc.mobile.my4cut.databinding.ActivityMainBinding
import com.umc.mobile.my4cut.ui.home.HomeFragment
import com.umc.mobile.my4cut.ui.mypage.MyPageFragment
import com.umc.mobile.my4cut.ui.retouch.RetouchFragment
import com.umc.mobile.my4cut.ui.album.MyAlbumFragment
import com.umc.mobile.my4cut.R

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initBottomNavigation()
    }

    private fun fixBottomNavText() {
        val bottomNavigationMenuView = binding.bnvMain.getChildAt(0) as? android.view.ViewGroup ?: return

        for (i in 0 until bottomNavigationMenuView.childCount) {
            val item = bottomNavigationMenuView.getChildAt(i) as? android.view.ViewGroup ?: continue

            // 내부의 텍스트 뷰들 찾는 로직
            val smallLabel = item.findViewById<android.widget.TextView>(com.google.android.material.R.id.navigation_bar_item_small_label_view)
            val largeLabel = item.findViewById<android.widget.TextView>(com.google.android.material.R.id.navigation_bar_item_large_label_view)

            // 두 줄 허용
            smallLabel?.setSingleLine(false)
            smallLabel?.setLines(2)
            // 중앙 정렬
            smallLabel?.gravity = android.view.Gravity.CENTER

            largeLabel?.setSingleLine(false)
            largeLabel?.setLines(2)
            largeLabel?.gravity = android.view.Gravity.CENTER
        }
    }
    private fun initBottomNavigation() {
        binding.bnvMain.itemIconTintList = null
        binding.bnvMain.post {
            fixBottomNavText()
        }

        supportFragmentManager.beginTransaction()
            .replace(R.id.fcv_main, HomeFragment())
            .commitAllowingStateLoss()

        // 네비게이션 바
        binding.bnvMain.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.menu_home -> {
                    changeFragment(HomeFragment())
                    true
                }
                R.id.menu_retouch -> {
                    changeFragment(RetouchFragment())
                    true
                }
                R.id.menu_album -> {
                    changeFragment(MyAlbumFragment())
                    true
                }
                R.id.menu_mypage -> {
                    changeFragment(MyPageFragment())
                    true
                }
                else -> false
            }
        }
    }

    private fun changeFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fcv_main, fragment)
            .commitAllowingStateLoss()
    }
}