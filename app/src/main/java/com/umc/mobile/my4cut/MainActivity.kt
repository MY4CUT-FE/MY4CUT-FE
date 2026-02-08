package com.umc.mobile.my4cut

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.umc.mobile.my4cut.data.network.RetrofitClient
import com.umc.mobile.my4cut.databinding.ActivityMainBinding
import com.umc.mobile.my4cut.ui.home.HomeFragment
import com.umc.mobile.my4cut.ui.myalbum.CalendarData
import com.umc.mobile.my4cut.ui.myalbum.CalendarMainFragment
import com.umc.mobile.my4cut.ui.myalbum.EntryDetailFragment
import com.umc.mobile.my4cut.ui.mypage.MyPageFragment
import com.umc.mobile.my4cut.ui.retouch.RetouchFragment
import com.umc.mobile.my4cut.ui.space.SpaceFragment

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        RetrofitClient.init(this)

        // 바텀 네비게이션 초기화 및 리스너 설정
        initBottomNavigation(savedInstanceState)

        // 외부에서 들어온 인텐트가 있는지 확인 (예: 앨범 상세 보기 등)
        checkIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // 액티비티가 이미 켜져 있는 상태에서 새로운 인텐트를 받았을 때 처리
        checkIntent(intent)
    }

    // 인텐트 처리 로직 (EntryDetailFragment 이동 등)
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
            // 상세 화면으로 프래그먼트 교체
            changeFragment(fragment)
        }
    }

    private fun initBottomNavigation(savedInstanceState: Bundle?) {
        // 아이콘 원래 색상 적용 (Tint 해제)
        binding.bnvMain.itemIconTintList = null

        // 라벨 2줄 허용 및 중앙 정렬 적용
        binding.bnvMain.post {
            fixBottomNavText()
        }

        // 앱이 처음 실행되었을 때만 홈 프래그먼트 로드 (화면 회전 등 재생성 시에는 상태 유지)
        if (savedInstanceState == null) {
            changeFragment(HomeFragment())
        }

        // 네비게이션 바 클릭 리스너
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

    // 바텀 네비게이션 텍스트 2줄 허용 및 중앙 정렬
    private fun fixBottomNavText() {
        val menuView = binding.bnvMain.getChildAt(0) as? ViewGroup ?: return

        for (i in 0 until menuView.childCount) {
            val item = menuView.getChildAt(i) as? ViewGroup ?: continue

            val smallLabel = item.findViewById<TextView>(com.google.android.material.R.id.navigation_bar_item_small_label_view)
            val largeLabel = item.findViewById<TextView>(com.google.android.material.R.id.navigation_bar_item_large_label_view)

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

    // 프래그먼트 교체 헬퍼 함수
    fun changeFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fcv_main, fragment) // 주의: activity_main.xml의 ID가 fcv_main인지 확인하세요!
            .commitAllowingStateLoss()
    }

    /** SpaceFragment 열기 (리터치 → 스페이스 이동용 외부 호출 함수) */
    fun openSpaceFragment() {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fcv_main, SpaceFragment())
            .addToBackStack(null)
            .commitAllowingStateLoss()
    }
}