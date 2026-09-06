package com.umc.mobile.my4cut.ui.myalbum

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
import com.umc.mobile.my4cut.data.album.model.AlbumResponse
import com.umc.mobile.my4cut.data.network.RetrofitClient
import kotlinx.coroutines.launch

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

        parentFragmentManager.setFragmentResultListener("album_changed", viewLifecycleOwner) { _, _ ->
            fetchAlbumList()
        }

        binding.cvCreateAlbum.setOnClickListener { showCreateDialog() }

        binding.swipeRefresh.setOnRefreshListener {
            fetchAlbumList()
        }
    }

    override fun onResume() {
        super.onResume()
        fetchAlbumList()
    }

    fun refresh() {
        if (!isAdded) return
        fetchAlbumList()
    }

    private fun setupRecyclerView() {
        albumAdapter = AlbumRVAdapter(albumList) { selected ->
            navigateToDetail(selected.id, selected.name)
        }

        binding.rvAlbums.adapter = albumAdapter
    }

    private fun fetchAlbumList() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = RetrofitClient.albumService.getAlbums()
                albumList.clear()
                response.data?.let { albumList.addAll(it) }
                albumAdapter.notifyDataSetChanged()

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

    private fun createNewAlbum(name: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = RetrofitClient.albumService.createAlbum(AlbumNameRequest(name))
                val newAlbum = response.data
                if (newAlbum != null) {
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