package com.example.lifetogether.data.repository

import com.example.lifetogether.data.local.LocalDataSource
import com.example.lifetogether.data.model.GroceryListEntity
import com.example.lifetogether.domain.callback.DefaultsResultListener
import com.example.lifetogether.domain.callback.ListItemsResultListener
import com.example.lifetogether.domain.callback.ResultListener
import com.example.lifetogether.domain.model.GroceryItem
import com.example.lifetogether.domain.model.Item
import com.example.lifetogether.domain.repository.ListRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import kotlin.reflect.KClass

class LocalListRepositoryImpl @Inject constructor(
    private val localDataSource: LocalDataSource,
) : ListRepository {

    override suspend fun fetchAllData(familyId: String): Flow<List<Any>> {
        TODO("Not yet implemented")
    }

    override suspend fun saveItem(
        item: Item,
        listName: String,
    ): ResultListener {
        TODO("Not yet implemented")
    }

    override suspend fun toggleItemCompletion(
        item: Item,
        listName: String,
    ): ResultListener {
        TODO("Not yet implemented")
    }

    override suspend fun fetchListDefaults(listName: String): DefaultsResultListener {
        TODO("Not yet implemented")
    }

    override suspend fun <T : Item> fetchListItems(
        listName: String,
        familyId: String,
        itemType: KClass<T>,
    ): Flow<ListItemsResultListener<T>> {
        println("LocalListRepoImpl fetchListItems init")
        return localDataSource.getListItems(familyId)
            .map { entities ->
                try {
                    // Convert entities to items
                    val itemsList = entities.map { it.toItem(itemType) }
                    println("LocalListRepoImpl after getting items from local data source")
                    println("itemList of specified itemType: $itemsList")
                    ListItemsResultListener.Success(itemsList) as ListItemsResultListener<T>
                } catch (e: Exception) {
                    ListItemsResultListener.Failure(e.message ?: "Unknown error")
                }
            }
    }

//    override suspend fun <T : Item> fetchListItems(
//        listName: String,
//        uid: String,
//        itemType: KClass<T>,
//    ): ListItemsResultListener<T> {
//        return try {
//            // Fetch items from the database
//            val itemsFlow = localDataSource.getListItems(uid)
//            // Collect the flow and convert it to a list
//            val itemsList = itemsFlow.first().map { it.toItem(itemType) }
//            // Return success with the list of items
//            ListItemsResultListener.Success(itemsList)
//        } catch (e: Exception) {
//            // Return failure if there's an error
//            ListItemsResultListener.Failure(e.message ?: "Unknown error")
//        }
//    }

    // Assuming GroceryItem is a subclass of Item and has a matching constructor
    // TODO ADD MORE ITEM CLASSES
    private fun <T : Item> GroceryListEntity.toItem(itemType: KClass<T>): T {
        @Suppress("UNCHECKED_CAST")
        return when (itemType) {
            GroceryItem::class -> GroceryItem(
                familyId = this.familyId,
                itemName = this.name,
                lastUpdated = this.lastUpdated,
                completed = this.completed,
                category = this.category,
            ) as T // Cast to T
            else -> throw IllegalArgumentException("Unsupported item type: $itemType")
        }
    }
}
