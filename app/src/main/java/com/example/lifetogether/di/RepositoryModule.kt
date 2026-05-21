package com.example.lifetogether.di

import com.example.lifetogether.data.repository.FamilyRepositoryImpl
import com.example.lifetogether.data.repository.GalleryRepositoryImpl
import com.example.lifetogether.data.repository.GroceryRepositoryImpl
import com.example.lifetogether.data.repository.GuideRepositoryImpl
import com.example.lifetogether.data.repository.ImageRepositoryImpl
import com.example.lifetogether.data.repository.MealPlannerRepositoryImpl
import com.example.lifetogether.data.repository.RecipeRepositoryImpl
import com.example.lifetogether.data.repository.TipTrackerRepositoryImpl
import com.example.lifetogether.data.repository.UserListRepositoryImpl
import com.example.lifetogether.data.repository.UserRepositoryImpl
import com.example.lifetogether.domain.repository.FamilyRepository
import com.example.lifetogether.domain.repository.GalleryRepository
import com.example.lifetogether.domain.repository.GroceryRepository
import com.example.lifetogether.domain.repository.GuideRepository
import com.example.lifetogether.domain.repository.ImageRepository
import com.example.lifetogether.domain.repository.MealPlannerRepository
import com.example.lifetogether.domain.repository.RecipeRepository
import com.example.lifetogether.domain.repository.TipTrackerRepository
import com.example.lifetogether.domain.repository.UserListRepository
import com.example.lifetogether.domain.repository.UserRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Singleton
    @Binds
    abstract fun bindUserRepository(
        userRepositoryImpl: UserRepositoryImpl,
    ): UserRepository

    @Singleton
    @Binds
    abstract fun bindGroceryRepository(
        groceryRepositoryImpl: GroceryRepositoryImpl
    ): GroceryRepository

    @Singleton
    @Binds
    abstract fun bindUserListRepository(
        userListRepositoryImpl: UserListRepositoryImpl
    ): UserListRepository

    @Singleton
    @Binds
    abstract fun bindRecipeRepository(
        recipeRepositoryImpl: RecipeRepositoryImpl
    ): RecipeRepository

    @Singleton
    @Binds
    abstract fun bindGuideRepository(
        guideRepositoryImpl: GuideRepositoryImpl
    ): GuideRepository

    @Singleton
    @Binds
    abstract fun bindTipTrackerRepository(
        tipTrackerRepositoryImpl: TipTrackerRepositoryImpl
    ): TipTrackerRepository

    @Singleton
    @Binds
    abstract fun bindGalleryRepository(
        galleryRepositoryImpl: GalleryRepositoryImpl
    ): GalleryRepository

    @Singleton
    @Binds
    abstract fun bindFamilyRepository(
        familyRepositoryImpl: FamilyRepositoryImpl
    ): FamilyRepository

    @Singleton
    @Binds
    abstract fun bindImageRepository(
        imageRepositoryImpl: ImageRepositoryImpl
    ): ImageRepository

    @Singleton
    @Binds
    abstract fun bindMealPlannerRepository(
        mealPlannerRepositoryImpl: MealPlannerRepositoryImpl
    ): MealPlannerRepository
}
