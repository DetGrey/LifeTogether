package com.example.lifetogether.di

import com.example.lifetogether.data.repository.CategoryRepositoryImpl
import com.example.lifetogether.data.repository.GroceryRepositoryImpl
import com.example.lifetogether.data.repository.LocalListRepositoryImpl
import com.example.lifetogether.data.repository.UserRepositoryImpl
import com.example.lifetogether.data.repository.RemoteAdminRepositoryImpl
import com.example.lifetogether.data.repository.RemoteListRepositoryImpl
import com.example.lifetogether.data.repository.UserListRepositoryImpl
import com.example.lifetogether.domain.repository.AdminRepository
import com.example.lifetogether.domain.repository.CategoryRepository
import com.example.lifetogether.domain.repository.GroceryRepository
import com.example.lifetogether.domain.repository.LegacyListRepository
import com.example.lifetogether.domain.repository.UserListRepository
import com.example.lifetogether.domain.repository.UserRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent

@Module
@InstallIn(ViewModelComponent::class)
abstract class RepositoryModule {

    @Binds
    abstract fun bindRemoteAdminRepository(
        remoteAdminRepositoryImpl: RemoteAdminRepositoryImpl,
    ): AdminRepository

    @Binds
    abstract fun bindRemoteListRepository(
        remoteListRepositoryImpl: RemoteListRepositoryImpl,
    ): LegacyListRepository

    @Binds
    abstract fun bindLocalListRepository(
        localListRepositoryImpl: LocalListRepositoryImpl,
    ): LegacyListRepository

    @Binds
    abstract fun bindUserRepository(
        userRepositoryImpl: UserRepositoryImpl,
    ): UserRepository

    @Binds
    abstract fun bindCategoryRepository(
        categoryRepositoryImpl: CategoryRepositoryImpl,
    ): CategoryRepository

    @Binds
    abstract fun bindGroceryRepository(
        groceryRepositoryImpl: GroceryRepositoryImpl
    ): GroceryRepository

    @Binds
    abstract fun bindUserListRepository(
        userListRepositoryImpl: UserListRepositoryImpl
    ): UserListRepository
}
