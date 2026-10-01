package com.nura.messaging.features.auth.data

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
open class OnboardingPreferences @Inject constructor(
    @ApplicationContext private val context: Context?
) {
    private val inMemoryCompleted = mutableMapOf<String, Boolean>()
    private val prefs = context?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    open fun isOnboardingCompleted(userId: String): Boolean {
        if (userId.isBlank()) return false
        return prefs?.getBoolean(KEY_PREFIX + userId, false) ?: (inMemoryCompleted[userId] ?: false)
    }

    open fun setOnboardingCompleted(userId: String, completed: Boolean = true) {
        if (userId.isNotBlank()) {
            inMemoryCompleted[userId] = completed
            prefs?.edit()?.putBoolean(KEY_PREFIX + userId, completed)?.apply()
        }
    }

    companion object {
        private const val PREFS_NAME = "nura_post_signup_onboarding_prefs"
        private const val KEY_PREFIX = "onboarding_completed_"
    }
}
