package com.umc.mobile.my4cut.data.workspace.remote

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object WorkspaceServiceApi {
    private const val BASE_URL = "https://api.my4cut.shop/"

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val service: WorkspaceService by lazy {
        retrofit.create(WorkspaceService::class.java)
    }

    val workspaceService: WorkspaceService by lazy {
        service
    }
}