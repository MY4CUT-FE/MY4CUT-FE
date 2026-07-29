package com.umc.mobile.my4cut.ui.myalbum

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.umc.mobile.my4cut.R
import com.umc.mobile.my4cut.databinding.ActivityGalleryPickerBinding
import com.umc.mobile.my4cut.network.RetrofitClient
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import java.time.YearMonth

class GalleryPickerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGalleryPickerBinding
    private lateinit var adapter: GalleryPickerAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGalleryPickerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adapter = GalleryPickerAdapter(
            maxSelectable = MAX_SELECTABLE,
            onSelectionChanged = { count -> updateUploadButtonColor(count) }
        )
        binding.rvGallery.layoutManager = GridLayoutManager(this, 3)
        binding.rvGallery.adapter = adapter

        updateUploadButtonColor(0)

        binding.btnBack.setOnClickListener {
            setResult(RESULT_CANCELED)
            finish()
        }

        binding.btnUpload.setOnClickListener {
            val selected = adapter.getSelectedUrls()
            if (selected.isEmpty()) {
                Toast.makeText(this, "사진을 선택해 주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val resultIntent = Intent().apply {
                putStringArrayListExtra(EXTRA_SELECTED_VIEW_URLS, ArrayList(selected))
            }
            setResult(Activity.RESULT_OK, resultIntent)
            finish()
        }

        loadDay4CutPhotos()
    }

    private fun updateUploadButtonColor(selectedCount: Int) {
        val colorRes = if (selectedCount > 0) R.color.coral_900 else R.color.gray_400
        binding.btnUpload.setTextColor(ContextCompat.getColor(this, colorRes))
    }

    // 홈/캘린더에서 Day4cut으로 저장한 네컷 사진을 전부 불러옴 (월별 API만 있어 달마다 순차 조회)
    private fun loadDay4CutPhotos() {
        val existingUrls = intent.getStringArrayListExtra(EXTRA_EXISTING_VIEW_URLS)?.toSet() ?: emptySet()
        binding.progressLoading.visibility = View.VISIBLE

        lifecycleScope.launch {
            val urls = fetchAllDay4CutPhotoUrls().filterNot { it in existingUrls }
            adapter.submitList(urls)
            binding.progressLoading.visibility = View.GONE

            if (urls.isEmpty()) {
                Toast.makeText(this@GalleryPickerActivity, "불러올 네컷 사진이 없어요.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private suspend fun fetchAllDay4CutPhotoUrls(): List<String> {
        val allUrls = mutableListOf<String>()
        var yearMonth = YearMonth.now()
        var consecutiveEmptyMonths = 0
        var monthsChecked = 0

        while (consecutiveEmptyMonths < MAX_CONSECUTIVE_EMPTY_MONTHS && monthsChecked < MAX_MONTHS_TO_CHECK) {
            monthsChecked++

            try {
                val calendarResponse = RetrofitClient.day4CutService.getCalendarStatus(
                    yearMonth.year,
                    yearMonth.monthValue
                )
                val days = calendarResponse.data?.dates.orEmpty()

                if (days.isEmpty()) {
                    consecutiveEmptyMonths++
                } else {
                    consecutiveEmptyMonths = 0

                    val dateStrings = days.sortedByDescending { it.day }.map { day ->
                        "%04d-%02d-%02d".format(yearMonth.year, yearMonth.monthValue, day.day)
                    }

                    val detailResponses = coroutineScope {
                        dateStrings.map { date ->
                            async {
                                runCatching { RetrofitClient.day4CutService.getDay4CutDetail(date) }.getOrNull()
                            }
                        }.awaitAll()
                    }

                    detailResponses.forEach { response ->
                        response?.data?.viewUrls?.let { allUrls.addAll(it) }
                    }
                }
            } catch (e: Exception) {
                Log.e("API_ERROR", "${yearMonth} 캘린더 조회 실패: ${e.message}")
                consecutiveEmptyMonths++
            }

            yearMonth = yearMonth.minusMonths(1)
        }

        return allUrls
    }

    companion object {
        const val EXTRA_SELECTED_VIEW_URLS = "extra_selected_view_urls"
        const val EXTRA_EXISTING_VIEW_URLS = "extra_existing_view_urls"
        private const val MAX_SELECTABLE = 50
        private const val MAX_CONSECUTIVE_EMPTY_MONTHS = 12
        private const val MAX_MONTHS_TO_CHECK = 60
    }
}