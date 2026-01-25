package com.example.lifetogether.domain.listener

import com.example.lifetogether.domain.model.Category

sealed class CategoriesListener {
    data class Success(val listItems: List<Category>) : CategoriesListener()
    data class Failure(val message: String) : CategoriesListener()
}
