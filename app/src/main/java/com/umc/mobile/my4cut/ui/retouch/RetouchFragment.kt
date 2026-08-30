package com.umc.mobile.my4cut.ui.retouch

import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

import android.content.Intent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.IntentFilter
import android.graphics.Color
import android.graphics.RectF
import androidx.core.content.ContextCompat
import com.umc.mobile.my4cut.ui.home.HomeFragment

import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.umc.mobile.my4cut.R
import com.umc.mobile.my4cut.databinding.FragmentRetouchBinding
import com.umc.mobile.my4cut.ui.friend.FriendsFragment
import com.umc.mobile.my4cut.ui.space.MySpaceFragment
import com.umc.mobile.my4cut.ui.notification.NotificationActivity
import com.umc.mobile.my4cut.network.RetrofitClient
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.ActivityResultLauncher
import androidx.recyclerview.widget.RecyclerView
import com.umc.mobile.my4cut.data.auth.local.TokenManager
import com.umc.mobile.my4cut.data.tutorial.TutorialManager
import com.umc.mobile.my4cut.data.tutorial.model.TutorialType
import com.umc.mobile.my4cut.ui.tutorial.TutorialDimView

class RetouchFragment : Fragment(R.layout.fragment_retouch) {

    private var _binding: FragmentRetouchBinding? = null
    private val binding get() = _binding!!

    private lateinit var notificationLauncher: ActivityResultLauncher<Intent>

    // FCM 푸시가 도착하면 리터치 화면의 알림 아이콘도 즉시 갱신
    private val notificationReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == HomeFragment.ACTION_NOTIFICATION_RECEIVED) {
                // 푸시 도착 직후 사용자가 바로 알 수 있도록 먼저 ON 아이콘으로 변경
                binding.ivNotification.setImageResource(R.drawable.ic_noti_on)

                // 알림창에 시스템 알림이 남아있어도, 서버 기준으로 전부 읽음이면 OFF 처리되도록 동기화
                updateNotificationIcon()
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentRetouchBinding.bind(view)

        notificationLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) {
            updateNotificationIcon()
        }

        binding.ivNotification.setOnClickListener {
            val intent = Intent(requireContext(), NotificationActivity::class.java)
            notificationLauncher.launch(intent)
        }

        binding.ivMypage.setOnClickListener {
            (requireActivity() as? com.umc.mobile.my4cut.MainActivity)
                ?.navigateToMyPage()
        }

        // 최초 로드
        loadChildFragments()

        updateNotificationIcon()
        // RetouchFragment가 살아있는 동안 푸시 수신 이벤트를 감지
        registerNotificationReceiver()

        // checkRetouchMainTutorial()
        showRetouchMainTutorial()
    }

    private fun loadChildFragments() {
        childFragmentManager.beginTransaction()
            .replace(R.id.containerMySpace, MySpaceFragment())
            .replace(R.id.containerFriends, FriendsFragment())
            .commit()
    }

    override fun onResume() {
        super.onResume()
        updateNotificationIcon()
    }

    // FCM 수신 브로드캐스트 Receiver 등록
    private fun registerNotificationReceiver() {
        val filter = IntentFilter(HomeFragment.ACTION_NOTIFICATION_RECEIVED)

        ContextCompat.registerReceiver(
            requireContext(),
            notificationReceiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    // Fragment View가 파괴될 때 Receiver 해제
    private fun unregisterNotificationReceiver() {
        try {
            requireContext().unregisterReceiver(notificationReceiver)
        } catch (_: IllegalArgumentException) {
            // 이미 해제된 경우 앱이 죽지 않도록 무시
        }
    }

    private fun updateNotificationIcon() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = RetrofitClient.notificationService.getUnreadStatus()
                val hasUnread = response.data?.hasUnread == true

                binding.ivNotification.setImageResource(
                    if (hasUnread) R.drawable.ic_noti_on
                    else R.drawable.ic_noti_off
                )
            } catch (e: Exception) {
                binding.ivNotification.setImageResource(R.drawable.ic_noti_off)
            }
        }
    }

    override fun onDestroyView() {
        tutorialView?.let { view ->
            (view.parent as? ViewGroup)?.removeView(view)
        }
        tutorialView = null

        unregisterNotificationReceiver()

        super.onDestroyView()
        _binding = null
    }

    private fun checkRetouchMainTutorial() {
        val userId = TokenManager.getUserId(requireContext()) ?: return

        viewLifecycleOwner.lifecycleScope.launch {
            when (
                TutorialManager.isCompleted(
                    requireContext(),
                    userId,
                    TutorialType.RETOUCH_MAIN
                )
            ) {
                true -> {
                    // 이미 완료 → 아무것도 안 함
                }

                false -> {
                    showRetouchMainTutorial()
                }

                null -> {
                    syncTutorialStatus(userId)
                }
            }
        }
    }

    private suspend fun syncTutorialStatus(userId: Long) {
        try {
            val response =
                RetrofitClient.tutorialService.getTutorialStatus()

            val tutorials = response.data?.tutorials ?: return

            TutorialManager.saveStatuses(
                requireContext(),
                userId,
                tutorials
            )

            val completed = tutorials
                .find { it.type == TutorialType.RETOUCH_MAIN }
                ?.completed

            if (completed == false) {
                showRetouchMainTutorial()
            }

        } catch (e: Exception) {
            Log.e("Tutorial", "튜토리얼 상태 조회 실패", e)
        }
    }

    private fun completeRetouchMainTutorial() {
        val userId = TokenManager.getUserId(requireContext()) ?: return

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                RetrofitClient.tutorialService.completeTutorial(
                    TutorialType.RETOUCH_MAIN
                )

                TutorialManager.setCompleted(
                    requireContext(),
                    userId,
                    TutorialType.RETOUCH_MAIN
                )

                hideRetouchMainTutorial()

            } catch (e: Exception) {
                Log.e("Tutorial", "RETOUCH_MAIN 완료 처리 실패", e)
            }
        }
    }

    private var tutorialView: View? = null

    private fun showRetouchMainTutorial() {
        if (tutorialView != null) return

        val root = requireActivity()
            .findViewById<ViewGroup>(android.R.id.content)

        val overlay = layoutInflater.inflate(
            R.layout.view_tutorial_retouch,
            root,
            false
        )

        tutorialView = overlay
        root.addView(overlay)

        overlay.findViewById<View>(
            R.id.ll_tutorial_close
        ).setOnClickListener {
            // API 연결할 때:
            // completeRetouchMainTutorial()

            // 지금은 UI 확인만
            hideRetouchMainTutorial()
        }

        overlay.post {
            setupRetouchMainTutorial()
        }
    }

    private fun hideRetouchMainTutorial() {
        tutorialView?.let { view ->
            (view.parent as? ViewGroup)?.removeView(view)
        }

        tutorialView = null
    }

    private fun dp(value: Float): Float {
        return value * resources.displayMetrics.density
    }

    private fun getRectInOverlay(
        target: View,
        overlay: View,
        padding: Float = 0f
    ): RectF {

        val targetLocation = IntArray(2)
        val overlayLocation = IntArray(2)

        target.getLocationOnScreen(targetLocation)
        overlay.getLocationOnScreen(overlayLocation)

        val left =
            targetLocation[0] -
                    overlayLocation[0] -
                    padding

        val top =
            targetLocation[1] -
                    overlayLocation[1] -
                    padding

        return RectF(
            left,
            top,
            left + target.width + padding * 2,
            top + target.height + padding * 2
        )
    }

    private fun positionView(
        view: View,
        x: Float,
        y: Float
    ) {
        view.x = x
        view.y = y
    }

    private fun positionHighlight(
        view: View,
        rect: RectF
    ) {
        val params =
            view.layoutParams as FrameLayout.LayoutParams

        params.width = rect.width().toInt()
        params.height = rect.height().toInt()

        view.layoutParams = params

        view.x = rect.left
        view.y = rect.top

        view.visibility = View.VISIBLE
    }

    private fun setupRetouchMainTutorial() {
        val overlay = tutorialView ?: return

        val dimView =
            overlay.findViewById<TutorialDimView>(
                R.id.tutorial_dim_view
            )

        val mySpaceFragment =
            childFragmentManager.findFragmentById(
                R.id.containerMySpace
            )

        val friendsFragment =
            childFragmentManager.findFragmentById(
                R.id.containerFriends
            )

        val mySpaceView =
            mySpaceFragment?.view ?: return

        val friendsView =
            friendsFragment?.view ?: return

        // 실제 화면에서 강조할 View
        val addSpaceView =
            mySpaceView.findViewById<View>(
                R.id.layoutAddSpace
            )

        val rvMySpaces =
            mySpaceView.findViewById<RecyclerView>(
                R.id.rvMySpaces
            )

        val firstSpaceCard =
            rvMySpaces.findViewHolderForAdapterPosition(0)
                ?.itemView

        val addFriendView =
            friendsView.findViewById<View>(
                R.id.tvFriendsAdd
            )

        // 좌표 계산
        val baseAddSpaceRect =
            getRectInOverlay(
                addSpaceView,
                overlay,
                dp(5f)
            )

        val addSpaceRect =
            RectF(
                baseAddSpaceRect.left - dp(2f),
                baseAddSpaceRect.top,
                baseAddSpaceRect.right + dp(2f),
                baseAddSpaceRect.bottom
            )

        val baseAddFriendRect =
            getRectInOverlay(
                addFriendView,
                overlay,
                dp(5f)
            )

        val addFriendRect =
            RectF(
                baseAddFriendRect.left - dp(4f),
                baseAddFriendRect.top,
                baseAddFriendRect.right + dp(4f),
                baseAddFriendRect.bottom
            )

        // Dim spotlight
        dimView.clearHighlights()

        dimView.addHighlight(
            addSpaceRect,
            dp(6f)
        )

        dimView.addHighlight(
            addFriendRect,
            dp(6f)
        )

        // My Space 추가 점선
        positionHighlight(
            overlay.findViewById(
                R.id.v_highlight_add_space
            ),
            addSpaceRect
        )

        // Friends 추가 점선
        positionHighlight(
            overlay.findViewById(
                R.id.v_highlight_add_friend
            ),
            addFriendRect
        )

        // 첫 번째 Space 카드
        firstSpaceCard?.let { card ->

            val cardRect =
                getRectInOverlay(
                    card,
                    overlay,
                    dp(8f)
                )

            dimView.addHighlight(
                cardRect,
                dp(21f)
            )

            positionHighlight(
                overlay.findViewById(
                    R.id.v_highlight_space_card
                ),
                cardRect
            )

            setupSpaceCardTutorial(
                overlay,
                cardRect
            )
        }

        // 추가 버튼 관련 설명 위치
        setupAddTutorial(
            overlay,
            addSpaceRect,
            addFriendRect
        )

        // 텍스트
        setupTutorialTexts(overlay)
    }

    private fun setupSpaceCardTutorial(
        overlay: View,
        cardRect: RectF
    ) {
        val expireText =
            overlay.findViewById<TextView>(
                R.id.tv_tutorial_expire
            )

        val expireArrow =
            overlay.findViewById<ImageView>(
                R.id.iv_arrow_expire
            )

        val newsText =
            overlay.findViewById<TextView>(
                R.id.tv_tutorial_news
            )

        val newsArrow =
            overlay.findViewById<ImageView>(
                R.id.iv_arrow_news
            )

        val membersText =
            overlay.findViewById<TextView>(
                R.id.tv_tutorial_members
            )

        val membersArrow =
            overlay.findViewById<ImageView>(
                R.id.iv_arrow_members
            )

        // 만료 설명:
        // 카드의 왼쪽 위를 기준으로 배치
        positionView(
            expireText,
            cardRect.left + dp(3f),
            cardRect.top - expireText.height - dp(13f)
        )

        // 만료 화살표:
        // 카드 너비의 약 15% 지점을 가리킴
        positionView(
            expireArrow,
            cardRect.left +
                    cardRect.width() * 0.15f -
                    expireArrow.width / 2f,
            cardRect.top -
                    expireArrow.height +
                    dp(27f)
        )

        // 소식 설명:
        // 카드 오른쪽 중앙에 위치
        // 화면 밖으로 넘어가지 않도록 제한
        val newsTextX =
            (cardRect.right + dp(12f))
                .coerceAtMost(
                    overlay.width.toFloat() -
                            newsText.width -
                            dp(2f)
                )

        positionView(
            newsText,
            newsTextX + dp(41f),
            cardRect.centerY() -
                    newsText.height / 2f + dp(20f)
        )

        // 소식 화살표:
        // 카드 오른쪽 중앙 기준
        positionView(
            newsArrow,
            cardRect.right - dp(33f),
            cardRect.centerY() -
                    newsArrow.height / 2f + dp(20f)
        )

        // 참여 인원 설명:
        // 카드 자체의 중앙에 맞춤
        positionView(
            membersText,
            cardRect.centerX() -
                    membersText.width / 2f + dp(57f),
            cardRect.bottom + dp(12f)
        )

        // 참여 인원 화살표:
        // 카드 왼쪽에서 약 22% 지점
        positionView(
            membersArrow,
            cardRect.left +
                    cardRect.width() * 0.22f -
                    membersArrow.width / 2f + dp(15f),
            cardRect.bottom - dp(23f)
        )
    }

    private fun setupAddTutorial(
        overlay: View,
        addSpaceRect: RectF,
        addFriendRect: RectF
    ) {
        val createText =
            overlay.findViewById<TextView>(
                R.id.tv_tutorial_create
            )

        val createArrow =
            overlay.findViewById<ImageView>(
                R.id.iv_arrow_create
            )

        val friendText =
            overlay.findViewById<TextView>(
                R.id.tv_tutorial_friend
            )

        val friendArrow =
            overlay.findViewById<ImageView>(
                R.id.iv_arrow_friend
            )

        // My Space 설명:
        // 추가 버튼의 오른쪽 끝과
        // TextView의 오른쪽 끝을 맞춤
        val createTextX =
            (addSpaceRect.right - createText.width)
                .coerceAtLeast(dp(8f))

        positionView(
            createText,
            createTextX - dp(2f),
            addSpaceRect.top -
                    createText.height -
                    dp(30f)
        )

        // My Space 곡선 화살표:
        // 추가 버튼 크기를 기준으로 상대 배치
        positionView(
            createArrow,
            addSpaceRect.left -
                    createArrow.width * 0.65f - dp(10f),
            addSpaceRect.top -
                    createArrow.height * 0.55f + dp(6f)
        )

        // Friends 설명:
        // Friends 추가 버튼 오른쪽 끝 기준
        val friendTextX =
            (addFriendRect.right - friendText.width)
                .coerceAtLeast(dp(8f))

        positionView(
            friendText,
            friendTextX - dp(5f),
            addFriendRect.bottom + dp(4f)
        )

        // Friends 화살표:
        // 추가 버튼의 오른쪽 아래를 기준으로 배치
        // 화면 오른쪽 밖으로 넘어가지 않도록 제한
        val friendArrowX =
            (addFriendRect.right -
                    friendArrow.width * 0.35f)
                .coerceAtMost(
                    overlay.width.toFloat() -
                            friendArrow.width -
                            dp(4f)
                )

        positionView(
            friendArrow,
            friendArrowX + dp(1f),
            addFriendRect.bottom - dp(11f)
        )
    }

    private fun setupTutorialTexts(
        overlay: View
    ) {
        setTutorialText(
            overlay.findViewById(
                R.id.tv_tutorial_create
            ),
            "스페이스를 만들고,\n친구를 초대해 보정본을 관리해요.",
            "보정본을 관리"
        )

        setTutorialText(
            overlay.findViewById(
                R.id.tv_tutorial_expire
            ),
            "스페이스는 생성일로부터 7일 후 자동 만료돼요.",
            "7일 후 자동 만료"
        )

        setTutorialText(
            overlay.findViewById(
                R.id.tv_tutorial_news
            ),
            "스페이스 소식을\n바로 확인해요.",
            "소식을\n바로 확인"
        )

        setTutorialText(
            overlay.findViewById(
                R.id.tv_tutorial_members
            ),
            "스페이스에 참여 중인 인원을 한눈에 확인해요.",
            "인원을 한눈에 확인"
        )

        setTutorialText(
            overlay.findViewById(
                R.id.tv_tutorial_friend
            ),
            "코드로 친구를 추가해 함께\n리터치 스페이스를 이용해 보세요.",
            "리터치 스페이스를 이용"
        )
    }

    private fun setTutorialText(
        textView: TextView,
        text: String,
        highlight: String
    ) {
        val spannable =
            SpannableString(text)

        val start =
            text.indexOf(highlight)

        if (start >= 0) {
            spannable.setSpan(
                ForegroundColorSpan(
                    Color.parseColor("#FF7E67")
                ),
                start,
                start + highlight.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }

        textView.text = spannable
    }
}