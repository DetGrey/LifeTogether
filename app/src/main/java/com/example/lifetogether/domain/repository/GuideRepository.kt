package com.example.lifetogether.domain.repository

import com.example.lifetogether.domain.result.AppError

import com.example.lifetogether.domain.model.guides.Guide
import com.example.lifetogether.domain.result.Result
import kotlinx.coroutines.flow.Flow

interface GuideRepository {
    fun observeGuides(familyId: String, uid: String): Flow<Result<List<Guide>, AppError>>
    fun syncGuidesFromRemote(uid: String, familyId: String): Flow<Result<Unit, AppError>>
    fun observeGuideById(familyId: String, id: String, uid: String): Flow<Result<Guide, AppError>>
    suspend fun saveGuide(guide: Guide): Result<String, AppError>
    suspend fun updateGuide(guide: Guide): Result<Unit, AppError>
    suspend fun deleteGuide(guideId: String): Result<Unit, AppError>
    suspend fun markGuideProgressDirty(guide: Guide, uid: String): Boolean
    suspend fun syncPendingGuideProgress(familyId: String, uid: String, force: Boolean, guideId: String?)
}
