package com.example.lifetogether.data.remote

import com.example.lifetogether.domain.callback.AuthResultListener
import com.example.lifetogether.domain.callback.CategoriesListener
import com.example.lifetogether.domain.callback.FamilyInformationResultListener
import com.example.lifetogether.domain.callback.GrocerySuggestionsListener
import com.example.lifetogether.domain.callback.ListItemsResultListener
import com.example.lifetogether.domain.callback.ResultListener
import com.example.lifetogether.domain.callback.StringResultListener
import com.example.lifetogether.domain.converter.itemToMap
import com.example.lifetogether.domain.model.Category
import com.example.lifetogether.domain.model.CompletableItem
import com.example.lifetogether.domain.model.GroceryItem
import com.example.lifetogether.domain.model.GrocerySuggestion
import com.example.lifetogether.domain.model.Item
import com.example.lifetogether.domain.model.UserInformation
import com.example.lifetogether.domain.model.family.FamilyInformation
import com.example.lifetogether.domain.model.family.FamilyMember
import com.example.lifetogether.domain.model.recipe.Recipe
import com.example.lifetogether.domain.model.sealed.ImageType
import com.example.lifetogether.util.Constants
import com.google.firebase.Firebase
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
                val membersData = snapshot.get("members") as? List<Map<String, String>> ?: emptyList()

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
            val documentReference = db.collection(Constants.FAMILIES_TABLE).document(familyId).get().await()

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
            val documentReference = db.collection(Constants.FAMILIES_TABLE).document(familyId).get().await()

            @Suppress("UNCHECKED_CAST")
            val members = documentReference.data?.get("members") as? List<Map<String, String>>

            // Remove the member from the list by matching the uid
            val updatedMembers = members?.filterNot { it["uid"] == uid }?.toMutableList() ?: mutableListOf()

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

            val usersRef = db.collection(Constants.USER_TABLE).whereEqualTo("familyId", familyId).get().await()

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

    // ------------------------------------------------------------------------------- IMAGES
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
                    db.collection("users").document(imageType.uid).update(photo).await()
                }
                is ImageType.FamilyImage -> {
                    db.collection("families").document(imageType.familyId).update(photo).await()
                }
                is ImageType.RecipeImage -> {
                    db.collection("recipes").document(imageType.recipeId).update(photo).await()
                }
            }

            return ResultListener.Success
        } catch (e: Exception) {
            println("Error: ${e.message}")
            return ResultListener.Failure("Error: ${e.message}")
        }
    }
}

 /* val list = listOf<Recipe>(
    Recipe(
        itemName = "Apple Cake",
        description = "A traditional Danish dessert made with layers of apple compote, whipped cream, and crushed macaroons.",
        ingredients = listOf(
            Ingredient(amount = 10.0, measureType = MeasureType.PIECE, itemName = "apple"),
            Ingredient(amount = 0.5, measureType = MeasureType.DECILITER, itemName = "water"),
            Ingredient(amount = 0.5, measureType = MeasureType.DECILITER, itemName = "sugar"),
            Ingredient(amount = 1.0, measureType = MeasureType.PIECE, itemName = "macaroon"),
            Ingredient(amount = 0.0, measureType = MeasureType.PIECE, itemName = "apple cake crumbs"),
            Ingredient(amount = 0.5, measureType = MeasureType.DECILITER, itemName = "whipping cream")
        ),
        instructions = listOf(
            Instruction(itemName = "Peel the apples"),
            Instruction(itemName = "Cut them into wedges/thin slices"),
            Instruction(itemName = "Put them in a pot with 0.5 dl water"),
            Instruction(itemName = "Bring the water to a boil"),
            Instruction(itemName = "Add sugar"),
            Instruction(itemName = "Cook for 20-25 minutes"),
            Instruction(itemName = "Let it cool completely"),
            Instruction(itemName = "Whip the cream"),
            Instruction(itemName = "Crush the macaroons (3-4 per layer)"),
            Instruction(itemName = "Layer apple compote, macaroons, and crumbs")
        ),
        preparationTimeMin = 45,
        favourite = false,
        servings = 4,
        tags = listOf("dessert", "cake", "fruit")
    ),
    Recipe(
        itemName = "American Cookies",
        description = "Delicious homemade American-style cookies with chocolate chunks.",
        ingredients = listOf(
            Ingredient(amount = 120.0, measureType = MeasureType.GRAM, itemName = "sugar"),
            Ingredient(amount = 140.0, measureType = MeasureType.GRAM, itemName = "brown sugar"),
            Ingredient(amount = 140.0, measureType = MeasureType.GRAM, itemName = "butter"),
            Ingredient(amount = 1.0, measureType = MeasureType.PIECE, itemName = "egg"),
            Ingredient(amount = 240.0, measureType = MeasureType.GRAM, itemName = "flour"),
            Ingredient(amount = 1.0, measureType = MeasureType.TEASPOON, itemName = "baking powder"),
            Ingredient(amount = 0.5, measureType = MeasureType.TEASPOON, itemName = "baking soda"),
            Ingredient(amount = 1.0, measureType = MeasureType.TEASPOON, itemName = "salt"),
            Ingredient(amount = 0.5, measureType = MeasureType.TEASPOON, itemName = "vanilla sugar"),
            Ingredient(amount = 200.0, measureType = MeasureType.GRAM, itemName = "dark chocolate")
        ),
        instructions = listOf(
            Instruction(itemName = "Mix sugar, brown sugar, vanilla sugar, and butter together"),
            Instruction(itemName = "Add the egg and mix again"),
            Instruction(itemName = "In a separate bowl, combine flour, baking powder, salt, and baking soda"),
            Instruction(itemName = "Gradually add the flour mixture while mixing on low speed"),
            Instruction(itemName = "Fold in the chocolate chunks with a spatula"),
            Instruction(itemName = "Chill the dough for at least 1 hour"),
            Instruction(itemName = "Bake at 175°C (top and bottom heat) for 10 minutes")
        ),
        preparationTimeMin = 70, // Including chilling time
        favourite = false,
        servings = 12, // Assuming the recipe makes 12 cookies
        tags = listOf("dessert", "cookies", "chocolate")
    ),
    Recipe(
        itemName = "Banana Muffins with Chocolate",
        description = "With a twist of cinnamon and chocolate icing on top - it doesn't get any better.",
        ingredients = listOf(
            Ingredient(amount = 2.0, measureType = MeasureType.PIECE, itemName = "egg"),
            Ingredient(amount = 150.0, measureType = MeasureType.GRAM, itemName = "sugar"),
            Ingredient(amount = 1.0, measureType = MeasureType.TEASPOON, itemName = "vanilla sugar"),
            Ingredient(amount = 200.0, measureType = MeasureType.GRAM, itemName = "all-purpose flour"),
            Ingredient(amount = 2.0, measureType = MeasureType.TEASPOON, itemName = "baking powder"),
            Ingredient(amount = 125.0, measureType = MeasureType.GRAM, itemName = "butter"),
            Ingredient(amount = 2.0, measureType = MeasureType.PIECE, itemName = "ripe banana"),
            Ingredient(amount = 100.0, measureType = MeasureType.GRAM, itemName = "dark chocolate")
        ),
        instructions = listOf(
            Instruction(itemName = "Whisk eggs, sugar, and vanilla sugar until white and frothy"),
            Instruction(itemName = "Mix flour with baking powder"),
            Instruction(itemName = "Melt the butter over low heat and cool"),
            Instruction(itemName = "Stir the flour mixture and melted cooled butter into the egg mixture along with mashed bananas and coarsely chopped chocolate"),
            Instruction(itemName = "Divide the batter into 12 paper muffin cups (approx. 1 dl each) and bake in the middle of the oven at 200°C for 15-20 minutes"),
            Instruction(itemName = "Icing:"),
            Instruction(itemName = "Mix powdered sugar with cocoa and a little boiling water to make an icing"),
            Instruction(itemName = "Put the icing in a freezer bag, cut a small hole in the corner, and decorate the cakes"),
        ),
        preparationTimeMin = 35, // Including baking time
        favourite = false,
        servings = 12,
        tags = listOf("dessert", "muffins", "fruit", "chocolate")
    ),
    Recipe(
        itemName = "Banana Pancakes",
        description = "Delicious and healthy banana pancakes, perfect for breakfast or as a snack.",
        ingredients = listOf(
            Ingredient(amount = 3.0, measureType = MeasureType.PIECE, itemName = "banana"),
            Ingredient(amount = 4.0, measureType = MeasureType.PIECE, itemName = "egg"),
            Ingredient(amount = 2.0, measureType = MeasureType.DECILITER, itemName = "oatmeal"),
            Ingredient(amount = 2.0, measureType = MeasureType.TEASPOON, itemName = "baking powder")
        ),
        instructions = listOf(
            Instruction(itemName = "Consider smashing the bananas in a bowl with a fork so it is easier to blend"),
            Instruction(itemName = "Blend bananas, eggs, oatmeal, and baking powder together using an blender, food processor, or similar"),
            Instruction(itemName = "Let the batter rest for 10 minutes to thicken. If it seems too thin, add a bit more oatmeal."),
            Instruction(itemName = "Heat a bit of fat in a pan over medium heat. Then make small pancakes."),
            Instruction(itemName = "Cook for about 40-60 seconds on each side")
        ),
        preparationTimeMin = 20,
        favourite = false,
        servings = 10,
        tags = listOf("breakfast", "pancakes", "fruit")
    ),
    Recipe(
        itemName = "Brownie Muffins with Raspberry Mousse",
        description = "Decadent brownie muffins topped with a light and fluffy raspberry mousse.",
        ingredients = listOf(
            Ingredient(amount = 200.0, measureType = MeasureType.GRAM, itemName = "butter"),
            Ingredient(amount = 200.0, measureType = MeasureType.GRAM, itemName = "dark chocolate"),
            Ingredient(amount = 100.0, measureType = MeasureType.GRAM, itemName = "all-purpose flour"),
            Ingredient(amount = 3.0, measureType = MeasureType.PIECE, itemName = "egg"),
            Ingredient(amount = 250.0, measureType = MeasureType.GRAM, itemName = "sugar"),
            Ingredient(amount = 1.0, measureType = MeasureType.PINCH, itemName = "salt"),
            Ingredient(amount = 3.0, measureType = MeasureType.PIECE, itemName = "gelatin sheet"),
            Ingredient(amount = 0.5, measureType = MeasureType.PIECE, itemName = "vanilla bean"),
            Ingredient(amount = 0.5, measureType = MeasureType.DECILITER, itemName = "sugar"),
            Ingredient(amount = 200.0, measureType = MeasureType.GRAM, itemName = "frozen raspberries"),
            Ingredient(amount = 2.5, measureType = MeasureType.DECILITER, itemName = "whipping cream")
        ),
        instructions = listOf(
            Instruction(itemName = "Preheat the oven to 150°C"),
            Instruction(itemName = "Carefully melt the butter and chocolate in a saucepan"),
            Instruction(itemName = "Whisk together flour, eggs, sugar, and salt in a bowl until fluffy"),
            Instruction(itemName = "Fold the chocolate mixture into the egg mixture"),
            Instruction(itemName = "Pour the batter into muffin cups, filling them ⅔ full"),
            Instruction(itemName = "Bake in the oven for 15 minutes"),
            Instruction(itemName = "Let them cool completely, preferably overnight in the refrigerator"),
            Instruction(itemName = "Soak the gelatin in cold water for 10 minutes"),
            Instruction(itemName = "Scrape the seeds from the vanilla bean and add both seeds and pod to a saucepan with raspberries and sugar"),
            Instruction(itemName = "Bring the raspberries to a boil and let simmer for 5 minutes"),
            Instruction(itemName = "Remove the vanilla pod from the saucepan"),
            Instruction(itemName = "Squeeze the water from the gelatin and add it to the saucepan, stirring until melted"),
            Instruction(itemName = "Strain the mixture into a bowl to remove the raspberry seeds"),
            Instruction(itemName = "Let the raspberry mixture cool to room temperature"),
            Instruction(itemName = "Whip the cream until fluffy"),
            Instruction(itemName = "Fold the whipped cream into the raspberry mixture a little at a time until you have a light raspberry mousse"),
            Instruction(itemName = "Chill the mousse until the cakes are ready to be served"),
            Instruction(itemName = "For serving:"),
            Instruction(itemName = "Whisk the raspberry mousse until smooth and fluffy"),
            Instruction(itemName = "Transfer to a piping bag, and pipe onto the cooled muffins"),
        ),
        preparationTimeMin = 45, // Excluding cooling time
        favourite = false,
        servings = 10,
        tags = listOf("dessert", "muffins", "chocolate", "fruit")
    ),
    Recipe(
        itemName = "Dumle Chocolate Cake with Toffifee, Dumle, and Fruit",
        description = "A rich chocolate cake topped with a variety of chocolates and fresh fruits.",
        ingredients = listOf(
            Ingredient(amount = 300.0, measureType = MeasureType.GRAM, itemName = "cake flour"),
            Ingredient(amount = 2.5, measureType = MeasureType.TEASPOON, itemName = "baking powder"),
            Ingredient(amount = 2.5, measureType = MeasureType.TEASPOON, itemName = "vanilla sugar"),
            Ingredient(amount = 0.25, measureType = MeasureType.TEASPOON, itemName = "salt"),
            Ingredient(amount = 180.0, measureType = MeasureType.GRAM, itemName = "butter"),
            Ingredient(amount = 375.0, measureType = MeasureType.GRAM, itemName = "sugar"),
            Ingredient(amount = 3.0, measureType = MeasureType.PIECE, itemName = "egg"),
            Ingredient(amount = 2.25, measureType = MeasureType.DECILITER, itemName = "milk"),
            Ingredient(amount = 75.0, measureType = MeasureType.GRAM, itemName = "cocoa powder"),
            Ingredient(amount = 4.0, measureType = MeasureType.TABLESPOON, itemName = "sugar"),
            Ingredient(amount = 4.0, measureType = MeasureType.TABLESPOON, itemName = "cocoa powder"),
            Ingredient(amount = 2.5, measureType = MeasureType.DECILITER, itemName = "cream"),
            Ingredient(amount = 0.0, measureType = MeasureType.GRAM, itemName = "Ferreo Rocher"),
            Ingredient(amount = 0.0, measureType = MeasureType.GRAM, itemName = "Maltesers"),
            Ingredient(amount = 0.0, measureType = MeasureType.GRAM, itemName = "Dumle"),
            Ingredient(amount = 0.0, measureType = MeasureType.GRAM, itemName = "Toffifee"),
            Ingredient(amount = 0.0, measureType = MeasureType.GRAM, itemName = "strawberry"),
            Ingredient(amount = 0.0, measureType = MeasureType.GRAM, itemName = "blueberry")
        ),
        instructions = listOf(
            Instruction(itemName = "Preheat the oven to 175°C (fan)"),
            Instruction(itemName = "Whisk together the sugar, eggs, and butter until light and fluffy"),
            Instruction(itemName = "In a separate bowl, mix the flour, salt, baking powder, and vanilla sugar"),
            Instruction(itemName = "Gradually add the flour mixture to the butter mixture, alternating with the milk, while whisking"),
            Instruction(itemName = "Add the cocoa powder and whisk until the batter is smooth"),
            Instruction(itemName = "Pour the batter into a greased springform pan (approx. 25 cm in diameter)"),
            Instruction(itemName = "Bake for at least 30 minutes, checking regularly after the first 30 minutes"),
            Instruction(itemName = "To prevent the cake from rising more in the middle, wrap a damp towel around the pan before baking"),
            Instruction(itemName = "Once the cake is completely cooled, prepare the chocolate cream"),
            Instruction(itemName = "Spread the chocolate cream over the cake and top with the decorations")
        ),
        preparationTimeMin = 55,
        favourite = false,
        servings = 8,
        tags = listOf("dessert", "cake", "chocolate", "fruit")
    ),
    Recipe(
        itemName = "Fruit Salad",
        description = "A fresh and sweet fruit salad with whipped cream and dark chocolate.",
        ingredients = listOf(
            Ingredient(amount = 5.0, measureType = MeasureType.PIECE, itemName = "fruit"),
            Ingredient(amount = 0.25, measureType = MeasureType.LITER, itemName = "whipping cream"),
            Ingredient(amount = 100.0, measureType = MeasureType.GRAM, itemName = "dark chocolate")
        ),
        instructions = listOf(
            Instruction(itemName = "Whip the cream with an electric mixer in a bowl"),
            Instruction(itemName = "Chop the fruit and add to the bowl"),
            Instruction(itemName = "Chop the dark chocolate to the desired size and add to the bowl"),
            Instruction(itemName = "Fold the fruit salad with a spatula to mix everything together")
        ),
        preparationTimeMin = 15,
        favourite = false,
        servings = 6,
        tags = listOf("dessert", "fruit", "chocolate")
    ),
    Recipe(
        itemName = "Cheese and Garlic Crack Bread (Pull Apart Bread)",
        description = "This is garlic bread - on crack! Great to share with a crowd, or as a centre piece for dinner accompanied by a simple salad.",
        ingredients = listOf(
            Ingredient(amount = 1.0, measureType = MeasureType.PIECE, itemName = "crusty loaf, preferably sourdough or Vienna"),
            Ingredient(amount = 100.0, measureType = MeasureType.GRAM, itemName = "shredded Mozzarella cheese"),
            Ingredient(amount = 100.0, measureType = MeasureType.GRAM, itemName = "unsalted butter"),
            Ingredient(amount = 2.0, measureType = MeasureType.CLOVE, itemName = "garlic"),
            Ingredient(amount = 0.75, measureType = MeasureType.TEASPOON, itemName = "salt"),
        ),
        instructions = listOf(
            Instruction(itemName = "Preheat the oven to 180°C/350°F"),
            Instruction(itemName = "Combine butter, garlic, and salt in a heatproof bowl and melt in the microwave"),
            Instruction(itemName = "Cut the bread on a diagonal into 2cm diamonds but do not cut all the way through the bread"),
            Instruction(itemName = "Use your fingers or a knife to pry open each crack and drizzle in a teaspoon of butter and stuff in a pinch of cheese"),
            Instruction(itemName = "Brush surface with remaining butter"),
            Instruction(itemName = "Wrap with foil and bake for 20 minutes until the cheese has mostly melted"),
            Instruction(itemName = "Then unwrap and bake for 5 - 10 minutes more to make the bread nice and crusty"),
            Instruction(itemName = "Serve immediately")
        ),
        preparationTimeMin = 30,
        favourite = false,
        servings = 8,
        tags = listOf("appetizer", "side dish")
    ),
    Recipe(
        itemName = "Guacamole",
        description = "A creamy and flavorful avocado dip with a hint of garlic.",
        ingredients = listOf(
            Ingredient(amount = 1.0, measureType = MeasureType.PIECE, itemName = "large garlic clove"),
            Ingredient(amount = 1.0, measureType = MeasureType.PIECE, itemName = "small garlic clove"),
            Ingredient(amount = 1.0, measureType = MeasureType.PIECE, itemName = "avocado"),
            Ingredient(amount = 3.5, measureType = MeasureType.TABLESPOON, itemName = "sour cream"),
            Ingredient(amount = 0.0, measureType = MeasureType.GRAM, itemName = "salt"),
            Ingredient(amount = 0.0, measureType = MeasureType.GRAM, itemName = "pepper")
        ),
        instructions = listOf(
            Instruction(itemName = "Cut the avocado in half and remove the pit"),
            Instruction(itemName = "Use a tablespoon to scoop the avocado out of the skin"),
            Instruction(itemName = "Mash the two avocado halves with a fork"),
            Instruction(itemName = "Add salt and pepper to taste"),
            Instruction(itemName = "Press the garlic cloves and add to the mixture"),
            Instruction(itemName = "Add the sour cream and mix well")
        ),
        preparationTimeMin = 10,
        favourite = false,
        servings = 4,
        tags = listOf("dip", "appetizer")
    ),
    Recipe(
        itemName = "Pancakes",
        description = "Classic pancakes perfect for a group, served with your favorite toppings.",
        ingredients = listOf(
            Ingredient(amount = 5.0, measureType = MeasureType.PIECE, itemName = "egg"),
            Ingredient(amount = 0.5, measureType = MeasureType.TEASPOON, itemName = "sugar"),
            Ingredient(amount = 0.25, measureType = MeasureType.TEASPOON, itemName = "salt"),
            Ingredient(amount = 125.0, measureType = MeasureType.GRAM, itemName = "flour"),
            Ingredient(amount = 0.5, measureType = MeasureType.CAN, itemName = "beer"),
            Ingredient(amount = 1.25, measureType = MeasureType.DECILITER, itemName = "milk"),
            Ingredient(amount = 2.0, measureType = MeasureType.TABLESPOON, itemName = "oil or margarine"),
            Ingredient(amount = 0.0, measureType = MeasureType.GRAM, itemName = "fat for baking")
        ),
        instructions = listOf(
            Instruction(itemName = "Whisk together eggs, sugar, and salt"),
            Instruction(itemName = "Add milk, oil (or margarine), and beer"),
            Instruction(itemName = "Gradually add flour until the batter becomes slightly thick"),
            Instruction(itemName = "Let the batter rest for 30-45 minutes"),
            Instruction(itemName = "Heat a bit of fat in a pan and then pour the batter onto the pan")
        ),
        preparationTimeMin = 60,
        favourite = false,
        servings = 6,
        tags = listOf("breakfast", "pancakes")
    ),
    Recipe(
        itemName = "Spaghetti Bolognese",
        description = "A classic Italian pasta dish with a rich and flavorful meat sauce.",
        ingredients = listOf(
            Ingredient(amount = 2.0, measureType = MeasureType.CLOVE, itemName = "garlic"),
            Ingredient(amount = 1.0, measureType = MeasureType.PIECE, itemName = "onion"),
            Ingredient(amount = 2.0, measureType = MeasureType.TABLESPOON, itemName = "olive oil"),
            Ingredient(amount = 500.0, measureType = MeasureType.GRAM, itemName = "ground beef"),
            Ingredient(amount = 1.0, measureType = MeasureType.PIECE, itemName = "beef bouillon cube"),
            Ingredient(amount = 1.0, measureType = MeasureType.CAN, itemName = "pureed tomatoes"),
            Ingredient(amount = 1.0, measureType = MeasureType.CAN, itemName = "tomato paste"),
            Ingredient(amount = 0.0, measureType = MeasureType.GRAM, itemName = "salt"),
            Ingredient(amount = 0.0, measureType = MeasureType.GRAM, itemName = "pepper"),
            Ingredient(amount = 0.0, measureType = MeasureType.GRAM, itemName = "paprika"),
            Ingredient(amount = 0.0, measureType = MeasureType.GRAM, itemName = "chili powder"),
            Ingredient(amount = 0.0, measureType = MeasureType.GRAM, itemName = "basil")
        ),
        instructions = listOf(
            Instruction(itemName = "Grate the onion"),
            Instruction(itemName = "Sauté the garlic and onion in oil in a pot until they are slightly colored"),
            Instruction(itemName = "Heat a cup of cold water in the microwave for 1 minute and 20 seconds"),
            Instruction(itemName = "Dissolve the bouillon in the water by letting it stand for a bit, then stir and mash it against the side until completely dissolved"),
            Instruction(itemName = "Add the ground beef to the onions and cook until browned"),
            Instruction(itemName = "Add a lot of paprika, a little chili powder, salt, and pepper. Add some basil (the rest will be added later)"),
            Instruction(itemName = "Add the bouillon and pureed tomatoes"),
            Instruction(itemName = "Cook uncovered for about 15 minutes"),
            Instruction(itemName = "Add the tomato paste and a little basil"),
            Instruction(itemName = "Cover and cook for another 5 minutes")
        ),
        preparationTimeMin = 30,
        favourite = false,
        servings = 5,
        tags = listOf("dinner", "pasta", "Italian")
    ),
    Recipe(
        itemName = "Pebernødder",
        description = "Traditional Danish Christmas cookies with a spicy kick.",
        ingredients = listOf(
            Ingredient(amount = 250.0, measureType = MeasureType.GRAM, itemName = "liquid margarine"),
            Ingredient(amount = 250.0, measureType = MeasureType.GRAM, itemName = "sugar"),
            Ingredient(amount = 1.0, measureType = MeasureType.TABLESPOON, itemName = "syrup"),
            Ingredient(amount = 2.0, measureType = MeasureType.TEASPOON, itemName = "cardamom"),
            Ingredient(amount = 2.0, measureType = MeasureType.TEASPOON, itemName = "baking soda"),
            Ingredient(amount = 1.0, measureType = MeasureType.PIECE, itemName = "egg"),
            Ingredient(amount = 425.0, measureType = MeasureType.GRAM, itemName = "flour") // Using the average of the range provided
        ),
        instructions = listOf(
            Instruction(itemName = "Mix the liquid ingredients together and the dry ingredients together"),
            Instruction(itemName = "Combine the contents of the two bowls"),
            Instruction(itemName = "Knead the mixture together"),
            Instruction(itemName = "Chill in the refrigerator for 1 hour"),
            Instruction(itemName = "Bake at 200°C for 8 minutes")
        ),
        preparationTimeMin = 68, // Including chilling time
        favourite = false,
        servings = 8, // Assuming the recipe makes around 8 servings
        tags = listOf("dessert", "cookies", "Christmas")
    ),
    Recipe(
        itemName = "Homemade Pizza",
        description = "A personal-sized pizza with your choice of toppings.",
        ingredients = listOf(
            Ingredient(amount = 1.0, measureType = MeasureType.DECILITER, itemName = "water"),
            Ingredient(amount = 0.25, measureType = MeasureType.PACKAGE, itemName = "yeast"),
            Ingredient(amount = 1.0, measureType = MeasureType.DECILITER, itemName = "flour"),
            Ingredient(amount = 1.0, measureType = MeasureType.TEASPOON, itemName = "salt"),
            Ingredient(amount = 1.5, measureType = MeasureType.DECILITER, itemName = "flour"),
            Ingredient(amount = 1.0, measureType = MeasureType.TEASPOON, itemName = "olive oil"),
            Ingredient(amount = 0.0, measureType = MeasureType.GRAM, itemName = "tomato sauce"),
            Ingredient(amount = 0.0, measureType = MeasureType.GRAM, itemName = "pizza toppings"),
            Ingredient(amount = 50.0, measureType = MeasureType.GRAM, itemName = "shredded cheese")
        ),
        instructions = listOf(
            Instruction(itemName = "Whisk together water, yeast, 1 dl flour, and a bit of salt in a bowl and cover"),
            Instruction(itemName = "Let the mixture rest for at least 45 minutes until it becomes a liquid 'dough'"),
            Instruction(itemName = "Gradually stir the remaining flour into the dough"),
            Instruction(itemName = "If the dough seems too sticky, add a bit more flour"),
            Instruction(itemName = "Pour the oil over the dough and turn it in the bowl so it becomes slightly greasy all over"),
            Instruction(itemName = "Let the dough rest while you prepare the toppings"),
            Instruction(itemName = "Preheat the oven to 250°C"),
            Instruction(itemName = "Shape the dough into a ball and dust it with flour"),
            Instruction(itemName = "Place the dough on a piece of baking paper and press it flat from the center outwards"),
            Instruction(itemName = "The dough should be thinner than you think and about 30 cm in diameter"),
            Instruction(itemName = "Cover the pizza with tomato sauce, toppings, and cheese"),
            Instruction(itemName = "Let the pizza rest for about 30 minutes, then bake on the top rack for 10-12 minutes")
        ),
        preparationTimeMin = 90,
        favourite = false,
        servings = 1,
        tags = listOf("dinner", "pizza", "Italian")
    ),
    Recipe(
        itemName = "Macaroni Pie / Torta de pasta",
        description = "A delicious and easy-to-make dish that's perfect for any occasion.",
        ingredients = listOf(
            Ingredient(amount = 454.0, measureType = MeasureType.GRAM, itemName = "short pasta (elbows, shells, rotini)"), // 1 pound = 453.592 grams
            Ingredient(amount = 454.0, measureType = MeasureType.GRAM, itemName = "mozzarella cheese, shredded, divided into 2 portions"), // 1 pound = 453.592 grams
            Ingredient(amount = 0.5, measureType = MeasureType.PIECE, itemName = "onion, grated"),
            Ingredient(amount = 3.0, measureType = MeasureType.TABLESPOON, itemName = "vegetable oil"),
            Ingredient(amount = 170.097, measureType = MeasureType.GRAM, itemName = "bacon"), // 6 ounces = 170.097 grams
            Ingredient(amount = 4.0, measureType = MeasureType.PIECE, itemName = "egg, beaten"),
            Ingredient(amount = 0.0, measureType = MeasureType.GRAM, itemName = "salt"),
            Ingredient(amount = 0.0, measureType = MeasureType.GRAM, itemName = "pepper"),
            Ingredient(amount = 2.0, measureType = MeasureType.TABLESPOON, itemName = "all-purpose flour"),
            Ingredient(amount = 4.75, measureType = MeasureType.DECILITER, itemName = "hot milk"),
            Ingredient(amount = 3.0, measureType = MeasureType.TABLESPOON, itemName = "unsalted butter"),
            Ingredient(amount = 0.5, measureType = MeasureType.TABLESPOON, itemName = "salt or chicken/beef bouillon powder"),
            Ingredient(amount = 0.0, measureType = MeasureType.GRAM, itemName = "pepper")
        ),
        instructions = listOf(
            Instruction(itemName = "Cook the pasta according to the instructions. Drain and rinse with cold water to prevent sticking. Set aside"),
            Instruction(itemName = "In a pan, sauté the onion in vegetable oil over medium heat until translucent"),
            Instruction(itemName = "Add the salt, and pepper. Cook for 2-3 minutes, stirring occasionally. Set aside"),
            Instruction(itemName = "Preheat the oven to 205°C (400°F)"),
            Instruction(itemName = "In a large bowl, mix the pasta, sautéed vegetables, and bechamel sauce"),
            Instruction(itemName = "Add half of the cheese, the ham, and the beaten eggs with a pinch of salt. Mix well to combine"),
            Instruction(itemName = "Adjust seasoning with more salt or pepper if needed"),
            Instruction(itemName = "Grease a baking dish with butter and add the pasta mixture, spreading it evenly"),
            Instruction(itemName = "Top with the remaining cheese"),
            Instruction(itemName = "Bake for 30 minutes, until the top is golden and bubbly. Let cool slightly before serving"),
            Instruction(itemName = "For the bechamel sauce:"),
            Instruction(itemName = "Heat the milk in a pot without boiling"),
            Instruction(itemName = "In a separate pan, melt the butter over medium heat"),
            Instruction(itemName = "Add the flour and whisk until smooth."),
            Instruction(itemName = "Gradually add the hot milk, whisking constantly until the sauce thickens"),
            Instruction(itemName = "Season with salt, pepper, and bouillon powder"),
            Instruction(itemName = "Remove from heat and set aside"),
        ),
        preparationTimeMin = 65,
        favourite = false,
        servings = 12,
        tags = listOf("dinner", "pasta")
    ),
    Recipe(
        itemName = "Yorkshire Pudding / Gratin",
        description = "A classic British dish that's perfect for breakfast or as a simple dinner.",
        ingredients = listOf(
            Ingredient(amount = 3.0, measureType = MeasureType.PIECE, itemName = "egg"),
            Ingredient(amount = 2.5, measureType = MeasureType.DECILITER, itemName = "milk"),
            Ingredient(amount = 0.5, measureType = MeasureType.TEASPOON, itemName = "salt"),
            Ingredient(amount = 2.5, measureType = MeasureType.DECILITER, itemName = "all-purpose flour"),
            Ingredient(amount = 1.0, measureType = MeasureType.CAN, itemName = "cocktail sausages"),
            Ingredient(amount = 3.0, measureType = MeasureType.TABLESPOON, itemName = "olive oil"),
            Ingredient(amount = 0.0, measureType = MeasureType.GRAM, itemName = "optional: bacon"),
            Ingredient(amount = 0.0, measureType = MeasureType.GRAM, itemName = "optional: butter and maple syrup")
        ),
        instructions = listOf(
            Instruction(itemName = "Preheat oven to 200°C"),
            Instruction(itemName = "In a small bowl, whisk eggs, milk, and salt"),
            Instruction(itemName = "Whisk flour into egg mixture until blended"),
            Instruction(itemName = "Let stand for 30 minutes"),
            Instruction(itemName = "Meanwhile, cook sausage according to package directions; cut each sausage into 3 pieces"),
            Instruction(itemName = "Place oil in a 12-in. nonstick ovenproof skillet"),
            Instruction(itemName = "Place in oven until hot, 3-4 minutes"),
            Instruction(itemName = "Stir batter and pour into prepared skillet; top with sausage"),
            Instruction(itemName = "Bake until golden brown and puffed, 20-25 minutes"),
            Instruction(itemName = "Remove from skillet; cut into wedges"),
            Instruction(itemName = "If desired, serve with butter and syrup")
        ),
        preparationTimeMin = 35,
        favourite = false,
        servings = 6,
        tags = listOf("breakfast", "dinner")
    ),
    Recipe(
        itemName = "Nachos with Chicken",
        description = "A quick and easy dish with layers of nachos, chicken, and melted cheese.",
        ingredients = listOf(
            Ingredient(amount = 1.0, measureType = MeasureType.PACKAGE, itemName = "nachos chips"),
            Ingredient(amount = 400.0, measureType = MeasureType.GRAM, itemName = "chicken"),
            Ingredient(amount = 400.0, measureType = MeasureType.GRAM, itemName = "shredded cheese"),
            Ingredient(amount = 0.0, measureType = MeasureType.GRAM, itemName = "salt and pepper"),
        ),
        instructions = listOf(
            Instruction(itemName = "Cut the chicken into thin slices"),
            Instruction(itemName = "Cook the chicken in a pan, seasoning with salt and pepper, until done"),
            Instruction(itemName = "Preheat the oven to 150°C"),
            Instruction(itemName = "Place half of the chips in a glass dish"),
            Instruction(itemName = "Add the chicken to the dish"),
            Instruction(itemName = "Sprinkle half of the cheese on top"),
            Instruction(itemName = "Add the remaining chips to the dish"),
            Instruction(itemName = "Sprinkle the remaining cheese on top"),
            Instruction(itemName = "Bake in the oven for 10 minutes")
        ),
        preparationTimeMin = 20,
        favourite = false,
        servings = 4,
        tags = listOf("dinner", "appetizer")
    )
).map { recipe ->
    recipe.copy(familyId = "jHTl4yhYOuruQmaBNUeU")
} */
