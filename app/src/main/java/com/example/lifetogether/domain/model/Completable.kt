package com.example.lifetogether.domain.model

import com.example.lifetogether.domain.model.recipe.Ingredient
import com.example.lifetogether.domain.model.recipe.Instruction

interface Completable {
    val itemName: String
    var completed: Boolean
}

inline fun <reified T : Completable> List<T>.toggleCompleted(itemName: String): List<T> {
    return this.map { completable ->
        if (completable.itemName == itemName) {
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
