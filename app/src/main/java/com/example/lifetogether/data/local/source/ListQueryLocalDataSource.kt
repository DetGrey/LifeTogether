package com.example.lifetogether.data.local.source

import android.util.Log
import com.example.lifetogether.data.local.dao.AlbumsDao
import com.example.lifetogether.data.local.dao.GalleryMediaDao
import com.example.lifetogether.data.local.dao.GroceryListDao
import com.example.lifetogether.data.local.dao.RecipesDao
import com.example.lifetogether.data.local.dao.RoutineListsDao
import com.example.lifetogether.data.local.dao.TipTrackerDao
import com.example.lifetogether.data.local.dao.UserListsDao
import com.example.lifetogether.data.model.Entity
import com.example.lifetogether.data.local.source.query.ListQueryType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ListQueryLocalDataSource @Inject constructor(
    private val groceryListDao: GroceryListDao,
    private val recipesDao: RecipesDao,
    private val albumsDao: AlbumsDao,
    private val galleryMediaDao: GalleryMediaDao,
    private val tipTrackerDao: TipTrackerDao,
    private val userListsDao: UserListsDao,
    private val routineListsDao: RoutineListsDao,
    private val guideLocalDataSource: GuideLocalDataSource,
) {
    companion object {
        private const val TAG = "ListQueryLocalDataSource"
    }

    fun getListItems(
        queryType: ListQueryType,
        familyId: String,
        uid: String? = null,
    ): Flow<List<Entity>> {
        Log.d(TAG, "getListItems queryType=$queryType")
        return when (queryType) {
            ListQueryType.Grocery -> groceryListDao.getItems(familyId).map { list ->
                list.map { Entity.GroceryList(it) }
            }

            ListQueryType.Recipes -> recipesDao.getItems(familyId).map { list ->
                list.map { Entity.Recipe(it) }
            }

            ListQueryType.Albums -> albumsDao.getItems(familyId).map { list ->
                list.map { Entity.Album(it) }
            }

            ListQueryType.GalleryMedia -> galleryMediaDao.getItems(familyId).map { list ->
                list.map { Entity.GalleryMedia(it) }
            }

            ListQueryType.TipTracker -> tipTrackerDao.getItems(familyId).map { list ->
                list.map { Entity.Tip(it) }
            }

            ListQueryType.Guides -> guideLocalDataSource.getItems(familyId = familyId, uid = uid)

            ListQueryType.UserLists -> userListsDao.getItems(familyId).map { list ->
                list.map { Entity.UserList(it) }
            }

            ListQueryType.RoutineListEntries -> {
                if (uid.isNullOrBlank()) {
                    routineListsDao.getItems(familyId).map { list -> list.map { Entity.RoutineListEntry(it) } }
                } else {
                    //todo when would this happen?
                    routineListsDao.getItemsByListId(familyId, uid).map { list -> list.map { Entity.RoutineListEntry(it) } }
                }
            }
        }
    }

    fun getItemById(
        queryType: ListQueryType,
        familyId: String,
        id: String,
        uid: String? = null,
    ): Flow<Entity> {
        return when (queryType) {
            ListQueryType.Recipes -> flow {
                recipesDao.getItemById(familyId, id)?.let { emit(Entity.Recipe(it)) }
            }

            ListQueryType.Albums -> flow {
                albumsDao.getItemById(familyId, id)?.let { emit(Entity.Album(it)) }
            }

            ListQueryType.GalleryMedia -> flow {
                galleryMediaDao.getItemById(familyId, id)?.let { emit(Entity.GalleryMedia(it)) }
            }

            ListQueryType.Guides -> guideLocalDataSource.getItemById(
                familyId = familyId,
                id = id,
                uid = uid,
            )

            ListQueryType.RoutineListEntries -> flow {
                routineListsDao.getItemById(id)?.let { emit(Entity.RoutineListEntry(it)) }
            }

            else -> flowOf()
        }
    }
}
