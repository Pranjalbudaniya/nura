package com.nura.messaging.core.network

object SupabaseConfig {
    val DEFAULT_SUPABASE_URL: String = BuildConfig.SUPABASE_URL
    val DEFAULT_ANON_KEY: String = BuildConfig.SUPABASE_ANON_KEY

    const val AUTH_SCHEME = "nura"
    const val AUTH_HOST = "auth-callback"
    const val REDIRECT_URL = "$AUTH_SCHEME://$AUTH_HOST"
}
