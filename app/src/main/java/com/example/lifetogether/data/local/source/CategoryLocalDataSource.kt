package com.example.lifetogether.data.local.source

import com.example.lifetogether.data.local.dao.CategoriesDao
import com.example.lifetogether.data.local.source.internal.computeItemsToDelete
import com.example.lifetogether.data.local.source.internal.computeItemsToUpdate
import com.example.lifetogether.data.model.CategoryEntity
import com.example.lifetogether.domain.model.Category
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CategoryLocalDataSource @Inject constructor(
    private val categoriesDao: CategoriesDao,
) {
    fun getCategories(): Flow<List<CategoryEntity>> = categoriesDao.getItems()

    suspend fun updateCategories(items: List<Category>) {
        val categoryEntities = items.map { category ->
            CategoryEntity(
                emoji = category.emoji,
                name = category.name,
            )
        }

        val currentItems = categoriesDao.getItems().first()
        val itemsToUpdate = computeItemsToUpdate(
            currentItems = currentItems,
            incomingItems = categoryEntities,
            key = { it.name },
        )
        val itemsToDelete = computeItemsToDelete(
            currentItems = currentItems,
            incomingItems = categoryEntities,
            key = { it.name },
        )

        categoriesDao.updateItems(itemsToUpdate)
        categoriesDao.deleteItems(itemsToDelete)
    }
}
