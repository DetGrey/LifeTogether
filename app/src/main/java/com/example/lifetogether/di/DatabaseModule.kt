package com.example.lifetogether.di

import android.content.Context
import androidx.room.Room
import com.example.lifetogether.data.local.MIGRATION_35_36
import com.example.lifetogether.data.local.MIGRATION_34_35
import com.example.lifetogether.data.local.MIGRATION_23_24
import com.example.lifetogether.data.local.MIGRATION_24_25
import com.example.lifetogether.data.local.MIGRATION_25_26
import com.example.lifetogether.data.local.MIGRATION_27_28
import com.example.lifetogether.data.local.MIGRATION_31_32
import com.example.lifetogether.data.local.MIGRATION_32_33
import com.example.lifetogether.data.local.MIGRATION_33_34
import com.example.lifetogether.data.local.AppDatabase
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
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "life_together_database",
        )
            .allowMainThreadQueries()
            .fallbackToDestructiveMigration(false)
            .addMigrations(
                MIGRATION_23_24,
                MIGRATION_24_25,
                MIGRATION_25_26,
                MIGRATION_27_28,
                MIGRATION_31_32,
                MIGRATION_32_33,
                MIGRATION_33_34,
                MIGRATION_34_35,
                MIGRATION_35_36,
            )
            .build()
    }

    @Provides
    @Singleton
    fun provideGroceryListDao(db: AppDatabase): GroceryListDao {
        return db.groceryListDao()
    }

    @Provides
    @Singleton
    fun provideGrocerySuggestionsDao(db: AppDatabase): GrocerySuggestionsDao {
        return db.grocerySuggestionsDao()
    }

    @Provides
    @Singleton
    fun provideUserInformationDao(db: AppDatabase): UserInformationDao {
        return db.userInformationDao()
    }

    @Provides
    @Singleton
    fun provideFamilyInformationDao(db: AppDatabase): FamilyInformationDao {
        return db.familyInformationDao()
    }

    @Provides
    @Singleton
    fun provideCategoriesDao(db: AppDatabase): CategoriesDao {
        return db.categoriesDao()
    }

    @Provides
    @Singleton
    fun provideRecipesDao(db: AppDatabase): RecipesDao {
        return db.recipesDao()
    }

    @Provides
    @Singleton
    fun provideAlbumsDao(db: AppDatabase): AlbumsDao {
        return db.albumsDao()
    }

    @Provides
    @Singleton
    fun provideGalleryMediaDao(db: AppDatabase): GalleryMediaDao {
        return db.galleryMediaDao()
    }

    @Provides
    @Singleton
    fun provideTipTrackerDao(db: AppDatabase): TipTrackerDao {
        return db.tipTrackerDao()
    }

    @Provides
    @Singleton
    fun provideGuidesDao(db: AppDatabase): GuidesDao {
        return db.guidesDao()
    }

    @Provides
    @Singleton
    fun provideGuideProgressDao(db: AppDatabase): GuideProgressDao {
        return db.guideProgressDao()
    }

    @Provides
    @Singleton
    fun provideUserListsDao(db: AppDatabase): UserListsDao {
        return db.userListsDao()
    }

    @Provides
    @Singleton
    fun provideRoutineListsDao(db: AppDatabase): RoutineListsDao {
        return db.routineListsDao()
    }

    @Provides
    @Singleton
    fun provideWishListsDao(db: AppDatabase): WishListsDao {
        return db.wishListsDao()
    }

    @Provides
    @Singleton
    fun provideNoteEntriesDao(db: AppDatabase): NoteEntriesDao {
        return db.noteEntriesDao()
    }

    @Provides
    @Singleton
    fun provideChecklistEntriesDao(db: AppDatabase): ChecklistEntriesDao {
        return db.checklistEntriesDao()
    }

    @Provides
    @Singleton
    fun provideMealPlanDao(db: AppDatabase): MealPlanDao {
        return db.mealPlanDao()
    }
}
