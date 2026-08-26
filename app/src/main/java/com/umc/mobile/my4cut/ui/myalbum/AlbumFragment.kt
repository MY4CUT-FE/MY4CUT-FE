package com.umc.mobile.my4cut.ui.myalbum

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.umc.mobile.my4cut.R
import com.umc.mobile.my4cut.databinding.DialogChangeBinding
import com.umc.mobile.my4cut.databinding.FragmentAlbumBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.umc.mobile.my4cut.data.album.model.AlbumNameRequest
import com.umc.mobile.my4cut.data.album.model.AlbumRequest
import com.umc.mobile.my4cut.data.album.model.AlbumResponse
import com.umc.mobile.my4cut.data.auth.local.TokenManager
import com.umc.mobile.my4cut.data.base.BaseResponse
import com.umc.mobile.my4cut.network.RetrofitClient
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class AlbumFragment : Fragment() {
    lateinit var binding: FragmentAlbumBinding
    private val albumList = mutableListOf<AlbumResponse>()
    private lateinit var albumAdapter: AlbumRVAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentAlbumBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        fetchAlbumList()

        // 상세 화면에서 수정/삭제/추가 작업이 있었다는 신호를 받으면 새로고침
        parentFragmentManager.setFragmentResultListener("album_changed", viewLifecycleOwner) { _, _ ->
            fetchAlbumList()
        }

        binding.btnCreateAlbum.setOnClickListener { showCreateDialog() }

        // 당겨서 새로고침
        binding.swipeRefresh.setOnRefreshListener {
            fetchAlbumList()
        }
    }

    override fun onResume() {
        super.onResume()
        fetchAlbumList()
    }

    // 부모(CalendarMainFragment)가 화면 재진입 시 직접 호출하는 새로고침 진입점.
    fun refresh() {
        if (!isAdded) return
        fetchAlbumList()
    }

    // 앨범을 눌렀을 때, 상세 프래그먼트에 전달해주는 내용
    private fun setupRecyclerView() {
        albumAdapter = AlbumRVAdapter(albumList) { selected ->
            navigateToDetail(selected.id, selected.name)
        }

        binding.rvAlbums.adapter = albumAdapter
    }

    // [GET] 앨범 목록 조회
    private fun fetchAlbumList() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = RetrofitClient.albumService.getAlbums()
                albumList.clear()
                response.data?.let { albumList.addAll(it) }

                // 🔍 진단용: 서버가 실제로 돌려준 이름들을 그대로 찍음
                // (여기 찍히는 이름이 아직 옛날 이름이면 → 서버/캐시 문제, 새 이름이면 → UI 바인딩 문제)
                Log.d("ALBUM_DEBUG", "받아온 앨범 목록: ${albumList.map { it.id to it.name }}")

                albumAdapter.notifyDataSetChanged()

                // 앨범이 하나도 없을 때만 빈 상태 캐릭터+텍스트 표시
                val emptyVisibility = if (albumList.isEmpty()) View.VISIBLE else View.GONE
                binding.ivEmptyState.visibility = emptyVisibility
                binding.tvEmptyState.visibility = emptyVisibility
            } catch (e: Exception) {
                Log.e("API_ERROR", "목록 조회 실패: ${e.message}")
            } finally {
                binding.swipeRefresh.isRefreshing = false
            }
        }
    }

    // [POST] 앨범 생성
    private fun createNewAlbum(name: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = RetrofitClient.albumService.createAlbum(AlbumNameRequest(name))
                val newAlbum = response.data
                if (newAlbum != null) {
                    // 상세 화면으로 이동 (ID와 이름 전달)
                    navigateToDetail(newAlbum.id, newAlbum.name)
                }
            } catch (e: Exception) {
                Log.e("API_ERROR", "앨범 생성 실패: ${e.message}")
            }
        }
    }

    private fun showCreateDialog() {
        val dialogBinding = DialogChangeBinding.inflate(layoutInflater)
        val builder = MaterialAlertDialogBuilder(requireContext())
            .setView(dialogBinding.root)
        val dialog = builder.create()

        dialog.setCanceledOnTouchOutside(true)

        dialogBinding.btnNext.setOnClickListener {
            val name = dialogBinding.etSpaceName.text.toString()

            if (name.isNotEmpty()) {
                createNewAlbum(name)

                dialog.dismiss()
            }
        }

        dialogBinding.ivClose.setOnClickListener { dialog.dismiss() }

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        dialog.show()
    }


    // 앨범 생성 모달에서 다음 버튼을 눌렀을 때 전달해주는 내용
    private fun navigateToDetail(albumId: Int, title: String) {
        val fragment = AlbumDetailFragment().apply {
            arguments = Bundle().apply {
                putInt("ALBUM_ID", albumId)
                putString("ALBUM_TITLE", title)
            }
        }

        requireActivity().supportFragmentManager.beginTransaction()
            .replace(R.id.fcv_main, fragment)
            .addToBackStack(null)
            .commit()
    }
}