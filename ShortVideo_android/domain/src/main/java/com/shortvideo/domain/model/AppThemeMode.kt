package com.shortvideo.domain.model

enum class AppThemeMode(val storageValue: String) {
    NIGHT("night"),
    LIGHT("light"),
    ;

    companion object {
        fun fromStorage(value: String?): AppThemeMode =
            entries.firstOrNull { it.storageValue.equals(value, ignoreCase = true) } ?: NIGHT
    }
}
