package com.umc.mobile.my4cut.data.photo.remote

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object WorkspacePhotoServiceApi {

    private const val BASE_URL = "https://api.my4cut.shop/"

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val service: WorkspacePhotoService by lazy {
        retrofit.create(WorkspacePhotoService::class.java)
    }
}