package com.example.lifetogether.ui.feature.recipes

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.example.lifetogether.domain.model.enums.MeasureType
import com.example.lifetogether.domain.model.recipe.Ingredient
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class AddNewIngredientViewModel @Inject constructor() : ViewModel() {
    var changeMeasureTypeExpanded: Boolean by mutableStateOf(false)
    var ingredient: Ingredient by mutableStateOf(Ingredient())
    var amount: String by mutableStateOf("")

    fun updateIngredient(variable: String, value: String) {
        when (variable) {
            "amount" -> ingredient = ingredient.copy(amount = value.toDoubleOrNull() ?: 0.0)
            "measureType" -> {
                val selectedType = measureTypeList.find { it.unit == value }
                if (selectedType != null) {
                    ingredient = ingredient.copy(measureType = selectedType)
                }
            }
            "itemName" -> ingredient = ingredient.copy(itemName = value)
        }
    }

    val measureTypeList: List<MeasureType> = MeasureType.entries
}
