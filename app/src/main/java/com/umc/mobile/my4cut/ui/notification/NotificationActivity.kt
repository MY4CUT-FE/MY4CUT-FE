package com.umc.mobile.my4cut.ui.notification

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.umc.mobile.my4cut.R
import com.umc.mobile.my4cut.databinding.ActivityNotificationBinding
import android.widget.Toast
import android.util.Log
import androidx.lifecycle.lifecycleScope
import com.umc.mobile.my4cut.network.RetrofitClient
import kotlinx.coroutines.launch

class NotificationActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNotificationBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNotificationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initClickListener()
        setupRecyclerView()
    }

    private fun initClickListener() {
        // 뒤로가기 버튼 클릭 시 종료
        binding.btnBack.setOnClickListener {
            finish()
        }
    }

    private fun setupRecyclerView() {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.notificationService.getNotifications()
                Log.d("NotificationAPI", "responseData=" + response.data)
                // Log each notification DTO in detail to inspect null fields
                response.data?.forEach { dto ->
                    Log.d(
                        "NotificationRaw",
                        "notificationId=${dto.notificationId}, type=${dto.type}, senderNickname=${dto.senderNickname}, workspaceName=${dto.workspaceName}, message=${dto.message}"
                    )
                }

                if (response.code.startsWith("C2") && response.data != null) {

                    val uiList = response.data.map { dto ->
                        Log.d(
                            "NotificationDebug",
                            "type=${dto.type}, referenceId=${dto.referenceId}, notificationId=${dto.notificationId}, senderNickname=${dto.senderNickname}, workspaceName=${dto.workspaceName}, message=${dto.message}"
                        )
                        NotificationData(
                            id = dto.referenceId ?: dto.notificationId,
                            type = dto.type,
                            iconResId = when (dto.type) {
                                "WORKSPACE_INVITE" -> R.drawable.ic_noti_invite
                                "FRIEND_REQUEST" -> R.drawable.ic_noti_friend_add
                                "MEDIA_COMMENT" -> R.drawable.ic_noti_comment
                                else -> R.drawable.ic_noti_people
                            },
                            category = when (dto.type) {
                                "FRIEND_REQUEST" -> "친구"
                                "WORKSPACE_INVITE" -> "초대"
                                "MEDIA_COMMENT" -> "댓글"
                                else -> dto.type
                            },
                            content = when (dto.type) {
                                "WORKSPACE_INVITE" -> {
                                    val sender = dto.senderNickname ?: "누군가"
                                    val workspace = dto.workspaceName ?: "워크스페이스"
                                    "${sender}님이 ${workspace}에 초대했습니다."
                                }
                                "FRIEND_REQUEST" -> dto.message ?: "친구 요청이 도착했습니다."
                                else -> dto.message ?: "알림이 도착했습니다."
                            },
                            time = dto.createdAt?.let { formatTimeAgo(it) } ?: "방금 전",
                            hasButtons = dto.type == "FRIEND_REQUEST" || dto.type == "WORKSPACE_INVITE"
                        )
                    }.toMutableList()

                    val adapter = NotificationAdapter(
                        uiList,
                        onAcceptClick = { item ->
                            lifecycleScope.launch {
                                try {
                                    Log.d("NotificationClick", "ACCEPT type=${item.type}, id=${item.id}")
                                    when (item.type) {
                                        "FRIEND_REQUEST" -> {
                                            RetrofitClient.friendService.acceptFriendRequest(item.id)
                                        }
                                        "WORKSPACE_INVITE" -> {
                                            RetrofitClient.workspaceInvitationService.acceptInvitation(item.id)
                                        }
                                    }
                                    Toast.makeText(this@NotificationActivity, "수락 처리되었습니다.", Toast.LENGTH_SHORT).show()
                                    val index = uiList.indexOf(item)
                                    if (index != -1) {
                                        uiList.removeAt(index)
                                        binding.rvNotification.adapter?.notifyItemRemoved(index)
                                    }
                                } catch (e: Exception) {
                                    Toast.makeText(this@NotificationActivity, "수락 실패: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        onDeclineClick = { item ->
                            lifecycleScope.launch {
                                try {
                                    Log.d("NotificationClick", "DECLINE type=${item.type}, id=${item.id}")
                                    when (item.type) {
                                        "FRIEND_REQUEST" -> {
                                            RetrofitClient.friendService.rejectFriendRequest(item.id)
                                        }
                                        "WORKSPACE_INVITE" -> {
                                            RetrofitClient.workspaceInvitationService.rejectInvitation(item.id)
                                        }
                                    }
                                    Toast.makeText(this@NotificationActivity, "거절 처리되었습니다.", Toast.LENGTH_SHORT).show()
                                    val index = uiList.indexOf(item)
                                    if (index != -1) {
                                        uiList.removeAt(index)
                                        binding.rvNotification.adapter?.notifyItemRemoved(index)
                                    }
                                } catch (e: Exception) {
                                    Toast.makeText(this@NotificationActivity, "거절 실패: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    )
                    binding.rvNotification.adapter = adapter
                    binding.rvNotification.layoutManager = LinearLayoutManager(this@NotificationActivity)
                }

            } catch (e: Exception) {
                Log.e("NotificationAPI", "error=" + e.message, e)
                Toast.makeText(this@NotificationActivity, "네트워크 오류: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun formatTimeAgo(createdAt: String): String {
        return try {
            val formatter = java.time.format.DateTimeFormatter.ISO_DATE_TIME
            val created = java.time.LocalDateTime.parse(createdAt, formatter)
            val now = java.time.LocalDateTime.now()

            val minutes = java.time.Duration.between(created, now).toMinutes()

            when {
                minutes < 1 -> "방금 전"
                minutes < 60 -> "${minutes}분 전"
                minutes < 60 * 24 -> "${minutes / 60}시간 전"
                else -> "${minutes / (60 * 24)}일 전"
            }
        } catch (e: Exception) {
            "방금 전"
        }
    }
}