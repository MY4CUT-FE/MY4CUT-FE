package com.umc.mobile.my4cut

import com.umc.mobile.my4cut.ui.pose.PoseRecommendFragment
import com.umc.mobile.my4cut.ui.space.SpaceFragment
import android.content.Intent
import android.graphics.Color
import android.graphics.Rect
import android.graphics.RectF
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.FrameLayout
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.view.WindowCompat
import androidx.core.widget.NestedScrollView
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.umc.mobile.my4cut.data.auth.local.TokenManager
import com.umc.mobile.my4cut.data.tutorial.TutorialManager
import com.umc.mobile.my4cut.data.tutorial.model.TutorialType
import com.umc.mobile.my4cut.databinding.ActivityMainBinding
import com.umc.mobile.my4cut.ui.home.HomeFragment
import com.umc.mobile.my4cut.ui.myalbum.CalendarData
import com.umc.mobile.my4cut.ui.myalbum.CalendarMainFragment
import com.umc.mobile.my4cut.ui.myalbum.EntryDetailFragment
import com.umc.mobile.my4cut.ui.mypage.MyPageFragment
import com.umc.mobile.my4cut.ui.photo.PhotoDialogFragment
import com.umc.mobile.my4cut.ui.retouch.RetouchFragment
import kotlinx.coroutines.launch

private object HomeTutorialLayout {
    const val MYPAGE_TEXT_GAP_DP = 12          // 마이페이지 안내 텍스트 ↔ 아이콘 간격
    const val MYPAGE_ARROW_ICON_GAP_DP = -12    // 마이페이지 화살표 끝(뾰족한 쪽) ↔ 아이콘 간격 (아이콘 기준)
    const val MYPAGE_ARROW_OFFSET_Y_DP = 8   // 마이페이지 화살표의 텍스트 기준 세로 오프셋
    const val MYPAGE_ARROW_ROTATION = 0f    // 마이페이지 화살표 회전 각도

    const val POSE_TEXT_GAP_DP = 12            // 포즈 추천 안내 텍스트 ↔ 카드 간격
    const val POSE_ARROW_OFFSET_X_DP = 160       // 포즈 추천 화살표의 텍스트(카드) 기준 가로 오프셋
    const val POSE_ARROW_OFFSET_Y_DP = -13       // 포즈 추천 화살표의 텍스트 기준 세로 오프셋

    const val RECORD_TEXT_RIGHT_SHIFT_DP = 30  // 네컷 기록 안내 텍스트를 화살표와 안 겹치게 오른쪽으로 미는 정도

    const val CLOSE_MARGIN_END_DP = 20         // 닫기 버튼 ↔ 화면 오른쪽 여백
    const val CLOSE_MARGIN_TOP_DP = 8          // 닫기 버튼 ↔ 네컷 기록 카드 하단 여백
    const val CLOSE_BUTTON_SPACE_DP = 44       // 닫기 버튼이 차지할 공간(자동 스크롤 여유 계산용, 버튼 높이보다 넉넉하게)
}

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 바텀 네비게이션 초기화 및 리스너 설정
        initBottomNavigation(savedInstanceState)

        val navigateToTab = intent.getIntExtra("NAVIGATE_TO_TAB", -1)

        if (navigateToTab != -1) {
            binding.bnvMain.selectedItemId = navigateToTab
        }

        // 알림 화면에서 전달된 이동 요청 처리
        handleNotificationNavigation(intent)

        // fcm 분기
        handlePushNavigation(intent)

        // 백스택에 쌓인 화면(마이페이지, 네컷 기록 상세 등)이 있으면 뒤로가기 시 그것부터 pop,
        // 없으면 앱을 종료
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (supportFragmentManager.backStackEntryCount > 0) {
                    supportFragmentManager.popBackStack()
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                    isEnabled = true
                }
            }
        })

        // 외부에서 들어온 인텐트가 있는지 확인 (예: 앨범 상세 보기 등)
         checkIntent(intent)
    }


    fun setStatusBarColor(isLightIcon: Boolean) {
        window.statusBarColor = Color.TRANSPARENT
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = isLightIcon
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)

        // fcm 분기
        handlePushNavigation(intent)

        handleNotificationNavigation(intent)

        if (intent.getBooleanExtra("NAVIGATE_TO_HOME", false)) {
            binding.bnvMain.selectedItemId = R.id.menu_home
            return
        }

        val navigateToTab = intent.getIntExtra("NAVIGATE_TO_TAB", -1)
        if (navigateToTab != -1) {
            if (intent.getBooleanExtra("NAVIGATE_TO_MYPAGE", false)) {
                navigateToMyPage()
            } else {
                binding.bnvMain.selectedItemId = navigateToTab
            }
            return
        }

        checkMoveToDetail(intent)

        // 액티비티가 이미 켜져 있는 상태에서 새로운 인텐트를 받았을 때 처리
         checkIntent(intent)
    }

    /**
     * NotificationActivity에서 전달받은 값을 확인하여
     * 해당 스페이스 또는 사진 상세 화면으로 이동
     */
    private fun handleNotificationNavigation(intent: Intent?) {

        val workspaceId =
            intent?.getLongExtra("OPEN_SPACE_ID", -1L) ?: -1L

        val photoId =
            intent?.getLongExtra("OPEN_PHOTO_ID", -1L) ?: -1L

        // 스페이스 이동 요청이 아닌 경우
        if (workspaceId == -1L) {
            return
        }

        val fragment = if (photoId != -1L) {

            // MEDIA_COMMENT / MEDIA_UPLOADED
            // 스페이스 진입 후 해당 사진 모달까지 오픈
            SpaceFragment.newInstance(
                spaceId = workspaceId,
                photoId = photoId
            )

        } else {

            // WORKSPACE_ACCEPTED
            // 해당 스페이스까지만 진입
            SpaceFragment.newInstance(
                spaceId = workspaceId
            )
        }

        supportFragmentManager
            .beginTransaction()
            .replace(
                R.id.fcv_main,
                fragment
            )
            .addToBackStack("SpaceFragment")
            .commit()
    }

    private fun checkMoveToDetail(intent: Intent?) {
        val shouldMove = intent?.getBooleanExtra("MOVE_TO_DETAIL", false) ?: false
        if (shouldMove) {
            val apiDate = intent?.getStringExtra("API_DATE")
            val selectedDate = intent?.getStringExtra("SELECTED_DATE")

            val detailFragment = EntryDetailFragment().apply {
                arguments = Bundle().apply {
                    putString("API_DATE", apiDate)
                    putString("SELECTED_DATE", selectedDate)
                }
            }

            supportFragmentManager.beginTransaction()
                .replace(R.id.fcv_main, detailFragment)
                .addToBackStack(null)
                .commit()
        }
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
                R.id.menu_pose -> {
                    changeFragment(PoseRecommendFragment())
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

    // 프래그먼트 교체 헬퍼 함수 (바텀 네비게이션 탭 전환용 - 백스택에 안 쌓임)
    fun changeFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fcv_main, fragment)
            .commitAllowingStateLoss()
    }

    // 마이페이지처럼 "들어갔다가 뒤로가기로 돌아와야 하는" 화면은 백스택에 쌓이도록 별도 처리
    fun navigateToMyPage() {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fcv_main, MyPageFragment())
            .addToBackStack(null)
            .commitAllowingStateLoss()
    }

    fun selectPoseTab() {
        binding.bnvMain.selectedItemId = R.id.menu_pose
    }

    fun showHomeTutorialIfNeeded(
        mypageBadge: View,
        poseCard: View,
        recordCard: CardView,
        recordCharacter: View,
        contentScrollView: NestedScrollView
    ) {
        val userId = TokenManager.getUserId(this) ?: return

        lifecycleScope.launch {
            if (TutorialManager.isTutorialCompleted(this@MainActivity, userId, TutorialType.HOME)) return@launch

            showHomeTutorialOverlay(userId, mypageBadge, poseCard, recordCard, recordCharacter, contentScrollView)
        }
    }

    private fun showHomeTutorialOverlay(
        userId: Long,
        mypageBadge: View,
        poseCard: View,
        recordCard: CardView,
        recordCharacter: View,
        contentScrollView: NestedScrollView
    ) {
        val overlay = binding.includeHomeTutorial
        overlay.root.elevation = dpToPx(20).toFloat()
        overlay.root.visibility = View.VISIBLE
        mypageBadge.visibility = View.VISIBLE

        val bnvOriginalElevation = binding.bnvMain.elevation
        binding.bnvMain.elevation = 0f

        // 튜토리얼을 2단계로 나눠 보여줌.
        // 1단계: 스크롤 없이 마이페이지+포즈 카드 안내 → 화면 탭 → 2단계: 스크롤 후 네컷 기록 카드 안내
        var tutorialStep = 1

        fun boundsOf(target: View): Rect {
            val rootLocation = IntArray(2)
            binding.root.getLocationInWindow(rootLocation)
            val loc = IntArray(2)
            target.getLocationInWindow(loc)
            val left = loc[0] - rootLocation[0]
            val top = loc[1] - rootLocation[1]
            return Rect(left, top, left + target.width, top + target.height)
        }

        fun positionOverlay() {
            // 2단계(네컷 기록 카드)에서만, 카드 전체 + 닫기 버튼이 들어갈 공간까지 미리 스크롤해
            // 화면 안으로 가져온다. 1단계(마이페이지+포즈 카드)는 스크롤 없이 기본 위치 그대로 둠.
            if (tutorialStep == 2) {
                val screenBottom = boundsOf(binding.bnvMain).top
                val recordBottomBeforeScroll = boundsOf(recordCard).bottom
                val neededSpaceBelowCard = dpToPx(HomeTutorialLayout.CLOSE_MARGIN_TOP_DP) +
                    dpToPx(HomeTutorialLayout.CLOSE_BUTTON_SPACE_DP)
                val overflow = (recordBottomBeforeScroll + neededSpaceBelowCard) - screenBottom
                if (overflow > 0) {
                    contentScrollView.scrollBy(0, overflow)
                }
            }

            val mypageBox = boundsOf(mypageBadge)
            val poseBox = boundsOf(poseCard).apply { inset(-dpToPx(2), -dpToPx(2)) }

            val recordRaw = boundsOf(recordCard)
            val recordBox = Rect(
                recordRaw.left + recordCard.contentPaddingLeft,
                recordRaw.top + recordCard.contentPaddingTop,
                recordRaw.right - recordCard.contentPaddingRight,
                recordRaw.bottom - recordCard.contentPaddingBottom
            ).apply { inset(-dpToPx(2), -dpToPx(2)) }

            overlay.tutorialDimView.setHoles(
                if (tutorialStep == 1) {
                    listOf(
                        RectF(mypageBox) to mypageBox.width() / 2f,
                        RectF(poseBox) to dpToPx(12).toFloat()
                    )
                } else {
                    listOf(RectF(recordBox) to dpToPx(12).toFloat())
                }
            )

            placeHighlight(overlay.vHighlightMypage, mypageBox)
            placeHighlight(overlay.vHighlightPose, poseBox)
            placeHighlight(overlay.vHighlightRecord, recordBox)

            // 마이페이지 안내 텍스트: 아이콘 왼쪽, 세로 중앙 정렬
            overlay.tvTutorialMypage.text = coralHighlightedText(
                "마이페이지에서 프로필을 관리해요.",
                "프로필을"
            )
            overlay.tvTutorialMypage.measure(
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            )
            val mypageTextHeight = overlay.tvTutorialMypage.measuredHeight
            val mypageTextLeft = mypageBox.left - dpToPx(HomeTutorialLayout.MYPAGE_TEXT_GAP_DP) - overlay.tvTutorialMypage.measuredWidth
            val mypageTextTop = mypageBox.top + (mypageBox.height() - mypageTextHeight) / 2
            (overlay.tvTutorialMypage.layoutParams as FrameLayout.LayoutParams).apply {
                leftMargin = mypageTextLeft
                topMargin = mypageTextTop
            }
            overlay.tvTutorialMypage.requestLayout()

            val mypageArrowWidth = (overlay.ivTutorialArrowMypage.layoutParams as FrameLayout.LayoutParams).width
            (overlay.ivTutorialArrowMypage.layoutParams as FrameLayout.LayoutParams).apply {
                leftMargin = mypageBox.left - mypageArrowWidth - dpToPx(HomeTutorialLayout.MYPAGE_ARROW_ICON_GAP_DP)
                topMargin = mypageTextTop + mypageTextHeight + dpToPx(HomeTutorialLayout.MYPAGE_ARROW_OFFSET_Y_DP)
            }
            overlay.ivTutorialArrowMypage.rotation = HomeTutorialLayout.MYPAGE_ARROW_ROTATION
            overlay.ivTutorialArrowMypage.requestLayout()

            // 포즈 추천 안내 텍스트: 카드 바로 위, 왼쪽 정렬
            overlay.tvTutorialPose.text = coralHighlightedText(
                "함께 찍는 인원수에 맞게\n포즈 추천을 받아보세요.",
                "포즈 추천"
            )
            val poseTextWidth = (overlay.tvTutorialPose.layoutParams as FrameLayout.LayoutParams).width
            overlay.tvTutorialPose.measure(
                View.MeasureSpec.makeMeasureSpec(poseTextWidth, View.MeasureSpec.AT_MOST),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            )
            val poseTextHeight = overlay.tvTutorialPose.measuredHeight
            val poseTextLeft = poseBox.left
            val poseTextTop = poseBox.top - dpToPx(HomeTutorialLayout.POSE_TEXT_GAP_DP) - poseTextHeight
            (overlay.tvTutorialPose.layoutParams as FrameLayout.LayoutParams).apply {
                leftMargin = poseTextLeft
                topMargin = poseTextTop
            }
            overlay.tvTutorialPose.requestLayout()

            // 포즈 추천 화살표: "포즈 추천" 문구 아래쪽에서 위를 향하도록 배치
            (overlay.ivTutorialArrowPose.layoutParams as FrameLayout.LayoutParams).apply {
                leftMargin = poseTextLeft + dpToPx(HomeTutorialLayout.POSE_ARROW_OFFSET_X_DP)
                topMargin = poseTextTop + poseTextHeight + dpToPx(HomeTutorialLayout.POSE_ARROW_OFFSET_Y_DP)
            }
            overlay.ivTutorialArrowPose.requestLayout()

            val recordArrowHeight = (overlay.ivTutorialArrowRecord.layoutParams as FrameLayout.LayoutParams).height
            overlay.tvTutorialRecord.text = coralHighlightedText(
                "울고 있는 포토리를 눌러\n네컷과 함께 하루를 기록해 보세요.",
                "네컷과 함께 하루를 기록"
            )

            val recordTextWidth = (overlay.tvTutorialRecord.layoutParams as FrameLayout.LayoutParams).width
            overlay.tvTutorialRecord.measure(
                View.MeasureSpec.makeMeasureSpec(recordTextWidth, View.MeasureSpec.AT_MOST),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            )
            val recordTextHeight = overlay.tvTutorialRecord.measuredHeight

            val recordArrowBottom = recordBox.top - dpToPx(2)
            val recordArrowTop = recordArrowBottom - recordArrowHeight
            val recordTextBottom = recordArrowTop - dpToPx(4)
            val recordTextTop = recordTextBottom - recordTextHeight

            val recordCenterX = (recordBox.left + recordBox.right) / 2
            (overlay.ivTutorialArrowRecord.layoutParams as FrameLayout.LayoutParams).apply {
                leftMargin = recordCenterX - width / 2
                topMargin = recordArrowTop
            }
            overlay.ivTutorialArrowRecord.requestLayout()

            (overlay.tvTutorialRecord.layoutParams as FrameLayout.LayoutParams).apply {
                leftMargin = recordBox.right - width + dpToPx(HomeTutorialLayout.RECORD_TEXT_RIGHT_SHIFT_DP)
                topMargin = recordTextTop
            }
            overlay.tvTutorialRecord.requestLayout()

            overlay.llTutorialClose.bringToFront()
            overlay.llTutorialClose.measure(
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            )
            val closeWidth = overlay.llTutorialClose.measuredWidth
            val closeHeight = overlay.llTutorialClose.measuredHeight
            val closeTopBelowCard = recordBox.bottom + dpToPx(HomeTutorialLayout.CLOSE_MARGIN_TOP_DP)
            val closeTopMaxWithinScreen = overlay.root.height - closeHeight
            overlay.llTutorialClose.x = (overlay.root.width - closeWidth - dpToPx(HomeTutorialLayout.CLOSE_MARGIN_END_DP)).toFloat()
            overlay.llTutorialClose.y = minOf(closeTopBelowCard, closeTopMaxWithinScreen).toFloat()

            // 현재 단계에 해당하는 안내 요소만 보이도록 처리
            val step1Visibility = if (tutorialStep == 1) View.VISIBLE else View.GONE
            val step2Visibility = if (tutorialStep == 2) View.VISIBLE else View.GONE
            overlay.vHighlightMypage.visibility = step1Visibility
            overlay.tvTutorialMypage.visibility = step1Visibility
            overlay.ivTutorialArrowMypage.visibility = step1Visibility
            overlay.vHighlightPose.visibility = step1Visibility
            overlay.tvTutorialPose.visibility = step1Visibility
            overlay.ivTutorialArrowPose.visibility = step1Visibility
            overlay.vHighlightRecord.visibility = step2Visibility
            overlay.tvTutorialRecord.visibility = step2Visibility
            overlay.ivTutorialArrowRecord.visibility = step2Visibility
            overlay.llTutorialClose.visibility = step2Visibility
        }

        overlay.root.post { positionOverlay() }

        val layoutListener = ViewTreeObserver.OnGlobalLayoutListener { positionOverlay() }
        recordCard.viewTreeObserver.addOnGlobalLayoutListener(layoutListener)

        // 1단계에서는 화면 아무 곳이나 탭하면 2단계(네컷 기록 카드 안내)로 넘어감.
        overlay.root.setOnClickListener {
            if (tutorialStep == 1) {
                tutorialStep = 2
                positionOverlay()
            }
        }

        overlay.llTutorialClose.setOnClickListener {
            recordCard.viewTreeObserver.removeOnGlobalLayoutListener(layoutListener)
            overlay.root.setOnClickListener(null)
            overlay.root.visibility = View.GONE
            mypageBadge.visibility = View.GONE
            binding.bnvMain.elevation = bnvOriginalElevation
            lifecycleScope.launch {
                TutorialManager.completeTutorial(this@MainActivity, userId, TutorialType.HOME)
            }
        }
    }

    private fun placeHighlight(target: View, rect: Rect) {
        (target.layoutParams as FrameLayout.LayoutParams).apply {
            width = rect.width()
            height = rect.height()
            leftMargin = rect.left
            topMargin = rect.top
        }
        target.requestLayout()
    }

    private fun coralHighlightedText(full: String, highlight: String): SpannableStringBuilder {
        val spannable = SpannableStringBuilder(full)
        val start = full.indexOf(highlight)
        if (start >= 0) {
            spannable.setSpan(
                ForegroundColorSpan(Color.parseColor("#FF7E67")),
                start,
                start + highlight.length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
        return spannable
    }

    private fun dpToPx(dp: Int): Int =
        (dp * resources.displayMetrics.density).toInt()

    fun selectHomeTab() {
        binding.bnvMain.selectedItemId = R.id.menu_home
    }

    private fun handlePushNavigation(intent: Intent?) {
        when (intent?.getStringExtra("type")
            ?: intent?.getStringExtra("PUSH_TYPE")) {

            "PHOTO_COMMENT" -> {
                val photoId = intent?.getStringExtra("photoId")
                    ?: intent?.getStringExtra("PHOTO_ID")

                val fragment = PhotoDialogFragment().apply {
                    arguments = Bundle().apply {
                        putString("PHOTO_ID", photoId)
                    }
                }

                supportFragmentManager.beginTransaction()
                    .replace(R.id.fcv_main, fragment)
                    .addToBackStack(null)
                    .commit()
            }
        }
    }
}