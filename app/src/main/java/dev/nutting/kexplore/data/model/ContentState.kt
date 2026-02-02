package dev.nutting.kexplore.data.model

sealed interface ContentState<out T> {
    data object Loading : ContentState<Nothing>
    data class Error(val message: String, val retry: (() -> Unit)? = null) : ContentState<Nothing>
    data class Success<T>(val data: T) : ContentState<T>
}
