package com.umc.mobile.my4cut.data.tutorial

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import com.umc.mobile.my4cut.data.tutorial.model.TutorialStatus
import com.umc.mobile.my4cut.data.tutorial.model.TutorialType
import kotlinx.coroutines.flow.first

private val Context.tutorialDataStore by preferencesDataStore(
    name = "tutorial_preferences"
)

object TutorialManager {

    private fun getKey(
        userId: Long,
        type: TutorialType
    ) = booleanPreferencesKey(
        "tutorial_${userId}_${type.name}"
    )

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
                preferences[getKey(userId, tutorial.type)] =
                    tutorial.completed
            }
        }
    }
}