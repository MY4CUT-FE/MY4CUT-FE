package com.umc.mobile.my4cut.ui.mypage

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.MediaStore
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.umc.mobile.my4cut.databinding.ActivityEditProfileBinding

class EditProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditProfileBinding

    // 선택된 이미지 URI를 저장할 변수 선언
    private var selectedImageUri: android.net.Uri? = null

    // 갤러리 런처
    private val galleryLauncher = registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val uri = result.data?.data
            if (uri != null) {
                // 변수에 저장해두기
                selectedImageUri = uri

                // 화면에 즉시 반영 (선택한 사진 미리보기)
                com.bumptech.glide.Glide.with(this)
                    .load(uri)
                    .circleCrop()
                    .into(binding.ivProfile)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 초기화: 넘어온 닉네임 등을 표시
        val currentNickname = intent.getStringExtra("nickname")
        binding.etNickname.setText(currentNickname)

        initClickListener()
    }

    private fun initClickListener() {
        binding.btnBack.setOnClickListener { finish() }

        // 사진 변경 버튼
        val imageClickListener = {
            val intent = android.content.Intent(android.content.Intent.ACTION_PICK)
            intent.type = "image/*"
            galleryLauncher.launch(intent)
        }
        binding.ivProfile.setOnClickListener { imageClickListener() }
        binding.ivEditIcon.setOnClickListener { imageClickListener() }

        // 확인 버튼: 데이터 담아서 보내기
        binding.btnConfirm.setOnClickListener {
            val newNickname = binding.etNickname.text.toString()

            // 1. 변경된 닉네임을 내부 저장소에 저장
            val sharedPref = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
            with(sharedPref.edit()) {
                putString("nickname", newNickname)
                apply() // 저장 확정
            }

            // 2. 결과 인텐트 설정
            val resultIntent = Intent()
            resultIntent.putExtra("nickname", newNickname)

            if (selectedImageUri != null) {
                resultIntent.putExtra("profile_image", selectedImageUri.toString())
            }

            setResult(RESULT_OK, resultIntent)
            finish()
        }
    }
}