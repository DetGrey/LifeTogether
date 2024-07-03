package com.example.lifetogether.data.remote

import com.example.lifetogether.data.local.LocalDataSource
import com.example.lifetogether.domain.callback.AuthResultListener
import com.example.lifetogether.domain.callback.CategoriesListener
import com.example.lifetogether.domain.callback.DefaultsResultListener
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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.tasks.await
import java.util.Date
import javax.inject.Inject
import kotlin.reflect.KClass

class FirestoreDataSource@Inject constructor(
    private val localDataSource: LocalDataSource,
) {
    private val db = Firebase.firestore
    // TODO Firebase.firestore.setPersistenceEnabled(true)

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
    suspend fun updateFamilyId(uid: String, familyId: String): ResultListener {
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
            val documentReference = querySnapshot.documents.firstOrNull()?.reference

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

    suspend fun fetchListDefaults(
        listName: String,
    ): DefaultsResultListener {
        return try {
            val documentSnapshot = db.collection(listName).document("default").get().await()
            DefaultsResultListener.Success(documentSnapshot)
        } catch (e: Exception) {
            println("Error: ${e.message}")
            DefaultsResultListener.Failure("Error: ${e.message}")
        }
    }

    suspend fun <T : Item> fetchListItems(
        listName: String,
        familyId: String,
        itemType: KClass<T>,
    ): Flow<ListItemsResultListener<T>> {
        try {
            val fetchResult = db.collection(listName).whereEqualTo("familyId", familyId).get().await()
            val itemsList = fetchResult.documents.mapNotNull { document ->
                document.toObject(itemType.java)
            }
            println("itemList: $itemsList")
            return flowOf(ListItemsResultListener.Success(itemsList))
        } catch (e: Exception) {
            println("Error: ${e.message}")
            return flowOf(ListItemsResultListener.Failure("Error fetching list items: ${e.message}"))
        }
    }

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

    suspend fun categoriesSnapshotListener() = callbackFlow {
        println("Firestore categoriesSnapshotListener init")
        val categoryItemsRef = Firebase.firestore.collection("grocery-list").document("default")
        val listenerRegistration = categoryItemsRef.addSnapshotListener { snapshot, e ->
            if (e != null) {
                // Handle error
                trySend(CategoriesListener.Failure("Error: ${e.message}")).isSuccess
                return@addSnapshotListener
            }

            if (snapshot != null && snapshot.exists()) {
                // Process the changes and update Room database
                @Suppress("UNCHECKED_CAST")
                val categories = snapshot.get("categories") as? List<Map<String, String>>

                val categoryItems = categories?.map { category ->
                    Category(
                        emoji = category["emoji"] ?: "",
                        name = category["name"] ?: "",
                    )
                } ?: emptyList()

                println("Snapshot items to CategoryItems: $categoryItems")
                if (categoryItems.isNotEmpty()) {
                    trySend(CategoriesListener.Success(categoryItems)).isSuccess
                }
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
