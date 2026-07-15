package com.umc.mobile.my4cut.ui.space

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.umc.mobile.my4cut.R
import com.umc.mobile.my4cut.databinding.FragmentMySpaceBinding
import com.umc.mobile.my4cut.network.RetrofitClient
import com.umc.mobile.my4cut.ui.space.model.Space
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.ZoneOffset

class MySpaceFragment : Fragment() {

    private val spaces = mutableListOf<Space>()

    private var _binding: FragmentMySpaceBinding? = null
    private val binding get() = _binding!!

    private lateinit var mySpaceAdapter: MySpaceAdapter
    private var currentPage = 0

    private val refreshHandler = Handler(Looper.getMainLooper())
    private val refreshRunnable = object : Runnable {
        override fun run() {
            loadSpacesFromApi()
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

        setupRecyclerView()
        setupAddButton()
        setupFragmentResultListener()
    }

    override fun onStart() {
        super.onStart()
        loadSpacesFromApi()
        refreshHandler.postDelayed(refreshRunnable, 30_000)
    }

    override fun onStop() {
        super.onStop()
        refreshHandler.removeCallbacks(refreshRunnable)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupRecyclerView() {
        mySpaceAdapter = MySpaceAdapter { space ->
            moveToSpaceDetail(space)
        }

        binding.rvMySpaces.apply {
            adapter = mySpaceAdapter
            layoutManager = LinearLayoutManager(
                requireContext(),
                LinearLayoutManager.HORIZONTAL,
                false
            )
            overScrollMode = View.OVER_SCROLL_NEVER

            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)

                    val layoutManager = recyclerView.layoutManager as? LinearLayoutManager ?: return
                    val firstVisiblePosition = layoutManager.findFirstVisibleItemPosition()

                    if (firstVisiblePosition != RecyclerView.NO_POSITION && currentPage != firstVisiblePosition) {
                        currentPage = firstVisiblePosition
                        updateIndicator()
                    }
                }
            })
        }
    }

    private fun setupAddButton() {
        binding.tvAddSpace.setOnClickListener {
            if (spaces.size >= MAX_SPACE_COUNT) {
                Toast.makeText(
                    requireContext(),
                    "스페이스는 최대 4개까지 생성할 수 있어요",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            CreateSpaceDialogFragment()
                .show(parentFragmentManager, "CreateSpaceDialog")
        }
    }

    private fun setupFragmentResultListener() {
        parentFragmentManager.setFragmentResultListener(
            "SPACE_CREATED",
            viewLifecycleOwner
        ) { _, _ ->
            loadSpacesFromApi()
        }
    }

    private fun loadSpacesFromApi() {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.workspaceService.getMyWorkspaces()

                if (response.data == null) {
                    android.util.Log.e("SPACE_API", "response.data is null")
                    return@launch
                }

                spaces.clear()
                spaces.addAll(
                    response.data.map {
                        Space(
                            id = it.id,
                            name = it.name,
                            currentMember = it.memberCount ?: 1,
                            maxMember = 10,
                            createdAt = parseIsoToMillis(it.createdAt),
                            expiredAt = parseIsoToMillis(it.expiresAt),
                            memberProfileImageUrls = it.memberProfiles.orEmpty(),
                            recentActivityType = it.recentActivityType,
                            recentActivityUserNickname = it.recentActivityUserNickname,
                            recentActivityAt = it.recentActivityAt
                        )
                    }
                )

                currentPage = 0
                mySpaceAdapter.submitList(spaces.take(MAX_SPACE_COUNT))
                updateIndicator()
            } catch (e: Exception) {
                android.util.Log.e("SPACE_API", "API 호출 실패", e)
            }
        }
    }

    private fun updateIndicator() {
        binding.layoutSpaceIndicator.removeAllViews()

        spaces.take(MAX_SPACE_COUNT).forEachIndexed { index, _ ->
            binding.layoutSpaceIndicator.addView(
                createIndicatorDot(isSelected = index == currentPage)
            )
        }
    }

    private fun createIndicatorDot(isSelected: Boolean): View {
        return View(requireContext()).apply {
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(
                    Color.parseColor(
                        if (isSelected) "#FF8E7F" else "#DADADA"
                    )
                )
            }

            layoutParams = LinearLayout.LayoutParams(
                dpToPx(7),
                dpToPx(7)
            ).apply {
                marginStart = dpToPx(4)
                marginEnd = dpToPx(4)
            }
        }
    }

    private fun moveToSpaceDetail(space: Space) {
        requireActivity()
            .supportFragmentManager
            .beginTransaction()
            .replace(
                R.id.fcv_main,
                SpaceFragment.newInstance(spaceId = space.id)
            )
            .addToBackStack("SpaceFragment")
            .commit()
    }

    private fun parseIsoToMillis(iso: String): Long {
        return try {
            LocalDateTime.parse(iso)
                .atOffset(ZoneOffset.UTC)
                .toInstant()
                .toEpochMilli()
        } catch (e: Exception) {
            android.util.Log.e("SPACE_API", "날짜 파싱 실패: $iso", e)
            System.currentTimeMillis()
        }
    }

    private fun dpToPx(dp: Int): Int =
        (dp * resources.displayMetrics.density).toInt()

    companion object {
        private const val MAX_SPACE_COUNT = 4
    }
}