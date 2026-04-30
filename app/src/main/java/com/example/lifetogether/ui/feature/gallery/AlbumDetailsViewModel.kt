package com.example.lifetogether.ui.feature.gallery

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lifetogether.domain.logic.toFullDateString
import com.example.lifetogether.domain.model.SaveProgress
import com.example.lifetogether.domain.model.gallery.GalleryImage
import com.example.lifetogether.domain.model.gallery.GalleryVideo
import com.example.lifetogether.domain.model.gallery.MediaUploadData
import com.example.lifetogether.domain.model.gallery.GalleryMedia
import com.example.lifetogether.domain.model.session.SessionState
import com.example.lifetogether.domain.repository.GalleryRepository
import com.example.lifetogether.domain.repository.SessionRepository
import com.example.lifetogether.domain.result.AppError
import com.example.lifetogether.domain.result.Result
import com.example.lifetogether.domain.result.toUserMessage
import com.example.lifetogether.domain.usecase.gallery.DeleteAlbumUseCase
import com.example.lifetogether.domain.usecase.gallery.DeleteMediaUseCase
import com.example.lifetogether.domain.usecase.gallery.GetAlbumDisplayModelsUseCase
import com.example.lifetogether.domain.usecase.item.MoveMediaToAlbumUseCase
import com.example.lifetogether.domain.usecase.image.UploadGalleryMediaItemsUseCase
import com.example.lifetogether.ui.common.event.UiCommand
import com.example.lifetogether.ui.common.snackbar.SnackbarSeverity
import com.example.lifetogether.ui.model.MenuAction
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.regex.Pattern
import javax.inject.Inject

@HiltViewModel
class AlbumDetailsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val sessionRepository: SessionRepository,
    private val galleryRepository: GalleryRepository,
    private val moveMediaToAlbumUseCase: MoveMediaToAlbumUseCase,
    private val deleteAlbumUseCase: DeleteAlbumUseCase,
    private val getAlbumDisplayModelsUseCase: GetAlbumDisplayModelsUseCase,
    private val deleteMediaUseCase: DeleteMediaUseCase,
    private val uploadGalleryMediaItemsUseCase: UploadGalleryMediaItemsUseCase,
    @param:ApplicationContext private val context: Context,
) : ViewModel() {
    private val _uiState = MutableStateFlow(AlbumDetailsUiState())
    val uiState: StateFlow<AlbumDetailsUiState> = _uiState.asStateFlow()

    private val _uiCommands = Channel<UiCommand>(Channel.BUFFERED)
    val uiCommands: Flow<UiCommand> = _uiCommands.receiveAsFlow()

    private val _commands = Channel<AlbumDetailsCommand>(Channel.BUFFERED)
    val commands: Flow<AlbumDetailsCommand> = _commands.receiveAsFlow()

    private val requestedThumbnailIds = mutableSetOf<String>()
    private var observeAlbumJob: Job? = null
    private var observeAlbumMediaJob: Job? = null
    private var familyId: String? = null
    private val albumId: String? = savedStateHandle["albumId"]

    private var syncRetryAttempts = 0
    private val maxSyncRetryAttempts = 3

    init {
        viewModelScope.launch {
            sessionRepository.sessionState.collect { state ->
                val newFamilyId = (state as? SessionState.Authenticated)?.user?.familyId
                if (newFamilyId != null && newFamilyId != familyId) {
                    familyId = newFamilyId
                    _uiState.update { it.copy(familyId = familyId) }
                    syncRetryAttempts = 0
                    observeAlbum()
                    observeAlbumMedia()
                } else if (state is SessionState.Unauthenticated) {
                    familyId = null
                    observeAlbumJob?.cancel()
                    observeAlbumMediaJob?.cancel()
                    observeAlbumJob = null
                    observeAlbumMediaJob = null
                }
            }
        }
    }

    suspend fun uploadGalleryMediaItems(uris: List<Uri>): Result<Unit, AppError> {
        val familyIdValue = familyId ?: return Result.Failure(AppError.Validation("Missing family context"))
        val albumIdValue = albumId ?: return Result.Failure(AppError.Validation("Missing album context"))

        val mediaUploadDataList = mutableListOf<MediaUploadData>()

        for (uri in uris) {
            val mimeType = context.contentResolver.getType(uri)
            when {
                mimeType?.startsWith("image/") == true -> {
                    val extension = getMimeTypeExtension(mimeType) ?: ".jpeg"
                    val metadata = extractImageMetadata(context, uri, familyIdValue, albumIdValue, extension)
                    mediaUploadDataList.add(
                        MediaUploadData.ImageUpload(
                            uri = uri,
                            mediaType = metadata.first,
                            extension = metadata.second,
                        ),
                    )
                }

                mimeType?.startsWith("video/") == true -> {
                    val extension = getMimeTypeExtension(mimeType) ?: ".mp4"
                    val metadata = extractVideoMetadata(context, uri, familyIdValue, albumIdValue, extension)
                    mediaUploadDataList.add(
                        MediaUploadData.VideoUpload(
                            uri = uri,
                            mediaType = metadata.first,
                            extension = metadata.second,
                        ),
                    )
                }
            }
        }

        if (mediaUploadDataList.isEmpty()) {
            return Result.Failure(AppError.Validation("No supported files found to upload."))
        }

        return uploadGalleryMediaItemsUseCase.invoke(mediaUploadDataList, context)
    }

    fun onUiEvent(event: AlbumDetailsUiEvent) {
        when (event) {
            AlbumDetailsUiEvent.RetryFetchAlbumMedia -> retryFetchAlbumMedia()
            AlbumDetailsUiEvent.ToggleOverflowMenu -> toggleOverflowMenu()
            AlbumDetailsUiEvent.ToggleSelectionMode -> toggleSelectionMode()
            AlbumDetailsUiEvent.ToggleAllMediaSelection -> toggleAllMediaSelection()
            is AlbumDetailsUiEvent.ToggleMediaSelection -> toggleMediaSelection(event.mediaId)
            is AlbumDetailsUiEvent.EnterSelectionMode -> enterSelectionMode(event.mediaId)
            AlbumDetailsUiEvent.RequestImageUpload -> _uiState.update {
                it.copy(showImageUploadDialog = true)
            }
            AlbumDetailsUiEvent.DismissImageUploadDialog,
            AlbumDetailsUiEvent.ConfirmImageUploadDialog ->  _uiState.update {
                it.copy(showImageUploadDialog = false)
            }
            is AlbumDetailsUiEvent.StartOverflowAction -> startOverflowAction(event.action)
            AlbumDetailsUiEvent.DismissOverflowMenuActionDialog -> dismissOverflowMenuActionDialog()
            is AlbumDetailsUiEvent.SetActionDialogText -> setActionDialogText(event.text)
            AlbumDetailsUiEvent.ConfirmRenameAlbum -> renameAlbum()
            AlbumDetailsUiEvent.ConfirmDeleteAlbum -> deleteAlbum()
            AlbumDetailsUiEvent.DownloadSelectedMedia -> downloadSelectedMedia()
            AlbumDetailsUiEvent.ConfirmDeleteSelectedMedia -> deleteSelectedMedia()
            is AlbumDetailsUiEvent.MoveSelectedMediaToAlbum -> moveSelectedMediaToAlbum(event.albumId)
            AlbumDetailsUiEvent.ConfirmMoveSelectedMedia -> showError("Please choose an album first")
        }
    }

    private fun observeAlbum() {
        val familyIdValue = familyId ?: return
        val albumIdValue = albumId ?: return

        observeAlbumJob?.cancel()
        observeAlbumJob = viewModelScope.launch {
            galleryRepository.observeAlbumById(familyIdValue, albumIdValue).collect { result ->
                when (result) {
                    is Result.Success -> {
                        val album = result.data
                        _uiState.update { state ->
                            state.copy(
                                album = album,
                                isSyncing = album.count > 0 && state.media.isEmpty(),
                            )
                        }
                    }

                    is Result.Failure -> showError(result.error.toUserMessage())
                }
            }
        }
    }

    private fun observeAlbumMedia() {
        val familyIdValue = familyId ?: return
        val albumIdValue = albumId ?: return

        observeAlbumMediaJob?.cancel()
        observeAlbumMediaJob = viewModelScope.launch {
            val expectedCount = _uiState.value.album?.count ?: 0
            if (expectedCount > 0 && _uiState.value.media.isEmpty()) {
                _uiState.update { it.copy(isSyncing = true) }
            }

            galleryRepository.observeAlbumMedia(familyIdValue, albumIdValue).collect { result ->
                when (result) {
                    is Result.Success -> handleMediaSuccess(result.data)
                    is Result.Failure -> handleMediaFailure(result.error.toUserMessage())
                }
            }
        }
    }

    private fun handleMediaSuccess(items: List<GalleryMedia>) {
        if (items.isNotEmpty()) {
            _uiState.update {
                it.copy(
                    media = items,
                    isSyncing = false,
                )
            }
            groupMedia()
            items.forEach { media ->
                val mediaId = media.id ?: return@forEach
                if (!requestedThumbnailIds.contains(mediaId)) {
                    requestedThumbnailIds.add(mediaId)
                    fetchThumbnail(mediaId)
                }
            }

            val expectedCount = _uiState.value.album?.count ?: 0
            if (expectedCount > items.size && syncRetryAttempts < maxSyncRetryAttempts) {
                _uiState.update { it.copy(isSyncing = true, isPartialLoad = true) }
                syncRetryAttempts += 1
                viewModelScope.launch {
                    delay(3000)
                    observeAlbumMedia()
                }
                return
            } else if (expectedCount > items.size) {
                _uiState.update { it.copy(isPartialLoad = true, isSyncing = false, isRefreshing = false) }
                return
            }

            syncRetryAttempts = 0
            _uiState.update { it.copy(isPartialLoad = false, isRefreshing = false) }
            return
        }

        val expectedCount = _uiState.value.album?.count ?: 0
        if (expectedCount > 0 && syncRetryAttempts < maxSyncRetryAttempts) {
            _uiState.update { it.copy(isSyncing = true) }
            syncRetryAttempts += 1
            viewModelScope.launch {
                delay(2000)
                observeAlbumMedia()
            }
        } else {
            _uiState.update { it.copy(isSyncing = false) }
        }
    }

    private fun groupMedia() {
        val grouped = uiState.value.media
            .sortedByDescending { it.dateCreated }
            .groupBy { it.dateCreated?.toFullDateString() ?: "Unknown Date" }
            .toList()

        _uiState.update { it.copy(groupedMedia = grouped) }
    }

    private fun handleMediaFailure(message: String) {
        _uiState.update { it.copy(isSyncing = false, isRefreshing = false) }
        showError(message)
    }

    private fun retryFetchAlbumMedia() {
        _uiState.update { it.copy(isRefreshing = true) }
        syncRetryAttempts = 0
        observeAlbumMedia()
    }

    private fun fetchThumbnail(mediaId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            when (val result = galleryRepository.getAlbumMediaThumbnail(mediaId)) {
                is Result.Success -> {
                    _uiState.update { state ->
                        state.copy(thumbnails = state.thumbnails + (mediaId to result.data))
                    }
                }

                is Result.Failure -> Unit
            }
        }
    }

    private fun renameAlbum() {
        val newName = _uiState.value.actionDialogText.trim()
        val currentAlbum = _uiState.value.album

        if (newName.isEmpty()) {
            showError("Album name cannot be empty")
            return
        }
        if (newName == currentAlbum?.itemName) {
            showError("Album already called $newName")
            return
        }

        val updatedAlbum = currentAlbum?.copy(itemName = newName) ?: return

        viewModelScope.launch {
            when (val result = galleryRepository.updateAlbum(updatedAlbum)) {
                is Result.Success -> {
                    _uiState.update { it.copy(album = updatedAlbum) }
                    dismissOverflowMenuActionDialog()
                }

                is Result.Failure -> showError(result.error.toUserMessage())
            }
        }
    }

    private fun deleteAlbum() {
        val albumIdValue = uiState.value.album?.id ?: return

        viewModelScope.launch {
            when (val result = deleteAlbumUseCase.invoke(albumIdValue, uiState.value.media)) {
                is Result.Success -> {
                    dismissOverflowMenuActionDialog()
                    sendCommand(AlbumDetailsCommand.NavigateBack)
                }

                is Result.Failure -> showError(result.error.toUserMessage())
            }
        }
    }

    private fun downloadSelectedMedia() {
        val familyIdValue = familyId
        val selectedMedia = _uiState.value.selectedMedia.toList()
        if (selectedMedia.isEmpty() || familyIdValue == null) {
            showError("Media data not available")
            return
        }

        viewModelScope.launch {
            galleryRepository.downloadMediaToGallery(
                mediaIds = selectedMedia,
                familyId = familyIdValue,
            ).collect { progress ->
                when (progress) {
                    is SaveProgress.Loading -> {
                        showProgress(
                            title = "Downloading...",
                            message = "Downloading ${progress.current} of ${progress.total}",
                        )
                    }

                    is SaveProgress.Finished -> {
                        if (progress.failureCount == 0) {
                            dismissOverflowMenuActionDialog()
                            val itemLabel = if (progress.successCount == 1) "item" else "items"
                            showProgress(
                                title = "Download complete",
                                message = "Downloaded ${progress.successCount} $itemLabel",
                                showProgress = false,
                            )
                            delay(2000)
                            hideProgress()
                        } else {
                            hideProgress()
                            showError("Failed to download media")
                        }
                    }

                    is SaveProgress.Error -> {
                        hideProgress()
                        showError(progress.message)
                    }
                }
            }
        }
    }

    private fun observeAlbums() {
        val familyIdValue = familyId ?: return

        viewModelScope.launch {
            getAlbumDisplayModelsUseCase.invoke(familyIdValue).collect { result ->
                when (result) {
                    is Result.Success -> {
                        val possibleAlbums = result.data.filterNot { it.id == albumId }
                        _uiState.update { it.copy(albums = possibleAlbums, showOverflowMenuActionDialog = true) }

                        possibleAlbums.forEach { model ->
                            if (model.thumbnail == null && !requestedThumbnailIds.contains(model.id)) {
                                requestedThumbnailIds.add(model.id)
                                galleryRepository.fetchAlbumThumbnail(model.id)
                            }
                        }
                    }

                    is Result.Failure -> showError(result.error.toUserMessage())
                }
            }
        }
    }

    private fun moveSelectedMediaToAlbum(newAlbumId: String) {
        val oldAlbumId = albumId ?: return
        val selectedMedia = _uiState.value.selectedMedia
        if (selectedMedia.isEmpty() || newAlbumId == _uiState.value.album?.id || newAlbumId.isEmpty()) return

        viewModelScope.launch {
            when (val result = moveMediaToAlbumUseCase.invoke(selectedMedia, newAlbumId, oldAlbumId)) {
                is Result.Success -> {
                    dismissOverflowMenuActionDialog()
                    toggleSelectionMode()
                }

                is Result.Failure -> showError(result.error.toUserMessage())
            }
        }
    }

    private suspend fun extractImageMetadata(
        context: Context,
        uri: Uri,
        familyId: String,
        albumId: String,
        ext: String,
    ): Pair<GalleryImage, String> = withContext(Dispatchers.IO) {
        val dateCreated = getBestDate(context, uri)
        val itemName = formatMediaName(dateCreated, ext)

        val galleryImage = GalleryImage(
            familyId = familyId,
            itemName = itemName,
            albumId = albumId,
            dateCreated = dateCreated,
        )
        Pair(galleryImage, ext)
    }

    private suspend fun extractVideoMetadata(
        context: Context,
        uri: Uri,
        familyId: String,
        albumId: String,
        ext: String,
    ): Pair<GalleryVideo, String> {
        var duration: Long? = null
        var dateCreated: Date? = null

        withContext(Dispatchers.IO) {
            val retriever = android.media.MediaMetadataRetriever()
            try {
                retriever.setDataSource(context, uri)
                val durationStr = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)
                duration = durationStr?.toLongOrNull()

                val dateStr = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DATE)
                if (dateStr != null) {
                    try {
                        val sdf = SimpleDateFormat("yyyyMMdd'T'HHmmss.SSS'Z'", Locale.UK)
                        sdf.timeZone = java.util.TimeZone.getTimeZone("UTC")
                        dateCreated = sdf.parse(dateStr)
                    } catch (_: Exception) {

                    }
                }
                dateCreated = dateCreated ?: getFileLastModifiedDate(context, uri)
            } finally {
                try {
                    retriever.release()
                } catch (_: IOException) {

                }
            }
        }
        dateCreated = dateCreated ?: Date()

        val itemName = formatMediaName(dateCreated, ext)
        val galleryVideo = GalleryVideo(
            familyId = familyId,
            itemName = itemName,
            albumId = albumId,
            dateCreated = dateCreated,
            duration = duration,
        )
        return Pair(galleryVideo, ext)
    }

    private fun getFileLastModifiedDate(context: Context, uri: Uri): Date? {
        return try {
            when (uri.scheme) {
                ContentResolver.SCHEME_CONTENT -> {
                    context.contentResolver.query(uri, arrayOf(MediaStore.MediaColumns.DATE_MODIFIED), null, null, null)?.use {
                        if (it.moveToFirst()) {
                            Date(it.getLong(it.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_MODIFIED)) * 1000)
                        } else {
                            null
                        }
                    }
                }

                ContentResolver.SCHEME_FILE -> {
                    val file = uri.path?.let { java.io.File(it) }
                    if (file?.exists() == true) Date(file.lastModified()) else null
                }

                else -> null
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun getMimeTypeExtension(mimeType: String?): String? {
        return mimeType?.let { android.webkit.MimeTypeMap.getSingleton().getExtensionFromMimeType(it) }?.let { ".$it" }
    }

    private fun getBestDate(context: Context, uri: Uri): Date? {
        val exifDate = getExifDate(context, uri)
        if (exifDate != null) return exifDate

        val documentDate = getDocumentLastModified(context, uri)
        if (documentDate != null) return documentDate

        return parseWhatsAppFilename(context, uri)
    }

    private fun getExifDate(context: Context, uri: Uri): Date? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val exif = androidx.exifinterface.media.ExifInterface(inputStream)
                val dateCreatedString = exif.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_DATETIME_ORIGINAL)
                    ?: exif.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_DATETIME)
                    ?: exif.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_DATETIME_DIGITIZED)

                if (!dateCreatedString.isNullOrBlank()) {
                    com.example.lifetogether.domain.logic.parseExifDate(dateCreatedString)
                } else {
                    getImageDateFromMediaStore(context, uri)?.let { com.example.lifetogether.domain.logic.parseExifDate(timestamp = it) }
                }
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun getDocumentLastModified(context: Context, uri: Uri): Date? {
        return try {
            context.contentResolver.query(uri, null, null, null, null)?.use {
                if (!it.moveToFirst()) return null

                val index = it.getColumnIndex("last_modified")
                if (index == -1) return null

                val lastModified = it.getLong(index)
                if (lastModified <= 0) return null

                if (lastModified < 10000000000L) Date(lastModified * 1000) else Date(lastModified)
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun parseWhatsAppFilename(context: Context, uri: Uri): Date? {
        return try {
            var filename: String? = null
            context.contentResolver.query(uri, arrayOf(MediaStore.MediaColumns.DISPLAY_NAME), null, null, null)?.use {
                if (it.moveToFirst()) {
                    filename = it.getString(it.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME))
                }
            }

            if (filename != null && filename.startsWith("IMG-")) {
                val matcher = Pattern.compile("IMG-(\\d{8})-WA.*").matcher(filename)
                if (matcher.find()) {
                    val dateString = matcher.group(1) ?: return null
                    SimpleDateFormat("yyyyMMdd", Locale.US).parse(dateString)
                } else {
                    null
                }
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun getImageDateFromMediaStore(context: Context, uri: Uri): Long? {
        if (uri.scheme != ContentResolver.SCHEME_CONTENT) return null

        val projection = arrayOf(MediaStore.Images.Media.DATE_TAKEN, MediaStore.Images.Media.DATE_MODIFIED)
        context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val dateTakenIndex = cursor.getColumnIndex(MediaStore.Images.Media.DATE_TAKEN)
                if (dateTakenIndex != -1) {
                    val dateTaken = cursor.getLong(dateTakenIndex)
                    if (dateTaken > 0) return dateTaken
                }

                val dateModifiedIndex = cursor.getColumnIndex(MediaStore.Images.Media.DATE_MODIFIED)
                if (dateModifiedIndex != -1) {
                    val dateModified = cursor.getLong(dateModifiedIndex)
                    if (dateModified > 0) return dateModified * 1000
                }
            }
        }
        return null
    }

    private fun formatMediaName(date: Date?, ext: String): String {
        if (date == null) return ""
        return try {
            val outputFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
            outputFormat.format(date) + ext
        } catch (_: Exception) {
            ""
        }
    }

    private fun deleteSelectedMedia() {
        val albumIdValue = uiState.value.album?.id ?: return
        val selectedMedia = uiState.value.media.filter { it.id in uiState.value.selectedMedia }

        if (selectedMedia.isEmpty()) return

        viewModelScope.launch {
            when (val result = deleteMediaUseCase.invoke(albumIdValue, selectedMedia)) {
                is Result.Success -> {
                    dismissOverflowMenuActionDialog()
                    toggleSelectionMode()
                }

                is Result.Failure -> showError(result.error.toUserMessage())
            }
        }
    }

    private fun toggleSelectionMode() {
        _uiState.update { state ->
            val newSelectionMode = !state.isSelectionModeActive
            state.copy(
                isSelectionModeActive = newSelectionMode,
                selectedMedia = if (newSelectionMode) state.selectedMedia else emptySet(),
                isAllMediaSelected = if (newSelectionMode) state.isAllMediaSelected else false,
            )
        }
    }

    private fun toggleOverflowMenu(show: Boolean? = null) {
        _uiState.update { it.copy(showOverflowMenu = show ?: !it.showOverflowMenu) }
    }

    private fun enterSelectionMode(mediaId: String?) {
        if (mediaId == null) return
        if (!_uiState.value.isSelectionModeActive) {
            toggleSelectionMode()
        }
        toggleMediaSelection(mediaId)
    }

    private fun toggleAllMediaSelection() {
        when (_uiState.value.isAllMediaSelected) {
            true -> {
                _uiState.update {
                    it.copy(
                        selectedMedia = emptySet(),
                        isAllMediaSelected = false,
                        isSelectionModeActive = false,
                    )
                }
            }

            false -> {
                _uiState.update { state ->
                    state.copy(
                        selectedMedia = state.media.mapNotNull { it.id }.toSet(),
                        isAllMediaSelected = true,
                    )
                }
            }
        }
    }

    private fun toggleMediaSelection(mediaId: String?) {
        if (mediaId == null) return

        if (_uiState.value.selectedMedia.contains(mediaId)) {
            _uiState.update { state ->
                state.copy(selectedMedia = state.selectedMedia - mediaId)
            }
        } else {
            _uiState.update { state ->
                state.copy(selectedMedia = state.selectedMedia + mediaId)
            }
        }
    }

    private fun startOverflowAction(action: MenuAction) {
        _uiState.update {
            it.copy(
                overflowMenuAction = action,
                showOverflowMenu = false,
            )
        }
        if (action == MenuAction.SelectionActions.MOVE) {
            observeAlbums()
        } else {
            _uiState.update { it.copy(showOverflowMenuActionDialog = true) }
        }
    }

    private fun dismissOverflowMenuActionDialog() {
        _uiState.update {
            it.copy(
                showOverflowMenuActionDialog = false,
                overflowMenuAction = null,
                actionDialogText = "",
            )
        }
    }

    private fun setActionDialogText(text: String) {
        _uiState.update { it.copy(actionDialogText = text) }
    }

    private fun showError(message: String) {
        viewModelScope.launch {
            _uiCommands.send(
                UiCommand.ShowSnackbar(
                    message = message,
                    withDismissAction = true,
                ),
            )
        }
    }

    private fun showProgress(
        title: String,
        message: String,
        showProgress: Boolean = false,
    ) {
        viewModelScope.launch {
            _uiCommands.send(
                UiCommand.ShowProgressSnackbar(
                    title = title,
                    message = message,
                    severity = SnackbarSeverity.Info,
                    showProgress = showProgress,
                ),
            )
        }
    }

    private fun hideProgress() {
        viewModelScope.launch {
            _uiCommands.send(UiCommand.HideProgressSnackbar)
        }
    }

    private fun sendCommand(command: AlbumDetailsCommand) {
        viewModelScope.launch {
            _commands.send(command)
        }
    }
}
