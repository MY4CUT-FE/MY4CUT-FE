package com.umc.mobile.my4cut.ui.myalbum

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.umc.mobile.my4cut.R
import com.umc.mobile.my4cut.databinding.DialogChangeBinding
import com.umc.mobile.my4cut.databinding.FragmentAlbumBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class AlbumFragment : Fragment() {
    lateinit var binding: FragmentAlbumBinding
    private val albumList = mutableListOf<AlbumData>()
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
        loadInitialData()

        parentFragmentManager.setFragmentResultListener("album_complete", viewLifecycleOwner) { _, bundle ->
            val title = bundle.getString("ALBUM_TITLE")
            // Uri(String) 대신 Int 리스트로 받기
            val photoResIds = bundle.getIntegerArrayList("PHOTO_RES_IDS") ?: arrayListOf()

            // 리스트에서 해당 타이틀을 가진 앨범을 찾음
            val index = albumList.indexOfFirst { it.title == title }

            if (index != -1) {
                // 1. 기존 앨범 수정: 사진 리스트와 대표 이미지 업데이트
                albumList[index].photoResIds.clear()
                albumList[index].photoResIds.addAll(photoResIds)
                // coverResId는 데이터 클래스 구조에 따라 새로 할당 (첫 번째 사진을 대표로)
                // 주의: AlbumData의 coverResId가 val이면 var로 바꾸거나 객체를 새로 생성해야 할 수 있습니다.
                albumAdapter.notifyItemChanged(index)
            } else {
                // 2. 새로운 앨범 생성: 리스트에 추가
                if (!title.isNullOrEmpty()) {
                    val newAlbum = AlbumData(
                        title = title,
                        photoResIds = photoResIds.toMutableList(),
                        coverResId = photoResIds.firstOrNull() // 첫 사진을 대표 이미지로
                    )
                    albumList.add(newAlbum)
                    albumAdapter.notifyItemInserted(albumList.size - 1)
                }
            }
        }

        parentFragmentManager.setFragmentResultListener("album_delete", viewLifecycleOwner) { _, bundle ->
            val deleteTitle = bundle.getString("DELETE_TITLE")

            val index = albumList.indexOfFirst { it.title == deleteTitle }
            if (index != -1) {
                albumList.removeAt(index)
                albumAdapter.notifyItemRemoved(index)
            }
        }

        binding.btnCreateAlbum.setOnClickListener {
            showChangeDialog()
        }
    }

    // 앨범을 눌렀을 때, 상세 프래그먼트에 전달해주는 내용
    private fun setupRecyclerView() {
        albumAdapter = AlbumRVAdapter(albumList) { selectedAlbum ->
            val fragment = AlbumDetailFragment().apply {
                arguments = Bundle().apply {
                    putString("ALBUM_TITLE", selectedAlbum.title)
                    putIntegerArrayList("PHOTO_RES_IDS", ArrayList(selectedAlbum.photoResIds))
                }
            }

            requireActivity().supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit()
        }

        binding.rvAlbums.adapter = albumAdapter
    }

    private fun loadInitialData() {
        val dummyPhotos = mutableListOf(
            R.drawable.image1,
            R.drawable.image2,
            R.drawable.image3,
            R.drawable.image4
        )

        albumList.clear()

        albumList.add(AlbumData("ALL", dummyPhotos, dummyPhotos.firstOrNull()))

        albumAdapter.notifyDataSetChanged()
    }

    private fun showChangeDialog() {
        val dialogBinding = DialogChangeBinding.inflate(layoutInflater)
        val builder = MaterialAlertDialogBuilder(requireContext())
            .setView(dialogBinding.root)
        val dialog = builder.create()

        dialog.setCanceledOnTouchOutside(true)

        dialogBinding.btnNext.setOnClickListener {
            val name = dialogBinding.etSpaceName.text.toString()

            if (name.isNotEmpty()) {
                val newAlbum = AlbumData(name, mutableListOf())

                albumList.add(newAlbum)
                albumAdapter.notifyItemInserted(albumList.size - 1)

                dialog.dismiss()

                navigateToDetail(newAlbum)
            }
        }

        dialogBinding.ivClose.setOnClickListener { dialog.dismiss() }

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        dialog.show()
    }


    // 앨범 생성 모달에서 다음 버튼을 눌렀을 때 전달해주는 내용
    private fun navigateToDetail(album: AlbumData) {
        val fragment = AlbumDetailFragment().apply {
            arguments = Bundle().apply {
                putString("ALBUM_TITLE", album.title)
            }
        }

        requireActivity().supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .addToBackStack(null)
            .commit()
    }
}