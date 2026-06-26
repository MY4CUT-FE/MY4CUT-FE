package com.umc.mobile.my4cut.ui.pose

import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.ContextThemeWrapper
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.bumptech.glide.Glide
import com.google.android.material.tabs.TabLayout
import com.umc.mobile.my4cut.MainActivity
import com.umc.mobile.my4cut.R
import com.umc.mobile.my4cut.data.base.BaseResponse
import com.umc.mobile.my4cut.databinding.ActivityPoseRecommendBinding
import com.umc.mobile.my4cut.databinding.DialogPoseDetailBinding
import com.umc.mobile.my4cut.network.RetrofitClient
import com.umc.mobile.my4cut.ui.notification.NotificationActivity
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
        // 1. 상단 아이콘 클릭
        binding.ivMypage.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra("NAVIGATE_TO_TAB", R.id.menu_home)
                putExtra("NAVIGATE_TO_MYPAGE", true)
            }
            startActivity(intent)
            finish()
        }

        binding.ivNotification.setOnClickListener {
            startActivity(Intent(this, NotificationActivity::class.java))
        }

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
        poseAdapter = PoseAdapter(
            emptyList(),
            onBookmarkClick = { pose, position ->
                toggleBookmark(pose, position)
            },
            onItemClick = { pose, position ->
                showPoseDetailDialog(pose, position)
            }
        )
        binding.rvPose.adapter = poseAdapter
        binding.rvPose.layoutManager = GridLayoutManager(this, 2)

        // 4. 필터 버튼
        binding.btnFilter.setOnClickListener { view ->
            showFilterPopup(view)
        }

        // 5. 바텀 네비게이션
        setupBottomNavigation()
    }

    private fun setupBottomNavigation() {
        binding.bnvPose.itemIconTintList = null
        binding.bnvPose.selectedItemId = R.id.menu_pose

        binding.bnvPose.post { fixBottomNavText() }

        binding.bnvPose.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.menu_pose -> true
                else -> {
                    val intent = Intent(this, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                        putExtra("NAVIGATE_TO_TAB", item.itemId)
                    }
                    startActivity(intent)
                    finish()
                    false
                }
            }
        }
    }

    private fun fixBottomNavText() {
        val menuView = binding.bnvPose.getChildAt(0) as? ViewGroup ?: return
        for (i in 0 until menuView.childCount) {
            val item = menuView.getChildAt(i) as? ViewGroup ?: continue
            val smallLabel = item.findViewById<TextView>(com.google.android.material.R.id.navigation_bar_item_small_label_view)
            val largeLabel = item.findViewById<TextView>(com.google.android.material.R.id.navigation_bar_item_large_label_view)
            smallLabel?.apply { setSingleLine(false); maxLines = 2; gravity = Gravity.CENTER }
            largeLabel?.apply { setSingleLine(false); maxLines = 2; gravity = Gravity.CENTER }
        }
    }

    // ✅ 포즈 상세 모달
    private fun showPoseDetailDialog(pose: PoseData, position: Int) {
        val dialog = Dialog(this)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val dialogBinding = DialogPoseDetailBinding.inflate(layoutInflater)
        dialog.setContentView(dialogBinding.root)

        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.85).toInt(),
            WindowManager.LayoutParams.WRAP_CONTENT
        )
        dialog.window?.setDimAmount(0.5f)
        dialog.window?.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)

        Glide.with(this)
            .load(pose.imageUrl)
            .placeholder(R.drawable.img_profile_default)
            .error(R.drawable.img_profile_default)
            .into(dialogBinding.ivDialogPose)

        dialogBinding.tvDialogPoseName.text = pose.title
        updateDialogStar(dialogBinding, pose.isFavorite)

        dialogBinding.ivDialogStar.setOnClickListener {
            toggleBookmark(pose, position)
            updateDialogStar(dialogBinding, pose.isFavorite)
        }

        dialogBinding.ivDialogClose.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun updateDialogStar(dialogBinding: DialogPoseDetailBinding, isFavorite: Boolean) {
        if (isFavorite) {
            dialogBinding.ivDialogStar.setImageResource(R.drawable.ic_star_on)
            dialogBinding.ivDialogStar.setColorFilter(Color.parseColor("#FFD83C"), PorterDuff.Mode.SRC_IN)
        } else {
            dialogBinding.ivDialogStar.setImageResource(R.drawable.ic_star_off)
            dialogBinding.ivDialogStar.clearColorFilter()
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

                            // ✅ 로컬 즐겨찾기 상태 적용
                            allPoseList.forEach { pose ->
                                pose.isFavorite = BookmarkManager.isBookmarked(this@PoseRecommendActivity, pose.poseId)
                            }

                            // ✅ 즐겨찾기순 필터링 (클라이언트에서 처리)
                            val filteredList = if (sort == "bookmark") {
                                allPoseList.filter { it.isFavorite }
                            } else {
                                allPoseList
                            }

                            poseAdapter.updateData(filteredList)
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
            removeBookmark(pose.poseId, position)
        } else {
            addBookmark(pose.poseId, position)
        }
    }

    // ✅ 즐겨찾기 등록 (로컬 우선)
    private fun addBookmark(poseId: Int, position: Int) {
        Log.d("PoseRecommend", "📤 Adding bookmark for poseId: $poseId")

        allPoseList[position].isFavorite = true
        poseAdapter.updateItem(position, true)
        BookmarkManager.addBookmark(this, poseId)
        Toast.makeText(this, "즐겨찾기에 추가되었습니다.", Toast.LENGTH_SHORT).show()

        RetrofitClient.poseService.addBookmark(poseId)
            .enqueue(object : Callback<BaseResponse<Any>> {
                override fun onResponse(
                    call: Call<BaseResponse<Any>>,
                    response: Response<BaseResponse<Any>>
                ) {
                    if (response.isSuccessful) {
                        Log.d("PoseRecommend", "✅ Bookmark synced to server")
                    } else {
                        Log.e("PoseRecommend", "⚠️ Server sync failed (${response.code()}), but local state saved")
                    }
                }

                override fun onFailure(call: Call<BaseResponse<Any>>, t: Throwable) {
                    Log.e("PoseRecommend", "⚠️ Network error, but local state saved", t)
                }
            })
    }

    // ✅ 즐겨찾기 해제 (로컬 우선)
    private fun removeBookmark(poseId: Int, position: Int) {
        Log.d("PoseRecommend", "📤 Removing bookmark for poseId: $poseId")

        allPoseList[position].isFavorite = false
        poseAdapter.updateItem(position, false)
        BookmarkManager.removeBookmark(this, poseId)
        Toast.makeText(this, "즐겨찾기가 해제되었습니다.", Toast.LENGTH_SHORT).show()

        RetrofitClient.poseService.removeBookmark(poseId)
            .enqueue(object : Callback<BaseResponse<Any>> {
                override fun onResponse(
                    call: Call<BaseResponse<Any>>,
                    response: Response<BaseResponse<Any>>
                ) {
                    if (response.isSuccessful) {
                        Log.d("PoseRecommend", "✅ Bookmark removal synced to server")
                    } else {
                        Log.e("PoseRecommend", "⚠️ Server sync failed (${response.code()}), but local state saved")
                    }
                }

                override fun onFailure(call: Call<BaseResponse<Any>>, t: Throwable) {
                    Log.e("PoseRecommend", "⚠️ Network error, but local state saved", t)
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
            loadPosesFromServer()
            true
        }
        popup.show()
    }
}
