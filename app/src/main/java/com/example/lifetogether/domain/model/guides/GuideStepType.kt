package com.example.lifetogether.domain.model.guides

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class GuideStepType(val value: String) {
    @SerialName("round")
    ROUND("round"),

    @SerialName("comment")
    COMMENT("comment"),

    @SerialName("numbered")
    NUMBERED("numbered"),

    @SerialName("subsection")
    SUBSECTION("subsection"),

    @SerialName("unknown")
    UNKNOWN("unknown"),
    ;

    companion object {
        fun fromValue(value: String?): GuideStepType {
            if (value.isNullOrBlank()) return NUMBERED
            return entries.firstOrNull { it.value.equals(value, ignoreCase = true) }
                ?: entries.firstOrNull { it.name.equals(value, ignoreCase = true) }
                ?: UNKNOWN
        }
    }
}
