package com.example.lifetogether.data.repository

import com.example.lifetogether.domain.callback.ResultListener
import com.example.lifetogether.domain.model.Item
import com.example.lifetogether.domain.repository.ListRepository
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore

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
}
