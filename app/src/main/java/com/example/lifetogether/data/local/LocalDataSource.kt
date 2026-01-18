package com.example.lifetogether.data.local

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import android.webkit.MimeTypeMap
import androidx.core.net.toUri
import com.example.lifetogether.data.local.dao.AlbumsDao
import com.example.lifetogether.data.local.dao.CategoriesDao
import com.example.lifetogether.data.local.dao.FamilyInformationDao
import com.example.lifetogether.data.local.dao.GalleryMediaDao
import com.example.lifetogether.data.local.dao.GroceryListDao
import com.example.lifetogether.data.local.dao.GrocerySuggestionsDao
import com.example.lifetogether.data.local.dao.RecipesDao
import com.example.lifetogether.data.local.dao.TipTrackerDao
import com.example.lifetogether.data.local.dao.UserInformationDao
import com.example.lifetogether.data.logic.generateImageThumbnailFromFile
import com.example.lifetogether.data.logic.generateVideoThumbnailFromFile
import com.example.lifetogether.data.model.AlbumEntity
import com.example.lifetogether.data.model.CategoryEntity
import com.example.lifetogether.data.model.Entity
import com.example.lifetogether.data.model.FamilyEntity
import com.example.lifetogether.data.model.FamilyMemberEntity
import com.example.lifetogether.data.model.GalleryMediaEntity
import com.example.lifetogether.data.model.GroceryListEntity
import com.example.lifetogether.data.model.GrocerySuggestionEntity
import com.example.lifetogether.data.model.RecipeEntity
import com.example.lifetogether.data.model.TipEntity
import com.example.lifetogether.data.model.UserEntity
import com.example.lifetogether.domain.callback.ResultListener
import com.example.lifetogether.domain.model.Category
import com.example.lifetogether.domain.model.TipItem
import com.example.lifetogether.domain.model.UserInformation
import com.example.lifetogether.domain.model.family.FamilyInformation
import com.example.lifetogether.domain.model.gallery.Album
import com.example.lifetogether.domain.model.gallery.GalleryImage
import com.example.lifetogether.domain.model.gallery.GalleryMedia
import com.example.lifetogether.domain.model.gallery.GalleryVideo
import com.example.lifetogether.domain.model.grocery.GroceryItem
import com.example.lifetogether.domain.model.grocery.GrocerySuggestion
import com.example.lifetogether.domain.model.recipe.Recipe
import com.example.lifetogether.domain.model.sealed.ImageType
import com.example.lifetogether.util.Constants
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import java.io.File
import java.io.FileInputStream
import javax.inject.Inject

class LocalDataSource @Inject constructor(
    @ApplicationContext private val context: Context, // Injected app context
    private val groceryListDao: GroceryListDao,
    private val recipesDao: RecipesDao,
    private val grocerySuggestionsDao: GrocerySuggestionsDao,
    private val categoriesDao: CategoriesDao,
    private val userInformationDao: UserInformationDao,
    private val familyInformationDao: FamilyInformationDao,
    private val galleryMediaDao: GalleryMediaDao,
    private val albumsDao: AlbumsDao,
    private val tipTrackerDao: TipTrackerDao,
) {
    // -------------------------------------------------------------- CATEGORIES
    fun getCategories(): Flow<List<CategoryEntity>> {
        return categoriesDao.getItems()
    }

    suspend fun updateCategories(items: List<Category>) {
        val categoryEntities = items.map { category ->
            CategoryEntity(
                emoji = category.emoji,
                name = category.name,
            )
        }
        categoriesDao.updateItems(categoryEntities)

        // Fetch the current items from the Room database
        //   getItems() returns a Flow, so you need to use first() to get the current value
        val currentItems = categoriesDao.getItems().first()

        // Determine the items to be inserted or updated
        val itemsToUpdate = categoryEntities.filter { newItem ->
            currentItems.none { currentItem -> newItem.name == currentItem.name && newItem.emoji == currentItem.emoji }
        }

        // Determine the items to be deleted
        val itemsToDelete = currentItems.filter { currentItem ->
            categoryEntities.none { newItem -> newItem.name == currentItem.name }
        }

        // Update the Room database with the new or changed items
        categoriesDao.updateItems(itemsToUpdate)

        // Delete the items that no longer exist in Firestore
        categoriesDao.deleteItems(itemsToDelete)
    }

    // -------------------------------------------------------------- ITEMS
    fun getListItems(
        listName: String,
        familyId: String,
    ): Flow<List<Entity>> {
        println("LocalDataSource getListItems listName: $listName")
        val items: Flow<List<Entity>> = when (listName) {
            Constants.GROCERY_TABLE -> groceryListDao.getItems(familyId).map { list ->
                list.map { Entity.GroceryList(it) }
            }
            Constants.RECIPES_TABLE -> recipesDao.getItems(familyId).map { list ->
                list.map { Entity.Recipe(it) }
            }
            Constants.ALBUMS_TABLE -> albumsDao.getItems(familyId).map { list ->
                list.map { Entity.Album(it) }
            }
            Constants.GALLERY_MEDIA_TABLE -> galleryMediaDao.getItems(familyId).map { list ->
                list.map { Entity.GalleryMedia(it) }
            }
            Constants.TIP_TRACKER_TABLE -> tipTrackerDao.getItems(familyId).map { list ->
                list.map { Entity.Tip(it) }
            }
            else -> flowOf(emptyList()) // Handle the case where the listName doesn't match any known entity
        }
        println("LocalDataSource getListItems: $items")
        return items
    }

    fun getItemById(
        listName: String,
        familyId: String,
        id: String,
    ): Flow<Entity> {
        return when (listName) {
            Constants.RECIPES_TABLE -> flow {
                val recipe = recipesDao.getItemById(familyId, id)
                if (recipe != null) {
                    emit(Entity.Recipe(recipe))
                }
            }
            Constants.ALBUMS_TABLE -> flow {
                val album = albumsDao.getItemById(familyId, id)
                if (album != null) {
                    emit(Entity.Album(album))
                }
            }
            Constants.GALLERY_MEDIA_TABLE -> flow {
                val galleryMedia = galleryMediaDao.getItemById(familyId, id)
                if (galleryMedia != null) {
                    emit(Entity.GalleryMedia(galleryMedia))
                }
            }
            else -> flowOf() // Handle the case where the listName doesn't match any known entity
        }
    }

    fun deleteItems(
        listName: String,
        itemIds: List<String>,
    ): ResultListener {
        println("LocalDataSource deleteItems()")
        try {
            when (listName) {
                Constants.GROCERY_TABLE -> groceryListDao.deleteItems(itemIds)
                else -> {}
            }
            return ResultListener.Success
        } catch (e: Exception) {
            return ResultListener.Failure("Error: ${e.message}")
        }
    }

    // -------------------------------------------------------------- RECIPES
    suspend fun updateRecipes(
        items: List<Recipe>,
        byteArrays: Map<String, ByteArray>,
    ) {
        println("LocalDataSource updateRecipes(): Trying to add firestore data to Room")

        println("Recipe list: $items")
        var recipeEntityList = items.map { item ->
            RecipeEntity(
                id = item.id ?: "",
                familyId = item.familyId,
                itemName = item.itemName,
                lastUpdated = item.lastUpdated,
                description = item.description,
                ingredients = item.ingredients,
                instructions = item.instructions,
                preparationTimeMin = item.preparationTimeMin,
                favourite = item.favourite,
                servings = item.servings,
                tags = item.tags,
            )
        }

        if (byteArrays.isNotEmpty()) {
            recipeEntityList = recipeEntityList.map { item ->
                item.copy(imageData = if (byteArrays[item.id] != null) byteArrays[item.id] else null)
            }
        }

//        println("recipeEntityList list: ${recipeEntityList.map { listOf(it.itemName, it.tags) }}")

        // Fetch the current items from the Room database
        val currentItems = recipesDao.getItems(items[0].familyId).first()

        for (item in currentItems) {
            if (item.itemName == "Chicken burger") {
                println("chicken burger currentItems: $item")
            }
        }
        for (item in recipeEntityList) {
            if (item.itemName == "Chicken burger") {
                println("chicken burger recipeEntityList: $item")
            }
        }

        // Determine the items to be inserted or updated
        val itemsToUpdate = recipeEntityList.filter { newItem ->
            currentItems.none { currentItem -> newItem.id == currentItem.id && newItem == currentItem }
        }

        println("recipeEntityList itemsToUpdate: ${itemsToUpdate.map { listOf(it.itemName, it.tags) }}")

        // Determine the items to be deleted
        val itemsToDelete = currentItems.filter { currentItem ->
            recipeEntityList.none { newItem -> newItem.id == currentItem.id }
        }

        // Update the Room database with the new or changed items
        recipesDao.updateItems(itemsToUpdate)

        // Delete the items that no longer exist in Firestore
        recipesDao.deleteItems(itemsToDelete.map { it.id })
    }

    suspend fun deleteFamilyRecipes(familyId: String) {
        val currentFamilyItems = recipesDao.getItems(familyId).firstOrNull()
        if (currentFamilyItems != null) {
            recipesDao.deleteItems(currentFamilyItems.map { it.id })
        }
    }

    suspend fun getRecipeIdsWithImages(familyId: String): Set<String> {
        return recipesDao.getRecipeIdsWithImages(familyId).toSet()
    }

    // -------------------------------------------------------------- GROCERY LIST
    suspend fun updateGroceryList(items: List<GroceryItem>) {
        println("LocalDataSource updateGroceryList(): Trying to add firestore data to Room")

        println("GroceryItem list: $items")
        val groceryListEntityList = items.map { item ->
            GroceryListEntity(
                id = item.id ?: "",
                familyId = item.familyId,
                name = item.itemName,
                lastUpdated = item.lastUpdated,
                completed = item.completed,
                category = item.category,
            )
        }
        println("groceryListEntity list: $groceryListEntityList")

        // Fetch the current items from the Room database
        val currentItems = groceryListDao.getItems(items[0].familyId).first()

        // Determine the items to be inserted or updated
        val itemsToUpdate = groceryListEntityList.filter { newItem ->
            currentItems.none { currentItem -> newItem.id == currentItem.id && newItem == currentItem }
        }

        // Determine the items to be deleted
        val itemsToDelete = currentItems.filter { currentItem ->
            groceryListEntityList.none { newItem -> newItem.id == currentItem.id }
        }

        // Update the Room database with the new or changed items
        groceryListDao.updateItems(itemsToUpdate)

        // Delete the items that no longer exist in Firestore
        groceryListDao.deleteItems(itemsToDelete.map { it.id })
    }

    suspend fun deleteFamilyGroceryItems(familyId: String) {
        val currentFamilyItems = groceryListDao.getItems(familyId).firstOrNull()
        if (currentFamilyItems != null) {
            groceryListDao.deleteItems(currentFamilyItems.map { it.id })
        }
    }

    // -------------------------------------------------------------- GROCERY SUGGESTIONS
    fun getGrocerySuggestions(): Flow<List<GrocerySuggestionEntity>> {
        return grocerySuggestionsDao.getItems()
    }

    suspend fun updateGrocerySuggestions(items: List<GrocerySuggestion>) {
        println("LocalDataSource updateGrocerySuggestions(): Trying to add firestore data to Room")
        val grocerySuggestionEntities = items.mapNotNull { grocerySuggestion ->
            grocerySuggestion.id?.let { id ->
                GrocerySuggestionEntity(
                    id = id,
                    suggestionName = grocerySuggestion.suggestionName,
                    category = grocerySuggestion.category,
                )
            }
        }
        grocerySuggestionsDao.updateItems(grocerySuggestionEntities)

        // Fetch the current items from the Room database
        //   getItems() returns a Flow, so you need to use first() to get the current value
        val currentItems = grocerySuggestionsDao.getItems().first()

        // Determine the items to be inserted or updated
        val itemsToUpdate = grocerySuggestionEntities.filter { newItem ->
            currentItems.none { currentItem -> newItem.id == currentItem.id }
        }

        // Determine the items to be deleted
        val itemsToDelete = currentItems.filter { currentItem ->
            grocerySuggestionEntities.none { newItem -> newItem.id == currentItem.id }
        }

        // Update the Room database with the new or changed items
        grocerySuggestionsDao.updateItems(itemsToUpdate)

        // Delete the items that no longer exist in Firestore
        grocerySuggestionsDao.deleteItems(itemsToDelete)
    }

    // -------------------------------------------------------------- USER INFORMATION
    fun getUserInformation(uid: String): Flow<UserEntity?> {
        return userInformationDao.getItems(uid)
    }

    suspend fun updateUserInformation(
        userInformation: UserInformation,
        byteArray: ByteArray? = null,
    ) {
        var userEntity = UserEntity(
            uid = userInformation.uid ?: "",
            email = userInformation.email,
            name = userInformation.name,
            birthday = userInformation.birthday,
            familyId = userInformation.familyId,
        )
        if (byteArray != null) {
            userEntity = userEntity.copy(imageData = byteArray)
        }

        println("updateUserInformation userEntity: $userEntity")

        userInformationDao.updateItems(userEntity)
    }

    suspend fun userHasProfileImage(uid: String): Boolean {
        return (userInformationDao.hasImageData(uid) ?: 0) == 1
    }

    fun clearUserInformationTables(): ResultListener {
        try {
            groceryListDao.deleteTable()
            recipesDao.deleteTable()
            userInformationDao.deleteTable()
            familyInformationDao.deleteFamiliesTable()
            familyInformationDao.deleteFamilyMembersTable()

            val resolver = context.contentResolver
            val galleryImages = galleryMediaDao.getAll()
            galleryImages.forEach { item ->
                item.mediaUri?.let { resolver.delete(it.toUri(), null, null) }
            }

            galleryMediaDao.deleteTable()
            albumsDao.deleteTable()

            return ResultListener.Success
        } catch (e: Exception) {
            return ResultListener.Failure("Error: $e")
        }
    }

    // -------------------------------------------------------------- FAMILY INFORMATION
    fun getFamilyInformation(familyId: String): Flow<FamilyEntity> {
        return familyInformationDao.getFamilyInfo(familyId)
    }

    fun getFamilyMembers(familyId: String): Flow<List<FamilyMemberEntity>> {
        return familyInformationDao.getFamilyMembers(familyId)
    }

    suspend fun updateFamilyInformation(
        familyInformation: FamilyInformation,
        byteArray: ByteArray? = null,
    ) {
        // Update FamilyEntity
        var familyEntity = FamilyEntity(
            familyId = familyInformation.familyId ?: "",
        )

        if (byteArray != null) {
            familyEntity = familyEntity.copy(imageData = byteArray)
        }

        println("updateFamilyInformation familyEntity: $familyEntity")

        // Update FamilyEntity in the database
        familyInformationDao.updateFamily(familyEntity)

        // Update Family Members
        val familyMembers = familyInformation.members?.map {
            FamilyMemberEntity(
                uid = it.uid ?: "",
                familyId = familyInformation.familyId,
                name = it.name,
            )
        } ?: emptyList()

        // Insert or update FamilyMemberEntity
        familyInformationDao.updateFamilyMembers(familyMembers)
    }

    suspend fun familyHasImage(familyId: String): Boolean {
        return (familyInformationDao.hasImageData(familyId) ?: 0) == 1
    }

    // -------------------------------------------------------------- GALLERY MEDIA
    fun getAlbumMedia(
        familyId: String,
        albumId: String,
    ): Flow<List<Entity.GalleryMedia>> {
        println("LocalDataSource getAlbumMedia")
        val items: Flow<List<Entity.GalleryMedia>> = galleryMediaDao.getItemsByAlbumId(familyId, albumId)
            .map { list ->
                list.map { Entity.GalleryMedia(it) }
            }
        println("LocalDataSource getAlbumMedia: $items")
        return items
    }

    suspend fun getAlbumMediaThumbnail(
        imageId: String,
    ): ByteArray? {
        return galleryMediaDao.getMediaThumbnail(imageId)
    }

    suspend fun getAlbumThumbnail(
        albumId: String,
    ): ByteArray? {
        return galleryMediaDao.getNewestMediaThumbnailByAlbumId(albumId)
    }

    suspend fun getExistingGalleryMediaInfo(
        familyId: String,
    ): Map<String, Pair<String?, String?>> {
        // Returns a map of mediaId to (mediaUri, mediaUrl)
        // Since we don't store mediaUrl, we return null for it
        // This checks if media exists locally by checking if mediaUri exists
        val entities = galleryMediaDao.getExistingMediaIdsWithUris(familyId)
        return entities.associate { entity ->
            entity.id to Pair(entity.media_uri, null)
        }
    }

    suspend fun updateGalleryMedia(
        familyId: String,
        items: List<Pair<GalleryMedia, File>>,
    ) {
        if (items.isEmpty()) {
            return
        }
        println("LocalDataSource updateGalleryMedia(): Saving images and videos to MediaStore")

        val resolver = context.contentResolver

        // Fetch current items from Room
        val currentRoomItems = galleryMediaDao.getItems(familyId).firstOrNull() ?: emptyList()
        val currentMediaStoreUrisMap = currentRoomItems.associateBy({ it.id }, { it.mediaUri })

        val entityList = mutableListOf<GalleryMediaEntity>()

        for ((mediaItem, downloadedFile) in items) {
            Log.d("LocalDataSource", "Processing item: ${mediaItem.itemName} (ID: ${mediaItem.id}), File: ${downloadedFile.name}")

            // 1. Save to MediaStore if new, or keep existing URI
            val mediaStoreUriString: String? = currentMediaStoreUrisMap[mediaItem.id]
                ?: saveMediaFileToGallery(context, downloadedFile, mediaItem)?.toString()

            if (mediaStoreUriString == null && currentMediaStoreUrisMap[mediaItem.id] == null) {
                Log.w("LocalDataSource", "Failed to save ${mediaItem.itemName} to MediaStore and no prior URI existed. Skipping.")
                // downloadedFile.delete() // Consider deleting temp file if it's truly temporary and save failed
                continue
            }

            // 2. Generate thumbnail from the file
            // Pass mediaItem to help generateThumbnailBytesFromFile determine type if needed,
            // though it can also infer from file extension or content.
            val thumbnailBytes = when (mediaItem) {
                is GalleryImage -> generateImageThumbnailFromFile(downloadedFile)
                is GalleryVideo -> generateVideoThumbnailFromFile(downloadedFile)
            }

            if (thumbnailBytes == null) {
                Log.w("LocalDataSource", "Failed to generate thumbnail for ${mediaItem.itemName}. It might be stored without a thumbnail.")
                // Decide if you want to proceed without a thumbnail or skip
            }

            val videoDuration = if (mediaItem is GalleryVideo) {
                mediaItem.duration
            } else {
                null
            }

            entityList.add(
                GalleryMediaEntity(
                    id = mediaItem.id!!,
                    mediaType = mediaItem.mediaType,
                    familyId = mediaItem.familyId,
                    itemName = mediaItem.itemName,
                    lastUpdated = mediaItem.lastUpdated,
                    albumId = mediaItem.albumId,
                    dateCreated = mediaItem.dateCreated,
                    mediaUri = mediaStoreUriString,
                    thumbnail = thumbnailBytes,
                    videoDuration = videoDuration,
                ),
            )

            // 3. IMPORTANT: Delete the temporary downloaded file after processing
            // Ensure this file is always a temporary one that should be cleaned up.
            if (downloadedFile.exists()) {
                val deleted = downloadedFile.delete()
                Log.d("LocalDataSource", "Temporary file ${downloadedFile.name} deleted: $deleted")
            }
        }
        Log.i("LocalDataSource", "Finished processing ${entityList.size} items into entities for Room.")

        // --- Logic for determining Room updates and deletes (largely the same as your original) ---

        val itemsToUpdateOrInsert = entityList.filter { newEntity ->
            currentRoomItems.none { currentEntity ->
                newEntity.id == currentEntity.id &&
                    newEntity.mediaUri == currentEntity.mediaUri && // Key comparison points
                    newEntity.itemName == currentEntity.itemName &&
                    newEntity.lastUpdated == currentEntity.lastUpdated &&
                    (newEntity.thumbnail?.contentEquals(currentEntity.thumbnail ?: byteArrayOf()) ?: (currentEntity.thumbnail == null))
                // Add other relevant fields if needed for "sameness"
            }
        }

        val currentIdsInRoom = currentRoomItems.map { it.id }.toSet()
        val newIdsFromSource = entityList.map { it.id }.toSet()
        val idsToDeleteFromRoom = currentIdsInRoom - newIdsFromSource
        val itemsToDeleteFromMediaStoreAndRoom = currentRoomItems.filter { it.id in idsToDeleteFromRoom }

        // Delete from MediaStore for items that are no longer in the source list
        if (itemsToDeleteFromMediaStoreAndRoom.isNotEmpty()) {
            Log.i("LocalDataSource", "Deleting ${itemsToDeleteFromMediaStoreAndRoom.size} items from MediaStore.")
            itemsToDeleteFromMediaStoreAndRoom.forEach { itemToDelete ->
                itemToDelete.mediaUri?.let { uriString ->
                    try {
                        val deletedRows = resolver.delete(uriString.toUri(), null, null)
                        Log.d("LocalDataSource", "Attempted to delete from MediaStore URI $uriString. Rows affected: $deletedRows")
                    } catch (e: Exception) {
                        Log.e("LocalDataSource", "Error deleting from MediaStore URI $uriString: ${e.message}")
                    }
                }
            }
        }

        // Delete from Room for items no longer in the source list
        if (idsToDeleteFromRoom.isNotEmpty()) {
            galleryMediaDao.deleteItems(idsToDeleteFromRoom.toList()) // Ensure you have this DAO method
            Log.i("LocalDataSource", "Deleted ${idsToDeleteFromRoom.size} items from Room by ID.")
        }

        // Update Room: Upsert new/changed items
        if (itemsToUpdateOrInsert.isNotEmpty()) {
            galleryMediaDao.updateItems(itemsToUpdateOrInsert) // Assuming this is an upsert (inserts or updates)
            Log.i("LocalDataSource", "Upserted ${itemsToUpdateOrInsert.size} items in Room.")
        }

        Log.i("LocalDataSource", "updateGalleryMedia finished.")
    }

    // --- Unified function to save ANY media File (image or video) to MediaStore ---
    // TODO use internal filestorage instead of mediastore
    private fun saveMediaFileToGallery(
        context: Context,
        mediaFile: File,
        mediaItem: GalleryMedia, // Pass the whole GalleryMedia item for its metadata
    ): Uri? {
        val resolver = context.contentResolver
        val contentValues = ContentValues()
        val collectionUri: Uri

        val displayName = mediaItem.itemName
        // Attempt to get MIME type from file, fallback based on GalleryMedia type
        val mimeType =
            MimeTypeMap.getSingleton().getMimeTypeFromExtension(mediaFile.extension)
                ?: when (mediaItem) {
                    is GalleryImage -> "image/jpeg" // Default if extension unknown
                    is GalleryVideo -> "video/mp4" // Default if extension unknown
                }

        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
        mediaItem.dateCreated?.time?.let { contentValues.put(MediaStore.MediaColumns.DATE_TAKEN, it) }

        when (mediaItem) {
            is GalleryImage -> {
                collectionUri =
                    MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                contentValues.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/LifeTogether")
            }
            is GalleryVideo -> {
                collectionUri =
                    MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                contentValues.put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/LifeTogether")
                mediaItem.duration?.let { contentValues.put(MediaStore.Video.Media.DURATION, it) }
            }
        }

        contentValues.put(MediaStore.MediaColumns.IS_PENDING, 1)

        var mediaStoreUri: Uri? = null
        try {
            mediaStoreUri = resolver.insert(collectionUri, contentValues)
            mediaStoreUri?.let { uri ->
                resolver.openOutputStream(uri)?.use { outputStream ->
                    FileInputStream(mediaFile).use { inputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
                contentValues.clear()
                contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                resolver.update(uri, contentValues, null, null)

                // Optional: Explicitly update DATE_MODIFIED and DATE_TAKEN again if needed
                val updateValues = ContentValues()
                mediaItem.dateCreated?.time?.let {
                    updateValues.put(MediaStore.MediaColumns.DATE_TAKEN, it)
                    updateValues.put(MediaStore.MediaColumns.DATE_MODIFIED, it / 1000) // Seconds
                }
                if (updateValues.size() > 0) {
                    resolver.update(uri, updateValues, null, null)
                }
                Log.d("LocalDataSource", "Saved ${mediaItem.itemName} to MediaStore: $uri")
            } ?: run {
                Log.e("LocalDataSource", "MediaStore insert returned null URI for ${mediaItem.itemName}")
            }
        } catch (e: Exception) {
            Log.e("LocalDataSource", "Error saving ${mediaItem.itemName} to MediaStore: ${e.message}", e)
            mediaStoreUri?.let { resolver.delete(it, null, null) } // Clean up failed entry
            mediaStoreUri = null // Ensure it's null on failure
        }
        return mediaStoreUri
    }

    suspend fun deleteFamilyGalleryMedia(familyId: String) {
        val currentFamilyItems = galleryMediaDao.getItems(familyId).firstOrNull()

        if (currentFamilyItems != null) {
            galleryMediaDao.deleteItems(currentFamilyItems.map { it.id })

//            val resolver = context.contentResolver
//
//            // Delete images from MediaStore
//            currentFamilyItems.forEach { item ->
//                item.mediaUri?.let { resolver.delete(it.toUri(), null, null) }
//            }
        }
    }

    // -------------------------------------------------------------- ALBUMS
    suspend fun updateAlbums(
        items: List<Album>,
    ) {
        println("LocalDataSource updateAlbums(): Trying to add firestore data to Room")

        println("Album list: $items")
        val entityList = items.map { item ->
            AlbumEntity(
                id = item.id ?: "",
                familyId = item.familyId,
                itemName = item.itemName,
                lastUpdated = item.lastUpdated,
                count = item.count,
            )
        }

        // Fetch the current items from the Room database
        val currentItems = albumsDao.getItems(items[0].familyId).first()

        // Determine the items to be inserted or updated
        val itemsToUpdate = entityList.filter { newItem ->
            currentItems.none { currentItem -> newItem.id == currentItem.id && newItem == currentItem }
        }

        println("albumEntityList itemsToUpdate: ${itemsToUpdate.map { it.itemName }}")

        // Determine the items to be deleted
        val itemsToDelete = currentItems.filter { currentItem ->
            entityList.none { newItem -> newItem.id == currentItem.id }
        }

        // Update the Room database with the new or changed items
        albumsDao.updateItems(itemsToUpdate)

        // Delete the items that no longer exist in Firestore
        albumsDao.deleteItems(itemsToDelete.map { it.id })
    }

    suspend fun deleteFamilyAlbums(familyId: String) {
        val currentFamilyItems = albumsDao.getItems(familyId).firstOrNull()
        if (currentFamilyItems != null) {
            albumsDao.deleteItems(currentFamilyItems.map { it.id })
        }
    }

    // -------------------------------------------------------------- GROCERY LIST
    suspend fun updateTipTracker(items: List<TipItem>) {
        println("LocalDataSource updateTipTracker(): Trying to add firestore data to Room")

        println("TipItem list: $items")
        val tipEntityList = items.map { item ->
            TipEntity(
                id = item.id ?: "",
                familyId = item.familyId,
                itemName = item.itemName,
                lastUpdated = item.lastUpdated,
                amount = item.amount,
                currency = item.currency,
                date = item.date,
            )
        }
        println("tipEntityList list: $tipEntityList")

        // Fetch the current items from the Room database
        val currentItems = tipTrackerDao.getItems(items[0].familyId).first()

        // Determine the items to be inserted or updated
        val itemsToUpdate = tipEntityList.filter { newItem ->
            currentItems.none { currentItem -> newItem.id == currentItem.id && newItem == currentItem }
        }

        // Determine the items to be deleted
        val itemsToDelete = currentItems.filter { currentItem ->
            tipEntityList.none { newItem -> newItem.id == currentItem.id }
        }

        // Update the Room database with the new or changed items
        tipTrackerDao.updateItems(itemsToUpdate)

        // Delete the items that no longer exist in Firestore
        tipTrackerDao.deleteItems(itemsToDelete.map { it.id })
    }

    suspend fun deleteFamilyTipItems(familyId: String) {
        val currentFamilyItems = tipTrackerDao.getItems(familyId).firstOrNull()
        if (currentFamilyItems != null) {
            tipTrackerDao.deleteItems(currentFamilyItems.map { it.id })
        }
    }

    // -------------------------------------------------------------- IMAGES
    fun getImageByteArray(imageType: ImageType): Flow<ByteArray?> {
        println("LocalDataSource getImageByteArray imageType: $imageType")
        return when (imageType) {
            is ImageType.ProfileImage -> userInformationDao.getImageByteArray(imageType.uid)

            is ImageType.FamilyImage -> familyInformationDao.getImageByteArray(imageType.familyId)

            is ImageType.RecipeImage -> recipesDao.getImageByteArray(imageType.familyId, imageType.recipeId)

            is ImageType.GalleryMedia -> flowOf(null)
        }
    }
}
