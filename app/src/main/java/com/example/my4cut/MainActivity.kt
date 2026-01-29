package com.example.my4cut

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.my4cut.R
import com.example.my4cut.fragment.HomeFragment
import com.example.my4cut.fragment.MyAlbumFragment
import com.example.my4cut.fragment.MyPageFragment
import com.example.my4cut.fragment.RetouchFragment
import com.example.my4cut.fragment.SpaceFragment
import com.example.my4cut.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initBottomNavigation(savedInstanceState)
    }

    private fun initBottomNavigation(savedInstanceState: Bundle?) {
        // Only add the initial fragment if not restoring from a previous state
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

    private fun changeFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fcv_main, fragment)
            .commit()
    }

    fun openSpaceFragment() {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fcv_main, SpaceFragment())
            .addToBackStack(null)
            .commit()
    }
}