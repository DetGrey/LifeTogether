package com.example.lifetogether.data.local.source

import android.content.Context
import androidx.core.net.toUri
import com.example.lifetogether.data.local.dao.AlbumsDao
import com.example.lifetogether.data.local.dao.FamilyInformationDao
import com.example.lifetogether.data.local.dao.GalleryMediaDao
import com.example.lifetogether.data.local.dao.GroceryListDao
import com.example.lifetogether.data.local.dao.GuidesDao
import com.example.lifetogether.data.local.dao.RecipesDao
import com.example.lifetogether.data.local.dao.UserInformationDao
import com.example.lifetogether.domain.listener.ResultListener
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionCleanupLocalDataSource @Inject constructor(
    @ApplicationContext private val context: Context,
    private val groceryListDao: GroceryListDao,
    private val recipesDao: RecipesDao,
    private val userInformationDao: UserInformationDao,
    private val familyInformationDao: FamilyInformationDao,
    private val galleryMediaDao: GalleryMediaDao,
    private val albumsDao: AlbumsDao,
    private val guidesDao: GuidesDao,
) {
    fun clearSessionTables(): ResultListener {
        return try {
            groceryListDao.deleteTable()
            recipesDao.deleteTable()
            userInformationDao.deleteTable()
            familyInformationDao.deleteFamiliesTable()
            familyInformationDao.deleteFamilyMembersTable()

            val resolver = context.contentResolver
            val galleryImages = galleryMediaDao.getAll()
            galleryImages.forEach { item ->
                item.mediaUri?.let { resolver.delete(it.toUri(), null, null) }
            }

            galleryMediaDao.deleteTable()
            albumsDao.deleteTable()
            guidesDao.deleteTable()
            ResultListener.Success
        } catch (e: Exception) {
            ResultListener.Failure("Error: $e")
        }
    }
}
