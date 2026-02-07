package com.umc.mobile.my4cut.network

import com.umc.mobile.my4cut.data.friend.remote.FriendService
import com.umc.mobile.my4cut.data.photo.remote.WorkspacePhotoService
import com.umc.mobile.my4cut.data.workspace.remote.WorkspaceService
import com.umc.mobile.my4cut.data.invitation.remote.WorkspaceInvitationService

import android.content.Context
import com.umc.mobile.my4cut.data.auth.local.TokenManager
import com.umc.mobile.my4cut.data.auth.remote.AuthService
import com.umc.mobile.my4cut.data.user.remote.UserService
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {

    private const val BASE_URL = "https://api.my4cut.shop/"
    private lateinit var appContext: Context

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    /* ------------------ 인증 없는 Client ------------------ */
    private val noAuthClient = OkHttpClient.Builder().build()

    private val noAuthRetrofit: Retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(noAuthClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val authService: AuthService =
        noAuthRetrofit.create(AuthService::class.java)

    /* ------------------ 인증 필요한 Client ------------------ */
    private val authInterceptor = Interceptor { chain ->
        val token = TokenManager.getAccessToken(appContext)

        val request = if (!token.isNullOrEmpty()) {
            chain.request().newBuilder()
                .addHeader("Authorization", "Bearer $token")
                .build()
        } else {
            chain.request()
        }

        chain.proceed(request)
    }

    private val authClient = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .build()

    private val authRetrofit: Retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(authClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

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
}
