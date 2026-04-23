package com.example.lifetogether.domain.repository

import com.example.lifetogether.domain.result.AppError

import android.content.Context
import android.net.Uri
import com.example.lifetogether.domain.model.SaveProgress
import com.example.lifetogether.domain.model.gallery.Album
import com.example.lifetogether.domain.model.gallery.GalleryMedia
import com.example.lifetogether.domain.result.Result
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface GalleryRepository {
    val thumbnailCache: StateFlow<Map<String, ByteArray>>
    fun observeAlbums(familyId: String): Flow<Result<List<Album>, AppError>>
    fun syncAlbumsFromRemote(familyId: String): Flow<Result<Unit, AppError>>
    fun observeAlbumById(familyId: String, albumId: String): Flow<Result<Album, AppError>>
    fun observeAlbumMedia(familyId: String, albumId: String): Flow<Result<List<GalleryMedia>, AppError>>
    fun syncGalleryMediaFromRemote(familyId: String, context: Context): Flow<Result<Unit, AppError>>
    fun downloadMediaToGallery(mediaIds: List<String>, familyId: String): Flow<SaveProgress>
    suspend fun saveAlbum(album: Album): Result<String, AppError>
    suspend fun updateAlbum(album: Album): Result<Unit, AppError>
    suspend fun fetchAlbumThumbnail(albumId: String)
    suspend fun getAlbumMediaThumbnail(mediaId: String): Result<ByteArray, AppError>
    suspend fun saveGalleryMediaMetaData(galleryMedia: List<GalleryMedia>): Result<Unit, AppError>
    suspend fun uploadVideo(uri: Uri, path: String, extension: String): Result<String, AppError>
    suspend fun deleteAlbum(albumId: String): Result<Unit, AppError>
    suspend fun deleteGalleryMedia(mediaIds: List<String>): Result<Unit, AppError>
    suspend fun updateAlbumCount(albumId: String, count: Int): Result<Unit, AppError>
    suspend fun moveMediaToAlbum(mediaIdList: Set<String>, newAlbumId: String, oldAlbumId: String): Result<Unit, AppError>
}
