package com.umc.mobile.my4cut.data.base

data class BaseResponse<T>(
    val code: String,
    val message: String,
    val data: T?
)
