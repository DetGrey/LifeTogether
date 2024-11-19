package com.example.lifetogether.ui.feature.recipes

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.lifetogether.domain.model.recipe.Ingredient
import com.example.lifetogether.ui.common.textfield.CustomTextField
import com.example.lifetogether.ui.common.dropdown.DarkDropdown
import com.example.lifetogether.ui.theme.LifeTogetherTheme

@Composable
fun AddNewIngredient(
    onAddClick: (Ingredient) -> Unit,
) {
    val addNewIngredientViewModel: AddNewIngredientViewModel = hiltViewModel()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(130.dp)
            .clip(shape = RoundedCornerShape(20))
            .background(color = MaterialTheme.colorScheme.onBackground),
        contentAlignment = Alignment.CenterStart,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),

        ) {
            Row(
                modifier = Modifier
                    .fillMaxHeight(0.5f),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth(0.5f),
                ) {
                    CustomTextField(
                        value = addNewIngredientViewModel.amount,
                        onValueChange = {
                            addNewIngredientViewModel.amount = it
                        },
                        label = "Amount",
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done,
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth(0.7f),
                ) {
                    DarkDropdown(
                        selectedValue = addNewIngredientViewModel.ingredient.measureType.unit,
                        expanded = addNewIngredientViewModel.changeMeasureTypeExpanded,
                        onExpandedChange = {
                            addNewIngredientViewModel.changeMeasureTypeExpanded = it
                        },
                        options = addNewIngredientViewModel.measureTypeList.map { it.unit },
                        label = null,
                        onValueChangedEvent = {
                            addNewIngredientViewModel.updateIngredient("measureType", it)
                        },
                    )
                }
            }

            Row {
                Row(
                    modifier = Modifier
                        .fillMaxWidth(0.8f),
                ) {
                    CustomTextField(
                        value = addNewIngredientViewModel.ingredient.itemName,
                        onValueChange = { addNewIngredientViewModel.updateIngredient("itemName", it) },
                        label = "Ingredient name...",
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Done,
                    )
                }

                Row(
                    modifier = Modifier
                        .padding(10.dp)
                        .fillMaxHeight()
                        .clickable {
                            addNewIngredientViewModel.updateIngredient(
                                "amount",
                                addNewIngredientViewModel.amount,
                            )
                            onAddClick(addNewIngredientViewModel.ingredient)
                        },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(text = "Add", color = Color.White)

                    Spacer(modifier = Modifier.width(5.dp))

                    Text(
                        text = ">",
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AddNewIngredientPreview() {
    LifeTogetherTheme {
        AddNewIngredient(
            onAddClick = {},
        )
    }
}
