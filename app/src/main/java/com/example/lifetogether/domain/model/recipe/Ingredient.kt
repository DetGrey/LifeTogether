package com.example.lifetogether.domain.model.recipe

import com.example.lifetogether.domain.model.Completable
import com.example.lifetogether.domain.model.enums.MeasureType

class Ingredient(
    var amount: Double = 0.0,
    var measureType: MeasureType,
    override var itemName: String,
    override var completed: Boolean = false,
) : Completable
