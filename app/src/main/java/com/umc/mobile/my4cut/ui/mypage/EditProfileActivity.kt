package com.umc.mobile.my4cut.ui.mypage

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.umc.mobile.my4cut.R
import com.umc.mobile.my4cut.data.base.BaseResponse
import com.umc.mobile.my4cut.data.user.model.ProfileImageRequest
import com.umc.mobile.my4cut.data.user.model.NicknameRequest
import com.umc.mobile.my4cut.data.user.model.UserMeResponse
import com.umc.mobile.my4cut.databinding.ActivityEditProfileBinding
import com.umc.mobile.my4cut.network.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class EditProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditProfileBinding
    private var selectedImageUri: Uri? = null

    // 갤러리 이미지 선택
    private val imagePickerLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) {
                selectedImageUri = uri
                Glide.with(this)
                    .load(uri)
                    .circleCrop()
                    .into(binding.ivProfile)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initView()
        initClickListener()
        loadMyInfo()
    }

    /** 초기 닉네임 */
    private fun initView() {
        val nickname = intent.getStringExtra("nickname")
        binding.etNickname.setText(nickname)
    }

    /** 클릭 리스너 */
    private fun initClickListener() {

        binding.btnBack.setOnClickListener { finish() }

        binding.ivProfile.setOnClickListener {
            imagePickerLauncher.launch("image/*")
        }

        binding.ivEditIcon.setOnClickListener {
            imagePickerLauncher.launch("image/*")
        }

        binding.btnConfirm.setOnClickListener {
            val nickname = binding.etNickname.text.toString()

            if (nickname.isBlank()) {
                Toast.makeText(this, "닉네임을 입력해주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (selectedImageUri != null) {
                updateProfileImage(selectedImageUri.toString()) {
                    updateNickname(nickname)
                }
            } else {
                updateNickname(nickname)
            }
        }
    }

    /** 내 정보 조회 (초기 프로필 이미지) */
    private fun loadMyInfo() {
        RetrofitClient.userService.getMyPage()
            .enqueue(object : Callback<BaseResponse<UserMeResponse>> {

                override fun onResponse(
                    call: Call<BaseResponse<UserMeResponse>>,
                    response: Response<BaseResponse<UserMeResponse>>
                ) {
                    val body = response.body()?.data ?: return

                    if (!body.profileImageUrl.isNullOrEmpty()) {
                        Glide.with(this@EditProfileActivity)
                            .load(body.profileImageUrl)
                            .circleCrop()
                            .into(binding.ivProfile)
                    } else {
                        binding.ivProfile.setImageResource(R.drawable.img_profile_default)
                    }
                }

                override fun onFailure(
                    call: Call<BaseResponse<UserMeResponse>>,
                    t: Throwable
                ) {
                    // 실패해도 기본 이미지 유지
                    binding.ivProfile.setImageResource(R.drawable.img_profile_default)
                }
            })
    }

    /** 프로필 이미지 변경 */
    private fun updateProfileImage(
        imageUrl: String,
        onSuccess: () -> Unit
    ) {
        RetrofitClient.userService.updateProfileImage(
            ProfileImageRequest(imageUrl)
        ).enqueue(object : Callback<BaseResponse<UserMeResponse>> {

            override fun onResponse(
                call: Call<BaseResponse<UserMeResponse>>,
                response: Response<BaseResponse<UserMeResponse>>
            ) {
                if (response.isSuccessful) {
                    onSuccess()
                } else {
                    Toast.makeText(
                        this@EditProfileActivity,
                        "프로필 이미지 변경 실패",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun onFailure(
                call: Call<BaseResponse<UserMeResponse>>,
                t: Throwable
            ) {
                Toast.makeText(
                    this@EditProfileActivity,
                    "네트워크 오류",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }

    /** 닉네임 변경 */
    private fun updateNickname(nickname: String) {
        RetrofitClient.userService.updateNickname(
            NicknameRequest(nickname)
        ).enqueue(object : Callback<BaseResponse<UserMeResponse>> {

            override fun onResponse(
                call: Call<BaseResponse<UserMeResponse>>,
                response: Response<BaseResponse<UserMeResponse>>
            ) {
                if (response.isSuccessful) {

                    val intent = Intent().apply {
                        putExtra("nickname", nickname)
                        putExtra("profile_image", selectedImageUri?.toString())
                    }
                    setResult(RESULT_OK, intent)

                    Toast.makeText(
                        this@EditProfileActivity,
                        "회원정보가 수정되었습니다.",
                        Toast.LENGTH_SHORT
                    ).show()

                    finish()
                } else {
                    Toast.makeText(
                        this@EditProfileActivity,
                        "닉네임 변경 실패",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun onFailure(
                call: Call<BaseResponse<UserMeResponse>>,
                t: Throwable
            ) {
                Toast.makeText(
                    this@EditProfileActivity,
                    "네트워크 오류",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }
}
