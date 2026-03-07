package com.example.lifetogether.domain.model.recipe

import com.example.lifetogether.domain.model.Completable
import kotlinx.serialization.Serializable

@Serializable
data class Instruction(
    override val itemName: String = "",
    override var completed: Boolean = false,
) : Completable
