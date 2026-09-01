package com.umc.mobile.my4cut.data.tutorial

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import com.umc.mobile.my4cut.data.network.RetrofitClient
import com.umc.mobile.my4cut.data.tutorial.model.TutorialStatus
import com.umc.mobile.my4cut.data.tutorial.model.TutorialType
import kotlinx.coroutines.flow.first

private val Context.tutorialDataStore by preferencesDataStore(
    name = "tutorial_preferences"
)

object TutorialManager {

    private const val TAG = "TutorialManager"

    private fun getKey(
        userId: Long,
        type: TutorialType
    ) = booleanPreferencesKey(
        "tutorial_${userId}_${type.name}"
    )

    private fun getSyncedKey(
        userId: Long
    ) = booleanPreferencesKey(
        "tutorial_${userId}_synced"
    )

    suspend fun isSynced(
        context: Context,
        userId: Long
    ): Boolean {
        val preferences =
            context.tutorialDataStore.data.first()

        return preferences[
            getSyncedKey(userId)
        ] ?: false
    }

    suspend fun isCompleted(
        context: Context,
        userId: Long,
        type: TutorialType
    ): Boolean? {
        val preferences = context.tutorialDataStore.data.first()

        return preferences[getKey(userId, type)]
    }

    suspend fun setCompleted(
        context: Context,
        userId: Long,
        type: TutorialType
    ) {
        context.tutorialDataStore.edit { preferences ->
            preferences[getKey(userId, type)] = true
        }
    }

    suspend fun saveStatuses(
        context: Context,
        userId: Long,
        tutorials: List<TutorialStatus>
    ) {
        context.tutorialDataStore.edit { preferences ->

            tutorials.forEach { tutorial ->
                preferences[
                    getKey(
                        userId,
                        tutorial.type
                    )
                ] = tutorial.completed
            }

            preferences[
                getSyncedKey(userId)
            ] = true
        }
    }

    /** 서버와 동기화된 완료 여부를 반환한다. 최초 1회는 서버 상태를 가져와 로컬에 캐싱한다. */
    suspend fun isTutorialCompleted(
        context: Context,
        userId: Long,
        type: TutorialType
    ): Boolean {
        if (!isSynced(context, userId)) {
            syncFromServer(context, userId)
        }

        return isCompleted(context, userId, type) ?: false
    }

    /** 튜토리얼 완료를 서버에 반영하고 로컬 캐시도 갱신한다. */
    suspend fun completeTutorial(
        context: Context,
        userId: Long,
        type: TutorialType
    ) {
        try {
            val response = RetrofitClient.tutorialService.completeTutorial(type)
            val tutorials = response.data?.tutorials

            if (tutorials != null) {
                saveStatuses(context, userId, tutorials)
            } else {
                setCompleted(context, userId, type)
            }
        } catch (e: Exception) {
            Log.e(TAG, "completeTutorial 실패: $type", e)
            setCompleted(context, userId, type)
        }
    }

    private suspend fun syncFromServer(
        context: Context,
        userId: Long
    ) {
        try {
            val response = RetrofitClient.tutorialService.getTutorialStatus()
            val tutorials = response.data?.tutorials ?: return
            saveStatuses(context, userId, tutorials)
        } catch (e: Exception) {
            Log.e(TAG, "getTutorialStatus 실패", e)
        }
    }
}