package com.example.lifetogether.ui.feature.recipes

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.lifetogether.domain.model.enums.MeasureType
import com.example.lifetogether.domain.model.recipe.Ingredient
import com.example.lifetogether.ui.common.dropdown.Dropdown
import com.example.lifetogether.ui.common.textfield.CustomTextField
import com.example.lifetogether.ui.theme.LifeTogetherTheme
import com.example.lifetogether.ui.theme.LifeTogetherTokens

@Composable
fun AddNewIngredient(
    onAddClick: (Ingredient) -> Unit,
) {
    val measureTypeList = remember { MeasureType.entries }
    var changeMeasureTypeExpanded by remember { mutableStateOf(false) }
    var ingredient by remember { mutableStateOf(Ingredient()) }
    var amount by remember { mutableStateOf("") }

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

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
        ),
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = LifeTogetherTokens.spacing.medium)
                .padding(bottom = LifeTogetherTokens.spacing.small),
        ) {
            CustomTextField(
                value = ingredient.itemName,
                onValueChange = { updateIngredient("itemName", it) },
                label = "Ingredient name...",
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Done,
            )

            Row {
                CustomTextField(
                    value = amount,
                    onValueChange = {
                        amount = it
                    },
                    label = "Amount",
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done,
                    modifier = Modifier.weight(1f)
                )

                Dropdown(
                    selectedValue = ingredient.measureType.unit,
                    expanded = changeMeasureTypeExpanded,
                    onExpandedChange = {
                        changeMeasureTypeExpanded = it
                    },
                    options = measureTypeList.map { it.unit },
                    label = null,
                    onValueChangedEvent = {
                        updateIngredient("measureType", it)
                    },
                    modifier = Modifier.weight(1f)
                )
                Row(
                    modifier = Modifier
                        .padding(LifeTogetherTokens.spacing.small)
                        .fillMaxHeight()
                        .clickable {
                            updateIngredient(
                                "amount",
                                amount,
                            )
                            onAddClick(ingredient)
                            ingredient = Ingredient()
                            amount = ""
                        },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(text = "Add", color = MaterialTheme.colorScheme.secondary)

                    Spacer(modifier = Modifier.width(LifeTogetherTokens.spacing.xSmall))

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
fun Preview() {
    LifeTogetherTheme {
        AddNewIngredient(
            onAddClick = {}
        )
    }
}
