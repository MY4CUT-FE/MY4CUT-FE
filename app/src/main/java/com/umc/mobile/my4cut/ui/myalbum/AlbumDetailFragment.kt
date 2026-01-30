package com.umc.mobile.my4cut.ui.myalbum

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.umc.mobile.my4cut.databinding.DialogChangeBinding
import com.umc.mobile.my4cut.databinding.DialogExitBinding
import com.umc.mobile.my4cut.databinding.FragmentAlbumDetailBinding
import com.umc.mobile.my4cut.databinding.ItemAlbumAddBinding
import com.umc.mobile.my4cut.databinding.ItemAlbumDetailBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class AlbumDetailFragment : Fragment() {
    private lateinit var binding: FragmentAlbumDetailBinding

    private val selectedImageUris = mutableListOf<Uri>()
    private lateinit var detailAdapter: AlbumDetailAdapter

    private val pickMultipleMedia = registerForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(50)) { uris ->
        if (uris.isNotEmpty()) {
            selectedImageUris.addAll(uris)
            detailAdapter.notifyDataSetChanged()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentAlbumDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val albumTitle = arguments?.getString("ALBUM_TITLE") ?: ""
        binding.tvTitle.text = albumTitle

        if (albumTitle == "ALL") {
            binding.btnEdit.visibility = View.GONE
            binding.btnDelete.visibility = View.GONE
        }

        binding.btnEdit.setOnClickListener { showChangeDialog() }
        binding.btnDelete.setOnClickListener { showDeleteDialog() }

        setupRecyclerView()
    }

    private fun setupRecyclerView() {
        detailAdapter = AlbumDetailAdapter(selectedImageUris) {
            pickMultipleMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }

        binding.rvAlbums.adapter = detailAdapter
    }

    private fun showChangeDialog() {
        val dialogBinding = DialogChangeBinding.inflate(layoutInflater)
        val builder = MaterialAlertDialogBuilder(requireContext())
            .setView(dialogBinding.root)
        val dialog = builder.create()

        dialog.setCanceledOnTouchOutside(true)

        dialogBinding.tvTitle.text = "앨범 이름 수정"

        val oldTitle = binding.tvTitle.text.toString()
        dialogBinding.etSpaceName.setText(oldTitle)

        // 수정한 제목을 가져와 바꾸는 로직
        dialogBinding.btnNext.setOnClickListener {
            val newName = dialogBinding.etSpaceName.text.toString()
            if (newName.isNotEmpty()) {
                binding.tvTitle.text = newName

                val result = Bundle().apply {
                    putString("OLD_TITLE", oldTitle)
                    putString("NEW_TITLE", newName)
                }
                parentFragmentManager.setFragmentResult("album_update", result)

                dialog.dismiss()
            }
        }

        dialogBinding.ivClose.setOnClickListener { dialog.dismiss() }

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        dialog.show()
    }

    private fun showDeleteDialog() {
        val dialogBinding = DialogExitBinding.inflate(layoutInflater)
        val builder = MaterialAlertDialogBuilder(requireContext())
            .setView(dialogBinding.root)
        val dialog = builder.create()

        dialog.setCanceledOnTouchOutside(true)

        dialogBinding.tvMessage.text = "삭제한 앨범은 다시 복구할 수 없어요."

        dialogBinding.btnExit.setOnClickListener {
            val result = Bundle().apply {
                putString("DELETE_TITLE", binding.tvTitle.text.toString())
            }
            parentFragmentManager.setFragmentResult("album_delete", result)

            dialog.dismiss()
            parentFragmentManager.popBackStack()
        }

        dialogBinding.btnCancel.setOnClickListener { dialog.dismiss() }

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        dialog.show()
    }

    inner class AlbumDetailAdapter(  // 앨범 상세 프래그먼트 어댑터
        private val imageUris: MutableList<Uri>,
        private val onAddClick: () -> Unit
    ) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        private val TYPE_PHOTO = 0
        private val TYPE_ADD = 1

        inner class PhotoViewHolder(val binding: ItemAlbumDetailBinding) : RecyclerView.ViewHolder(binding.root)
        inner class AddViewHolder(val binding: ItemAlbumAddBinding) : RecyclerView.ViewHolder(binding.root)

        override fun getItemViewType(position: Int): Int {
            return if (position == imageUris.size) TYPE_ADD else TYPE_PHOTO
        }

        override fun getItemCount(): Int = imageUris.size + 1

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val inflater = LayoutInflater.from(parent.context)
            return if (viewType == TYPE_PHOTO) {
                val binding = ItemAlbumDetailBinding.inflate(inflater, parent, false)
                PhotoViewHolder(binding)
            } else {
                val binding = ItemAlbumAddBinding.inflate(inflater, parent, false)
                AddViewHolder(binding)
            }
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            if (holder is PhotoViewHolder) {
                holder.binding.ivAlbumCover.setImageURI(imageUris[position])
            } else if (holder is AddViewHolder) {
                holder.itemView.setOnClickListener { onAddClick() }
            }
        }
    }
}