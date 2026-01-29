package com.example.my4cut

import android.content.Intent
import android.os.Bundle
import com.example.my4cut.databinding.ActivityMainBinding
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.my4cut.ui.myalbum.CalendarData
import com.example.my4cut.ui.myalbum.CalendarMainFragment
import com.example.my4cut.ui.myalbum.EntryDetailFragment
import com.example.my4cut.ui.myalbum.HomeFragment
import com.example.my4cut.ui.myalbum.MyPageFragment
import com.example.my4cut.ui.myalbum.RetouchFragment

class MainActivity : AppCompatActivity() {
    lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initBottomNavigation()

        checkIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // 액티비티가 이미 켜져 있는 상태에서 신호를 받았을 때
        checkIntent(intent)
    }

    private fun checkIntent(intent: Intent?) {
        val target = intent?.getStringExtra("TARGET_FRAGMENT")
        if (target == "ENTRY_DETAIL") {
            val dateString = intent.getStringExtra("selected_date")
            val calendarData = intent.getSerializableExtra("calendar_data") as? CalendarData

            val fragment = EntryDetailFragment().apply {
                arguments = Bundle().apply {
                    putString("selected_date", dateString)
                    putSerializable("calendar_data", calendarData)
                }
            }

            changeFragment(fragment)
        }
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
            .replace(R.id.fragment_container, HomeFragment())
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
                    changeFragment(CalendarMainFragment())
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

    fun changeFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commitAllowingStateLoss()
    }
}