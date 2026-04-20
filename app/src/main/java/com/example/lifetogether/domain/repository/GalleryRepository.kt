package com.example.lifetogether.domain.repository

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
    fun observeAlbums(familyId: String): Flow<Result<List<Album>, String>>
    fun syncAlbumsFromRemote(familyId: String): Flow<Result<Unit, String>>
    fun observeAlbumById(familyId: String, albumId: String): Flow<Result<Album, String>>
    fun observeAlbumMedia(familyId: String, albumId: String): Flow<Result<List<GalleryMedia>, String>>
    fun syncGalleryMediaFromRemote(familyId: String, context: Context): Flow<Result<Unit, String>>
    fun downloadMediaToGallery(mediaIds: List<String>, familyId: String): Flow<SaveProgress>
    suspend fun saveAlbum(album: Album): Result<String, String>
    suspend fun updateAlbum(album: Album): Result<Unit, String>
    suspend fun fetchAlbumThumbnail(albumId: String)
    suspend fun getAlbumMediaThumbnail(mediaId: String): Result<ByteArray, String>
    suspend fun saveGalleryMediaMetaData(galleryMedia: List<GalleryMedia>): Result<Unit, String>
    suspend fun uploadVideo(uri: Uri, path: String, extension: String): Result<String, String>
    suspend fun deleteAlbum(albumId: String): Result<Unit, String>
    suspend fun deleteGalleryMedia(mediaIds: List<String>): Result<Unit, String>
    suspend fun updateAlbumCount(albumId: String, count: Int): Result<Unit, String>
    suspend fun moveMediaToAlbum(mediaIdList: Set<String>, newAlbumId: String, oldAlbumId: String): Result<Unit, String>
}
