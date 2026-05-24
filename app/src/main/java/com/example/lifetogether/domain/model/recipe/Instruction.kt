package com.example.lifetogether.domain.model.recipe

import com.example.lifetogether.domain.model.Completable
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class Instruction(
    override val id: String = UUID.randomUUID().toString(),
    override val itemName: String,
    override var completed: Boolean = false,
    val sortOrder: Int = 0,
) : Completable
