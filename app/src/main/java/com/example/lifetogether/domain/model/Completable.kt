package com.example.lifetogether.domain.model

import com.example.lifetogether.domain.model.recipe.Ingredient
import com.example.lifetogether.domain.model.recipe.Instruction

interface Completable {
    val id: String
    val itemName: String
    var completed: Boolean
}

inline fun <reified T : Completable> List<T>.toggleCompleted(itemId: String): List<T> {
    return this.map { completable ->
        if (completable.id == itemId) {
            when (completable) {
                is Instruction -> completable.copy(completed = !completable.completed) as T
                is Ingredient -> completable.copy(completed = !completable.completed) as T
                else -> completable
            }
        } else {
            completable
        }
    }
}
