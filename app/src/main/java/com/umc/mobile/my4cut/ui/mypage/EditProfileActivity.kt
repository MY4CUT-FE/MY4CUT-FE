package com.umc.mobile.my4cut.ui.mypage

import android.content.Intent
import android.os.Bundle
import android.provider.MediaStore
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.umc.mobile.my4cut.databinding.ActivityEditProfileBinding

class EditProfileActivity : AppCompatActivity() {
    private lateinit var binding: ActivityEditProfileBinding

    // 갤러리 런처
    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val imageUri = result.data?.data
            imageUri?.let {
                Glide.with(this).load(it).circleCrop().into(binding.ivProfile)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 마이페이지에서 받아온 기존 닉네임 표시
        val currentNickname = intent.getStringExtra("nickname")
        binding.etNickname.setText(currentNickname)

        initClickListener()
    }

    private fun initClickListener() {
        binding.btnBack.setOnClickListener { finish() }

        // 프로필 사진 변경
        val imageClickListener = {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            galleryLauncher.launch(intent)
        }
        binding.ivProfile.setOnClickListener { imageClickListener() }
        binding.ivEditIcon.setOnClickListener { imageClickListener() }

        // 확인 버튼: 변경된 닉네임을 결과로 담아서 종료
        binding.btnConfirm.setOnClickListener {
            val newNickname = binding.etNickname.text.toString()
            val resultIntent = Intent()
            resultIntent.putExtra("nickname", newNickname)
            setResult(RESULT_OK, resultIntent) // 결과 설정
            finish() // 액티비티 종료 -> 마이페이지로 복귀
        }
    }
}