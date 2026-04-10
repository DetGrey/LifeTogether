package com.example.lifetogether.data.local

import androidx.room.TypeConverter
import com.example.lifetogether.domain.model.guides.GuideResume
import com.example.lifetogether.domain.model.guides.GuideSection
import com.example.lifetogether.domain.model.recipe.Ingredient
import com.example.lifetogether.domain.model.recipe.Instruction
import java.util.Date
import kotlinx.serialization.json.Json

class Converters {
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        coerceInputValues = true
    }

    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }

    @TypeConverter
    fun fromStringList(value: String): List<String> {
        if (value.isBlank()) return emptyList()
        return runCatching { json.decodeFromString<List<String>>(value) }.getOrDefault(emptyList())
    }

    @TypeConverter
    fun fromListString(list: List<String>): String {
        return json.encodeToString(list)
    }

    @TypeConverter
    fun fromIngredientList(value: String): List<Ingredient> {
        if (value.isBlank()) return emptyList()
        return runCatching { json.decodeFromString<List<Ingredient>>(value) }.getOrDefault(emptyList())
    }

    @TypeConverter
    fun fromListIngredient(list: List<Ingredient>): String {
        return json.encodeToString(list)
    }

    @TypeConverter
    fun fromInstructionList(value: String): List<Instruction> {
        if (value.isBlank()) return emptyList()
        return runCatching { json.decodeFromString<List<Instruction>>(value) }.getOrDefault(emptyList())
    }

    @TypeConverter
    fun fromListInstruction(list: List<Instruction>): String {
        return json.encodeToString(list)
    }

    @TypeConverter
    fun fromGuideSectionList(value: String): List<GuideSection> {
        if (value.isBlank()) return emptyList()
        return runCatching { json.decodeFromString<List<GuideSection>>(value) }.getOrDefault(emptyList())
    }

    @TypeConverter
    fun fromListGuideSection(list: List<GuideSection>): String {
        return json.encodeToString(list)
    }

    @TypeConverter
    fun fromGuideResume(value: String?): GuideResume? {
        if (value.isNullOrBlank() || value == "null") return null
        return runCatching { json.decodeFromString<GuideResume>(value) }.getOrNull()
    }

    @TypeConverter
    fun fromGuideResumeToString(resume: GuideResume?): String? {
        return resume?.let { json.encodeToString(it) }
    }

    @TypeConverter
    fun fromIntList(value: String): List<Int> {
        if (value.isBlank()) return emptyList()
        return runCatching { json.decodeFromString<List<Int>>(value) }.getOrDefault(emptyList())
    }

    @TypeConverter
    fun fromListInt(list: List<Int>): String {
        return json.encodeToString(list)
    }
}
