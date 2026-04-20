package com.example.lifetogether.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.lifetogether.domain.model.guides.GuideResume
import com.example.lifetogether.util.Constants
import java.util.Date

@Entity(tableName = Constants.GUIDE_PROGRESS_TABLE)
data class GuideProgressEntity(
    @PrimaryKey
    val id: String,
    @ColumnInfo(name = "family_id")
    val familyId: String,
    val uid: String,
    @ColumnInfo(name = "guide_id")
    val guideId: String,
    @ColumnInfo(name = "content_version")
    val contentVersion: Long = 1,
    val started: Boolean = false,
    @ColumnInfo(name = "completed_pointer_keys")
    val completedPointerKeys: List<String> = emptyList(),
    val resume: GuideResume? = null,
    @ColumnInfo(name = "last_updated")
    val lastUpdated: Date = Date(),
    @ColumnInfo(name = "pending_sync")
    val pendingSync: Boolean = true,
    @ColumnInfo(name = "local_updated_at")
    val localUpdatedAt: Date = Date(),
    @ColumnInfo(name = "last_uploaded_at")
    val lastUploadedAt: Date? = null,
)
