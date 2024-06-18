package com.example.lifetogether.di

import android.content.Context
import androidx.room.Room
import com.example.lifetogether.data.local.AppDatabase
import com.example.lifetogether.data.local.GroceryListDao
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
            "database_name",
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

    // Example of providing another DAO
//    @Provides
//    @Singleton
//    fun provideAnotherDao(db: AppDatabase): AnotherDao {
//        return db.anotherDao()
//    }
}
