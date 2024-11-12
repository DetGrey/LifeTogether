package com.example.lifetogether.data.remote

import com.example.lifetogether.domain.callback.AuthResultListener
import com.example.lifetogether.domain.callback.CategoriesListener
import com.example.lifetogether.domain.callback.ListItemsResultListener
import com.example.lifetogether.domain.callback.ResultListener
import com.example.lifetogether.domain.callback.StringResultListener
import com.example.lifetogether.domain.model.Category
import com.example.lifetogether.domain.model.GroceryItem
import com.example.lifetogether.domain.model.Item
import com.example.lifetogether.domain.model.UserInformation
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.Date
import javax.inject.Inject

class FirestoreDataSource@Inject constructor() {
    private val db = Firebase.firestore

    // -------------------------------------- USERS
    suspend fun getUserInformation(uid: String): AuthResultListener {
        try {
            val documentSnapshot = db.collection("users").document(uid).get().await()
            println("documentSnapshot: $documentSnapshot")
            val userInformation = documentSnapshot.toObject(UserInformation::class.java)
            println("userInformation: $userInformation")

            return if (userInformation != null) {
                AuthResultListener.Success(userInformation)
            } else {
                AuthResultListener.Failure("Could not fetch document")
            }
        } catch (e: Exception) {
            println("Error: ${e.message}")
            return AuthResultListener.Failure("Error: ${e.message}")
        }
    }

    suspend fun uploadUserInformation(userInformation: UserInformation): ResultListener {
        println("FirestoreDataSource uploadUserInformation getting uploaded")
        try {
            if (userInformation.uid != null) {
                db.collection("users").document(userInformation.uid).set(userInformation).await()
                return ResultListener.Success
            } else {
                return ResultListener.Failure("Cannot upload without being logged in")
            }
        } catch (e: Exception) {
            println("Error: ${e.message}")
            return ResultListener.Failure("Error: ${e.message}")
        }
    }

    suspend fun changeName(uid: String, newName: String): ResultListener {
        try {
            db.collection("users").document(uid).update("name", newName).await()
            return ResultListener.Success
        } catch (e: Exception) {
            println("Error: ${e.message}")
            return ResultListener.Failure("Error: ${e.message}")
        }
    }

    // -------------------------------------- FAMILY
    suspend fun joinFamily(
        familyId: String,
        uid: String,
    ): ResultListener {
        println("FirestoreDataSource joinFamily()")
        try {
            val documentReference = db.collection("families").document(familyId).get().await()

            @Suppress("UNCHECKED_CAST")
            val members = documentReference.data?.get("members") as? List<String>

            val updatedMembers = members?.toMutableList() ?: mutableListOf()
            updatedMembers.add(uid)

            db.collection("families").document(familyId).update("members", updatedMembers).await()

            return ResultListener.Success
        } catch (e: Exception) {
            println("Error: ${e.message}")
            return ResultListener.Failure("Error: ${e.message}")
        }
    }

    suspend fun createNewFamily(uid: String): StringResultListener {
        println("FirestoreDataSource createNewFamily getting uploaded")
        val map = mapOf("owner" to uid)

        try {
            val documentReference = db.collection("families").add(map).await()
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
            val documentReference = db.collection("families").document(familyId).get().await()

            @Suppress("UNCHECKED_CAST")
            val members = documentReference.data?.get("members") as? List<String>

            val updatedMembers = members?.toMutableList() ?: mutableListOf()
            updatedMembers.remove(uid)

            db.collection("families").document(familyId).update("members", updatedMembers).await()

            return ResultListener.Success
        } catch (e: Exception) {
            println("Error: ${e.message}")
            return ResultListener.Failure("Error: ${e.message}")
        }
    }

    suspend fun updateFamilyId(uid: String, familyId: String?): ResultListener {
        try {
            db.collection("users").document(uid).update("familyId", familyId).await()
            return ResultListener.Success
        } catch (e: Exception) {
            println("Error: ${e.message}")
            return ResultListener.Failure("Error: ${e.message}")
        }
    }

    // -------------------------------------- ITEMS
    suspend fun saveItem(
        item: Item,
        listName: String,
    ): ResultListener {
        try {
            db.collection(listName).add(item).await()
            return ResultListener.Success
        } catch (e: Exception) {
            println("Error: ${e.message}")
            return ResultListener.Failure("Error: ${e.message}")
        }
    }

    suspend fun toggleItemCompletion(
        item: Item,
        listName: String,
    ): ResultListener {
        try {
            val query = db.collection(listName)
                .whereEqualTo("familyId", item.familyId)
                .whereEqualTo("itemName", item.itemName)

            if (item is GroceryItem) { // Check if item is of type GroceryItem
                query.whereEqualTo("category", item.category)
            }

            val querySnapshot = query.get().await()

            // Assuming there's only one matching document, get its reference
            // Find the document with the exact ID
            val documentReference = querySnapshot.documents
                .firstOrNull { it.id == item.id }?.reference

            if (documentReference != null) {
                // Update the 'completed' field and 'lastUpdated' field of the document
                documentReference.update(
                    mapOf(
                        "completed" to item.completed,
                        "lastUpdated" to Date(System.currentTimeMillis()), // Set to current time
                    ),
                ).await()
                return ResultListener.Success
            } else {
                return ResultListener.Failure("Document not found")
            }
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

    // -------------------------------------- CATEGORIES
    suspend fun deleteCategory(
        category: Category,
    ): ResultListener {
        println("FirestoreDataSource deleteCategory()")
        try {
            // Query the collection to find the document with the matching 'name' field
            val querySnapshot = db.collection("categories")
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

    suspend fun addCategory(
        category: Category,
    ): ResultListener {
        try {
            db.collection("categories").add(category).await()
            return ResultListener.Success
        } catch (e: Exception) {
            println("Error: ${e.message}")
            return ResultListener.Failure("Error: ${e.message}")
        }
    }

//    suspend fun fetchListDefaults(
//        listName: String,
//    ): DefaultsResultListener {
//        return try {
//            val documentSnapshot = db.collection(listName).document("default").get().await()
//            DefaultsResultListener.Success(documentSnapshot)
//        } catch (e: Exception) {
//            println("Error: ${e.message}")
//            DefaultsResultListener.Failure("Error: ${e.message}")
//        }
//    }
//
//    suspend fun <T : Item> fetchListItems(
//        listName: String,
//        familyId: String,
//        itemType: KClass<T>,
//    ): Flow<ListItemsResultListener<T>> {
//        try {
//            val fetchResult = db.collection(listName).whereEqualTo("familyId", familyId).get().await()
//            val itemsList = fetchResult.documents.mapNotNull { document ->
//                document.toObject(itemType.java)
//            }
//            println("itemList: $itemsList")
//            return flowOf(ListItemsResultListener.Success(itemsList))
//        } catch (e: Exception) {
//            println("Error: ${e.message}")
//            return flowOf(ListItemsResultListener.Failure("Error fetching list items: ${e.message}"))
//        }
//    }

    // -------------------------------------- COLLECTION SNAPSHOT LISTENERS
    suspend fun grocerySnapshotListener(familyId: String) = callbackFlow {
        println("Firestore grocerySnapshotListener init")
        val groceryItemsRef = Firebase.firestore.collection("grocery-list").whereEqualTo("familyId", familyId) // TODO only check user's data, not the whole collection
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
            }
        }
        // Await close tells the flow builder to suspend until the flow collector is cancelled or disposed.
        awaitClose { listenerRegistration.remove() }
    }

    fun categoriesSnapshotListener() = callbackFlow {
        println("Firestore categoriesSnapshotListener init")
        val categoryItemsRef = Firebase.firestore.collection("categories")
        val listenerRegistration = categoryItemsRef.addSnapshotListener { snapshot, e ->
            if (e != null) {
                // Handle error
                trySend(CategoriesListener.Failure("Error: ${e.message}")).isSuccess
                return@addSnapshotListener
            }

            if (snapshot != null) {
                // Process the changes and update Room database
                val categoryItems = snapshot.documents.mapNotNull { document ->
                    val emoji = document.getString("emoji")
                    val name = document.getString("name")
                    if (!emoji.isNullOrEmpty() && !name.isNullOrEmpty()) {
                        Category(emoji = emoji, name = name)
                    } else {
                        null
                    }
                }

                println("Snapshot items to CategoryItems: $categoryItems")
                trySend(CategoriesListener.Success(categoryItems)).isSuccess
            }
        }
        // Await close tells the flow builder to suspend until the flow collector is cancelled or disposed.
        awaitClose { listenerRegistration.remove() }
    }


    suspend fun userInformationSnapshotListener(uid: String) = callbackFlow {
        println("Firestore userInformationSnapshotListener init")
        val userInformationRef = Firebase.firestore.collection("users").document(uid) // TODO only check user's data, not the whole collection
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
                if (userInformation != null) {
                    trySend(AuthResultListener.Success(userInformation)).isSuccess
                }
            }
        }
        // Await close tells the flow builder to suspend until the flow collector is cancelled or disposed.
        awaitClose { listenerRegistration.remove() }
    }
}
