package com.umc.mobile.my4cut.ui.mypage

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.exifinterface.media.ExifInterface
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.umc.mobile.my4cut.R
import com.umc.mobile.my4cut.data.base.BaseResponse
import com.umc.mobile.my4cut.data.user.model.NicknameRequest
import com.umc.mobile.my4cut.data.user.model.ProfileImageResponse
import com.umc.mobile.my4cut.data.user.model.UserMeResponse
import com.umc.mobile.my4cut.databinding.ActivityEditProfileBinding
import com.umc.mobile.my4cut.network.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

class EditProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditProfileBinding
    private var selectedImageUri: Uri? = null

    private val imagePickerLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) {
                selectedImageUri = uri
                // ✅ Glide 사용 (기존 코드와 동일)
                com.bumptech.glide.Glide.with(this)
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

    private fun initView() {
        val nickname = intent.getStringExtra("nickname")
        binding.etNickname.setText(nickname)
    }

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
                // 이미지 + 닉네임 함께 수정
                updateProfileImage(selectedImageUri!!) {
                    updateNicknameOnly(nickname)
                }
            } else {
                // 닉네임만 수정
                updateNicknameOnly(nickname)
            }
        }
    }

    private fun loadMyInfo() {
        RetrofitClient.userService.getMyPage()
            .enqueue(object : Callback<BaseResponse<UserMeResponse>> {
                override fun onResponse(
                    call: Call<BaseResponse<UserMeResponse>>,
                    response: Response<BaseResponse<UserMeResponse>>
                ) {
                    val body = response.body()?.data ?: return

                    if (!body.profileImageViewUrl.isNullOrEmpty()) {
                        com.bumptech.glide.Glide.with(this@EditProfileActivity)
                            .load(body.profileImageViewUrl)
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
                    binding.ivProfile.setImageResource(R.drawable.img_profile_default)
                }
            })
    }

    /**
     * ✅ 프로필 이미지 업로드 (multipart/form-data로 직접 업로드)
     */
    private fun updateProfileImage(uri: Uri, onSuccess: () -> Unit) {
        lifecycleScope.launch {
            try {
                Log.d("EditProfile", "🔄 Profile image upload started")

                // 1. 이미지 압축
                val compressedFile = compressImage(uri)
                if (compressedFile == null) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@EditProfileActivity, "이미지 압축 실패", Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }

                // 2. MultipartBody 생성
                val requestBody = compressedFile.asRequestBody("image/webp".toMediaTypeOrNull())
                val filePart = MultipartBody.Part.createFormData("file", compressedFile.name, requestBody)

                Log.d("EditProfile", "📤 Uploading to PATCH /users/me/image")

                // 3. PATCH /users/me/image 호출 (multipart)
                val response = withContext(Dispatchers.IO) {
                    RetrofitClient.userService.updateProfileImageMultipart(filePart)
                }

                withContext(Dispatchers.Main) {
                    Log.d("EditProfile", "📨 Response code: ${response.code()}")
                    Log.d("EditProfile", "Response body: ${response.body()}")

                    if (response.isSuccessful && response.body()?.data != null) {
                        val imageData = response.body()!!.data!!
                        Log.d("EditProfile", "✅ Profile image updated")
                        Log.d("EditProfile", "   fileKey: ${imageData.fileKey}")
                        Log.d("EditProfile", "   viewUrl: ${imageData.viewUrl}")

                        compressedFile.delete()
                        onSuccess()
                    } else {
                        Log.e("EditProfile", "Error body: ${response.errorBody()?.string()}")
                        Toast.makeText(
                            this@EditProfileActivity,
                            "프로필 이미지 변경 실패 (${response.code()})",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

            } catch (e: Exception) {
                Log.e("EditProfile", "💥 Upload failed", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@EditProfileActivity, "업로드 실패: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    /**
     * 이미지 압축
     */
    private fun compressImage(uri: Uri): File? {
        return try {
            val inputStream = contentResolver.openInputStream(uri) ?: return null
            val originalBitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()

            if (originalBitmap == null) return null

            val rotatedBitmap = rotateImageIfRequired(uri, originalBitmap)
            val resizedBitmap = resizeBitmap(rotatedBitmap, 1920)

            val outputStream = ByteArrayOutputStream()
            resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
            val compressedBytes = outputStream.toByteArray()

            val tempFile = File(cacheDir, "profile_${System.currentTimeMillis()}.jpg")
            FileOutputStream(tempFile).use { fos ->
                fos.write(compressedBytes)
            }

            if (rotatedBitmap != originalBitmap) originalBitmap.recycle()
            resizedBitmap.recycle()

            tempFile
        } catch (e: Exception) {
            Log.e("EditProfile", "Compression failed", e)
            null
        }
    }

    private fun rotateImageIfRequired(uri: Uri, bitmap: Bitmap): Bitmap {
        return try {
            val inputStream = contentResolver.openInputStream(uri) ?: return bitmap
            val exif = ExifInterface(inputStream)
            inputStream.close()

            val orientation = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_UNDEFINED
            )

            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> rotateBitmap(bitmap, 90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> rotateBitmap(bitmap, 180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> rotateBitmap(bitmap, 270f)
                else -> bitmap
            }
        } catch (e: Exception) {
            bitmap
        }
    }

    private fun rotateBitmap(bitmap: Bitmap, degrees: Float): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(degrees)
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private fun resizeBitmap(bitmap: Bitmap, maxSize: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height

        if (width <= maxSize && height <= maxSize) return bitmap

        val ratio = minOf(
            maxSize.toFloat() / width,
            maxSize.toFloat() / height
        )

        val newWidth = (width * ratio).toInt()
        val newHeight = (height * ratio).toInt()

        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    /**
     * 닉네임만 수정
     */
    private fun updateNicknameOnly(nickname: String) {
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
                        // ✅ 프로필 이미지도 함께 전달
                        selectedImageUri?.let {
                            putExtra("profile_image", it.toString())
                        }
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
                        "닉네임 변경 실패 (${response.code()})",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun onFailure(call: Call<BaseResponse<UserMeResponse>>, t: Throwable) {
                Toast.makeText(this@EditProfileActivity, "네트워크 오류", Toast.LENGTH_SHORT).show()
            }
        })
    }
}