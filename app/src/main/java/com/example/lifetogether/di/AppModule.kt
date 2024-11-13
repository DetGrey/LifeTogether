package com.example.lifetogether.di

import com.example.lifetogether.domain.usecase.observers.ObserveCategoriesUseCase
import com.example.lifetogether.domain.usecase.observers.ObserveGroceryListUseCase
import com.example.lifetogether.domain.usecase.observers.ObserveGrocerySuggestionsUseCase
import com.example.lifetogether.domain.usecase.observers.ObserveUserInformationUseCase
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
    fun provideObserverViewModel(
        observeGroceryListUseCase: ObserveGroceryListUseCase,
        observeGrocerySuggestionsUseCase: ObserveGrocerySuggestionsUseCase,
        observeCategoriesUseCase: ObserveCategoriesUseCase,
        observeUserInformationUseCase: ObserveUserInformationUseCase,
    ): ObserverViewModel {
        return ObserverViewModel(
            observeGroceryListUseCase,
            observeGrocerySuggestionsUseCase,
            observeCategoriesUseCase,
            observeUserInformationUseCase,
        ) // ViewModel constructor with dependencies
    }
}
