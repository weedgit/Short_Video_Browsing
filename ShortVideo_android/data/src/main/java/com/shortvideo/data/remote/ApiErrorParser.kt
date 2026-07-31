package com.shortvideo.data.remote

import com.google.gson.Gson
import com.shortvideo.data.remote.dto.ApiErrorBody
import retrofit2.HttpException
import java.io.IOException

object ApiErrorParser {
    private val gson = Gson()

    fun messageFrom(throwable: Throwable): String {
        return when (throwable) {
            is HttpException -> parseHttpException(throwable)
            is IOException -> "Network error. Check your connection and try again."
            else -> throwable.message ?: "Something went wrong."
        }
    }

    private fun parseHttpException(exception: HttpException): String {
        val errorBody = exception.response()?.errorBody()?.string()
        if (!errorBody.isNullOrBlank()) {
            runCatching {
                gson.fromJson(errorBody, ApiErrorBody::class.java)?.error?.message
            }.getOrNull()?.let { return it }
        }
        return "Request failed (${exception.code()})"
    }
}
