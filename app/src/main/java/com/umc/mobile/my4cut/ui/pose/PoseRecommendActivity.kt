package com.umc.mobile.my4cut.ui.pose

import android.graphics.Color
import android.os.Bundle
import android.view.ContextThemeWrapper
import android.view.Gravity
import android.view.View
import android.widget.PopupMenu
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.tabs.TabLayout
import com.umc.mobile.my4cut.R
import com.umc.mobile.my4cut.ui.pose.PoseData
import com.umc.mobile.my4cut.databinding.ActivityPoseRecommendBinding

class PoseRecommendActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPoseRecommendBinding
    private lateinit var poseAdapter: PoseAdapter

    // 전체 데이터 리스트 (가짜 데이터)
    private val allPoseList = ArrayList<PoseData>()

    // 현재 선택된 필터 상태
    private var currentTabPosition = 0 // 0:전체, 1:1인 ...
    private var isFavoriteFilterOn = false // false:기본순, true:즐겨찾기순

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPoseRecommendBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initDummyData() // 데이터 생성
        initViews()     // 뷰 초기화
        filterList()    // 초기 리스트 보여주기
    }

    private fun initViews() {
        // 1. 뒤로가기
        binding.btnBack.setOnClickListener { finish() }

        // 2. 탭 설정 (전체, 1인, 2인, 3인, 4인)
        val tabTitles = listOf("전체", "1인", "2인", "3인", "4인")
        tabTitles.forEach { title ->
            binding.tabLayout.addTab(binding.tabLayout.newTab().setText(title))
        }

        // 탭 색상 설정 (미선택: #6A6A6A, 선택: #FF7E67)
        binding.tabLayout.setTabTextColors(Color.parseColor("#6A6A6A"), Color.parseColor("#FF7E67"))

        // 탭 클릭 리스너
        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                currentTabPosition = tab?.position ?: 0
                filterList() // 탭 바뀔 때마다 필터링
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        // 3. 리사이클러뷰 설정
        poseAdapter = PoseAdapter(emptyList())
        binding.rvPose.adapter = poseAdapter
        binding.rvPose.layoutManager = GridLayoutManager(this, 2) // 2열 그리드

        // 4. 필터 버튼 (팝업 메뉴)
        binding.btnFilter.setOnClickListener { view ->
            showFilterPopup(view)
        }
    }

    // 팝업 메뉴 표시 함수
    private fun showFilterPopup(view: View) {
        val contextWrapper = ContextThemeWrapper(this, R.style.FilterMenuTheme)

        val popup = PopupMenu(contextWrapper, view)
        // 팝업 위치를 버튼의 오른쪽 끝에 맞춤
        popup.gravity = Gravity.END

        popup.menu.add(0, 0, 0, "기본순")
        popup.menu.add(0, 1, 0, "즐겨찾기순")

        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                0 -> { // 기본순
                    isFavoriteFilterOn = false
                    binding.tvFilterText.text = "기본순"
                }
                1 -> { // 즐겨찾기순
                    isFavoriteFilterOn = true
                    binding.tvFilterText.text = "즐겨찾기순"
                }
            }
            filterList() // 필터 바뀔 때마다 리스트 갱신
            true
        }
        popup.show()
    }

    // 조건(탭 + 필터)에 맞춰 리스트 갱신
    private fun filterList() {
        // 1차 필터: 인원수 (탭)
        val step1List = if (currentTabPosition == 0) {
            allPoseList // 전체
        } else {
            allPoseList.filter { it.peopleCount == currentTabPosition } // 1인=1, 2인=2 ...
        }

        // 2차 필터: 즐겨찾기 여부
        val finalList = if (isFavoriteFilterOn) {
            step1List.filter { it.isFavorite }
        } else {
            step1List
        }

        poseAdapter.updateData(finalList)
    }

    // 테스트용 더미 데이터 생성
    private fun initDummyData() {
        for (i in 1..20) {
            // 1~4명 랜덤 배정
            val people = (i % 4) + 1
            allPoseList.add(PoseData(i, "포즈 이름 $i", people, false))
        }
    }
}