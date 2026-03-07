package com.example.lifetogether.domain.model.guides

import com.example.lifetogether.util.Constants

enum class GuideVisibility(val value: String) {
    FAMILY(Constants.GUIDE_VISIBILITY_FAMILY),
    PRIVATE(Constants.GUIDE_VISIBILITY_PRIVATE),
    ;

    companion object {
        fun fromValue(value: String?): GuideVisibility {
            return entries.firstOrNull { it.value.equals(value, ignoreCase = true) } ?: PRIVATE
        }
    }
}
