package com.umc.mobile.my4cut

import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.umc.mobile.my4cut.databinding.ActivityMainBinding
import com.umc.mobile.my4cut.ui.home.HomeFragment
import com.umc.mobile.my4cut.ui.album.MyAlbumFragment
import com.umc.mobile.my4cut.ui.mypage.MyPageFragment
import com.umc.mobile.my4cut.ui.retouch.RetouchFragment
import com.umc.mobile.my4cut.ui.space.SpaceFragment

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initBottomNavigation(savedInstanceState)
    }

    private fun initBottomNavigation(savedInstanceState: Bundle?) {
        binding.bnvMain.itemIconTintList = null

        binding.bnvMain.post {
            fixBottomNavText()
        }

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fcv_main, HomeFragment())
                .commit()
        }

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

    private fun fixBottomNavText() {
        val menuView = binding.bnvMain.getChildAt(0) as? ViewGroup ?: return

        for (i in 0 until menuView.childCount) {
            val item = menuView.getChildAt(i) as? ViewGroup ?: continue

            val smallLabel =
                item.findViewById<TextView>(com.google.android.material.R.id.navigation_bar_item_small_label_view)
            val largeLabel =
                item.findViewById<TextView>(com.google.android.material.R.id.navigation_bar_item_large_label_view)

            smallLabel?.apply {
                setSingleLine(false)
                maxLines = 2
                gravity = Gravity.CENTER
            }

            largeLabel?.apply {
                setSingleLine(false)
                maxLines = 2
                gravity = Gravity.CENTER
            }
        }
    }

    private fun changeFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fcv_main, fragment)
            .commit()
    }

    /** SpaceFragment 열기 (리터치 → 스페이스 이동용) */
    fun openSpaceFragment() {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fcv_main, SpaceFragment())
            .addToBackStack(null)
            .commit()
    }
}
