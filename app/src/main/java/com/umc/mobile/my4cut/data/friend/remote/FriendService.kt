package com.umc.mobile.my4cut.data.friend.remote

import com.umc.mobile.my4cut.data.base.BaseResponse
import com.umc.mobile.my4cut.data.friend.model.FriendDto
import com.umc.mobile.my4cut.data.friend.model.FriendRequestCreateDto
import com.umc.mobile.my4cut.data.friend.model.FriendRequestDto
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface FriendService {

    @POST("friends/{id}/favorites")
    suspend fun addFavorite(@Path("id") id: Long): BaseResponse<Unit>

    @DELETE("friends/{id}/favorites")
    suspend fun removeFavorite(@Path("id") id: Long): BaseResponse<Unit>

    @GET("friends")
    suspend fun getFriends(): BaseResponse<List<FriendDto>>

    @GET("friends/requests")
    suspend fun getRequests(): BaseResponse<List<FriendRequestDto>>

    @POST("friends/requests")
    suspend fun requestFriend(
        @Body request: FriendRequestCreateDto
    ): BaseResponse<Unit>

    @POST("friends/requests/{requestId}/accept")
    suspend fun accept(@Path("requestId") requestId: Long): BaseResponse<Unit>

    @POST("friends/requests/{requestId}/reject")
    suspend fun reject(@Path("requestId") requestId: Long): BaseResponse<Unit>

    @DELETE("friends/{friendId}")
    suspend fun deleteFriend(@Path("friendId") friendId: Long): BaseResponse<Unit>
}