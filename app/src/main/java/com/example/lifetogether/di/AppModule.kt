package com.example.lifetogether.di

import com.example.lifetogether.domain.usecase.item.ObserveGroceryListUseCase
import com.example.lifetogether.ui.viewmodel.ObserverViewModel
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Singleton
    @Provides
    fun provideGroceryListViewModel(
        // Replace with actual dependencies
        observeGroceryListUseCase: ObserveGroceryListUseCase,
    ): ObserverViewModel {
//        return ObserverViewModel(someDependency) // ViewModel constructor with dependencies
        return ObserverViewModel(observeGroceryListUseCase) // ViewModel constructor with dependencies
    }
}
