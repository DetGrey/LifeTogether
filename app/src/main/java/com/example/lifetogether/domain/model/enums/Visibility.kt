package com.example.lifetogether.domain.model.enums

import com.example.lifetogether.util.Constants

enum class Visibility(val value: String, val tag: String) {
    FAMILY(Constants.VISIBILITY_FAMILY, "family"),
    PRIVATE(Constants.VISIBILITY_PRIVATE, "private"),
    ;

    companion object {
        fun fromValue(value: String?): Visibility? {
            return entries.firstOrNull { it.value.equals(value, ignoreCase = true) }
        }
    }
}
