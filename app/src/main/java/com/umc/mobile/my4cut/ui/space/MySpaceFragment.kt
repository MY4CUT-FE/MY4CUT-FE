package com.umc.mobile.my4cut.ui.space

import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.umc.mobile.my4cut.network.RetrofitClient

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
            // 만료 자동 삭제 로직 비활성화 (서버 기준으로만 관리)
            updateSpaceUi()
            expireHandler.postDelayed(this, 60_000)
        }
    }

    // Handler & Runnable for periodic API refresh
    private val refreshHandler = Handler(Looper.getMainLooper())
    private val refreshRunnable = object : Runnable {
        override fun run() {
            loadSpacesFromApi()
            // Refresh every 30 seconds
            refreshHandler.postDelayed(this, 30_000)
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

        parentFragmentManager.setFragmentResultListener(
            "SPACE_CREATED",
            viewLifecycleOwner
        ) { _, _ ->
            loadSpacesFromApi()
        }

        binding.tvAddSpace.setOnClickListener {
            if (spaces.size >= 4) return@setOnClickListener

            val dialog = CreateSpaceDialogFragment()
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
                            SpaceFragment.newInstance(
                                spaceId = space.id.toLong()
                            )
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
            id = (spaces.size + 1).toLong(),
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
        // 서버에서 만료 관리하므로 클라이언트에서 삭제하지 않음
    }

    override fun onStart() {
        super.onStart()
        expireHandler.post(expireCheckRunnable)
        loadSpacesFromApi()
        refreshHandler.postDelayed(refreshRunnable, 30_000)
    }

    private fun loadSpacesFromApi() {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.workspaceService.getMyWorkspaces()

                android.util.Log.d("SPACE_API", "전체 response = $response")
                android.util.Log.d("SPACE_API", "code = ${response.code}")
                android.util.Log.d("SPACE_API", "data = ${response.data}")

                if (response.data != null) {
                    spaces.clear()
                    spaces.addAll(
                        response.data.map {
                            android.util.Log.d("SPACE_API", "workspace item = $it")
                            android.util.Log.d(
                                "SPACE_API",
                                "workspace ownerId=${it.ownerId}, id=${it.id}, name=${it.name}, expiresAt=${it.expiresAt}, memberCount=${it.memberCount}"
                            )
                            Space(
                                id = it.id,
                                name = it.name,
                                currentMember = it.memberCount ?: 1,
                                maxMember = 10,
                                createdAt = parseIsoToMillis(it.createdAt),
                                expiredAt = parseIsoToMillis(it.expiresAt)
                            )
                        }
                    )

                    android.util.Log.d("SPACE_API", "spaces.size = ${spaces.size}")
                    updateSpaceUi()
                } else {
                    android.util.Log.e("SPACE_API", "response.data is null")
                }
            } catch (e: Exception) {
                android.util.Log.e("SPACE_API", "API 호출 실패", e)
            }
        }
    }

    private fun parseIsoToMillis(iso: String): Long {
        return try {
            // 서버 시간이 timezone 없이 내려오기 때문에 LocalDateTime으로 파싱 후
            // 시스템 기본 timezone을 적용해서 millis로 변환
            val localDateTime = java.time.LocalDateTime.parse(iso)
            val zoned = localDateTime.atZone(java.time.ZoneId.systemDefault())
            zoned.toInstant().toEpochMilli()
        } catch (e: Exception) {
            android.util.Log.e("SPACE_API", "날짜 파싱 실패: $iso", e)
            // 파싱 실패 시 바로 만료 처리되지 않도록 현재 시각 + 1일을 기본값으로 사용
            System.currentTimeMillis() + (24L * 60 * 60 * 1000)
        }
    }

    override fun onStop() {
        super.onStop()
        expireHandler.removeCallbacks(expireCheckRunnable)
        refreshHandler.removeCallbacks(refreshRunnable)
    }
}