package com.example.lifetogether.domain.repository

import android.net.Uri
import com.example.lifetogether.domain.model.beach.BeachAlbum
import com.example.lifetogether.domain.model.beach.BeachMedia
import com.example.lifetogether.domain.result.AppError
import com.example.lifetogether.domain.result.Result
import kotlinx.coroutines.flow.Flow

interface BeachRepository {
    fun observeBeachAlbums(familyId: String): Flow<Result<List<BeachAlbum>, AppError>>
    fun observeBeachAlbumThumbnails(familyId: String): Flow<Map<String, ByteArray>>
    fun observeBeachAlbumById(familyId: String, albumId: String): Flow<Result<BeachAlbum, AppError>>
    fun observeBeachMedia(familyId: String, albumId: String): Flow<Result<List<BeachMedia>, AppError>>
    fun syncBeachAlbumsFromRemote(familyId: String): Flow<Result<Unit, AppError>>
    fun syncBeachMediaFromRemote(familyId: String, albumId: String): Flow<Result<Unit, AppError>>
    suspend fun createBeachAlbum(familyId: String, name: String): Result<BeachAlbum, AppError>
    suspend fun updateBeachAlbum(album: BeachAlbum): Result<Unit, AppError>
    suspend fun deleteBeachAlbum(familyId: String, albumId: String): Result<Unit, AppError>
    suspend fun deleteBeachAlbums(familyId: String, albumIds: List<String>): Result<Unit, AppError>
    suspend fun uploadBeachMedia(
        familyId: String,
        albumId: String,
        imageUris: List<Uri>,
        onProgress: (current: Int, total: Int) -> Unit,
    ): Result<Unit, AppError>
    suspend fun deleteBeachMedia(familyId: String, mediaIds: List<String>, albumId: String): Result<Unit, AppError>
    suspend fun retryBeachMediaDownloads(mediaIds: List<String>, familyId: String): Result<Unit, AppError>
}
