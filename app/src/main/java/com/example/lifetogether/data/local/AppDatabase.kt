package com.example.lifetogether.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.lifetogether.data.local.dao.AlbumsDao
import com.example.lifetogether.data.local.dao.ChecklistEntriesDao
import com.example.lifetogether.data.local.dao.CategoriesDao
import com.example.lifetogether.data.local.dao.FamilyInformationDao
import com.example.lifetogether.data.local.dao.GalleryMediaDao
import com.example.lifetogether.data.local.dao.GuideProgressDao
import com.example.lifetogether.data.local.dao.GuidesDao
import com.example.lifetogether.data.local.dao.GroceryListDao
import com.example.lifetogether.data.local.dao.GrocerySuggestionsDao
import com.example.lifetogether.data.local.dao.MealPlanDao
import com.example.lifetogether.data.local.dao.NoteEntriesDao
import com.example.lifetogether.data.local.dao.RoutineListsDao
import com.example.lifetogether.data.local.dao.UserListsDao
import com.example.lifetogether.data.local.dao.WishListsDao
import com.example.lifetogether.data.local.dao.RecipesDao
import com.example.lifetogether.data.local.dao.TipTrackerDao
import com.example.lifetogether.data.local.dao.UserInformationDao
import com.example.lifetogether.data.model.AlbumEntity
import com.example.lifetogether.data.model.ChecklistEntryEntity
import com.example.lifetogether.data.model.CategoryEntity
import com.example.lifetogether.data.model.FamilyEntity
import com.example.lifetogether.data.model.FamilyMemberEntity
import com.example.lifetogether.data.model.GalleryMediaEntity
import com.example.lifetogether.data.model.GuideProgressEntity
import com.example.lifetogether.data.model.GuideEntity
import com.example.lifetogether.data.model.GroceryListEntity
import com.example.lifetogether.data.model.GrocerySuggestionEntity
import com.example.lifetogether.data.model.MealPlanEntity
import com.example.lifetogether.data.model.RecipeIngredientEntity
import com.example.lifetogether.data.model.RecipeInstructionEntity
import com.example.lifetogether.data.model.NoteEntryEntity
import com.example.lifetogether.data.model.RoutineListEntryEntity
import com.example.lifetogether.data.model.UserListEntity
import com.example.lifetogether.data.model.RecipeEntity
import com.example.lifetogether.data.model.TipEntity
import com.example.lifetogether.data.model.UserEntity
import com.example.lifetogether.data.model.WishListEntryEntity

@Database(
    entities = [
        GroceryListEntity::class,
        GrocerySuggestionEntity::class,
        CategoryEntity::class,
        UserEntity::class,
        FamilyEntity::class,
        FamilyMemberEntity::class,
        RecipeEntity::class,
        RecipeIngredientEntity::class,
        RecipeInstructionEntity::class,
        AlbumEntity::class,
        GalleryMediaEntity::class,
        TipEntity::class,
        GuideEntity::class,
        GuideProgressEntity::class,
        UserListEntity::class,
        RoutineListEntryEntity::class,
        WishListEntryEntity::class,
        NoteEntryEntity::class,
        ChecklistEntryEntity::class,
        MealPlanEntity::class,
    ],
    version = 39,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun groceryListDao(): GroceryListDao
    abstract fun grocerySuggestionsDao(): GrocerySuggestionsDao
    abstract fun userInformationDao(): UserInformationDao
    abstract fun familyInformationDao(): FamilyInformationDao
    abstract fun categoriesDao(): CategoriesDao
    abstract fun recipesDao(): RecipesDao
    abstract fun albumsDao(): AlbumsDao
    abstract fun galleryMediaDao(): GalleryMediaDao
    abstract fun tipTrackerDao(): TipTrackerDao
    abstract fun guidesDao(): GuidesDao
    abstract fun guideProgressDao(): GuideProgressDao
    abstract fun userListsDao(): UserListsDao
    abstract fun routineListsDao(): RoutineListsDao
    abstract fun wishListsDao(): WishListsDao
    abstract fun noteEntriesDao(): NoteEntriesDao
    abstract fun checklistEntriesDao(): ChecklistEntriesDao
    abstract fun mealPlanDao(): MealPlanDao
}
