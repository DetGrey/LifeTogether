package com.example.lifetogether.domain.repository

import com.example.lifetogether.domain.model.guides.Guide
import com.example.lifetogether.domain.result.Result
import kotlinx.coroutines.flow.Flow

interface GuideRepository {
    fun observeGuides(familyId: String, uid: String): Flow<Result<List<Guide>, String>>
    fun observeGuideById(familyId: String, id: String, uid: String): Flow<Result<Guide, String>>
    suspend fun saveGuide(guide: Guide): Result<String, String>
    suspend fun updateGuide(guide: Guide): Result<Unit, String>
    suspend fun deleteGuide(guideId: String): Result<Unit, String>
    suspend fun markGuideProgressDirty(guide: Guide, uid: String): Boolean
    suspend fun syncPendingGuideProgress(familyId: String, uid: String, force: Boolean, guideId: String?)
}
