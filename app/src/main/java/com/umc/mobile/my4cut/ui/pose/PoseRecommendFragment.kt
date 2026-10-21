package com.umc.mobile.my4cut.ui.pose

import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.PopupWindow
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import com.bumptech.glide.Glide
import com.google.android.material.tabs.TabLayout
import com.umc.mobile.my4cut.MainActivity
import com.umc.mobile.my4cut.R
import com.umc.mobile.my4cut.data.base.BaseResponse
import com.umc.mobile.my4cut.databinding.DialogPoseDetailBinding
import com.umc.mobile.my4cut.databinding.FragmentPoseRecommendBinding
import com.umc.mobile.my4cut.databinding.PopupPoseFilterBinding
import com.umc.mobile.my4cut.data.network.RetrofitClient
import com.umc.mobile.my4cut.ui.notification.NotificationActivity
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class PoseRecommendFragment : Fragment() {

    private var _binding: FragmentPoseRecommendBinding? = null
    private val binding get() = _binding!!

    private lateinit var poseAdapter: PoseAdapter

    // 전체 데이터 리스트
    private val allPoseList = ArrayList<PoseData>()

    // 현재 선택된 필터 상태
    private var currentTabPosition = 0 // 0:전체, 1:1인, 2:2인, 3:3인, 4:4인
    private var isFavoriteFilterOn = false // false:기본순, true:즐겨찾기순

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPoseRecommendBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews()
        loadPosesFromServer() // 서버에서 데이터 로드
    }

    private fun initViews() {
        // 1. 상단 아이콘 클릭
        binding.ivMypage.setOnClickListener {
            (activity as? MainActivity)?.navigateToMyPage()
        }

        binding.ivNotification.setOnClickListener {
            startActivity(Intent(requireContext(), NotificationActivity::class.java))
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
                loadPosesFromServer() // 탭 변경 시 서버에서 다시 로드
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
        binding.rvPose.layoutManager = GridLayoutManager(requireContext(), 2)

        // 4. 필터 버튼
        binding.btnFilter.setOnClickListener { view ->
            showFilterPopup(view)
        }
    }

    // 포즈 상세 모달
    private fun showPoseDetailDialog(pose: PoseData, position: Int) {
        val dialog = Dialog(requireContext())
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
        dialogBinding.tvDialogPeopleCount.text = "${pose.peopleCount}인 추천 포즈"
        updateDialogStar(dialogBinding, pose.isFavorite)

        dialogBinding.ivDialogStar.setOnClickListener {
            toggleBookmark(pose, position) { finalState ->
                updateDialogStar(dialogBinding, finalState)
            }
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

    // 서버에서 포즈 목록 로드
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
                    if (_binding == null) return

                    if (response.isSuccessful) {
                        val poseList = response.body()?.data
                        if (poseList != null) {
                            Log.d("PoseRecommend", "✅ Loaded ${poseList.size} poses")
                            allPoseList.clear()
                            allPoseList.addAll(poseList)

                            // 서버가 sort=bookmark 파라미터를 받고도 실제로는 정렬해주지 않아서(백엔드
                            // 미구현 확인됨), 화면에 보여줄 때만 클라이언트에서 정렬한다. allPoseList
                            // 자체의 순서/인덱스는 서버가 준 그대로 유지하고, 즐겨찾기 등록/해제는 position이
                            // 아니라 poseId로 allPoseList에서 찾아 갱신하므로(아래 addBookmark/removeBookmark
                            // 참고) 화면 정렬 순서와 무관하게 항상 정확한 포즈가 갱신된다.
                            val displayList = if (sort == "bookmark") {
                                allPoseList.sortedByDescending { it.isFavorite }
                            } else {
                                allPoseList
                            }
                            poseAdapter.updateData(displayList)
                        } else {
                            Log.e("PoseRecommend", "❌ Data is null")
                            Toast.makeText(requireContext(), "데이터를 불러올 수 없습니다.", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Log.e("PoseRecommend", "❌ Failed: ${response.code()}")
                        val errorBody = response.errorBody()?.string()
                        Log.e("PoseRecommend", "Error Body: $errorBody")
                        Toast.makeText(requireContext(), "포즈 목록을 불러오지 못했습니다.", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<BaseResponse<List<PoseData>>>, t: Throwable) {
                    Log.e("PoseRecommend", "❌ Network Error", t)
                    if (_binding == null) return
                    Toast.makeText(requireContext(), "네트워크 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
                }
            })
    }

    // 즐겨찾기 토글
    private fun toggleBookmark(pose: PoseData, position: Int, onResult: ((Boolean) -> Unit)? = null) {
        if (pose.isFavorite) {
            removeBookmark(pose.poseId, position, onResult)
        } else {
            addBookmark(pose.poseId, position, onResult)
        }
    }

    // "즐겨찾기순" 표시를 위해 클라이언트에서 정렬을 하다 보니, 어댑터가 넘기는 position은 화면에 보이는
    // 순서일 뿐 allPoseList의 실제 인덱스와 다를 수 있다. 그래서 allPoseList는 항상 poseId로 찾아 갱신한다.
    private fun setLocalFavorite(poseId: Int, isFavorite: Boolean) {
        allPoseList.find { it.poseId == poseId }?.isFavorite = isFavorite
    }

    // 즐겨찾기 등록 (서버가 단일 소스 오브 트루스 - 화면은 낙관적으로 먼저 바꾸고, 실패하면 되돌림)
    private fun addBookmark(poseId: Int, position: Int, onResult: ((Boolean) -> Unit)? = null) {
        Log.d("PoseRecommend", "📤 Adding bookmark for poseId: $poseId")

        setLocalFavorite(poseId, true)
        poseAdapter.updateItem(position, true)

        RetrofitClient.poseService.addBookmark(poseId)
            .enqueue(object : Callback<BaseResponse<Any>> {
                override fun onResponse(
                    call: Call<BaseResponse<Any>>,
                    response: Response<BaseResponse<Any>>
                ) {
                    if (_binding == null) return
                    if (response.isSuccessful) {
                        Log.d("PoseRecommend", "✅ Bookmark synced to server")
                        Toast.makeText(requireContext(), "즐겨찾기에 추가되었습니다.", Toast.LENGTH_SHORT).show()
                        onResult?.invoke(true)
                    } else {
                        val errorBody = try { response.errorBody()?.string() } catch (e: Exception) { null }
                        Log.e("PoseRecommend", "❌ Server sync failed (${response.code()}), body=$errorBody, reverting")
                        setLocalFavorite(poseId, false)
                        poseAdapter.updateItem(position, false)
                        Toast.makeText(requireContext(), "즐겨찾기 등록에 실패했습니다.", Toast.LENGTH_SHORT).show()
                        onResult?.invoke(false)
                    }
                }

                override fun onFailure(call: Call<BaseResponse<Any>>, t: Throwable) {
                    Log.e("PoseRecommend", "❌ Network error, reverting", t)
                    if (_binding == null) return
                    setLocalFavorite(poseId, false)
                    poseAdapter.updateItem(position, false)
                    Toast.makeText(requireContext(), "네트워크 오류로 즐겨찾기에 실패했습니다.", Toast.LENGTH_SHORT).show()
                    onResult?.invoke(false)
                }
            })
    }

    // 즐겨찾기 해제 (서버가 단일 소스 오브 트루스 - 화면은 낙관적으로 먼저 바꾸고, 실패하면 되돌림)
    private fun removeBookmark(poseId: Int, position: Int, onResult: ((Boolean) -> Unit)? = null) {
        Log.d("PoseRecommend", "📤 Removing bookmark for poseId: $poseId")

        setLocalFavorite(poseId, false)
        poseAdapter.updateItem(position, false)

        RetrofitClient.poseService.removeBookmark(poseId)
            .enqueue(object : Callback<BaseResponse<Any>> {
                override fun onResponse(
                    call: Call<BaseResponse<Any>>,
                    response: Response<BaseResponse<Any>>
                ) {
                    if (_binding == null) return
                    if (response.isSuccessful) {
                        Log.d("PoseRecommend", "✅ Bookmark removal synced to server")
                        Toast.makeText(requireContext(), "즐겨찾기가 해제되었습니다.", Toast.LENGTH_SHORT).show()
                        onResult?.invoke(false)
                    } else {
                        val errorBody = try { response.errorBody()?.string() } catch (e: Exception) { null }
                        Log.e("PoseRecommend", "❌ Server sync failed (${response.code()}), body=$errorBody, reverting")
                        setLocalFavorite(poseId, true)
                        poseAdapter.updateItem(position, true)
                        Toast.makeText(requireContext(), "즐겨찾기 해제에 실패했습니다.", Toast.LENGTH_SHORT).show()
                        onResult?.invoke(true)
                    }
                }

                override fun onFailure(call: Call<BaseResponse<Any>>, t: Throwable) {
                    Log.e("PoseRecommend", "❌ Network error, reverting", t)
                    if (_binding == null) return
                    setLocalFavorite(poseId, true)
                    poseAdapter.updateItem(position, true)
                    Toast.makeText(requireContext(), "네트워크 오류로 즐겨찾기 해제에 실패했습니다.", Toast.LENGTH_SHORT).show()
                    onResult?.invoke(true)
                }
            })
    }

    // 필터 정렬 선택 드롭다운
    private fun showFilterPopup(anchor: View) {
        val popupBinding = PopupPoseFilterBinding.inflate(LayoutInflater.from(requireContext()))

        val popupWindow = PopupWindow(
            popupBinding.root,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        )
        popupWindow.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        popupWindow.elevation = 8f

        // 현재 선택된 정렬 기준 옆에 체크 표시
        popupBinding.ivCheckDefault.visibility = if (!isFavoriteFilterOn) View.VISIBLE else View.GONE
        popupBinding.ivCheckFavorite.visibility = if (isFavoriteFilterOn) View.VISIBLE else View.GONE

        popupBinding.rowSortDefault.setOnClickListener {
            isFavoriteFilterOn = false
            binding.tvFilterText.text = "기본순"
            loadPosesFromServer()
            popupWindow.dismiss()
        }

        popupBinding.rowSortFavorite.setOnClickListener {
            isFavoriteFilterOn = true
            binding.tvFilterText.text = "즐겨찾기순"
            loadPosesFromServer()
            popupWindow.dismiss()
        }

        popupBinding.root.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
        val xOffset = anchor.width - popupBinding.root.measuredWidth
        // 필터 버튼과 드롭다운 사이 간격
        val yOffset = (8 * resources.displayMetrics.density).toInt()

        // 필터 버튼 우측 하단에 맞춰 드롭다운 표시
        popupWindow.showAsDropDown(anchor, xOffset, yOffset)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
