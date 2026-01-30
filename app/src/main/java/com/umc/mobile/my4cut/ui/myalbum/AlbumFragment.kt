package com.umc.mobile.my4cut.ui.myalbum

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

        parentFragmentManager.setFragmentResultListener("album_update", viewLifecycleOwner) { _, bundle ->
            val newTitle = bundle.getString("NEW_TITLE")
            if (newTitle != null) {
                val newAlbum = AlbumData(newTitle, mutableListOf())
                albumList.add(newAlbum)
                albumAdapter.notifyItemInserted(albumList.size - 1)
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
                .replace(R.id.fcv_main, fragment)
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
            .replace(R.id.fcv_main, fragment)
            .addToBackStack(null)
            .commit()
    }
}