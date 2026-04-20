package com.example.lifetogether.domain.repository

import android.net.Uri
import com.example.lifetogether.domain.listener.ByteArrayResultListener
import com.example.lifetogether.domain.listener.ResultListener
import com.example.lifetogether.domain.listener.StringResultListener
import com.example.lifetogether.domain.model.gallery.Album
import com.example.lifetogether.domain.model.gallery.GalleryMedia
import com.example.lifetogether.domain.result.Result
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface GalleryRepository {
    val thumbnailCache: StateFlow<Map<String, ByteArray>>
    fun observeAlbums(familyId: String): Flow<Result<List<Album>, String>>
    fun observeAlbumById(familyId: String, albumId: String): Flow<Result<Album, String>>
    fun observeAlbumMedia(familyId: String, albumId: String): Flow<Result<List<GalleryMedia>, String>>
    suspend fun saveAlbum(album: Album): Result<String, String>
    suspend fun updateAlbum(album: Album): Result<Unit, String>
    suspend fun fetchAlbumThumbnail(albumId: String)
    suspend fun getAlbumMediaThumbnail(mediaId: String): ByteArrayResultListener
    suspend fun saveGalleryMediaMetaData(galleryMedia: List<GalleryMedia>): ResultListener
    suspend fun uploadVideo(uri: Uri, path: String, extension: String): StringResultListener
}
