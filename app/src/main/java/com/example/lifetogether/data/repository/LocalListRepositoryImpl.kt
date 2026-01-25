package com.example.lifetogether.data.repository

import androidx.core.net.toUri
import com.example.lifetogether.data.local.LocalDataSource
import com.example.lifetogether.data.model.Entity
import com.example.lifetogether.domain.listener.CategoriesListener
import com.example.lifetogether.domain.listener.GrocerySuggestionsListener
import com.example.lifetogether.domain.listener.ItemResultListener
import com.example.lifetogether.domain.listener.ListItemsResultListener
import com.example.lifetogether.domain.listener.ResultListener
import com.example.lifetogether.domain.listener.StringResultListener
import com.example.lifetogether.domain.model.Category
import com.example.lifetogether.domain.model.Item
import com.example.lifetogether.domain.model.TipItem
import com.example.lifetogether.domain.model.enums.MediaType
import com.example.lifetogether.domain.model.gallery.Album
import com.example.lifetogether.domain.model.gallery.GalleryImage
import com.example.lifetogether.domain.model.gallery.GalleryMedia
import com.example.lifetogether.domain.model.gallery.GalleryVideo
import com.example.lifetogether.domain.model.grocery.GroceryItem
import com.example.lifetogether.domain.model.grocery.GrocerySuggestion
import com.example.lifetogether.domain.model.recipe.Recipe
import com.example.lifetogether.domain.repository.ListRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import kotlin.reflect.KClass

class LocalListRepositoryImpl @Inject constructor(
    private val localDataSource: LocalDataSource,
) : ListRepository {

    override suspend fun saveItem(
        item: Item,
        listName: String,
    ): StringResultListener {
        TODO("Not yet implemented")
    }

    fun deleteItems(
        listName: String,
        itemIds: List<String>,
    ): ResultListener {
        println("LocalListRepositoryImpl deleteItems()")
        return localDataSource.deleteItems(listName, itemIds)
    }

    fun getCategories(): Flow<CategoriesListener> {
        println("LocalListRepositoryImpl getCategories()")
        return localDataSource.getCategories().map { list ->
            try {
                CategoriesListener.Success(
                    list.map { category ->
                        Category(
                            emoji = category.emoji,
                            name = category.name,
                        )
                    },
                )
            } catch (e: Exception) {
                CategoriesListener.Failure(e.message ?: "Unknown error")
            }
        }
    }

    fun getGrocerySuggestions(): Flow<GrocerySuggestionsListener> {
        println("LocalListRepositoryImpl getGrocerySuggestions()")
        return localDataSource.getGrocerySuggestions().map { list ->
            println("Grocery suggestions: $list")
            try {
                GrocerySuggestionsListener.Success(
                    list.map { grocerySuggestion ->
                        GrocerySuggestion(
                            id = grocerySuggestion.id,
                            suggestionName = grocerySuggestion.suggestionName,
                            category = grocerySuggestion.category,
                        )
                    },
                )
            } catch (e: Exception) {
                GrocerySuggestionsListener.Failure(e.message ?: "Unknown error")
            }
        }
    }

    fun fetchAlbumMedia(
        familyId: String,
        albumId: String,
    ): Flow<ListItemsResultListener<GalleryMedia>> {
        println("LocalListRepoImpl fetchAlbumMedia init")
        return localDataSource.getAlbumMedia(familyId, albumId)
            .map { entities ->
                try {
                    println("LocalListRepoImpl fetchAlbumMedia entities: ${ entities.map { it.entity.copy(mediaUri = null, thumbnail = null) } }")
                    // Convert entities to items
                    val itemsList = entities.map { entityWrapper ->
                        val entity = entityWrapper.entity

                        when (entity.mediaType) {
                            MediaType.IMAGE -> GalleryImage(
                                id = entity.id,
                                familyId = entity.familyId,
                                itemName = entity.itemName,
                                lastUpdated = entity.lastUpdated,
                                albumId = entity.albumId,
                                dateCreated = entity.dateCreated,
                                mediaType = MediaType.IMAGE,
                                mediaUrl = null,
                                mediaUri = entity.mediaUri?.toUri(),
                            )

                            MediaType.VIDEO -> GalleryVideo(
                                id = entity.id,
                                familyId = entity.familyId,
                                itemName = entity.itemName,
                                lastUpdated = entity.lastUpdated,
                                albumId = entity.albumId,
                                dateCreated = entity.dateCreated,
                                mediaType = MediaType.VIDEO,
                                mediaUrl = null,
                                mediaUri = entity.mediaUri?.toUri(),
                                duration = entity.videoDuration,
                            )
                        }
                    }
                    println("LocalListRepoImpl after getting items from local data source")
                    println("fetchAlbumMedia of specified itemType: $itemsList")
                    ListItemsResultListener.Success(itemsList)
                } catch (e: Exception) {
                    println("Error: ${e.message}")
                    ListItemsResultListener.Failure(e.message ?: "Unknown error")
                }
            }
    }

    fun <T : Item> fetchListItems(
        listName: String,
        familyId: String,
        itemType: KClass<T>,
    ): Flow<ListItemsResultListener<Item>> {
        println("LocalListRepoImpl fetchListItems init")
        return localDataSource.getListItems(listName, familyId)
            .map { entities ->
                try {
                    println("LocalListRepoImpl fetchListItems entities: $entities")
                    // Convert entities to items
                    val itemsList = entities.map { it.toItem(itemType) }.sortedBy { it.itemName }
                    println("LocalListRepoImpl after getting items from local data source")
                    println("fetchListItems of specified itemType: $itemsList")
                    ListItemsResultListener.Success(itemsList)
                } catch (e: Exception) {
                    println("Error: ${e.message}")
                    ListItemsResultListener.Failure(e.message ?: "Unknown error")
                }
            }
    }

    fun fetchItemById(
        listName: String,
        familyId: String,
        id: String,
        itemType: KClass<out Item>,
    ): Flow<ItemResultListener<Item>> {
        return localDataSource.getItemById(listName, familyId, id)
            .map { entity ->
                try {
                    println("LocalListRepoImpl fetchItemById entity: $entity")
                    val item = entity.toItem(itemType)
                    println("fetchItemById of specified itemType: $item")
                    ItemResultListener.Success(item)
                } catch (e: Exception) {
                    ItemResultListener.Failure(e.message ?: "Unknown error")
                }
            }
    }

    private fun Entity.toItem(itemType: KClass<out Item>): Item {
        return when (this) {
            is Entity.GroceryList -> when (itemType) {
                GroceryItem::class -> GroceryItem(
                    id = this.entity.id,
                    familyId = this.entity.familyId,
                    itemName = this.entity.name,
                    lastUpdated = this.entity.lastUpdated,
                    completed = this.entity.completed,
                    category = this.entity.category,
                )

                else -> throw IllegalArgumentException("Unsupported item type: $itemType")
            }

            is Entity.Recipe -> when (itemType) {
                Recipe::class -> Recipe(
                    id = this.entity.id,
                    familyId = this.entity.familyId,
                    itemName = this.entity.itemName,
                    lastUpdated = this.entity.lastUpdated,
                    description = this.entity.description,
                    ingredients = this.entity.ingredients,
                    instructions = this.entity.instructions,
                    preparationTimeMin = this.entity.preparationTimeMin,
                    favourite = this.entity.favourite,
                    servings = this.entity.servings,
                    tags = this.entity.tags,
                )

                else -> throw IllegalArgumentException("Unsupported item type: $itemType")
            }

            is Entity.Album -> when (itemType) {
                Album::class -> Album(
                    id = this.entity.id,
                    familyId = this.entity.familyId,
                    itemName = this.entity.itemName,
                    lastUpdated = this.entity.lastUpdated,
                    count = this.entity.count,
                )

                else -> throw IllegalArgumentException("Unsupported item type: $itemType")
            }

            is Entity.GalleryMedia -> when (itemType) {
                GalleryMedia::class -> {
                    if (this.entity.mediaType == MediaType.IMAGE) {
                        GalleryImage(
                            id = this.entity.id,
                            familyId = this.entity.familyId,
                            itemName = this.entity.itemName,
                            lastUpdated = this.entity.lastUpdated,
                            albumId = this.entity.albumId,
                            dateCreated = this.entity.dateCreated,
                            mediaType = MediaType.IMAGE,
                            mediaUri = this.entity.mediaUri?.toUri(),
                        )
                    } else if (this.entity.mediaType == MediaType.VIDEO) {
                        GalleryVideo(
                            id = this.entity.id,
                            familyId = this.entity.familyId,
                            itemName = this.entity.itemName,
                            lastUpdated = this.entity.lastUpdated,
                            albumId = this.entity.albumId,
                            dateCreated = this.entity.dateCreated,
                            mediaType = MediaType.VIDEO,
                            mediaUri = this.entity.mediaUri?.toUri(),
                            duration = this.entity.videoDuration,
                        )
                    } else {
                        throw IllegalArgumentException("Unsupported media type: ${this.entity.mediaType}")
                    }
                }

                else -> throw IllegalArgumentException("Unsupported item type: $itemType")
            }

            is Entity.Tip -> when (itemType) {
                TipItem::class -> TipItem(
                    id = this.entity.id,
                    familyId = this.entity.familyId,
                    itemName = this.entity.itemName,
                    lastUpdated = this.entity.lastUpdated,
                    amount = this.entity.amount,
                    currency = this.entity.currency,
                    date = this.entity.date,
                )

                else -> throw IllegalArgumentException("Unsupported item type: $itemType")
            }
        }
    }
}
