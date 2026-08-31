package com.umc.mobile.my4cut.ui.notification

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.umc.mobile.my4cut.R
import com.umc.mobile.my4cut.databinding.ActivityNotificationBinding
import android.widget.Toast
import android.util.Log
import androidx.lifecycle.lifecycleScope
import com.umc.mobile.my4cut.data.network.RetrofitClient
import kotlinx.coroutines.launch
import android.view.View
import android.app.Dialog
import android.content.Intent
import android.view.Window
import com.umc.mobile.my4cut.MainActivity
import com.umc.mobile.my4cut.data.notification.model.NotificationMarkReadByIdsDto
import com.umc.mobile.my4cut.databinding.DialogDeleteNotiAllBinding

class NotificationActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNotificationBinding

    private var currentPage = 0
    private var hasNextPage = true
    private val notificationPageSize = 8

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
        binding.tvDeleteAll.setOnClickListener {
            showDeleteAllDialog()
        }
    }

    private fun showDeleteAllDialog() {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)

        val dialogBinding = DialogDeleteNotiAllBinding.inflate(layoutInflater)
        dialog.setContentView(dialogBinding.root)
        dialog.setCancelable(true)

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        val params = dialog.window?.attributes
        params?.width = (resources.displayMetrics.widthPixels * 0.9).toInt()
        dialog.window?.attributes = params

        dialogBinding.btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialogBinding.btnExit.setOnClickListener {
            lifecycleScope.launch {
                try {
                    RetrofitClient.notificationService.deleteAllNotifications()
                    binding.rvNotification.adapter = null
                    binding.btnMore.visibility = View.GONE
                    Toast.makeText(this@NotificationActivity, "전체 삭제되었습니다.", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                } catch (e: Exception) {
                    Toast.makeText(this@NotificationActivity, "전체 삭제 실패: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }

        dialog.show()
    }

    private fun setupRecyclerView() {
        lifecycleScope.launch {
            try {
                currentPage = 0
                hasNextPage = true
                val response = RetrofitClient.notificationService.getNotifications(page = currentPage)
                Log.d("NotificationAPI", "responseData=" + response.data)
                // Log each notification DTO in detail to inspect null fields
                response.data?.forEach { dto ->
                    Log.d(
                        "NotificationRaw",
                        "notificationId=${dto.notificationId}, type=${dto.type}, senderNickname=${dto.senderNickname}, workspaceName=${dto.workspaceName}, message=${dto.message}"
                    )
                }

                if (response.code.startsWith("C2") && response.data != null) {
                    val uiList = response.data
                        .map { dto ->
                        Log.d(
                            "NotificationDebug",
                            "type=${dto.type}, referenceId=${dto.referenceId}, notificationId=${dto.notificationId}, senderNickname=${dto.senderNickname}, workspaceName=${dto.workspaceName}, message=${dto.message}"
                        )
                            NotificationData(
                                id = dto.notificationId,

                                // 알림 종류별 대상 ID
                                // 친구 요청이면 friendRequestId,
                                // 워크스페이스 초대면 invitationId
                                referenceId = dto.referenceId ?: dto.notificationId,

                                // 사진 상세 이동에 사용할 mediaId
                                mediaId = dto.mediaId,

                                // 해당 알림이 어느 워크스페이스에 속하는지
                                workspaceId = dto.workspaceId,

                                // 알림을 발생시킨 사용자 ID
                                senderId = dto.senderId,

                                type = dto.type,
                            iconResId = when (dto.type) {
                                "WORKSPACE_INVITE", "WORKSPACE_ACCEPTED" -> R.drawable.ic_noti_invite
                                "FRIEND_REQUEST" -> R.drawable.ic_noti_friend_add
                                "FRIEND_ACCEPTED" -> R.drawable.ic_noti_people
                                "MEDIA_COMMENT" -> R.drawable.ic_noti_comment
                                "MEDIA_UPLOADED" -> R.drawable.ic_noti_photo
                                else -> R.drawable.ic_noti_people
                            },
                            category = when (dto.type) {
                                "FRIEND_REQUEST", "FRIEND_ACCEPTED" -> "친구"
                                "WORKSPACE_INVITE", "WORKSPACE_ACCEPTED" -> "초대"
                                "MEDIA_COMMENT" -> "댓글"
                                "MEDIA_UPLOADED" -> "사진"
                                else -> dto.type
                            },
                            content = when (dto.type) {
                                "WORKSPACE_INVITE" -> {
                                    val sender = dto.senderNickname ?: "누군가"
                                    val workspace = dto.workspaceName ?: "워크스페이스"
                                    "${sender}님이 ${workspace}에 초대했습니다."
                                }
                                "FRIEND_REQUEST" -> dto.message ?: "친구 요청이 도착했습니다."
                                "MEDIA_UPLOADED" -> {
                                    val sender = dto.senderNickname ?: "누군가"
                                    val workspace = dto.workspaceName ?: "워크스페이스"
                                    "${sender}님이 ${workspace}에 사진을 업로드했습니다."
                                }
                                else -> dto.message ?: "알림이 도착했습니다."
                            },
                            time = dto.createdAt?.let { formatTimeAgo(it) } ?: "방금 전",
                            hasButtons = dto.type == "FRIEND_REQUEST" || dto.type == "WORKSPACE_INVITE"
                        )
                    }.toMutableList()


                    suspend fun markVisibleItemsAsRead(endExclusive: Int) {
                        val safeEnd = minOf(endExclusive, uiList.size)
                        uiList.take(safeEnd)
                            .forEach { item ->
                                try {
                                    RetrofitClient.notificationService.markPageAsRead(
                                        NotificationMarkReadByIdsDto(
                                            notificationIds = listOf(item.id)
                                        )
                                    )
                                } catch (e: Exception) {
                                    Log.e("NotificationRead", "알림 읽음 처리 실패: id=${item.id}", e)
                                }
                            }
                    }

                    markVisibleItemsAsRead(8)

                    fun setMoreButtonMode() {
                        binding.btnMore.visibility = View.VISIBLE
                        binding.btnMore.setImageResource(R.drawable.ic_noti_more)
                        binding.btnMore.contentDescription = "더보기"
                    }

                    fun setToTopButtonMode() {
                        hasNextPage = false
                        binding.btnMore.visibility = View.VISIBLE
                        binding.btnMore.setImageResource(R.drawable.ic_noti_to_top)
                        binding.btnMore.contentDescription = "맨 위로"
                        binding.btnMore.setOnClickListener {
                            binding.rvNotification.smoothScrollToPosition(0)
                        }
                    }

                    lateinit var adapter: NotificationAdapter
                    adapter = NotificationAdapter(
                        uiList,
                        onAcceptClick = { item ->
                            lifecycleScope.launch {
                                try {
                                    Log.d("NotificationClick", "ACCEPT type=${item.type}, id=${item.id}")
                                    when (item.type) {
                                        "FRIEND_REQUEST" -> {
                                            RetrofitClient.friendService.acceptFriendRequest(item.referenceId)
                                        }
                                        "WORKSPACE_INVITE" -> {
                                            // 먼저 현재 참여 중인 스페이스 개수 조회
                                            val workspaceResponse = RetrofitClient.workspaceService.getMyWorkspaces()
                                            val workspaceCount = workspaceResponse.data?.size ?: 0

                                            // 최대 4개 제한
                                            if (workspaceCount >= 4) {
                                                Toast.makeText(
                                                    this@NotificationActivity,
                                                    "최대 스페이스 개수를 넘어섰어요",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                                return@launch
                                            }

                                            // 제한 미만이면 수락 진행
                                            RetrofitClient.workspaceInvitationService.acceptInvitation(item.referenceId)
                                        }
                                    }
                                    Toast.makeText(this@NotificationActivity, "수락 처리되었습니다.", Toast.LENGTH_SHORT).show()
                                    val index = uiList.indexOf(item)
                                    if (index != -1) {
                                        uiList.removeAt(index)
                                        binding.rvNotification.adapter?.notifyItemRemoved(index)
                                        binding.btnMore.visibility = View.VISIBLE
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
                                            RetrofitClient.friendService.rejectFriendRequest(item.referenceId)
                                        }
                                        "WORKSPACE_INVITE" -> {
                                            RetrofitClient.workspaceInvitationService.rejectInvitation(item.referenceId)
                                        }
                                    }
                                    Toast.makeText(this@NotificationActivity, "거절 처리되었습니다.", Toast.LENGTH_SHORT).show()
                                    val index = uiList.indexOf(item)
                                    if (index != -1) {
                                        uiList.removeAt(index)
                                        binding.rvNotification.adapter?.notifyItemRemoved(index)
                                        binding.btnMore.visibility = View.VISIBLE
                                    }
                                } catch (e: Exception) {
                                    Toast.makeText(this@NotificationActivity, "거절 실패: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        onDeleteClick = { item ->
                            lifecycleScope.launch {
                                try {
                                    RetrofitClient.notificationService.deleteNotification(item.id)
                                    val index = uiList.indexOf(item)
                                    if (index != -1) {
                                        uiList.removeAt(index)
                                        binding.rvNotification.adapter?.notifyItemRemoved(index)
                                        binding.btnMore.visibility = View.VISIBLE
                                    }
                                } catch (e: Exception) {
                                    Toast.makeText(this@NotificationActivity, "삭제 실패: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        onItemClick = { item ->
                            handleNotificationClick(item)
                        }
                    )
                    binding.btnMore.setOnClickListener {
                        lifecycleScope.launch {
                            if (!hasNextPage) {
                                binding.rvNotification.smoothScrollToPosition(0)
                                return@launch
                            }

                            try {
                                currentPage += 1
                                val nextResponse = RetrofitClient.notificationService.getNotifications(page = currentPage)
                                val nextList = nextResponse.data
                                    ?.map { dto ->
                                        Log.d(
                                            "NotificationDebug",
                                            "type=${dto.type}, referenceId=${dto.referenceId}, notificationId=${dto.notificationId}, senderNickname=${dto.senderNickname}, workspaceName=${dto.workspaceName}, message=${dto.message}"
                                        )
                                        NotificationData(
                                            id = dto.notificationId,

                                            // 알림 대상 ID
                                            referenceId = dto.referenceId ?: dto.notificationId,

                                            // 사진 상세 이동에 사용할 mediaId
                                            mediaId = dto.mediaId,

                                            // 해당 알림의 워크스페이스 ID
                                            workspaceId = dto.workspaceId,

                                            // 알림을 발생시킨 사용자 ID
                                            senderId = dto.senderId,

                                            type = dto.type,
                                            iconResId = when (dto.type) {
                                                "WORKSPACE_INVITE", "WORKSPACE_ACCEPTED" -> R.drawable.ic_noti_invite
                                                "FRIEND_REQUEST" -> R.drawable.ic_noti_friend_add
                                                "FRIEND_ACCEPTED" -> R.drawable.ic_noti_people
                                                "MEDIA_COMMENT" -> R.drawable.ic_noti_comment
                                                "MEDIA_UPLOADED" -> R.drawable.ic_noti_photo
                                                else -> R.drawable.ic_noti_people
                                            },
                                            category = when (dto.type) {
                                                "FRIEND_REQUEST", "FRIEND_ACCEPTED" -> "친구"
                                                "WORKSPACE_INVITE", "WORKSPACE_ACCEPTED" -> "초대"
                                                "MEDIA_COMMENT" -> "댓글"
                                                "MEDIA_UPLOADED" -> "사진"
                                                else -> dto.type
                                            },
                                            content = when (dto.type) {
                                                "WORKSPACE_INVITE" -> {
                                                    val sender = dto.senderNickname ?: "누군가"
                                                    val workspace = dto.workspaceName ?: "워크스페이스"
                                                    "${sender}님이 ${workspace}에 초대했습니다."
                                                }
                                                "FRIEND_REQUEST" -> dto.message ?: "친구 요청이 도착했습니다."
                                                "MEDIA_UPLOADED" -> {
                                                    val sender = dto.senderNickname ?: "누군가"
                                                    val workspace = dto.workspaceName ?: "워크스페이스"
                                                    "${sender}님이 ${workspace}에 사진을 업로드했습니다."
                                                }
                                                else -> dto.message ?: "알림이 도착했습니다."
                                            },
                                            time = dto.createdAt?.let { formatTimeAgo(it) } ?: "방금 전",
                                            hasButtons = dto.type == "FRIEND_REQUEST" || dto.type == "WORKSPACE_INVITE"
                                        )
                                    }
                                    .orEmpty()

                                if (nextResponse.code.startsWith("C2") && nextList.isNotEmpty()) {
                                    adapter.appendItems(nextList)
                                    markVisibleItemsAsRead(adapter.itemCount)

                                    if (nextList.size < notificationPageSize) {
                                        setToTopButtonMode()
                                    }
                                } else {
                                    currentPage -= 1
                                    setToTopButtonMode()
                                }
                            } catch (e: Exception) {
                                currentPage -= 1
                                Toast.makeText(this@NotificationActivity, "알림을 더 불러오지 못했습니다.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                    binding.rvNotification.adapter = adapter
                    binding.rvNotification.layoutManager = LinearLayoutManager(this@NotificationActivity)
                    if (uiList.size < notificationPageSize) {
                        setToTopButtonMode()
                    } else {
                        setMoreButtonMode()
                    }
                }

            } catch (e: Exception) {
                Log.e("NotificationAPI", "error=" + e.message, e)
                Toast.makeText(this@NotificationActivity, "네트워크 오류: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * 알림 아이템 클릭 시
     * 알림 타입에 따라 이동할 화면을 결정한다.
     */
    /**
     * 알림 아이템 클릭 시
     * 알림 타입에 따라 MainActivity에 이동 정보를 전달한다.
     */
    private fun handleNotificationClick(item: NotificationData) {

        Log.d(
            "NotificationClick",
            "type=${item.type}, referenceId=${item.referenceId}, " +
                    "workspaceId=${item.workspaceId}, senderId=${item.senderId}"
        )

        when (item.type) {

            // 사진에 댓글이 달린 경우
            // 해당 스페이스로 이동한 뒤 그 사진의 상세 모달을 연다.
            "MEDIA_COMMENT" -> {
                val workspaceId = item.workspaceId ?: return
                val mediaId = item.mediaId ?: return

                val intent = Intent(this, MainActivity::class.java).apply {
                    putExtra("OPEN_SPACE_ID", workspaceId)
                    putExtra("OPEN_PHOTO_ID", mediaId)
                }

                startActivity(intent)
                finish()
            }

            // 워크스페이스에 새 사진이 올라온 경우
            // 해당 스페이스로 이동한 뒤 업로드된 사진 상세를 연다.
            "MEDIA_UPLOADED" -> {
                val workspaceId = item.workspaceId ?: return
                val mediaId = item.referenceId

                val intent = Intent(this, MainActivity::class.java).apply {
                    putExtra("OPEN_SPACE_ID", workspaceId)
                    putExtra("OPEN_PHOTO_ID", mediaId)
                }

                startActivity(intent)
                finish()
            }

            // 상대방이 워크스페이스 초대를 수락한 경우
            // 해당 스페이스 상세까지만 이동한다.
            "WORKSPACE_ACCEPTED" -> {
                val workspaceId = item.workspaceId ?: return

                val intent = Intent(this, MainActivity::class.java).apply {
                    putExtra("OPEN_SPACE_ID", workspaceId)
                }

                startActivity(intent)
                finish()
            }

            // 상대방이 내 친구 요청을 수락한 경우
            "FRIEND_ACCEPTED" -> {
                val intent = Intent(this, MainActivity::class.java).apply {
                    putExtra("NAVIGATE_TO_TAB", R.id.menu_retouch)
                }

                startActivity(intent)
                finish()
            }

            // 알림 화면 자체에서 수락/거절하므로
            // 아이템 클릭 시에는 이동하지 않는다.
            "WORKSPACE_INVITE",
            "FRIEND_REQUEST" -> {
                // 아무 동작 없음
            }
        }
    }

    private fun formatTimeAgo(createdAt: String): String {
        return try {
            val now = java.time.ZonedDateTime.now(java.time.ZoneId.of("Asia/Seoul"))

            val parsed = try {
                java.time.OffsetDateTime.parse(createdAt)
                    .atZoneSameInstant(java.time.ZoneId.of("Asia/Seoul"))
            } catch (e1: Exception) {
                try {
                    java.time.Instant.parse(createdAt)
                        .atZone(java.time.ZoneId.of("Asia/Seoul"))
                } catch (e2: Exception) {
                    java.time.LocalDateTime.parse(createdAt)
                        .atOffset(java.time.ZoneOffset.UTC)
                        .atZoneSameInstant(java.time.ZoneId.of("Asia/Seoul"))
                }
            }

            val minutes = java.time.Duration.between(parsed, now).toMinutes()

            when {
                minutes < 1 -> "방금 전"
                minutes < 60 -> "${minutes}분 전"
                minutes < 60 * 24 -> "${minutes / 60}시간 전"
                else -> "${minutes / (60 * 24)}일 전"
            }
        } catch (e: Exception) {
            Log.e("NotificationTime", "createdAt parse 실패: $createdAt", e)
            "방금 전"
        }
    }
}