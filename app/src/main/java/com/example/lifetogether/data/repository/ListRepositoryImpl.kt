package com.example.lifetogether.data.repository

import com.example.lifetogether.domain.callback.DefaultsResultListener
import com.example.lifetogether.domain.callback.ListItemsResultListener
import com.example.lifetogether.domain.callback.ResultListener
import com.example.lifetogether.domain.model.Item
import com.example.lifetogether.domain.repository.ListRepository
import com.google.firebase.Firebase
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.tasks.await
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
