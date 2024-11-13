package com.example.lifetogether.di

import android.content.Context
import androidx.room.Room
import com.example.lifetogether.data.local.AppDatabase
import com.example.lifetogether.data.local.dao.CategoriesDao
import com.example.lifetogether.data.local.dao.GroceryListDao
import com.example.lifetogether.data.local.dao.GrocerySuggestionsDao
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
            .fallbackToDestructiveMigration()
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
    fun provideCategoriesDao(db: AppDatabase): CategoriesDao {
        return db.categoriesDao()
    }
}
