package com.example.lifetogether.domain.model.enums

import com.example.lifetogether.util.Constants

enum class Visibility(val value: String) {
    FAMILY(Constants.VISIBILITY_FAMILY),
    PRIVATE(Constants.VISIBILITY_PRIVATE),
    ;

    companion object {
        fun fromValue(value: String?): Visibility {
            return entries.firstOrNull { it.value.equals(value, ignoreCase = true) } ?: PRIVATE
        }
    }
}