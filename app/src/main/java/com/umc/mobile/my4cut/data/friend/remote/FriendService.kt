package com.umc.mobile.my4cut.data.friend.remote

import com.umc.mobile.my4cut.data.base.BaseResponse
import com.umc.mobile.my4cut.data.friend.model.FriendDto
import com.umc.mobile.my4cut.data.friend.model.FriendRequestCreateDto
import com.umc.mobile.my4cut.data.friend.model.FriendRequestDto
import com.umc.mobile.my4cut.data.friend.model.RequestStatusDto
import retrofit2.http.*

interface FriendService {

    /** 친구 즐겨찾기 */
    @POST("friends/{id}/favorites")
    suspend fun addFavorite(
        @Path("id") friendId: Long
    ): BaseResponse<Unit>

    /** 친구 즐겨찾기 해제 */
    @DELETE("friends/{id}/favorites")
    suspend fun removeFavorite(
        @Path("id") friendId: Long
    ): BaseResponse<Unit>

    /** 친구 추가 요청 목록 조회 */
    @GET("friends/requests")
    suspend fun getFriendRequests(): BaseResponse<List<FriendRequestDto>>

    /** 친구 코드로 검색 */
    @GET("friends/search")
    suspend fun searchFriendByCode(
        @Query("code") code: String
    ): BaseResponse<FriendDto>

    /** 친구 추가 요청 */
    @POST("friends/requests")
    suspend fun requestFriend(
        @Body request: FriendRequestCreateDto
    ): BaseResponse<Unit>

    /** 친구 요청 수락 */
    @POST("friends/requests/{requestId}/accept")
    suspend fun acceptFriendRequest(
        @Path("requestId") requestId: Long
    ): BaseResponse<RequestStatusDto>

    /** 친구 요청 거절 */
    @POST("friends/requests/{requestId}/reject")
    suspend fun rejectFriendRequest(
        @Path("requestId") requestId: Long
    ): BaseResponse<RequestStatusDto>

    /** 보낸 친구 요청 취소 */
    @DELETE("friends/requests/{requestId}")
    suspend fun cancelFriendRequest(
        @Path("requestId") requestId: Long
    ): BaseResponse<Unit>

    /** 내 친구 목록 조회 */
    @GET("friends")
    suspend fun getFriends(): BaseResponse<List<FriendDto>>

    /** 친구 삭제 */
    @DELETE("friends/{friendId}")
    suspend fun deleteFriend(
        @Path("friendId") friendId: Long
    ): BaseResponse<Unit>
}