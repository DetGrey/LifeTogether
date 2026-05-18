package com.example.lifetogether.data.local.source

import com.example.lifetogether.data.logic.appResultOfSuspend

import com.example.lifetogether.domain.result.AppError

import android.content.Context
import androidx.core.net.toUri
import com.example.lifetogether.data.local.AppDatabase
import com.example.lifetogether.data.local.dao.GalleryMediaDao
import com.example.lifetogether.domain.result.Result
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionCleanupLocalDataSource @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val appDatabase: AppDatabase,
    private val galleryMediaDao: GalleryMediaDao,
) {
    suspend fun clearSessionTables(): Result<Unit, AppError> {
        return appResultOfSuspend {
            val resolver = context.contentResolver
            galleryMediaDao.getAll().forEach { item ->
                item.mediaUri?.let { resolver.delete(it.toUri(), null, null) }
            }
            withContext(Dispatchers.IO) {
                appDatabase.clearAllTables()
            }
        }
    }
}
