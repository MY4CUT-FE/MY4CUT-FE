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
    const val MYPAGE_ARROW_OFFSET_X_DP = 170 // 마이페이지 화살표의 텍스트 기준 가로 오프셋
    const val MYPAGE_ARROW_OFFSET_Y_DP = 8   // 마이페이지 화살표의 텍스트 기준 세로 오프셋
    const val MYPAGE_ARROW_ROTATION = 0f    // 마이페이지 화살표 회전 각도

    const val POSE_TEXT_GAP_DP = 12            // 포즈 추천 안내 텍스트 ↔ 카드 간격
    const val POSE_ARROW_OFFSET_X_DP = 130       // 포즈 추천 화살표의 텍스트 기준 가로 오프셋
    const val POSE_ARROW_OFFSET_Y_DP = -13       // 포즈 추천 화살표의 텍스트 기준 세로 오프셋

    const val RECORD_TEXT_RIGHT_SHIFT_DP = 30  // 네컷 기록 안내 텍스트를 화살표와 안 겹치게 오른쪽으로 미는 정도

    const val CLOSE_MARGIN_END_DP = 20         // 닫기 버튼 ↔ 화면 오른쪽 여백
    const val CLOSE_MARGIN_BOTTOM_DP = 16      // 닫기 버튼 ↔ 화면 아래쪽 여백
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
     * 해당 스페이스 또는 사진 상세 화면으로 이동한다.
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
        recordCharacter: View
    ) {
        val userId = TokenManager.getUserId(this) ?: return

        lifecycleScope.launch {
            if (TutorialManager.isTutorialCompleted(this@MainActivity, userId, TutorialType.HOME)) return@launch

            showHomeTutorialOverlay(userId, mypageBadge, poseCard, recordCard, recordCharacter)
        }
    }

    private fun showHomeTutorialOverlay(
        userId: Long,
        mypageBadge: View,
        poseCard: View,
        recordCard: CardView,
        recordCharacter: View
    ) {
        val overlay = binding.includeHomeTutorial
        overlay.root.elevation = dpToPx(20).toFloat()
        overlay.root.visibility = View.VISIBLE
        mypageBadge.visibility = View.VISIBLE

        // bnv_main(하단 네비게이션 바)에는 elevation="10dp"가 붙어 있는데, 일부 실기기의 하드웨어
        // 가속 렌더링에서는 이 elevation 차이로 인한 Z-순서 처리가 오버레이(20dp)보다 위로 나오는
        // 경우가 있어(에뮬레이터에서는 재현되지 않음), 튜토리얼이 떠 있는 동안은 네비게이션 바의
        // elevation을 임시로 0으로 낮춰 Z-순서 모호함을 없앤다. 닫을 때 원래 값(10dp)으로 복원한다.
        val bnvOriginalElevation = binding.bnvMain.elevation
        binding.bnvMain.elevation = 0f

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
            val bnvTop = boundsOf(binding.bnvMain).top

            val mypageBox = boundsOf(mypageBadge)
            val poseBox = boundsOf(poseCard).apply { inset(-dpToPx(2), -dpToPx(2)) }

            // CardView는 elevation 그림자 여백(compat padding)까지 포함해서 측정되므로,
            // contentPadding만큼 안쪽으로 보정해 실제 보이는 흰 카드 영역만 감싸도록 함
            val recordRaw = boundsOf(recordCard)
            val recordBox = Rect(
                recordRaw.left + recordCard.contentPaddingLeft,
                recordRaw.top + recordCard.contentPaddingTop,
                recordRaw.right - recordCard.contentPaddingRight,
                recordRaw.bottom - recordCard.contentPaddingBottom
            ).apply { inset(-dpToPx(2), -dpToPx(2)) }

            // 딤에 스포트라이트(완전 투명) 구멍을 뚫어 실제 홈 화면 요소가 어둡게 가려지지 않도록 함
            // (카드 강조 영역은 실제 카드 크기 그대로 유지 — 절대 줄이지 않는다)
            overlay.tutorialDimView.setHoles(
                listOf(
                    RectF(mypageBox) to mypageBox.width() / 2f,
                    RectF(poseBox) to dpToPx(12).toFloat(),
                    RectF(recordBox) to dpToPx(12).toFloat()
                )
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

            // 마이페이지 화살표: 안내 텍스트 아래쪽에서 아이콘 쪽으로 비스듬히 위를 향하도록 배치
            (overlay.ivTutorialArrowMypage.layoutParams as FrameLayout.LayoutParams).apply {
                leftMargin = mypageTextLeft + dpToPx(HomeTutorialLayout.MYPAGE_ARROW_OFFSET_X_DP)
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

            // 네컷 기록 화살표 + 안내 텍스트: "카드 아래"가 아니라 "카드 위쪽"(포즈 카드~네컷 기록
            // 카드 사이의 안정적인 공간)에 배치한다. 카드 아래쪽~네비게이션 바 사이 여유 공간은
            // 카드 높이·화면 비율·기기 화면 설정에 따라 편차가 커서(예: 갤럭시 S24 실기기에서 확인됨)
            // 어떤 값으로 튜닝해도 특정 기기에서 화면 밖으로 밀려날 위험이 있다. 반면 카드 위쪽 공간은
            // 이미 포즈 추천 안내(위쪽 배치)가 모든 기기에서 안정적으로 동작하고 있는 영역이라
            // 같은 패턴을 재사용해 기기 의존성을 없앤다.
            val recordArrowHeight = (overlay.ivTutorialArrowRecord.layoutParams as FrameLayout.LayoutParams).height
            overlay.tvTutorialRecord.text = coralHighlightedText(
                "울고 있는 포토리를 눌러\n네컷과 함께 하루를 기록해 보세요.",
                "네컷과 함께 하루를 기록"
            )
            overlay.tvTutorialRecord.measure(
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            )
            val recordTextHeight = overlay.tvTutorialRecord.measuredHeight

            // 화살표(카드를 가리키며 아래를 향함)를 카드 바로 위에 붙이고, 텍스트는 그 화살표 위에 배치
            val recordArrowBottom = recordBox.top - dpToPx(2)
            val recordArrowTop = recordArrowBottom - recordArrowHeight
            val recordTextBottom = recordArrowTop - dpToPx(4)
            val recordTextTop = recordTextBottom - recordTextHeight

            // 화살표 가로 위치는 카드 자체의 가로 중심을 기준으로 한다. recordCharacter(카드 안
            // 캐릭터 아이콘)의 측정값을 쓰면, 실기기에서 그 뷰가 아직 측정되기 전에 이 계산이
            // 먼저 실행될 경우 좌표가 0으로 잡혀 화살표가 화면 왼쪽 끝으로 밀려나는 문제가 있었다.
            // recordBox는 이미 안정적으로 측정된 값이라 이를 기준으로 삼는다.
            val recordCenterX = (recordBox.left + recordBox.right) / 2
            (overlay.ivTutorialArrowRecord.layoutParams as FrameLayout.LayoutParams).apply {
                leftMargin = recordCenterX - width / 2
                topMargin = recordArrowTop
            }
            overlay.ivTutorialArrowRecord.requestLayout()

            // 텍스트 박스가 카드 우측 끝에 딱 맞춰져 있으면 카드 중앙쯤에 있는 화살표와 겹치므로,
            // 화살표와 겹치지 않도록 텍스트를 오른쪽으로 조금 더 밀어 배치한다.
            (overlay.tvTutorialRecord.layoutParams as FrameLayout.LayoutParams).apply {
                leftMargin = recordBox.right - width + dpToPx(HomeTutorialLayout.RECORD_TEXT_RIGHT_SHIFT_DP)
                topMargin = recordTextTop
            }
            overlay.tvTutorialRecord.requestLayout()

            // 닫기 버튼: 하단 네비게이션 바의 실제 top 좌표 바로 위 (텍스트/화살표가 더 이상
            // 화면 아래쪽에 있지 않으므로 별도 회피 계산 없이 항상 이 위치로 고정)
            overlay.llTutorialClose.bringToFront()
            overlay.llTutorialClose.measure(
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            )
            val closeWidth = overlay.llTutorialClose.measuredWidth
            val closeHeight = overlay.llTutorialClose.measuredHeight
            overlay.llTutorialClose.x = (overlay.root.width - closeWidth - dpToPx(HomeTutorialLayout.CLOSE_MARGIN_END_DP)).toFloat()
            overlay.llTutorialClose.y = (bnvTop - closeHeight - dpToPx(HomeTutorialLayout.CLOSE_MARGIN_BOTTOM_DP)).toFloat()
        }

        overlay.root.post { positionOverlay() }

        // 네트워크 응답 등으로 네컷 기록 카드 크기가 나중에 바뀌어도 하이라이트가 어긋나지 않도록,
        // 카드 레이아웃이 다시 잡힐 때마다 위치를 재계산한다.
        val layoutListener = ViewTreeObserver.OnGlobalLayoutListener { positionOverlay() }
        recordCard.viewTreeObserver.addOnGlobalLayoutListener(layoutListener)

        overlay.llTutorialClose.setOnClickListener {
            recordCard.viewTreeObserver.removeOnGlobalLayoutListener(layoutListener)
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