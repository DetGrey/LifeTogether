package com.example.lifetogether.data.remote

import android.util.Log
import com.example.lifetogether.domain.listener.CategoriesListener
import com.example.lifetogether.domain.listener.FamilyInformationResultListener
import com.example.lifetogether.domain.listener.GrocerySuggestionsListener
import com.example.lifetogether.domain.listener.ListItemsResultListener
import com.example.lifetogether.domain.listener.StringResultListener
import com.example.lifetogether.domain.listener.AuthResultListener
import com.example.lifetogether.domain.listener.ResultListener
import com.example.lifetogether.domain.logic.GuideParser
import com.example.lifetogether.domain.model.Category
import com.example.lifetogether.domain.model.CompletableItem
import com.example.lifetogether.domain.model.Item
import com.example.lifetogether.domain.model.TipItem
import com.example.lifetogether.domain.model.UserInformation
import com.example.lifetogether.domain.model.family.FamilyInformation
import com.example.lifetogether.domain.model.family.FamilyMember
import com.example.lifetogether.domain.model.gallery.Album
import com.example.lifetogether.domain.model.gallery.GalleryImage
import com.example.lifetogether.domain.model.gallery.GalleryMedia
import com.example.lifetogether.domain.model.gallery.GalleryVideo
import com.example.lifetogether.domain.model.grocery.GroceryItem
import com.example.lifetogether.domain.model.grocery.GrocerySuggestion
import com.example.lifetogether.domain.model.guides.Guide
import com.example.lifetogether.domain.model.guides.GuideVisibility
import com.example.lifetogether.domain.model.recipe.Recipe
import com.example.lifetogether.domain.model.sealed.ImageType
import com.example.lifetogether.util.Constants
import com.google.firebase.Firebase
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.MetadataChanges
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.firestore
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.Date
import javax.inject.Inject

class FirestoreDataSource @Inject constructor() {
    companion object {
        private const val TAG = "FirestoreDataSource"
    }
    private val db = Firebase.firestore

    private fun Throwable.toFirestoreFailureMessage(
        operation: String,
        listName: String? = null,
        documentId: String? = null,
    ): String {
        val code = (this as? FirebaseFirestoreException)?.code?.name
        val location = buildString {
            if (!listName.isNullOrBlank()) append("list=$listName")
            if (!documentId.isNullOrBlank()) {
                if (isNotEmpty()) append(", ")
                append("id=$documentId")
            }
        }
        val context = if (location.isBlank()) "" else " ($location)"
        val details = message ?: "Unknown error"
        return if (code == null) {
            "$operation failed$context: $details"
        } else {
            "$operation failed$context [Firestore:$code]: $details"
        }
    }

    // ------------------------------------------------------------------------------- USERS
    fun userInformationSnapshotListener(uid: String) = callbackFlow {
        Log.d(TAG, "userInformationSnapshotListener init")
        val userInformationRef = db.collection(Constants.USER_TABLE).document(uid)
        val listenerRegistration = userInformationRef.addSnapshotListener { snapshot, e ->
            if (e != null) {
                trySend(AuthResultListener.Failure("Error: ${e.message}")).isSuccess
                return@addSnapshotListener
            }

            if (snapshot != null) {
                // Process the changes and update Room database
                val userInformation = snapshot.toObject(UserInformation::class.java)
                Log.d(TAG, "Snapshot of userInformation: loaded")
                if (userInformation != null) {
                    trySend(AuthResultListener.Success(userInformation)).isSuccess
                }
            }
        }
        // Await close tells the flow builder to suspend until the flow collector is cancelled or disposed.
        awaitClose { listenerRegistration.remove() }
    }

    suspend fun uploadUserInformation(userInformation: UserInformation): ResultListener {
        Log.d(TAG, "uploadUserInformation getting uploaded")
        try {
            if (userInformation.uid != null) {
                db.collection(Constants.USER_TABLE).document(userInformation.uid)
                    .set(userInformation).await()
                return ResultListener.Success
            } else {
                return ResultListener.Failure("Cannot upload without being logged in")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error", e)
            return ResultListener.Failure("Error: ${e.message}")
        }
    }

    suspend fun updateFamilyId(uid: String, familyId: String?): ResultListener {
        try {
            db.collection(Constants.USER_TABLE).document(uid).update("familyId", familyId).await()
            return ResultListener.Success
        } catch (e: Exception) {
            Log.e(TAG, "Error", e)
            return ResultListener.Failure("Error: ${e.message}")
        }
    }

    suspend fun changeName(
        uid: String,
        familyId: String?,
        newName: String,
    ): ResultListener {
        try {
            // Update the name in the user's document
            db.collection(Constants.USER_TABLE).document(uid).update("name", newName).await()

            // Also update the name in the family document
            if (familyId != null) {
                val familyDocRef = db.collection(Constants.FAMILIES_TABLE).document(familyId)
                val familySnapshot = familyDocRef.get().await()

                // Check if family document exists
                if (familySnapshot.exists()) {
                    // Fetch current members list
                    @Suppress("UNCHECKED_CAST")
                    val members =
                        familySnapshot.get("members") as? List<Map<String, String>> ?: emptyList()

                    // Update the name in the family document for the matching uid
                    val updatedMembers = members.map { member ->
                        if (member["uid"] == uid) {
                            member.toMutableMap().apply { this["name"] = newName }
                        } else {
                            member
                        }
                    }

                    // Save the updated members list to the family document
                    familyDocRef.update("members", updatedMembers).await()
                }
            }

            return ResultListener.Success
        } catch (e: Exception) {
            Log.e(TAG, "Error", e)
            return ResultListener.Failure("Error: ${e.message}")
        }
    }

    // ------------------------------------------------------------------------------- FAMILY
    fun familyInformationSnapshotListener(familyId: String) = callbackFlow {
        Log.d(TAG, "Firestore familyInformationSnapshotListener init")
        val familyInformationRef = db.collection(Constants.FAMILIES_TABLE).document(familyId)
        val listenerRegistration = familyInformationRef.addSnapshotListener { snapshot, e ->
            if (e != null) {
                // Handle error
                trySend(AuthResultListener.Failure("Error: ${e.message}")).isSuccess
                return@addSnapshotListener
            }

            if (snapshot != null && snapshot.exists()) {
                // Fetch members data (uid and name)
                @Suppress("UNCHECKED_CAST")
                val membersData =
                    snapshot.get("members") as? List<Map<String, String>> ?: emptyList()

                // Map members data into FamilyMember objects
                val membersList = membersData.map { member ->
                    FamilyMember(
                        uid = member["uid"],
                        name = member["name"],
                    )
                }

                val familyInformation = FamilyInformation(
                    familyId = familyId,
                    members = membersList,
                    imageUrl = snapshot.getString("imageUrl"),
                )

                Log.d(TAG, "Snapshot of familyInformation: loaded")
                trySend(FamilyInformationResultListener.Success(familyInformation)).isSuccess
            }
        }
        // Await close tells the flow builder to suspend until the flow collector is cancelled or disposed.
        awaitClose { listenerRegistration.remove() }
    }

    suspend fun joinFamily(
        familyId: String,
        uid: String,
        name: String,
    ): ResultListener {
        Log.d(TAG, "joinFamily")
        try {
            val documentReference =
                db.collection(Constants.FAMILIES_TABLE).document(familyId).get().await()

            @Suppress("UNCHECKED_CAST")
            val membersData = documentReference.data?.get("members") as? List<Map<String, String>>

            val updatedMembers = membersData?.toMutableList() ?: mutableListOf()

            // Add the new member with uid and a default null name
            updatedMembers.add(mapOf("uid" to uid, "name" to name))

            db.collection(Constants.FAMILIES_TABLE).document(familyId)
                .update("members", updatedMembers)
                .await()

            return ResultListener.Success
        } catch (e: Exception) {
            Log.e(TAG, "Error", e)
            return ResultListener.Failure("Error: ${e.message}")
        }
    }

    suspend fun createNewFamily(
        uid: String,
        name: String,
    ): StringResultListener {
        Log.d(TAG, "createNewFamily getting uploaded")
        val map = mapOf(
            "members" to listOf(mapOf("uid" to uid, "name" to name)),
        )

        try {
            val documentReference = db.collection(Constants.FAMILIES_TABLE).add(map).await()
            return StringResultListener.Success(documentReference.id)
        } catch (e: Exception) {
            Log.e(TAG, "Error", e)
            return StringResultListener.Failure("Error: ${e.message}")
        }
    }

    suspend fun leaveFamily(
        familyId: String,
        uid: String,
    ): ResultListener {
        Log.d(TAG, "leaveFamily")
        try {
            val documentReference =
                db.collection(Constants.FAMILIES_TABLE).document(familyId).get().await()

            @Suppress("UNCHECKED_CAST")
            val members = documentReference.data?.get("members") as? List<Map<String, String>>

            // Remove the member from the list by matching the uid
            val updatedMembers =
                members?.filterNot { it["uid"] == uid }?.toMutableList() ?: mutableListOf()

            db.collection(Constants.FAMILIES_TABLE).document(familyId)
                .update("members", updatedMembers)
                .await()

            return ResultListener.Success
        } catch (e: Exception) {
            Log.e(TAG, "Error", e)
            return ResultListener.Failure("Error: ${e.message}")
        }
    }

    suspend fun deleteFamily(
        familyId: String,
    ): ResultListener {
        Log.d(TAG, "deleteFamily")
        try {
            db.collection(Constants.FAMILIES_TABLE).document(familyId).delete().await()

            val usersRef =
                db.collection(Constants.USER_TABLE).whereEqualTo("familyId", familyId).get().await()

            // Iterate over each document in the result set
            val failures = mutableListOf<String>()

            for (userDocument in usersRef.documents) {
                val uid = userDocument.id
                val result = updateFamilyId(uid, null)
                if (result is ResultListener.Failure) {
                    failures.add(result.message)
                }
            }

            if (failures.isNotEmpty()) {
                return ResultListener.Failure("Could not remove familyId from all users: $failures")
            }

            return ResultListener.Success
        } catch (e: Exception) {
            Log.e(TAG, "Error", e)
            return ResultListener.Failure("Error: ${e.message}")
        }
    }

    // ------------------------------------------------------------------------------- GROCERY LIST
    fun grocerySnapshotListener(familyId: String) = callbackFlow {
        Log.d(TAG, "Firestore grocerySnapshotListener init")
        val groceryItemsRef =
            db.collection(Constants.GROCERY_TABLE).whereEqualTo("familyId", familyId)
        val listenerRegistration = groceryItemsRef.addSnapshotListener { snapshot, e ->
            if (e != null) {
                // Handle error
                trySend(ListItemsResultListener.Failure("Error: ${e.message}")).isSuccess
                return@addSnapshotListener
            }

            if (snapshot != null) {
                // Process the changes and update Room database
                val items = snapshot.documents.mapNotNull { document ->
                    runCatching {
                        document.toObject(GroceryItem::class.java)?.copy(id = document.id)
                    }.onFailure { parseError ->
                        Log.e(TAG, "Failed parsing grocery item ${document.id}", parseError)
                    }.getOrNull()
                }
                Log.d(TAG, "Snapshot items to GroceryItem: loaded")
                trySend(ListItemsResultListener.Success(items)).isSuccess
            } else {
                trySend(ListItemsResultListener.Failure("Error: Empty snapshot")).isSuccess
            }
        }
        // Await close tells the flow builder to suspend until the flow collector is cancelled or disposed.
        awaitClose { listenerRegistration.remove() }
    }

    // ------------------------------------------------------------------------------- RECIPES
    fun recipeSnapshotListener(familyId: String) = callbackFlow {
        Log.d(TAG, "Firestore recipeSnapshotListener init")
        val recipeItemsRef =
            db.collection(Constants.RECIPES_TABLE).whereEqualTo("familyId", familyId)
        val listenerRegistration = recipeItemsRef.addSnapshotListener { snapshot, e ->
            if (e != null) {
                // Handle error
                Log.e(TAG, "Firestore recipeSnapshotListener", e)
                trySend(ListItemsResultListener.Failure("Error: ${e.message}")).isSuccess
                return@addSnapshotListener
            }

            if (snapshot != null) {
                // Process the changes and update Room database
                val items = snapshot.documents.mapNotNull { document ->
                    runCatching {
                        document.toObject(Recipe::class.java)?.copy(id = document.id)
                    }.onFailure { parseError ->
                        Log.e(TAG, "Failed parsing recipe ${document.id}", parseError)
                    }.getOrNull()
                }
                Log.d(TAG, "Snapshot items to Recipe: loaded")
                trySend(ListItemsResultListener.Success(items)).isSuccess
            } else {
                trySend(ListItemsResultListener.Failure("Error: Empty snapshot")).isSuccess
            }
        }
        // Await close tells the flow builder to suspend until the flow collector is cancelled or disposed.
        awaitClose { listenerRegistration.remove() }
    }


    // ------------------------------------------------------------------------------- GUIDES
    fun familySharedGuidesSnapshotListener(familyId: String) = callbackFlow {
        Log.d(TAG, "Firestore familySharedGuidesSnapshotListener init")
        val guidesRef = db.collection(Constants.GUIDES_TABLE)
            .whereEqualTo("familyId", familyId)

        val listenerRegistration = guidesRef.addSnapshotListener(MetadataChanges.INCLUDE) { snapshot, e ->
            if (e != null) {
                Log.e(TAG, "familySharedGuidesSnapshotListener error", e)
                trySend(ListItemsResultListener.Failure("Error: ${e.message}")).isSuccess
                return@addSnapshotListener
            }

            if (snapshot == null) {
                trySend(ListItemsResultListener.Failure("Error: Empty snapshot")).isSuccess
                return@addSnapshotListener
            }
            Log.d(
                TAG,
                "familySharedGuidesSnapshotListener snapshot familyId=$familyId size=${snapshot.size()} pendingWrites=${snapshot.metadata.hasPendingWrites()} fromCache=${snapshot.metadata.isFromCache}",
            )

            val parsedGuides = snapshot.documents.mapNotNull { document ->
                val data = document.data ?: return@mapNotNull null
                runCatching {
                    GuideParser.parseFirestoreGuide(
                        documentId = document.id,
                        data = data,
                    )
                }.onFailure { parseError ->
                    Log.e(TAG, "Failed parsing family guide ${document.id}", parseError)
                }.getOrNull()
            }
            val guides = parsedGuides.filter { it.visibility == GuideVisibility.FAMILY }
            Log.d(
                TAG,
                "familySharedGuidesSnapshotListener parsedCount=${parsedGuides.size} filteredFamilyCount=${guides.size} ids=${guides.mapNotNull { it.id }.take(5)}",
            )

            trySend(ListItemsResultListener.Success(guides)).isSuccess
        }

        awaitClose { listenerRegistration.remove() }
    }

    fun privateGuidesSnapshotListener(
        familyId: String,
        uid: String,
    ) = callbackFlow {
        Log.d(TAG, "Firestore privateGuidesSnapshotListener init")
        val guidesRef = db.collection(Constants.GUIDES_TABLE)
            .whereEqualTo("familyId", familyId)
            .whereEqualTo("ownerUid", uid)

        val listenerRegistration = guidesRef.addSnapshotListener(MetadataChanges.INCLUDE) { snapshot, e ->
            if (e != null) {
                Log.e(TAG, "privateGuidesSnapshotListener error", e)
                trySend(ListItemsResultListener.Failure("Error: ${e.message}")).isSuccess
                return@addSnapshotListener
            }

            if (snapshot == null) {
                trySend(ListItemsResultListener.Failure("Error: Empty snapshot")).isSuccess
                return@addSnapshotListener
            }
            Log.d(
                TAG,
                "privateGuidesSnapshotListener snapshot familyId=$familyId uid=$uid size=${snapshot.size()} pendingWrites=${snapshot.metadata.hasPendingWrites()} fromCache=${snapshot.metadata.isFromCache}",
            )

            val parsedGuides = snapshot.documents.mapNotNull { document ->
                val data = document.data ?: return@mapNotNull null
                runCatching {
                    GuideParser.parseFirestoreGuide(
                        documentId = document.id,
                        data = data,
                    )
                }.onFailure { parseError ->
                    Log.e(TAG, "Failed parsing private guide ${document.id}", parseError)
                }.getOrNull()
            }
            val guides = parsedGuides.filter { it.visibility == GuideVisibility.PRIVATE }
            Log.d(
                TAG,
                "privateGuidesSnapshotListener parsedCount=${parsedGuides.size} filteredPrivateCount=${guides.size} ids=${guides.mapNotNull { it.id }.take(5)}",
            )

            trySend(ListItemsResultListener.Success(guides)).isSuccess
        }

        awaitClose { listenerRegistration.remove() }
    }

    // ------------------------------------------------------------------------------- ALBUMS
    fun albumsSnapshotListener(familyId: String) = callbackFlow {
        Log.d(TAG, "Firestore albumSnapshotListener init")
        val itemsRef =
            db.collection(Constants.ALBUMS_TABLE).whereEqualTo("familyId", familyId)
        val listenerRegistration = itemsRef.addSnapshotListener { snapshot, e ->
            if (e != null) {
                // Handle error
                Log.e(TAG, "Firestore albumSnapshotListener", e)
                trySend(ListItemsResultListener.Failure("Error: ${e.message}")).isSuccess
                return@addSnapshotListener
            }

            if (snapshot != null) {
                // Process the changes and update Room database
                val items = snapshot.documents.mapNotNull { document ->
                    runCatching {
                        document.toObject(Album::class.java)?.copy(id = document.id)
                    }.onFailure { parseError ->
                        Log.e(TAG, "Failed parsing album ${document.id}", parseError)
                    }.getOrNull()
                }
                Log.d(TAG, "Snapshot items to Album: loaded")
                trySend(ListItemsResultListener.Success(items)).isSuccess
            } else {
                trySend(ListItemsResultListener.Failure("Error: Empty snapshot")).isSuccess
            }
        }
        // Await close tells the flow builder to suspend until the flow collector is cancelled or disposed.
        awaitClose { listenerRegistration.remove() }
    }

    suspend fun updateAlbumCount(albumId: String, increment: Int): ResultListener {
        try {
            Log.d(TAG, "updateAlbumCount with increment: $increment")
            val albumDocRef = db.collection(Constants.ALBUMS_TABLE).document(albumId)
            albumDocRef.update("count", FieldValue.increment(increment.toDouble())).await()
            Log.d(TAG, "updateAlbumCount successful")
            return ResultListener.Success
        } catch (e: Exception) {
            Log.e(TAG, "Error", e)
            return ResultListener.Failure("Error: ${e.message}")
        }
    }

    // ------------------------------------------------------------------------------- GALLERY MEDIA
    fun galleryMediaSnapshotListener(familyId: String) = callbackFlow {
        Log.d(TAG, "Firestore galleryMediaSnapshotListener init")
        val itemsRef = db.collection(Constants.GALLERY_MEDIA_TABLE)
            .whereEqualTo("familyId", familyId)
        val listenerRegistration = itemsRef.addSnapshotListener { snapshot, e ->
            if (e != null) {
                // Handle error
                Log.e(TAG, "Firestore galleryMediaSnapshotListener", e)
                trySend(ListItemsResultListener.Failure("Error: ${e.message}")).isSuccess
                return@addSnapshotListener
            }

//            if (snapshot != null) {
//                // Process the changes and update Room database
//                val items = snapshot.toObjects(GalleryImage::class.java)
//                Log.d(LOG_TAG, "Snapshot items to GalleryImage: loaded")
//                trySend(ListItemsResultListener.Success(items)).isSuccess
//            } else {
//                trySend(ListItemsResultListener.Failure("Error: Empty snapshot")).isSuccess
//            }
            if (snapshot != null) {
                val mediaItems = mutableListOf<GalleryMedia>()
                for (document in snapshot.documents) {
                    try {
                        val mediaType = document.getString("mediaType")
                        val item: GalleryMedia? = when (mediaType?.lowercase()) {
                            "image" -> document.toObject(GalleryImage::class.java) as GalleryMedia
                            "video" -> document.toObject(GalleryVideo::class.java) as GalleryMedia
                            else -> {
                                Log.w("FirestoreDS", "Unknown mediaType '$mediaType' for document ${document.id}")
                                null
                            }
                        }
                        item?.let { mediaItems.add(it) }
                    } catch (parseEx: Exception) {
                        Log.e("FirestoreDS", "Error parsing document ${document.id} to GalleryMedia: ${parseEx.message}", parseEx)
                    }
                }
                val isFromCache = snapshot.metadata.isFromCache
                Log.d("FirestoreDS", "Snapshot items mapped to GalleryMedia: $mediaItems (isFromCache: $isFromCache)")
                trySend(ListItemsResultListener.Success(mediaItems, isFromCache)).isSuccess
            } else {
                trySend(ListItemsResultListener.Failure("Error: Empty snapshot")).isSuccess
            }
        }
        // Await close tells the flow builder to suspend until the flow collector is cancelled or disposed.
        awaitClose { listenerRegistration.remove() }
    }
    suspend fun moveMediaToAlbum(
        mediaIdList: Set<String>,
        newAlbumId: String,
        oldAlbumId: String,
    ): ResultListener {
        try {
            Log.d(TAG, "moveMediaToAlbum")
            val batch = db.batch()

            mediaIdList.forEach { id ->
                val documentRef = db.collection(Constants.GALLERY_MEDIA_TABLE).document(id)
                batch.update(documentRef, "albumId", newAlbumId)
            }
            val newAlbumRef = db.collection(Constants.ALBUMS_TABLE).document(newAlbumId)
            batch.update(newAlbumRef, "count", FieldValue.increment(mediaIdList.size.toDouble()))
            val oldAlbumRef = db.collection(Constants.ALBUMS_TABLE).document(oldAlbumId)
            batch.update(oldAlbumRef, "count", FieldValue.increment(-mediaIdList.size.toDouble()))

            batch.commit().await()
            return ResultListener.Success
        } catch (e: Exception) {
            println("Error: ${e.message}")
            return ResultListener.Failure("Error: ${e.message}")
        }
    }

    // ------------------------------------------------------------------------------- ITEMS
    suspend fun saveItem(
        item: Item,
        listName: String,
    ): StringResultListener {
        val startedAt = System.currentTimeMillis()
        try {
            val uploadItem = if (item is Guide) GuideParser.guideToFirestoreMap(item) else item
            val documentReference = db.collection(listName).add(uploadItem).await()
            Log.d(
                TAG,
                "saveItem success list=$listName id=${documentReference.id} durationMs=${System.currentTimeMillis() - startedAt}",
            )
            return StringResultListener.Success(documentReference.id)
        } catch (e: Exception) {
            val message = e.toFirestoreFailureMessage(operation = "saveItem", listName = listName)
            Log.e(TAG, message, e)
            return StringResultListener.Failure(message)
        }
    }

    suspend fun updateItem(
        item: Item,
        listName: String,
    ): ResultListener {
        val startedAt = System.currentTimeMillis()
        Log.d(TAG, "updateItem start list=$listName id=${item.id} type=${item::class.simpleName}")
        try {
            if (!item.id.isNullOrBlank()) {
                val uploadItem = if (item is Guide) GuideParser.guideToFirestoreMap(item) else item

                db.collection(listName)
                    .document(item.id!!)
                    .set(uploadItem, SetOptions.merge())
                    .await()

                Log.d(
                    TAG,
                    "updateItem success list=$listName id=${item.id} durationMs=${System.currentTimeMillis() - startedAt}",
                )
                return ResultListener.Success
            } else {
                return ResultListener.Failure("updateItem failed (list=$listName): missing document id")
            }
        } catch (e: Exception) {
            val message = e.toFirestoreFailureMessage(
                operation = "updateItem",
                listName = listName,
                documentId = item.id,
            )
            Log.e(TAG, message, e)
            return ResultListener.Failure(message)
        }
    }

    suspend fun toggleCompletableItemCompletion(
        item: CompletableItem,
        listName: String,
    ): ResultListener {
        val startedAt = System.currentTimeMillis()
        try {
            if (!item.id.isNullOrBlank()) {
                db.collection(listName).document(item.id!!).update(
                    mapOf(
                        "completed" to item.completed,
                        "lastUpdated" to Date(System.currentTimeMillis()), // Set to current time
                    ),
                ).await()
                Log.d(
                    TAG,
                    "toggleCompletableItemCompletion success list=$listName id=${item.id} completed=${item.completed} durationMs=${System.currentTimeMillis() - startedAt}",
                )
                return ResultListener.Success
            } else {
                return ResultListener.Failure("toggleCompletableItemCompletion failed (list=$listName): missing document id")
            }
        } catch (e: Exception) {
            val message = e.toFirestoreFailureMessage(
                operation = "toggleCompletableItemCompletion",
                listName = listName,
                documentId = item.id,
            )
            Log.e(TAG, message, e)
            return ResultListener.Failure(message)
        }
    }

    suspend fun deleteItem(
        itemId: String,
        listName: String,
    ): ResultListener {
        Log.d(TAG, "deleteItem")
        try {
            db.collection(listName).document(itemId).delete().await()
            return ResultListener.Success
        } catch (e: Exception) {
            Log.e(TAG, "Error", e)
            return ResultListener.Failure("Error: ${e.message}")
        }
    }

    suspend fun deleteItems(
        listName: String,
        idsList: List<String>,
    ): ResultListener {
        Log.d(TAG, "deleteItems")
        try {
            val batch = db.batch()

            idsList.forEach { id ->
                Log.d(TAG, "Processing document")
                val documentRef = db.collection(listName).document(id)
                batch.delete(documentRef)
            }

            batch.commit().await()
            return ResultListener.Success
        } catch (e: Exception) {
            Log.e(TAG, "Error", e)
            return ResultListener.Failure("Error: ${e.message}")
        }
    }

    // ------------------------------------------------------------------------------- CATEGORIES
    fun categoriesSnapshotListener() = callbackFlow {
        Log.d(TAG, "Firestore categoriesSnapshotListener init")
        val categoryItemsRef = db.collection(Constants.CATEGORY_TABLE)
        val listenerRegistration = categoryItemsRef.addSnapshotListener { snapshot, e ->
            if (e != null) {
                // Handle error
                trySend(CategoriesListener.Failure("Error: ${e.message}")).isSuccess
                return@addSnapshotListener
            }

            if (snapshot != null) {
                // Process the changes and update Room database
                val categoryItems = snapshot.documents.mapNotNull { document ->
                    document.toObject(Category::class.java)
                }

                Log.d(TAG, "Snapshot items to CategoryItems: loaded")
                trySend(CategoriesListener.Success(categoryItems)).isSuccess
            }
        }
        // Await close tells the flow builder to suspend until the flow collector is cancelled or disposed.
        awaitClose { listenerRegistration.remove() }
    }

    suspend fun addCategory(
        category: Category,
    ): ResultListener {
        try {
            db.collection(Constants.CATEGORY_TABLE).add(category).await()
            return ResultListener.Success
        } catch (e: Exception) {
            Log.e(TAG, "Error", e)
            return ResultListener.Failure("Error: ${e.message}")
        }
    }

    suspend fun deleteCategory(
        category: Category,
    ): ResultListener {
        Log.d(TAG, "deleteCategory")
        try {
            // Query the collection to find the document with the matching 'name' field
            val querySnapshot = db.collection(Constants.CATEGORY_TABLE)
                .whereEqualTo("name", category.name)
                .get()
                .await()
            // Check if any documents were found
            if (querySnapshot.documents.isNotEmpty()) {
                // Assuming 'name' is unique, delete the first matching document
                val documentRef = querySnapshot.documents[0].reference
                documentRef.delete().await()
            }
            return ResultListener.Success
        } catch (e: Exception) {
            Log.e(TAG, "Error", e)
            return ResultListener.Failure("Error: ${e.message}")
        }
    }

    // ------------------------------------------------------------------------------- GROCERY SUGGESTIONS
    fun grocerySuggestionsSnapshotListener() = callbackFlow {
        Log.d(TAG, "Firestore grocerySuggestionsSnapshotListener init")
        val grocerySuggestionsItemsRef = db.collection(Constants.GROCERY_SUGGESTIONS_TABLE)
        val listenerRegistration = grocerySuggestionsItemsRef.addSnapshotListener { snapshot, e ->
            if (e != null) {
                // Handle error
                trySend(GrocerySuggestionsListener.Failure("Error: ${e.message}")).isSuccess
                return@addSnapshotListener
            }

            if (snapshot != null) {
                // Process the changes and update Room database
                val grocerySuggestions = snapshot.documents.mapNotNull { document ->
                    runCatching {
                        document.toObject(GrocerySuggestion::class.java)?.copy(id = document.id)
                    }.onFailure { parseError ->
                        Log.e(TAG, "Failed parsing grocery suggestion ${document.id}", parseError)
                    }.getOrNull()
                }

                Log.d(TAG, "Snapshot items to GrocerySuggestions: loaded")
                trySend(GrocerySuggestionsListener.Success(grocerySuggestions)).isSuccess
            }
        }
        // Await close tells the flow builder to suspend until the flow collector is cancelled or disposed.
        awaitClose { listenerRegistration.remove() }
    }

    suspend fun deleteGrocerySuggestion(
        grocerySuggestion: GrocerySuggestion,
    ): ResultListener {
        Log.d(TAG, "deleteGrocerySuggestion")
        try {
            // Query the collection to find the document with the matching 'name' field
            if (grocerySuggestion.id is String) {
                db.collection(Constants.GROCERY_SUGGESTIONS_TABLE).document(grocerySuggestion.id)
                    .delete().await()
                return ResultListener.Success
            } else {
                return ResultListener.Failure("Problems with grocery suggestion id")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error", e)
            return ResultListener.Failure("Error: ${e.message}")
        }
    }

    suspend fun addGrocerySuggestion(
        grocerySuggestion: GrocerySuggestion,
    ): ResultListener {
        try {
            db.collection(Constants.GROCERY_SUGGESTIONS_TABLE).add(grocerySuggestion).await()
            return ResultListener.Success
        } catch (e: Exception) {
            Log.e(TAG, "Error", e)
            return ResultListener.Failure("Error: ${e.message}")
        }
    }

    // ------------------------------------------------------------------------------- IMAGES
    suspend fun getImageUrl(
        imageType: ImageType,
    ): StringResultListener? {
        Log.d(TAG, "getImageUrl")
        try {
            when (imageType) {
                is ImageType.ProfileImage -> {
                    val documentReference =
                        db.collection(Constants.USER_TABLE).document(imageType.uid).get().await()
                    return documentReference.getString("imageUrl")
                        ?.let { StringResultListener.Success(it) }
                }

                is ImageType.FamilyImage -> {
                    val documentReference =
                        db.collection(Constants.FAMILIES_TABLE).document(imageType.familyId).get().await()
                    return documentReference.getString("imageUrl")
                        ?.let { StringResultListener.Success(it) }
                }

                is ImageType.RecipeImage -> {
                    val documentReference =
                        db.collection(Constants.RECIPES_TABLE).document(imageType.recipeId).get().await()
                    return documentReference.getString("imageUrl")
                        ?.let { StringResultListener.Success(it) }
                }

                is ImageType.GalleryMedia -> return StringResultListener.Failure("Image type GalleryImage is not connected to one specific document")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error", e)
            return StringResultListener.Failure("Error: ${e.message}")
        }
    }

    suspend fun saveImageDownloadUrl(
        url: String,
        imageType: ImageType,
    ): ResultListener {
        try {
            Log.d(TAG, "saveImageDownloadUrl")


            val photo = mapOf(
                "imageUrl" to url,
            )

            when (imageType) {
                is ImageType.ProfileImage -> {
                    db.collection(Constants.USER_TABLE).document(imageType.uid).update(photo).await()
                }

                is ImageType.FamilyImage -> {
                    db.collection(Constants.FAMILIES_TABLE).document(imageType.familyId).update(photo).await()
                }

                is ImageType.RecipeImage -> {
                    db.collection(Constants.RECIPES_TABLE).document(imageType.recipeId).update(photo).await()
                }

                else -> {
                    return ResultListener.Failure("Image type is not connected to one specific document")
                }
            }

            return ResultListener.Success
        } catch (e: Exception) {
            Log.e(TAG, "Error", e)
            return ResultListener.Failure("Error: ${e.message}")
        }
    }

    suspend fun saveGalleryMediaMetaData(
        galleryMedia: List<GalleryMedia>,
    ): ResultListener {
        try {
            Log.d(TAG, "saveGalleryMediaMetaData")

            val batch = db.batch() // Create a batched write
            val collectionRef = db.collection(Constants.GALLERY_MEDIA_TABLE)

            galleryMedia.forEach { mediaItem ->
                val docRef = collectionRef.document() // Generate a new document reference
                batch.set(docRef, mediaItem) // Add set operation to batch
            }

            batch.commit().await() // Commit the batch operation

            return ResultListener.Success
        } catch (e: Exception) {
            Log.e(TAG, "Error", e)
            return ResultListener.Failure("Error: ${e.message}")
        }
    }

    // ------------------------------------------------------------------------------- TIP TRACKER
    fun tipTrackerSnapshotListener(familyId: String) = callbackFlow {
        Log.d(TAG, "Firestore tipTrackerSnapshotListener init")
        val tipTrackerItemsRef =
            db.collection(Constants.TIP_TRACKER_TABLE).whereEqualTo("familyId", familyId)
        val listenerRegistration = tipTrackerItemsRef.addSnapshotListener { snapshot, e ->
            if (e != null) {
                // Handle error
                trySend(ListItemsResultListener.Failure("Error: ${e.message}")).isSuccess
                return@addSnapshotListener
            }

            if (snapshot != null) {
                // Process the changes and update Room database
                val tips = snapshot.documents.mapNotNull { document ->
                    runCatching {
                        document.toObject(TipItem::class.java)?.copy(id = document.id)
                    }.onFailure { parseError ->
                        Log.e(TAG, "Failed parsing tip item ${document.id}", parseError)
                    }.getOrNull()
                }

                Log.d(TAG, "Snapshot items to TipItems: loaded")
                trySend(ListItemsResultListener.Success(tips)).isSuccess
            } else {
                trySend(ListItemsResultListener.Failure("Error: Empty snapshot")).isSuccess
            }
        }
        // Await close tells the flow builder to suspend until the flow collector is cancelled or disposed.
        awaitClose { listenerRegistration.remove() }
    }

    // ------------------------------------------------------------------------------- DEVICE TOKEN MANAGEMENT
    // Store the FCM token along with familyId
    suspend fun storeFcmToken(uid: String, familyId: String) {
        val fcmToken = try {
            FirebaseMessaging.getInstance().token.await()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch FCM token", e)
            return
        }

        if (fcmToken.isBlank()) {
            Log.w(TAG, "FCM token is blank")
            return
        }

        try {
            val familyDocRef = db.collection(Constants.FAMILIES_TABLE).document(familyId)
            val familyDocSnapshot = familyDocRef.get().await()

            if (!familyDocSnapshot.exists()) {
                Log.w(TAG, "Family document not found")
                return
            }

            @Suppress("UNCHECKED_CAST")
            val members = familyDocSnapshot.get("members") as? List<Map<String, Any>> ?: emptyList()

            val updatedMembers = members.map { member ->
                if (member["uid"] == uid) {
                    val currentToken = member["fcmToken"] as? String
                    if (currentToken != fcmToken) {
                        member.toMutableMap().apply { put("fcmToken", fcmToken) }
                    } else {
                        member
                    }
                } else {
                    member
                }
            }

            val memberChanged = updatedMembers != members
            if (memberChanged) {
                familyDocRef.update("members", updatedMembers).await()
                Log.d(TAG, "FCM token updated successfully")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating FCM token", e)
        }
    }

    suspend fun removeDeviceToken(uid: String, familyId: String): ResultListener {
        return try {
            val familyDocRef = db.collection(Constants.FAMILIES_TABLE).document(familyId)
            val document = familyDocRef.get().await()

            if (!document.exists()) {
                Log.w(TAG, "Family document not found")
                return ResultListener.Failure("Family document not found")
            }

            @Suppress("UNCHECKED_CAST")
            val members = document.get("members") as? List<Map<String, Any>> ?: emptyList()

            val updatedMembers = members.map { member ->
                if (member["uid"] == uid) {
                    member.toMutableMap().apply { remove("fcmToken") }
                } else {
                    member
                }
            }

            familyDocRef.update("members", updatedMembers).await()
            Log.d(TAG, "FCM token removed successfully")
            ResultListener.Success
        } catch (e: Exception) {
            Log.e(TAG, "Error removing FCM token", e)
            ResultListener.Failure("Error: ${e.message}")
        }
    }

    suspend fun getFcmTokensFromFamily(familyId: String): List<String>? {
        // Get the family document to retrieve the fcmTokens
        val familyDocRef = db.collection(Constants.FAMILIES_TABLE).document(familyId).get().await()

        val document = familyDocRef.data
        @Suppress("UNCHECKED_CAST")
        val members = document?.get("members") as? List<Map<String, Any>> ?: emptyList()

        // Extract fcmTokens for all members
        val tokens = members.mapNotNull { it["fcmToken"] as? String }

        if (tokens.isNotEmpty()) {
            // Send notification to all tokens
            return tokens
        }
        return null
    }
}
