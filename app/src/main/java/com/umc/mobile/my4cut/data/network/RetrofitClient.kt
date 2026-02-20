package com.umc.mobile.my4cut.network

import android.content.Context
import android.content.Intent
import android.os.Looper
import android.util.Log
import android.widget.Toast
import com.umc.mobile.my4cut.data.album.remote.AlbumService
import com.umc.mobile.my4cut.data.album.remote.ImageService
import com.umc.mobile.my4cut.data.auth.local.TokenManager
import com.umc.mobile.my4cut.data.auth.remote.AuthService
import com.umc.mobile.my4cut.data.day4cut.remote.Day4CutService
import com.umc.mobile.my4cut.data.pose.remote.PoseService
import com.umc.mobile.my4cut.data.user.remote.UserService
import com.umc.mobile.my4cut.data.friend.remote.FriendService
import com.umc.mobile.my4cut.data.photo.remote.WorkspacePhotoService
import com.umc.mobile.my4cut.data.workspace.remote.WorkspaceService
import com.umc.mobile.my4cut.data.invitation.remote.WorkspaceInvitationService
import com.umc.mobile.my4cut.data.media.remote.MediaService
import com.umc.mobile.my4cut.data.notification.remote.NotificationService
import com.umc.mobile.my4cut.data.workspace.remote.WorkspaceMemberService
import com.umc.mobile.my4cut.ui.intro.IntroActivity
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {

    private const val BASE_URL = "https://api.my4cut.shop/"
    private lateinit var appContext: Context

    fun init(context: Context) {
        appContext = context.applicationContext
        Log.d("RetrofitClient", "✅ Context initialized")
    }

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    /* ------------------ 인증 없는 Client ------------------ */
    private val noAuthClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .build()

    private val noAuthRetrofit: Retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(noAuthClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val authServiceNoAuth: AuthService = noAuthRetrofit.create(AuthService::class.java)

    /* ------------------ 인증 필요한 Client ------------------ */
    private val authInterceptor = Interceptor { chain ->
        val originalRequest = chain.request()

        if (!::appContext.isInitialized) {
            Log.e("RetrofitClient", "❌ appContext is NOT initialized!")
            return@Interceptor chain.proceed(originalRequest)
        }

        val token = TokenManager.getAccessToken(appContext)
        Log.d("RetrofitClient", "🔑 Token from Interceptor: '$token'")

        val newRequest = if (!token.isNullOrEmpty()) {
            originalRequest.newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        } else {
            Log.e("RetrofitClient", "❌ Token is null or empty!")
            originalRequest
        }

        Log.d("RetrofitClient", "📍 Request URL: ${newRequest.url}")

        val response = chain.proceed(newRequest)
        Log.d("RetrofitClient", "📥 Response Code: ${response.code}")

        // ✅ 401 에러 시 토큰 갱신 시도
        if (response.code == 401) {
            Log.d("RetrofitClient", "🔄 Token expired, attempting refresh...")

            val refreshToken = TokenManager.getRefreshToken(appContext)
            Log.d("RetrofitClient", "🔑 Refresh Token: '$refreshToken'")

            if (!refreshToken.isNullOrEmpty()) {
                val refreshResponse = try {
                    Log.d("RetrofitClient", "📤 Calling refresh API with Authorization header")

                    // ✅ Bearer 추가하여 호출
                    val result = authServiceNoAuth.refresh("Bearer $refreshToken").execute()

                    Log.d("RetrofitClient", "📥 Refresh Response Code: ${result.code()}")
                    Log.d("RetrofitClient", "📥 Refresh Response Body: ${result.body()}")

                    result
                } catch (e: Exception) {
                    Log.e("RetrofitClient", "❌ Refresh exception", e)
                    null
                }

                if (refreshResponse?.isSuccessful == true) {
                    val newTokens = refreshResponse.body()?.data
                    Log.d("RetrofitClient", "📦 New Tokens: $newTokens")

                    if (newTokens != null) {
                        // 새 토큰 저장
                        TokenManager.saveTokens(
                            appContext,
                            newTokens.accessToken,
                            newTokens.refreshToken
                        )

                        Log.d("RetrofitClient", "✅ Token refreshed successfully")

                        // 새 토큰으로 원래 요청 재시도
                        val newAuthRequest = originalRequest.newBuilder()
                            .header("Authorization", "Bearer ${newTokens.accessToken}")
                            .build()

                        response.close()
                        return@Interceptor chain.proceed(newAuthRequest)
                    } else {
                        Log.e("RetrofitClient", "❌ New tokens data is null")
                    }
                } else {
                    Log.e("RetrofitClient", "❌ Refresh response not successful or null")
                }
            } else {
                Log.e("RetrofitClient", "❌ Refresh token is null or empty")
            }

            Log.e("RetrofitClient", "❌ Token refresh failed - User needs to re-login")
        }

        response
    }

    private val authClient = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .addInterceptor(loggingInterceptor)
        .build()

    private val authRetrofit: Retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(authClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val authService: AuthService = authRetrofit.create(AuthService::class.java)
    val userService: UserService = authRetrofit.create(UserService::class.java)
    val poseService: PoseService = authRetrofit.create(PoseService::class.java)
    val day4CutService: Day4CutService = authRetrofit.create(Day4CutService::class.java)

    val imageService: ImageService = authRetrofit.create(ImageService::class.java)
    val albumService: AlbumService = authRetrofit.create(AlbumService::class.java)
    val friendService: FriendService = authRetrofit.create(FriendService::class.java)
    val workspaceService: WorkspaceService = authRetrofit.create(WorkspaceService::class.java)

    val workspacePhotoService: WorkspacePhotoService = authRetrofit.create(WorkspacePhotoService::class.java)
    val workspaceInvitationService: WorkspaceInvitationService = authRetrofit.create(WorkspaceInvitationService::class.java)
    val workspaceMemberService: WorkspaceMemberService = authRetrofit.create(WorkspaceMemberService::class.java)
    val notificationService: NotificationService = authRetrofit.create(NotificationService::class.java)
    val mediaService: MediaService = authRetrofit.create(MediaService::class.java)


}