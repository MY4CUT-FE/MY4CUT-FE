package com.umc.mobile.my4cut.ui.space

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.umc.mobile.my4cut.R
import com.umc.mobile.my4cut.databinding.FragmentMySpaceBinding

class MySpaceFragment : Fragment() {

    private val spaces = mutableListOf<Space>()

    private var _binding: FragmentMySpaceBinding? = null
    private val binding get() = _binding!!

    // Handler & Runnable for periodic expired space removal
    private val expireHandler = Handler(Looper.getMainLooper())
    private val expireCheckRunnable = object : Runnable {
        override fun run() {
            val beforeSize = spaces.size
            removeExpiredSpaces()
            if (spaces.size != beforeSize) {
                updateSpaceUi()
            }
            // Check again after 1 minute (60,000 ms)
            expireHandler.postDelayed(this, 60_000)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMySpaceBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.tvAddSpace.setOnClickListener {
            if (spaces.size >= 4) return@setOnClickListener

            val dialog = CreateSpaceDialogFragment()
            dialog.setOnConfirmListener { result ->
                addNewSpace(
                    name = result.spaceName,
                    currentMember = result.currentMember
                )
            }
            dialog.show(parentFragmentManager, "CreateSpaceDialog")
        }

        // 배경 원 클릭 이벤트 (필요 시 수정)
        binding.viewOrangeCircle.setOnClickListener {
            // ... 이동 로직
        }

        updateSpaceUi()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // 좌표 계산 로직 개선
    private fun addSpaceCircle(
        parent: FrameLayout,
        view: View,
        percentX: Float, // -0.5 ~ 0.5 (중심 기준 좌우)
        percentY: Float  // -0.5 ~ 0.5 (중심 기준 상하)
    ) {
        parent.post {
            // 부모 뷰(FrameLayout)의 크기
            val parentWidth = parent.width
            val parentHeight = parent.height

            // 자식 뷰(원)의 크기
            val childSize = view.layoutParams.width

            // 중심 좌표
            val centerX = parentWidth / 2f
            val centerY = parentHeight / 2f

            // 배치할 수 있는 유효 반지름 (부모 반지름 - 자식 반지름의 절반 정도 여유)
            // 값을 조정하여 퍼짐 정도를 결정 (0.8f는 부모 크기의 80% 영역 사용)
            val spreadRadius = (parentWidth / 2f) * 0.8f

            // 최종 좌표 계산 (중심점 + 오프셋 - 뷰의 절반 크기)
            val x = (centerX + (spreadRadius * percentX) - (childSize / 2f)).toInt()
            val y = (centerY + (spreadRadius * percentY) - (childSize / 2f)).toInt()

            val params = FrameLayout.LayoutParams(
                childSize, // width
                childSize  // height
            )
            params.leftMargin = x
            params.topMargin = y

            // 뷰 추가
            parent.addView(view, params)
        }
    }

    private fun dpToPx(dp: Int): Int =
        (dp * resources.displayMetrics.density).toInt()

    private fun updateSpaceUi() {
        val parent = binding.layoutSpaceItems as FrameLayout
        parent.removeAllViews()

        removeExpiredSpaces()

        spaces.take(4).forEachIndexed { index, space ->
            val view = inflateCircleByIndex(index, parent, space)
            val (x, y) = getOffsetByIndex(index)
            addSpaceCircle(parent, view, x, y)
        }
    }

    private fun inflateCircleByIndex(index: Int, parent: FrameLayout, space: Space): View {
        val sizeDp = listOf(120, 100, 90, 80)[index]

        return LayoutInflater.from(requireContext())
            .inflate(R.layout.item_space_circle, parent, false).apply {
                val sizePx = dpToPx(sizeDp)
                layoutParams = FrameLayout.LayoutParams(sizePx, sizePx)

                // ----- 데이터 바인딩 -----
                findViewById<TextView>(R.id.tvSpaceName)?.text = space.name
                findViewById<TextView>(R.id.tvMemberCount)?.text =
                    "${space.currentMember}/${space.maxMember}"

                val remainDays =
                    ((space.expiredAt - System.currentTimeMillis()) / (1000 * 60 * 60 * 24))
                        .coerceAtLeast(0)

                findViewById<TextView>(R.id.tvExpire)?.text = "만료까지 ${remainDays}일"

                // ExpireCircleView에 생성/만료 정보 전달
                findViewById<ExpireCircleView>(R.id.expireCircle)
                    ?.setExpireInfo(space.createdAt, space.expiredAt)

                // ===== 스페이스 클릭 → SpaceFragment 이동 =====
                setOnClickListener {
                    android.util.Log.d("MySpaceFragment", "Space clicked: ${space.id}")
                    requireActivity()
                        .supportFragmentManager
                        .beginTransaction()
                        .replace(
                            R.id.fcv_main,
                            SpaceFragment.newInstance(space.id)
                        )
                        .addToBackStack("SpaceFragment")
                        .commit()
                }
            }
    }

    private fun getOffsetByIndex(index: Int): Pair<Float, Float> =
        listOf(
            Pair(0.3f, -0.3f),    // 0: 우상
            Pair(-0.45f, -0.1f),  // 1: 좌
            Pair(-0.15f, 0.5f),   // 2: 하
            Pair(0.5f, 0.35f)     // 3: 우하
        )[index]

    private fun addNewSpace(
        name: String,
        currentMember: Int
    ) {
        val now = System.currentTimeMillis()
        val sevenDays = 7L * 24 * 60 * 60 * 1000

        val newSpace = Space(
            id = spaces.size + 1,
            name = name,
            currentMember = currentMember,
            maxMember = 10,
            createdAt = now,
            expiredAt = now + sevenDays
        )

        spaces.add(newSpace)
        updateSpaceUi()
    }

    private fun removeExpiredSpaces() {
        val now = System.currentTimeMillis()
        val iterator = spaces.iterator()
        while (iterator.hasNext()) {
            if (iterator.next().expiredAt <= now) {
                iterator.remove()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        expireHandler.post(expireCheckRunnable)
    }

    override fun onStop() {
        super.onStop()
        expireHandler.removeCallbacks(expireCheckRunnable)
    }
}