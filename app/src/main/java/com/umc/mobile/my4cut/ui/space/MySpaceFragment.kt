package com.umc.mobile.my4cut.ui.space

import android.view.ViewGroup
import com.umc.mobile.my4cut.ui.space.SpaceCircleSize
import com.umc.mobile.my4cut.ui.space.SpaceCircleStyle

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
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

        // 스페이스 생성하기 버튼 클릭 -> 모달 표시
        binding.tvAddSpace.setOnClickListener {
            val dialog = CreateSpaceDialogFragment()
            dialog.show(parentFragmentManager, "CreateSpaceDialog")
        }

        binding.viewOrangeCircle.setOnClickListener {
            requireActivity().supportFragmentManager.beginTransaction()
                .replace(R.id.fcv_main, SpaceFragment())
                .addToBackStack(null)
                .commit()
        }

        setupSpaceCircles()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun addSpaceCircle(
        parent: FrameLayout,
        view: View,
        percentX: Float,
        percentY: Float
    ) {
        parent.post {
            val parentSize = parent.width.coerceAtMost(parent.height)
            val childSize = view.layoutParams.width
            val radius = parentSize / 2f
            val safeRadius = radius - childSize / 2f - dpToPx(8)

            val x = (radius + safeRadius * percentX - childSize / 2f).toInt()
            val y = (radius + safeRadius * percentY - childSize / 2f).toInt()

            val params = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            params.leftMargin = x
            params.topMargin = y
            parent.addView(view, params)
        }
    }

    private fun dpToPx(dp: Int): Int =
        (dp * resources.displayMetrics.density).toInt()

    private fun setupSpaceCircles() {
        val parent = binding.layoutSpaceItems as FrameLayout
        parent.removeAllViews()

        val spaceCount = 4 // TODO replace with real data size

        fun inflateCircle(sizeDp: Int): View {
            return LayoutInflater.from(requireContext())
                .inflate(R.layout.item_space_circle, parent, false).apply {
                    val sizePx = dpToPx(sizeDp)
                    layoutParams = FrameLayout.LayoutParams(sizePx, sizePx)
                }
        }

        val view1 = inflateCircle(220)
        val view2 = inflateCircle(180)
        val view3 = inflateCircle(150)
        val view4 = inflateCircle(120)

        when (spaceCount) {
            1 -> {
                addSpaceCircle(parent, view1, 0f, 0f)
            }
            2 -> {
                addSpaceCircle(parent, view1, -0.25f, 0.1f)
                addSpaceCircle(parent, view2, 0.25f, -0.1f)
            }
            3 -> {
                addSpaceCircle(parent, view1, 0f, -0.25f)
                addSpaceCircle(parent, view2, -0.35f, 0.25f)
                addSpaceCircle(parent, view3, 0.35f, 0.25f)
            }
            4 -> {
                addSpaceCircle(parent, view1, 0f, -0.3f)
                addSpaceCircle(parent, view2, -0.45f, 0.2f)
                addSpaceCircle(parent, view3, 0.45f, 0.2f)
                addSpaceCircle(parent, view4, 0f, 0.55f)
            }
        }
    }
}