package com.example.lifetogether.data.repository

import android.util.Log
import androidx.core.net.toUri
import com.example.lifetogether.data.local.source.AlbumLocalDataSource
import com.example.lifetogether.data.local.source.GroceryLocalDataSource
import com.example.lifetogether.data.local.source.ListQueryLocalDataSource
import com.example.lifetogether.data.local.source.RoutineListEntryLocalDataSource
import com.example.lifetogether.data.local.source.query.ListQueryType
import com.example.lifetogether.data.local.source.query.ListQueryTypeMapper
import com.example.lifetogether.data.model.Entity
import com.example.lifetogether.domain.listener.ItemResultListener
import com.example.lifetogether.domain.listener.ListItemsResultListener
import com.example.lifetogether.domain.model.Item
import com.example.lifetogether.domain.model.TipItem
import com.example.lifetogether.domain.model.enums.MediaType
import com.example.lifetogether.domain.model.gallery.Album
import com.example.lifetogether.domain.model.gallery.GalleryImage
import com.example.lifetogether.domain.model.gallery.GalleryMedia
import com.example.lifetogether.domain.model.gallery.GalleryVideo
import com.example.lifetogether.domain.model.guides.Guide
import com.example.lifetogether.domain.model.grocery.GroceryItem
import com.example.lifetogether.domain.model.lists.RoutineListEntry
import com.example.lifetogether.domain.model.lists.UserList
import com.example.lifetogether.domain.model.recipe.Recipe
import com.example.lifetogether.domain.repository.ListRepository
import com.example.lifetogether.domain.result.Result
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import kotlin.reflect.KClass

class LocalListRepositoryImpl @Inject constructor(
    private val listQueryLocalDataSource: ListQueryLocalDataSource,
    private val groceryLocalDataSource: GroceryLocalDataSource,
    private val albumLocalDataSource: AlbumLocalDataSource,
    private val routineListEntryLocalDataSource: RoutineListEntryLocalDataSource,
) : ListRepository {
    private companion object {
        const val TAG = "LocalListRepository"
    }

    fun deleteItems(
        queryType: ListQueryType,
        itemIds: List<String>,
    ): Result<Unit, String> {
        return when (queryType) {
            ListQueryType.Grocery -> groceryLocalDataSource.deleteItems(itemIds)
            ListQueryType.RoutineListEntries -> routineListEntryLocalDataSource.deleteItems(itemIds)
            else -> Result.Failure("Unsupported delete type: $queryType")
        }
    }

    fun fetchAlbumMedia(
        familyId: String,
        albumId: String,
    ): Flow<ListItemsResultListener<GalleryMedia>> {
        Log.d(TAG, "fetchAlbumMedia init familyId=$familyId albumId=$albumId")
        return albumLocalDataSource.getAlbumMedia(familyId, albumId)
            .map { entities ->
                try {
                    Log.d(TAG, "fetchAlbumMedia entitiesCount=${entities.size}")
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
                    Log.d(TAG, "fetchAlbumMedia mappedItemsCount=${itemsList.size}")
                    ListItemsResultListener.Success(itemsList)
                } catch (e: Exception) {
                    Log.e(TAG, "fetchAlbumMedia mapping error", e)
                    ListItemsResultListener.Failure(e.message ?: "Unknown error")
                }
            }
    }

    fun <T : Item> getListItemsFlow(
        queryType: ListQueryType,
        familyId: String,
        itemType: KClass<T>,
        uid: String? = null,
    ): Flow<Result<List<T>, String>> {
        Log.d(TAG, "getListItemsFlow init queryType=$queryType familyId=$familyId uid=$uid itemType=${itemType.simpleName}")
        return listQueryLocalDataSource.getListItems(queryType, familyId, uid)
            .map { entities ->
                try {
                    Log.d(TAG, "getListItemsFlow entitiesCount=${entities.size} queryType=$queryType")
                    val itemsList = entities.map { it.toItem(itemType) }
                        .filterIsInstance(itemType.java)
                        .sortedBy { it.itemName }
                    Log.d(TAG, "getListItemsFlow mappedItemsCount=${itemsList.size} queryType=$queryType")
                    Result.Success(itemsList)
                } catch (e: Exception) {
                    Log.e(TAG, "getListItemsFlow mapping error queryType=$queryType", e)
                    Result.Failure(e.message ?: "Unknown mapping error")
                }
            }
    }

    @Deprecated(
        message = "Use typed fetchListItems(ListQueryType, familyId, itemType, uid).",
        replaceWith = ReplaceWith(
            expression = "fetchListItems(queryType, familyId, itemType, uid)",
            imports = ["com.example.lifetogether.data.local.source.query.ListQueryType"],
        ),
        level = DeprecationLevel.WARNING,
    )
    fun <T : Item> fetchListItems(
        listName: String,
        familyId: String,
        itemType: KClass<T>,
        uid: String? = null,
    ): Flow<ListItemsResultListener<Item>> {
        // TODO(v2-phase2-cleanup): remove temporary String-based API once call sites use ListQueryType directly.
        val queryType = ListQueryTypeMapper.fromTableNameOrNull(listName)
            ?: return flowOf(
                ListItemsResultListener.Success(emptyList()),
            )
        return getListItemsFlow(queryType, familyId, itemType, uid).map {
            when (it) {
                is Result.Success -> ListItemsResultListener.Success(it.data)
                is Result.Failure -> ListItemsResultListener.Failure(it.error)
            }
        }
    }

    fun getItemByIdFlow(
        queryType: ListQueryType,
        familyId: String,
        id: String,
        itemType: KClass<out Item>,
        uid: String? = null,
    ): Flow<Result<Item, String>> {
        Log.d(TAG, "fetchItemById queryType=$queryType familyId=$familyId uid=$uid id=$id itemType=${itemType.simpleName}")
        return listQueryLocalDataSource.getItemById(queryType, familyId, id, uid)
            .map { entity ->
                try {
                    val entityLabel = when (entity) {
                        is Entity.GroceryList -> "GroceryList(${entity.entity.id})"
                        is Entity.Recipe -> "Recipe(${entity.entity.id})"
                        is Entity.Album -> "Album(${entity.entity.id})"
                        is Entity.GalleryMedia -> "GalleryMedia(${entity.entity.id})"
                        is Entity.Tip -> "Tip(${entity.entity.id})"
                        is Entity.Guide -> "Guide(${entity.entity.id}, started=${entity.entity.started}, resume=${entity.entity.resume})"
                        is Entity.UserList -> "UserList(${entity.entity.id})"
                        is Entity.RoutineListEntry -> "RoutineListEntry(${entity.entity.id})"
                    }
                    Log.d(TAG, "fetchItemById entity=$entityLabel")
                    val item = entity.toItem(itemType)
                    Log.d(TAG, "fetchItemById mapped item id=${item.id} itemType=${item::class.simpleName}")
                    Result.Success(item)
                } catch (e: Exception) {
                    Log.e(TAG, "fetchItemById mapping failure queryType=$queryType id=$id", e)
                    Result.Failure(e.message ?: "Unknown error")
                }
            }
    }

    @Deprecated(
        message = "Use typed fetchItemById(ListQueryType, familyId, id, itemType, uid).",
        replaceWith = ReplaceWith(
            expression = "fetchItemById(queryType, familyId, id, itemType, uid)",
            imports = ["com.example.lifetogether.data.local.source.query.ListQueryType"],
        ),
        level = DeprecationLevel.WARNING,
    )
    fun fetchItemById(
        listName: String,
        familyId: String,
        id: String,
        itemType: KClass<out Item>,
        uid: String? = null,
    ): Flow<ItemResultListener<Item>> {
        // TODO(v2-phase2-cleanup): remove temporary String-based API once call sites use ListQueryType directly.
        val queryType = ListQueryTypeMapper.fromTableNameOrNull(listName)
            ?: return emptyFlow()
        return getItemByIdFlow(queryType, familyId, id, itemType, uid).map {
            when (it) {
                is Result.Success -> ItemResultListener.Success(it.data)
                is Result.Failure -> ItemResultListener.Failure(it.error)
            }
        }
    }

    private fun Entity.toItem(itemType: KClass<out Item>): Item { //todo can this be done in a better way or is it okay?
        return when (this) {
            is Entity.GroceryList -> when (itemType) {
                GroceryItem::class -> GroceryItem(
                    id = this.entity.id,
                    familyId = this.entity.familyId,
                    itemName = this.entity.name,
                    lastUpdated = this.entity.lastUpdated,
                    completed = this.entity.completed,
                    category = this.entity.category,
                    approxPrice = this.entity.approxPrice,
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

            is Entity.Guide -> when (itemType) {
                Guide::class -> Guide(
                    id = this.entity.id,
                    familyId = this.entity.familyId,
                    itemName = this.entity.itemName,
                    lastUpdated = this.entity.lastUpdated,
                    description = this.entity.description,
                    visibility = this.entity.visibility,
                    ownerUid = this.entity.ownerUid,
                    contentVersion = this.entity.contentVersion,
                    started = this.entity.started,
                    sections = this.entity.sections,
                    resume = this.entity.resume,
                )

                else -> throw IllegalArgumentException("Unsupported item type: $itemType")
            }

            is Entity.UserList -> when (itemType) {
                UserList::class -> UserList(
                    id = this.entity.id,
                    familyId = this.entity.familyId,
                    itemName = this.entity.itemName,
                    lastUpdated = this.entity.lastUpdated,
                    dateCreated = this.entity.dateCreated,
                    type = this.entity.type,
                    visibility = this.entity.visibility,
                    ownerUid = this.entity.ownerUid,
                )

                else -> throw IllegalArgumentException("Unsupported item type: $itemType")
            }

            is Entity.RoutineListEntry -> when (itemType) {
                RoutineListEntry::class -> RoutineListEntry(
                    id = this.entity.id,
                    familyId = this.entity.familyId,
                    listId = this.entity.listId,
                    itemName = this.entity.itemName,
                    lastUpdated = this.entity.lastUpdated,
                    dateCreated = this.entity.dateCreated,
                    nextDate = this.entity.nextDate,
                    lastCompletedAt = this.entity.lastCompletedAt,
                    completionCount = this.entity.completionCount,
                    recurrenceUnit = this.entity.recurrenceUnit,
                    interval = this.entity.interval,
                    weekdays = this.entity.weekdays,
                )

                else -> throw IllegalArgumentException("Unsupported item type: $itemType")
            }
        }
    }
}
