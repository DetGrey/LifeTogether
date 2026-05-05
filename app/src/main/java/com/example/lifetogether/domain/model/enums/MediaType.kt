package com.example.lifetogether.domain.model.enums

enum class MediaType(val value: String) {
    IMAGE("image"),
    VIDEO("video"),
    ;

    companion object {
        fun fromValue(value: String?): MediaType? {
            if (value.isNullOrBlank()) return null
            return entries.firstOrNull { it.value.equals(value, ignoreCase = true) }
                ?: entries.firstOrNull { it.name.equals(value, ignoreCase = true) }
                ?: entries.firstOrNull { it.name.equals(value.substringAfterLast('.'), ignoreCase = true) }
        }
    }
}
