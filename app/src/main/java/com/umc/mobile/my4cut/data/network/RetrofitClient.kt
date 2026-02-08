package com.umc.mobile.my4cut.network

import android.content.Context
import android.util.Log
import com.umc.mobile.my4cut.data.auth.local.TokenManager
import com.umc.mobile.my4cut.data.auth.remote.AuthService
import com.umc.mobile.my4cut.data.user.remote.UserService
import com.umc.mobile.my4cut.data.friend.remote.FriendService
import com.umc.mobile.my4cut.data.photo.remote.WorkspacePhotoService
import com.umc.mobile.my4cut.data.workspace.remote.WorkspaceService
import com.umc.mobile.my4cut.data.invitation.remote.WorkspaceInvitationService
import com.umc.mobile.my4cut.data.notification.remote.NotificationService
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {

    private const val BASE_URL = "https://api.my4cut.shop/"
    private lateinit var appContext: Context

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    fun init(context: Context) {
        appContext = context.applicationContext
        Log.d("RetrofitClient", "✅ Context initialized")
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

    val authServiceNoAuth: AuthService =
        noAuthRetrofit.create(AuthService::class.java)

    /* ------------------ 인증 필요한 Client ------------------ */
    private val authInterceptor = Interceptor { chain ->
        if (!::appContext.isInitialized) {
            Log.e("RetrofitClient", "❌ appContext is NOT initialized!")
            return@Interceptor chain.proceed(chain.request())
        }

        val token = TokenManager.getAccessToken(appContext)

        Log.d("RetrofitClient", "🔑 Token from Interceptor: '$token'")

        val request = if (!token.isNullOrEmpty()) {
            val newRequest = chain.request().newBuilder()
                .addHeader("Authorization", "Bearer $token")
                .build()
            Log.d("RetrofitClient", "✅ Authorization Header Added")
            newRequest
        } else {
            Log.e("RetrofitClient", "❌ Token is null or empty! Headers NOT added")
            chain.request()
        }

        chain.proceed(request)
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

    val authService: AuthService =
        authRetrofit.create(AuthService::class.java)

    val userService: UserService =
        authRetrofit.create(UserService::class.java)

    val friendService: FriendService =
        authRetrofit.create(FriendService::class.java)

    val workspaceService: WorkspaceService =
        authRetrofit.create(WorkspaceService::class.java)

    val workspacePhotoService: WorkspacePhotoService =
        authRetrofit.create(WorkspacePhotoService::class.java)

    val workspaceInvitationService: WorkspaceInvitationService =
        authRetrofit.create(WorkspaceInvitationService::class.java)

    val notificationService: NotificationService =
        authRetrofit.create(NotificationService::class.java)
}