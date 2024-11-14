package com.example.lifetogether.data.local

import androidx.room.TypeConverter
import com.example.lifetogether.domain.model.recipe.Ingredient
import com.example.lifetogether.domain.model.recipe.Instruction
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.Date

class Converters {
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
        val listType = object : TypeToken<List<String>>() {}.type
        return Gson().fromJson(value, listType)
    }

    @TypeConverter
    fun fromListString(list: List<String>): String {
        return Gson().toJson(list)
    }

    @TypeConverter
    fun fromIngredientList(value: String): List<Ingredient> {
        val listType = object : TypeToken<List<Ingredient>>() {}.type
        return Gson().fromJson(value, listType)
    }

    @TypeConverter
    fun fromListIngredient(list: List<Ingredient>): String {
        return Gson().toJson(list)
    }

    @TypeConverter
    fun fromInstructionList(value: String): List<Instruction> {
        val listType = object : TypeToken<List<Instruction>>() {}.type
        return Gson().fromJson(value, listType)
    }

    @TypeConverter
    fun fromListInstruction(list: List<Instruction>): String {
        return Gson().toJson(list)
    }
}
