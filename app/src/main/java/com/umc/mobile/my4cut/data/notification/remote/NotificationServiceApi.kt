package com.umc.mobile.my4cut.data.notification.remote

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object NotificationServiceApi {

    private const val BASE_URL = "https://api.my4cut.shop/"

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val service: NotificationService by lazy {
        retrofit.create(NotificationService::class.java)
    }
}