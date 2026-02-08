package com.umc.mobile.my4cut.ui.pose

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.ContextThemeWrapper
import android.view.Gravity
import android.view.View
import android.widget.PopupMenu
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.tabs.TabLayout
import com.umc.mobile.my4cut.R
import com.umc.mobile.my4cut.data.base.BaseResponse
import com.umc.mobile.my4cut.databinding.ActivityPoseRecommendBinding
import com.umc.mobile.my4cut.network.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class PoseRecommendActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPoseRecommendBinding
    private lateinit var poseAdapter: PoseAdapter

    // 전체 데이터 리스트
    private val allPoseList = ArrayList<PoseData>()

    // 현재 선택된 필터 상태
    private var currentTabPosition = 0 // 0:전체, 1:1인, 2:2인, 3:3인, 4:4인
    private var isFavoriteFilterOn = false // false:기본순, true:즐겨찾기순

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPoseRecommendBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initViews()
        loadPosesFromServer() // ✅ 서버에서 데이터 로드
    }

    private fun initViews() {
        // 1. 뒤로가기
        binding.btnBack.setOnClickListener { finish() }

        // 2. 탭 설정
        val tabTitles = listOf("전체", "1인", "2인", "3인", "4인")
        tabTitles.forEach { title ->
            binding.tabLayout.addTab(binding.tabLayout.newTab().setText(title))
        }

        binding.tabLayout.setTabTextColors(Color.parseColor("#6A6A6A"), Color.parseColor("#FF7E67"))

        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                currentTabPosition = tab?.position ?: 0
                loadPosesFromServer() // ✅ 탭 변경 시 서버에서 다시 로드
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        // 3. 리사이클러뷰 설정
        poseAdapter = PoseAdapter(emptyList()) { pose, position ->
            // ✅ 즐겨찾기 클릭 시
            toggleBookmark(pose, position)
        }
        binding.rvPose.adapter = poseAdapter
        binding.rvPose.layoutManager = GridLayoutManager(this, 2)

        // 4. 필터 버튼
        binding.btnFilter.setOnClickListener { view ->
            showFilterPopup(view)
        }
    }

    // ✅ 서버에서 포즈 목록 로드
    private fun loadPosesFromServer() {
        val peopleCount = if (currentTabPosition == 0) null else currentTabPosition
        val sort = if (isFavoriteFilterOn) "bookmark" else null

        Log.d("PoseRecommend", "📤 Loading poses - peopleCount: $peopleCount, sort: $sort")

        RetrofitClient.poseService.getPoses(sort, peopleCount)
            .enqueue(object : Callback<BaseResponse<List<PoseData>>> {
                override fun onResponse(
                    call: Call<BaseResponse<List<PoseData>>>,
                    response: Response<BaseResponse<List<PoseData>>>
                ) {
                    Log.d("PoseRecommend", "📥 Response Code: ${response.code()}")

                    if (response.isSuccessful) {
                        val poseList = response.body()?.data
                        if (poseList != null) {
                            Log.d("PoseRecommend", "✅ Loaded ${poseList.size} poses")
                            allPoseList.clear()
                            allPoseList.addAll(poseList)
                            poseAdapter.updateData(allPoseList)
                        } else {
                            Log.e("PoseRecommend", "❌ Data is null")
                            Toast.makeText(this@PoseRecommendActivity, "데이터를 불러올 수 없습니다.", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Log.e("PoseRecommend", "❌ Failed: ${response.code()}")
                        val errorBody = response.errorBody()?.string()
                        Log.e("PoseRecommend", "Error Body: $errorBody")
                        Toast.makeText(this@PoseRecommendActivity, "포즈 목록을 불러오지 못했습니다.", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<BaseResponse<List<PoseData>>>, t: Throwable) {
                    Log.e("PoseRecommend", "❌ Network Error", t)
                    Toast.makeText(this@PoseRecommendActivity, "네트워크 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
                }
            })
    }

    // ✅ 즐겨찾기 토글
    private fun toggleBookmark(pose: PoseData, position: Int) {
        if (pose.isFavorite) {
            // 즐겨찾기 해제
            removeBookmark(pose.poseId, position)
        } else {
            // 즐겨찾기 등록
            addBookmark(pose.poseId, position)
        }
    }

    // ✅ 즐겨찾기 등록 API
    private fun addBookmark(poseId: Int, position: Int) {
        Log.d("PoseRecommend", "📤 Adding bookmark for poseId: $poseId")

        RetrofitClient.poseService.addBookmark(poseId)
            .enqueue(object : Callback<BaseResponse<String>> {
                override fun onResponse(
                    call: Call<BaseResponse<String>>,
                    response: Response<BaseResponse<String>>
                ) {
                    if (response.isSuccessful) {
                        Log.d("PoseRecommend", "✅ Bookmark added")
                        allPoseList[position].isFavorite = true
                        poseAdapter.updateItem(position, true)
                        Toast.makeText(this@PoseRecommendActivity, "즐겨찾기에 추가되었습니다.", Toast.LENGTH_SHORT).show()
                    } else {
                        Log.e("PoseRecommend", "❌ Add bookmark failed: ${response.code()}")
                        Toast.makeText(this@PoseRecommendActivity, "즐겨찾기 추가 실패", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<BaseResponse<String>>, t: Throwable) {
                    Log.e("PoseRecommend", "❌ Network error", t)
                    Toast.makeText(this@PoseRecommendActivity, "네트워크 오류", Toast.LENGTH_SHORT).show()
                }
            })
    }

    // ✅ 즐겨찾기 해제 API
    private fun removeBookmark(poseId: Int, position: Int) {
        Log.d("PoseRecommend", "📤 Removing bookmark for poseId: $poseId")

        RetrofitClient.poseService.removeBookmark(poseId)
            .enqueue(object : Callback<BaseResponse<String>> {
                override fun onResponse(
                    call: Call<BaseResponse<String>>,
                    response: Response<BaseResponse<String>>
                ) {
                    if (response.isSuccessful) {
                        Log.d("PoseRecommend", "✅ Bookmark removed")
                        allPoseList[position].isFavorite = false
                        poseAdapter.updateItem(position, false)
                        Toast.makeText(this@PoseRecommendActivity, "즐겨찾기가 해제되었습니다.", Toast.LENGTH_SHORT).show()
                    } else {
                        Log.e("PoseRecommend", "❌ Remove bookmark failed: ${response.code()}")
                        Toast.makeText(this@PoseRecommendActivity, "즐겨찾기 해제 실패", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<BaseResponse<String>>, t: Throwable) {
                    Log.e("PoseRecommend", "❌ Network error", t)
                    Toast.makeText(this@PoseRecommendActivity, "네트워크 오류", Toast.LENGTH_SHORT).show()
                }
            })
    }

    // 팝업 메뉴
    private fun showFilterPopup(view: View) {
        val contextWrapper = ContextThemeWrapper(this, R.style.FilterMenuTheme)
        val popup = PopupMenu(contextWrapper, view)
        popup.gravity = Gravity.END

        popup.menu.add(0, 0, 0, "기본순")
        popup.menu.add(0, 1, 0, "즐겨찾기순")

        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                0 -> {
                    isFavoriteFilterOn = false
                    binding.tvFilterText.text = "기본순"
                }
                1 -> {
                    isFavoriteFilterOn = true
                    binding.tvFilterText.text = "즐겨찾기순"
                }
            }
            loadPosesFromServer() // ✅ 필터 변경 시 다시 로드
            true
        }
        popup.show()
    }
}