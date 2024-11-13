package com.example.lifetogether.domain.repository

import com.example.lifetogether.domain.callback.ResultListener
import com.example.lifetogether.domain.model.Category

interface AdminRepository {
    suspend fun deleteCategory(category: Category): ResultListener
}
