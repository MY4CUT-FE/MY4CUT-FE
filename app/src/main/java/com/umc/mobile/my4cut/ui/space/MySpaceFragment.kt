package com.umc.mobile.my4cut.ui.space

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.fragment.app.Fragment
import com.umc.mobile.my4cut.R
import com.umc.mobile.my4cut.databinding.FragmentMySpaceBinding

class MySpaceFragment : Fragment() {

    private var _binding: FragmentMySpaceBinding? = null
    private val binding get() = _binding!!

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
            val dialog = CreateSpaceDialogFragment()
            dialog.show(parentFragmentManager, "CreateSpaceDialog")
        }

        // 배경 원 클릭 이벤트 (필요 시 수정)
        binding.viewOrangeCircle.setOnClickListener {
            // ... 이동 로직
        }

        setupSpaceCircles()
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

    private fun setupSpaceCircles() {
        val parent = binding.layoutSpaceItems as FrameLayout
        parent.removeAllViews()

        // 테스트 데이터: 4개
        val spaceCount = 4

        // [수정 1] 원의 크기를 화면에 맞게 전체적으로 줄임 (겹침 방지)
        // 기존: 150, 110... -> 수정: 120, 100, 90, 80
        fun inflateCircle(sizeDp: Int): View {
            return LayoutInflater.from(requireContext())
                .inflate(R.layout.item_space_circle, parent, false).apply {
                    val sizePx = dpToPx(sizeDp)
                    layoutParams = FrameLayout.LayoutParams(sizePx, sizePx)
                }
        }

        // 크기 정의
        val view1 = inflateCircle(120) // 메인 (Top-Right)
        val view2 = inflateCircle(100) // 서브 1 (Left)
        val view3 = inflateCircle(90)  // 서브 2 (Bottom)
        val view4 = inflateCircle(80)  // 서브 3 (Small filler)

        // [수정 2] 좌표를 더 바깥으로 분산
        when (spaceCount) {
            1 -> {
                // 정중앙
                addSpaceCircle(parent, view1, 0f, 0f)
            }
            2 -> {
                // 양쪽 대각선으로 벌림
                addSpaceCircle(parent, view1, -0.3f, -0.3f)
                addSpaceCircle(parent, view2, 0.3f, 0.3f)
            }
            3 -> {
                // 삼각형 구도
                addSpaceCircle(parent, view1, 0.3f, -0.2f)   // 우측 상단 (메인)
                addSpaceCircle(parent, view2, -0.4f, 0.1f)   // 좌측 중앙
                addSpaceCircle(parent, view3, 0.2f, 0.45f)   // 우측 하단
            }
            4 -> {
                // 4개 배치 (겹치지 않게 사방으로 분산)

                // 1. 메인 (우측 상단)
                addSpaceCircle(parent, view1, 0.3f, -0.3f)

                // 2. 서브 (좌측) - 약간 위로 올림
                addSpaceCircle(parent, view2, -0.45f, -0.1f)

                // 3. 서브 (하단 중앙) - 왼쪽으로 치우치게
                addSpaceCircle(parent, view3, -0.15f, 0.5f)

                // 4. 서브 (우측 하단 구석) - 작게 채우기
                addSpaceCircle(parent, view4, 0.5f, 0.35f)
            }
        }
    }
}