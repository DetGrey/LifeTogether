package com.example.lifetogether.data.remote

import com.example.lifetogether.domain.callback.AuthResultListener
import com.example.lifetogether.domain.callback.DefaultsResultListener
import com.example.lifetogether.domain.callback.ListItemsResultListener
import com.example.lifetogether.domain.callback.ResultListener
import com.example.lifetogether.domain.model.GroceryItem
import com.example.lifetogether.domain.model.Item
import com.example.lifetogether.domain.model.UserInformation
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.tasks.await
import java.util.Date
import kotlin.reflect.KClass

class FirestoreDataSource() {
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
                .whereEqualTo("uid", item.uid)
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
        uid: String,
        itemType: KClass<T>,
    ): ListItemsResultListener<T> {
        try {
            val fetchResult = db.collection(listName).whereEqualTo("uid", uid).get().await()
            val itemsList = fetchResult.documents.mapNotNull { document ->
                document.toObject(itemType.java)
            }
            println("itemList: $itemsList")
            return ListItemsResultListener.Success(itemsList)
        } catch (e: Exception) {
            println("Error: ${e.message}")
            return ListItemsResultListener.Failure("Error fetching list items: ${e.message}")
        }
    }
}
