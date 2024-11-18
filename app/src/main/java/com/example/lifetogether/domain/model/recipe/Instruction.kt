package com.example.lifetogether.domain.model.recipe

import com.example.lifetogether.domain.model.Completable

data class Instruction(
    override val itemName: String = "",
    override var completed: Boolean = false,
) : Completable

fun Instruction.toMap(): Map<String, Any?> {
    return mapOf(
        "itemName" to itemName,
        "completed" to completed,
    )
}
