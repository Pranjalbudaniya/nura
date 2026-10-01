package com.nura.messaging.core.common.util

sealed interface Resource<out T> {
    data class Success<out T>(val data: T) : Resource<T>
    data class Error(val error: Throwable, val message: String? = null) : Resource<Nothing>
    data object Loading : Resource<Nothing>
}
