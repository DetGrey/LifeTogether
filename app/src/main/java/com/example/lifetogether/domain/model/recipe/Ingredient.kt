package com.example.lifetogether.domain.model.recipe

import com.example.lifetogether.domain.model.Completable
import com.example.lifetogether.domain.model.enums.MeasureType
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class Ingredient(
    override val id: String = UUID.randomUUID().toString(),
    var amount: Double,
    var measureType: MeasureType,
    override var itemName: String,
    override var completed: Boolean = false,
    val sortOrder: Int = 0,
) : Completable
