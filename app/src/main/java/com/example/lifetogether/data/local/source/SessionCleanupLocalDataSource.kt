package com.example.lifetogether.data.local.source

import com.example.lifetogether.data.logic.appResultOfSuspend
import com.example.lifetogether.domain.result.AppError
import com.example.lifetogether.data.local.AppDatabase
import com.example.lifetogether.data.local.dao.GalleryMediaDao
import com.example.lifetogether.domain.result.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionCleanupLocalDataSource @Inject constructor(
    private val appDatabase: AppDatabase,
    private val galleryMediaDao: GalleryMediaDao,
) {
    suspend fun clearSessionTables(): Result<Unit, AppError> {
        return appResultOfSuspend {
            galleryMediaDao.getAll().forEach { item ->
                item.mediaUri?.let { File(it).delete() }
            }
            withContext(Dispatchers.IO) {
                appDatabase.clearAllTables()
            }
        }
    }
}
