package com.example.lifetogether.domain.model.recipe

import com.example.lifetogether.domain.model.Completable
import com.example.lifetogether.domain.model.enums.MeasureType
import kotlinx.serialization.Serializable

@Serializable
data class Ingredient(
    var amount: Double,
    var measureType: MeasureType,
    override var itemName: String,
    override var completed: Boolean = false,
) : Completable
