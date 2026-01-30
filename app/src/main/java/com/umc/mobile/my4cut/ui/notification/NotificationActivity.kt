package com.umc.mobile.my4cut.ui.notification

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.umc.mobile.my4cut.R
import com.umc.mobile.my4cut.data.NotificationData
import com.umc.mobile.my4cut.databinding.ActivityNotificationBinding

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
        val dummyList = listOf(
            NotificationData(
                R.drawable.ic_noti_invite,
                "초대",
                "화운님이 1104 네컷 스페이스에 회원님을 초대했습니다.",
                "13분 전",
                true // 버튼 있음
            ),
            NotificationData(
                R.drawable.ic_noti_friend_add,
                "친구",
                "유복치님이 회원님에게 친구 요청을 보냈습니다.",
                "21시간 전",
                true // 버튼 있음
            ),
            NotificationData(
                R.drawable.ic_noti_comment,
                "댓글",
                "예디님이 마이포컷 스페이스에 댓글을 남겼습니다.",
                "2일 전",
                false // 버튼 없음
            ),
            NotificationData(
                R.drawable.ic_noti_comment,
                "댓글",
                "화운님이 마이포컷 스페이스에 댓글을 남겼습니다.",
                "2일 전",
                false // 버튼 없음
            ),
            NotificationData(
                R.drawable.ic_noti_people,
                "친구",
                "네버님이 친구 초대를 수락하였습니다.",
                "3일 전",
                false // 버튼 없음
            )
        )

        // 2. 어댑터 연결
        val adapter = NotificationAdapter(dummyList)
        binding.rvNotification.adapter = adapter
        binding.rvNotification.layoutManager = LinearLayoutManager(this)
    }
}