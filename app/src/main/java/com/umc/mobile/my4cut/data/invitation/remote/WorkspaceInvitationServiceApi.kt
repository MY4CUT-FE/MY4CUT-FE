package com.umc.mobile.my4cut.data.invitation.remote

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object WorkspaceInvitationServiceApi {

    private const val BASE_URL = "https://api.my4cut.shop/"

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val service: WorkspaceInvitationService by lazy {
        retrofit.create(WorkspaceInvitationService::class.java)
    }
}