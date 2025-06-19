package com.example.lifetogether.data.remote

import android.util.Log
import com.example.lifetogether.domain.callback.AuthResultListener
import com.example.lifetogether.domain.callback.CategoriesListener
import com.example.lifetogether.domain.callback.FamilyInformationResultListener
import com.example.lifetogether.domain.callback.GrocerySuggestionsListener
import com.example.lifetogether.domain.callback.ListItemsResultListener
import com.example.lifetogether.domain.callback.ResultListener
import com.example.lifetogether.domain.callback.StringResultListener
import com.example.lifetogether.domain.logic.itemToMap
import com.example.lifetogether.domain.model.Category
import com.example.lifetogether.domain.model.CompletableItem
import com.example.lifetogether.domain.model.Item
import com.example.lifetogether.domain.model.TipItem
import com.example.lifetogether.domain.model.UserInformation
import com.example.lifetogether.domain.model.family.FamilyInformation
import com.example.lifetogether.domain.model.family.FamilyMember
import com.example.lifetogether.domain.model.gallery.Album
import com.example.lifetogether.domain.model.gallery.GalleryImage
import com.example.lifetogether.domain.model.grocery.GroceryItem
import com.example.lifetogether.domain.model.grocery.GrocerySuggestion
import com.example.lifetogether.domain.model.recipe.Recipe
import com.example.lifetogether.domain.model.sealed.ImageType
import com.example.lifetogether.util.Constants
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.Date
import javax.inject.Inject

class FirestoreDataSource@Inject constructor() {
    private val db = Firebase.firestore

    // ------------------------------------------------------------------------------- USERS
    fun userInformationSnapshotListener(uid: String) = callbackFlow {
        println("Firestore userInformationSnapshotListener init")
        val userInformationRef = db.collection(Constants.USER_TABLE).document(uid)
        val listenerRegistration = userInformationRef.addSnapshotListener { snapshot, e ->
            if (e != null) {
                // Handle error
                trySend(AuthResultListener.Failure("Error: ${e.message}")).isSuccess
                return@addSnapshotListener
            }

            if (snapshot != null) {
                // Process the changes and update Room database
                val userInformation = snapshot.toObject(UserInformation::class.java)
                println("Snapshot of userInformation: $userInformation")
//                if (userInformation != null) {
//                    userInformation.uid?.let { uid ->
//                        userInformation.familyId?.let { familyId ->
//                            storeDeviceToken(uid, familyId)
//                        }
//                    }
//                }
                if (userInformation != null) {
                    trySend(AuthResultListener.Success(userInformation)).isSuccess
                }
            }
        }
        // Await close tells the flow builder to suspend until the flow collector is cancelled or disposed.
        awaitClose { listenerRegistration.remove() }
    }

    suspend fun uploadUserInformation(userInformation: UserInformation): ResultListener {
        println("FirestoreDataSource uploadUserInformation getting uploaded")
        try {
            if (userInformation.uid != null) {
                db.collection(Constants.USER_TABLE).document(userInformation.uid)
                    .set(userInformation).await()
                return ResultListener.Success
            } else {
                return ResultListener.Failure("Cannot upload without being logged in")
            }
        } catch (e: Exception) {
            println("Error: ${e.message}")
            return ResultListener.Failure("Error: ${e.message}")
        }
    }

    suspend fun updateFamilyId(uid: String, familyId: String?): ResultListener {
        try {
            db.collection(Constants.USER_TABLE).document(uid).update("familyId", familyId).await()
            return ResultListener.Success
        } catch (e: Exception) {
            println("Error: ${e.message}")
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
            println("Error: ${e.message}")
            return ResultListener.Failure("Error: ${e.message}")
        }
    }

    // ------------------------------------------------------------------------------- FAMILY
    fun familyInformationSnapshotListener(familyId: String) = callbackFlow {
        println("Firestore familyInformationSnapshotListener init")
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

                println("Snapshot of familyInformation: $familyInformation")
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
        println("FirestoreDataSource joinFamily()")
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
            println("Error: ${e.message}")
            return ResultListener.Failure("Error: ${e.message}")
        }
    }

    suspend fun createNewFamily(
        uid: String,
        name: String,
    ): StringResultListener {
        println("FirestoreDataSource createNewFamily getting uploaded")
        val map = mapOf(
            "members" to listOf(mapOf("uid" to uid, "name" to name)),
        )

        try {
            val documentReference = db.collection(Constants.FAMILIES_TABLE).add(map).await()
            return StringResultListener.Success(documentReference.id)
        } catch (e: Exception) {
            println("Error: ${e.message}")
            return StringResultListener.Failure("Error: ${e.message}")
        }
    }

    suspend fun leaveFamily(
        familyId: String,
        uid: String,
    ): ResultListener {
        println("FirestoreDataSource leaveFamily()")
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
            println("Error: ${e.message}")
            return ResultListener.Failure("Error: ${e.message}")
        }
    }

    suspend fun deleteFamily(
        familyId: String,
    ): ResultListener {
        println("FirestoreDataSource deleteFamily()")
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
            println("Error: ${e.message}")
            return ResultListener.Failure("Error: ${e.message}")
        }
    }

    // ------------------------------------------------------------------------------- GROCERY LIST
    fun grocerySnapshotListener(familyId: String) = callbackFlow {
        println("Firestore grocerySnapshotListener init")
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
                val items = snapshot.toObjects(GroceryItem::class.java)
                println("Snapshot items to GroceryItem: $items")
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
        println("Firestore recipeSnapshotListener init")
        val recipeItemsRef =
            db.collection(Constants.RECIPES_TABLE).whereEqualTo("familyId", familyId)
        val listenerRegistration = recipeItemsRef.addSnapshotListener { snapshot, e ->
            if (e != null) {
                // Handle error
                println("Firestore recipeSnapshotListener error: ${e.message}")
                trySend(ListItemsResultListener.Failure("Error: ${e.message}")).isSuccess
                return@addSnapshotListener
            }

            if (snapshot != null) {
                // Process the changes and update Room database
                val items = snapshot.toObjects(Recipe::class.java)
                println("Snapshot items to Recipe: $items")
                trySend(ListItemsResultListener.Success(items)).isSuccess
            } else {
                trySend(ListItemsResultListener.Failure("Error: Empty snapshot")).isSuccess
            }
        }
        // Await close tells the flow builder to suspend until the flow collector is cancelled or disposed.
        awaitClose { listenerRegistration.remove() }
    }

    // ------------------------------------------------------------------------------- ALBUMS
    fun albumsSnapshotListener(familyId: String) = callbackFlow {
        println("Firestore albumSnapshotListener init")
        val itemsRef =
            db.collection(Constants.ALBUMS_TABLE).whereEqualTo("familyId", familyId)
        val listenerRegistration = itemsRef.addSnapshotListener { snapshot, e ->
            if (e != null) {
                // Handle error
                println("Firestore albumSnapshotListener error: ${e.message}")
                trySend(ListItemsResultListener.Failure("Error: ${e.message}")).isSuccess
                return@addSnapshotListener
            }

            if (snapshot != null) {
                // Process the changes and update Room database
                val items = snapshot.toObjects(Album::class.java)
                println("Snapshot items to Album: $items")
                trySend(ListItemsResultListener.Success(items)).isSuccess
            } else {
                trySend(ListItemsResultListener.Failure("Error: Empty snapshot")).isSuccess
            }
        }
        // Await close tells the flow builder to suspend until the flow collector is cancelled or disposed.
        awaitClose { listenerRegistration.remove() }
    }

    fun updateAlbumCount(albumId: String, count: Int): ResultListener { // TODO not working
        try {
            println("FirestoreDataSource updateAlbumCount()")
            val oldCount = db.collection(Constants.ALBUMS_TABLE).document(albumId).get().result.getLong("count")?.toInt()
            val newCount = oldCount?.plus(count) ?: count
            db.collection(Constants.ALBUMS_TABLE).document(albumId).update("count", newCount)
            return ResultListener.Success
        } catch (e: Exception) {
            println("Error: ${e.message}")
            return ResultListener.Failure("Error: ${e.message}")
        }
    }

    // ------------------------------------------------------------------------------- GALLERY IMAGES
    fun galleryImagesSnapshotListener(familyId: String) = callbackFlow {
        println("Firestore galleryImagesSnapshotListener init")
        val itemsRef =
            db.collection(Constants.GALLERY_IMAGES_TABLE).whereEqualTo("familyId", familyId)
        val listenerRegistration = itemsRef.addSnapshotListener { snapshot, e ->
            if (e != null) {
                // Handle error
                println("Firestore galleryImagesSnapshotListener error: ${e.message}")
                trySend(ListItemsResultListener.Failure("Error: ${e.message}")).isSuccess
                return@addSnapshotListener
            }

            if (snapshot != null) {
                // Process the changes and update Room database
                val items = snapshot.toObjects(GalleryImage::class.java)
                println("Snapshot items to GalleryImage: $items")
                trySend(ListItemsResultListener.Success(items)).isSuccess
            } else {
                trySend(ListItemsResultListener.Failure("Error: Empty snapshot")).isSuccess
            }
        }
        // Await close tells the flow builder to suspend until the flow collector is cancelled or disposed.
        awaitClose { listenerRegistration.remove() }
    }

    // ------------------------------------------------------------------------------- ITEMS
    suspend fun saveItem(
        item: Item,
        listName: String,
    ): StringResultListener {
        try {
            val documentReference = db.collection(listName).add(item).await()
            return StringResultListener.Success(documentReference.id)
        } catch (e: Exception) {
            println("Error: ${e.message}")
            return StringResultListener.Failure("Error: ${e.message}")
        }
    }

    suspend fun updateItem(
        item: Item,
        listName: String,
    ): ResultListener {
        println("FirestoreDataSource updateItem()")
        println("FirestoreDataSource updateItem() id: ${item.id}")
        try {
            if (item.id != null) {
                val map = itemToMap(item)
                println("updateItem map: $map")
                if (map != null) {
                    db.collection(listName).document(item.id!!).update(map).await()
                }
                return ResultListener.Success
            } else {
                return ResultListener.Failure("Error: No document id")
            }
        } catch (e: Exception) {
            println("Error: ${e.message}")
            return ResultListener.Failure("Error: ${e.message}")
        }
    }

    suspend fun toggleCompletableItemCompletion(
        item: CompletableItem,
        listName: String,
    ): ResultListener {
        try {
            if (item.id is String) {
                println("item: $item")
                val result = db.collection(listName).document(item.id!!).update(
                    mapOf(
                        "completed" to item.completed,
                        "lastUpdated" to Date(System.currentTimeMillis()), // Set to current time
                    ),
                ).await()
                println("Update successful: $result")
                return ResultListener.Success
            } else {
                return ResultListener.Failure("Document not found")
            }
        } catch (e: Exception) {
            println("Error: ${e.message}")
            return ResultListener.Failure("Error: ${e.message}")
        }
    }

    fun deleteItem(
        itemId: String,
        listName: String,
    ): ResultListener {
        println("FirestoreDataSource deleteItem()")
        try {
            db.collection(listName).document(itemId).delete()
            return ResultListener.Success
        } catch (e: Exception) {
            println("Error: ${e.message}")
            return ResultListener.Failure("Error: ${e.message}")
        }
    }

    suspend fun deleteItems(
        listName: String,
        items: List<Item>,
    ): ResultListener {
        println("FirestoreDataSource deleteItems()")
        try {
            val batch = db.batch()

            items.forEach { item ->
                println("item id: ${item.id}")
                if (item.id != null) {
                    val documentRef = db.collection(listName).document(item.id!!)
                    batch.delete(documentRef)
                }
            }

            batch.commit().await()
            return ResultListener.Success
        } catch (e: Exception) {
            println("Error: ${e.message}")
            return ResultListener.Failure("Error: ${e.message}")
        }
    }

    // ------------------------------------------------------------------------------- CATEGORIES
    fun categoriesSnapshotListener() = callbackFlow {
        println("Firestore categoriesSnapshotListener init")
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

                println("Snapshot items to CategoryItems: $categoryItems")
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
            println("Error: ${e.message}")
            return ResultListener.Failure("Error: ${e.message}")
        }
    }

    suspend fun deleteCategory(
        category: Category,
    ): ResultListener {
        println("FirestoreDataSource deleteCategory()")
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
            println("Error: ${e.message}")
            return ResultListener.Failure("Error: ${e.message}")
        }
    }

    // ------------------------------------------------------------------------------- GROCERY SUGGESTIONS
    fun grocerySuggestionsSnapshotListener() = callbackFlow {
        println("Firestore grocerySuggestionsSnapshotListener init")
        val grocerySuggestionsItemsRef = db.collection(Constants.GROCERY_SUGGESTIONS_TABLE)
        val listenerRegistration = grocerySuggestionsItemsRef.addSnapshotListener { snapshot, e ->
            if (e != null) {
                // Handle error
                trySend(GrocerySuggestionsListener.Failure("Error: ${e.message}")).isSuccess
                return@addSnapshotListener
            }

            if (snapshot != null) {
                // Process the changes and update Room database
                val grocerySuggestions = snapshot.toObjects(GrocerySuggestion::class.java)

                println("Snapshot items to GrocerySuggestions: $grocerySuggestions")
                trySend(GrocerySuggestionsListener.Success(grocerySuggestions)).isSuccess
            }
        }
        // Await close tells the flow builder to suspend until the flow collector is cancelled or disposed.
        awaitClose { listenerRegistration.remove() }
    }

    suspend fun deleteGrocerySuggestion(
        grocerySuggestion: GrocerySuggestion,
    ): ResultListener {
        println("FirestoreDataSource deleteGrocerySuggestion()")
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
            println("Error: ${e.message}")
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
            println("Error: ${e.message}")
            return ResultListener.Failure("Error: ${e.message}")
        }
    }

    // ------------------------------------------------------------------------------- IMAGES
    suspend fun getImageUrl(
        imageType: ImageType,
    ): StringResultListener? {
        println("getImageUrl imageType: $imageType")
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

                is ImageType.GalleryImage -> return StringResultListener.Failure("Image type GalleryImage is not connected to one specific document")
            }
        } catch (e: Exception) {
            println("Error: ${e.message}")
            return StringResultListener.Failure("Error: ${e.message}")
        }
    }

    suspend fun saveImageDownloadUrl(
        url: String,
        imageType: ImageType,
    ): ResultListener {
        try {
            println("saveImageDownloadUrl url: $url")
            println("saveImageDownloadUrl imageType: $imageType")

            val photo = mapOf(
                "imageUrl" to url,
            )
            println("saveImageDownloadUrl map: $photo")

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
            println("Error: ${e.message}")
            return ResultListener.Failure("Error: ${e.message}")
        }
    }

    suspend fun saveImagesMetaData(
        imageType: ImageType,
        imageDataList: List<Item>,
    ): ResultListener {
        try {
            println("saveImageDownloadUrl imageType: $imageType")

            when (imageType) {
                is ImageType.GalleryImage -> {
                    val batch = db.batch() // Create a batched write
                    val collectionRef = db.collection(Constants.GALLERY_IMAGES_TABLE)

                    imageDataList.forEach { image ->
                        val docRef = collectionRef.document() // Generate a new document reference
                        batch.set(docRef, image) // Add set operation to batch
                    }

                    batch.commit().await() // Commit the batch operation
                }

                else -> {
                    ResultListener.Failure("Image type does not include metadata")
                }
            }

            return ResultListener.Success
        } catch (e: Exception) {
            println("Error: ${e.message}")
            return ResultListener.Failure("Error: ${e.message}")
        }
    }

    // ------------------------------------------------------------------------------- TIP TRACKER
    fun tipTrackerSnapshotListener(familyId: String) = callbackFlow {
        println("Firestore tipTrackerSnapshotListener init")
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
                val tips = snapshot.toObjects(TipItem::class.java)

                println("Snapshot items to TipItems: $tips")
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
        var fcmToken: String? = null
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                fcmToken = task.result
                // Process updatedMembers here
            } else {
                Log.e("FirestoreDataSource", "Failed to fetch FCM token: ${task.exception}")
            }
        }.await()

        if (fcmToken == null) {
            println("Failed to fetch FCM token")
            return
        }

        val familyDocRef = db.collection(Constants.FAMILIES_TABLE).document(familyId)

        val familyDocSnaphot = familyDocRef.get().await()

        if (familyDocSnaphot.exists()) {
            // Fetch the current family document
            val members = familyDocSnaphot.get("members") as? List<Map<String, Any>> ?: emptyList()

            // Find the member with the matching uid
            var updateNeeded = false
            val updatedMembers = members.map { member ->
                if (member["uid"] == uid) {
                    val currentToken = member["fcmToken"] as? String
                    if (currentToken == null || currentToken != fcmToken) {
                        // Update only if no token exists or the token is different
                        updateNeeded = true
                        member.toMutableMap().apply {
                            put("fcmToken", fcmToken!!)
                        }
                    } else {
                        member
                    }
                } else {
                    member
                }
            }

            println("Is it needed to update FCM token: $updateNeeded")
            if (updateNeeded) {
                // Update the members array in Firestore
                familyDocRef.update("members", updatedMembers)
                    .addOnSuccessListener {
                        println("FCM token updated successfully.")
                    }
                    .addOnFailureListener { e ->
                        println("Error updating FCM token: ${e.message}")
                    }
            }
        } else {
            println("Family document not found")
        }
    }

    fun removeDeviceToken(uid: String, familyId: String) {
        val familyDocRef = db.collection(Constants.FAMILIES_TABLE).document(familyId)

        // Fetch the current family document
        familyDocRef.get().addOnSuccessListener { document ->
            if (document.exists()) {
                val members = document.get("members") as? List<Map<String, Any>> ?: emptyList()

                // Remove the fcmToken for the specified uid
                val updatedMembers = members.map { member ->
                    if (member["uid"] == uid) {
                        // Remove the fcmToken
                        member.toMutableMap().apply {
                            remove("fcmToken")
                        }
                    } else {
                        member
                    }
                }

                // Update the members array in Firestore
                familyDocRef.update("members", updatedMembers)
                    .addOnSuccessListener {
                        println("FCM token removed successfully.")
                    }
                    .addOnFailureListener { e ->
                        println("Error removing FCM token: ${e.message}")
                    }
            }
        }.addOnFailureListener { e ->
            println("Error fetching family document: ${e.message}")
        }
    }

    suspend fun getFcmTokensFromFamily(familyId: String): List<String>? {
        // Get the family document to retrieve the fcmTokens
        val familyDocRef = db.collection(Constants.FAMILIES_TABLE).document(familyId).get().await()

        val document = familyDocRef.data
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
