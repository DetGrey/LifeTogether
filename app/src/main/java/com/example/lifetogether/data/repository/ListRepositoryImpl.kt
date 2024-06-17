package com.example.lifetogether.data.repository

import com.example.lifetogether.domain.callback.DefaultsResultListener
import com.example.lifetogether.domain.callback.ListItemsResultListener
import com.example.lifetogether.domain.callback.ResultListener
import com.example.lifetogether.domain.model.GroceryItem
import com.example.lifetogether.domain.model.Item
import com.example.lifetogether.domain.repository.ListRepository
import com.google.firebase.Firebase
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.tasks.await
import java.util.Date
import kotlin.reflect.KClass

class ListRepositoryImpl : ListRepository {
    override suspend fun saveItemToGroceryList(item: Item): ResultListener {
        val db = Firebase.firestore
        return try {
            db.collection("grocery-list").add(item)
            return ResultListener.Success
        } catch (e: Exception) {
            println("Error: ${e.message}")
            ResultListener.Failure("Error: ${e.message}")
        }
    }

    override suspend fun toggleItemCompletionInGroceryList(item: GroceryItem): ResultListener {
        val db = Firebase.firestore
        val querySnapshot = db.collection("grocery-list")
            .whereEqualTo("uid", item.uid)
            .whereEqualTo("itemName", item.itemName)
            .whereEqualTo("category", item.category)
            .get()
            .await()

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
    }

    override suspend fun fetchListDefaults(listName: String): DefaultsResultListener {
        val db = Firebase.firestore
        return try {
            val fetchResult = db.collection(listName).document("default").get().await()
            val documentSnapshot: DocumentSnapshot = fetchResult
            return DefaultsResultListener.Success(documentSnapshot)
        } catch (e: Exception) {
            println("Error: ${e.message}")
            DefaultsResultListener.Failure("Error: ${e.message}")
        }
    }

    override suspend fun <T : Item> fetchListItems(
        listName: String,
        uid: String,
        itemType: KClass<T>,
    ): ListItemsResultListener<T> {
        val db = Firebase.firestore
        return try {
            val fetchResult = db.collection(listName).whereEqualTo("uid", uid).get().await()
            val querySnapshotList: MutableList<DocumentSnapshot> = fetchResult.documents
            val itemsList = querySnapshotList.mapNotNull { document ->
                document.toObject(itemType.java)
            }
            println("itemList: $itemsList")
            ListItemsResultListener.Success(itemsList)
        } catch (e: Exception) {
            println("Error: ${e.message}")
            ListItemsResultListener.Failure("Error fetching list items")
        }
    }
}
