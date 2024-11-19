package com.example.lifetogether.data.remote

import com.example.lifetogether.domain.callback.AuthResultListener
import com.example.lifetogether.domain.callback.CategoriesListener
import com.example.lifetogether.domain.callback.GrocerySuggestionsListener
import com.example.lifetogether.domain.callback.ListItemsResultListener
import com.example.lifetogether.domain.callback.ResultListener
import com.example.lifetogether.domain.callback.StringResultListener
import com.example.lifetogether.domain.model.Category
import com.example.lifetogether.domain.model.CompletableItem
import com.example.lifetogether.domain.model.GroceryItem
import com.example.lifetogether.domain.model.GrocerySuggestion
import com.example.lifetogether.domain.model.Item
import com.example.lifetogether.domain.model.UserInformation
import com.example.lifetogether.domain.model.recipe.Recipe
import com.example.lifetogether.domain.model.toMap
import com.example.lifetogether.util.Constants
import com.google.firebase.Firebase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
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
                db.collection(Constants.USER_TABLE).document(userInformation.uid).set(userInformation).await()
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
            db.collection(Constants.USER_TABLE).document(uid).update("name", newName).await()
            return ResultListener.Success
        } catch (e: Exception) {
            println("Error: ${e.message}")
            return ResultListener.Failure("Error: ${e.message}")
        }
    }

    // ------------------------------------------------------------------------------- FAMILY
    suspend fun joinFamily(
        familyId: String,
        uid: String,
    ): ResultListener {
        println("FirestoreDataSource joinFamily()")
        try {
            val documentReference = db.collection(Constants.FAMILIES_TABLE).document(familyId).get().await()

            @Suppress("UNCHECKED_CAST")
            val members = documentReference.data?.get("members") as? List<String>

            val updatedMembers = members?.toMutableList() ?: mutableListOf()
            updatedMembers.add(uid)

            db.collection(Constants.FAMILIES_TABLE).document(familyId).update("members", updatedMembers).await()

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
            val documentReference = db.collection(Constants.FAMILIES_TABLE).document(familyId).get().await()

            @Suppress("UNCHECKED_CAST")
            val members = documentReference.data?.get("members") as? List<String>

            val updatedMembers = members?.toMutableList() ?: mutableListOf()
            updatedMembers.remove(uid)

            db.collection(Constants.FAMILIES_TABLE).document(familyId).update("members", updatedMembers).await()

            return ResultListener.Success
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

    // ------------------------------------------------------------------------------- GROCERY LIST
    fun grocerySnapshotListener(familyId: String) = callbackFlow {
        println("Firestore grocerySnapshotListener init")
        val groceryItemsRef = db.collection(Constants.GROCERY_TABLE).whereEqualTo("familyId", familyId)
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
        val recipeItemsRef = db.collection(Constants.RECIPES_TABLE).whereEqualTo("familyId", familyId)
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
                val map = item.toMap()
                println("updateItem map: $map")
                db.collection(listName).document(item.id!!).update(map).await()
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

    suspend fun deleteItem(
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
                db.collection(Constants.GROCERY_SUGGESTIONS_TABLE).document(grocerySuggestion.id).delete().await()
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
}
