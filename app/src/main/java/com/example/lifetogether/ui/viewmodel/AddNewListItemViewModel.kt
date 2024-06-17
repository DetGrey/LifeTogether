package com.example.lifetogether.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

class AddNewListItemViewModel : ViewModel() {
    var changeCategoryExpanded: Boolean by mutableStateOf(false)
    var showDialog: Boolean by mutableStateOf(false)
    var selectedCategory: String by mutableStateOf("")
    var categoryOptions: List<String> by mutableStateOf(listOf())
}
