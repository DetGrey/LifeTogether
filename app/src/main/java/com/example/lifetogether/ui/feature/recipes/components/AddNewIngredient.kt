package com.example.lifetogether.ui.feature.recipes.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import com.example.lifetogether.domain.model.enums.MeasureType
import com.example.lifetogether.ui.common.dropdown.Dropdown
import com.example.lifetogether.ui.common.textfield.CustomTextField
import com.example.lifetogether.ui.theme.LifeTogetherTheme
import com.example.lifetogether.ui.theme.LifeTogetherTokens

@Composable
fun AddNewIngredient(
    modifier: Modifier = Modifier,
    itemName: String,
    onItemNameChange: (String) -> Unit,
    amount: String,
    onAmountChange: (String) -> Unit,
    measureType: MeasureType,
    onMeasureTypeChange: (MeasureType) -> Unit,
    actionLabel: String = "Add",
    onActionClick: () -> Unit,
) {
    val measureTypeList = remember { MeasureType.entries }
    var changeMeasureTypeExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
        ),
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = LifeTogetherTokens.spacing.medium)
                .padding(bottom = LifeTogetherTokens.spacing.small),
        ) {
            CustomTextField(
                value = itemName,
                onValueChange = onItemNameChange,
                label = "Ingredient name...",
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Done,
            )

            Row(Modifier.height(IntrinsicSize.Min)) {
                CustomTextField(
                    value = amount,
                    onValueChange = onAmountChange,
                    label = "Amount",
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done,
                    modifier = Modifier.weight(1f)
                )

                Dropdown(
                    selectedValue = measureType.unit,
                    expanded = changeMeasureTypeExpanded,
                    onExpandedChange = {
                        changeMeasureTypeExpanded = it
                    },
                    options = measureTypeList.map { it.unit },
                    label = null,
                    onValueChangedEvent = {
                        measureTypeList.find { measureTypeEntry -> measureTypeEntry.unit == it }?.let(onMeasureTypeChange)
                    },
                    modifier = Modifier.weight(1f)
                )
                Row(
                    modifier = Modifier
                        .padding(horizontal = LifeTogetherTokens.spacing.small)
                        .fillMaxHeight()
                        .clickable(
                            enabled = itemName.isNotBlank() && amount.toDoubleOrNull() != null,
                        ) {
                            onActionClick()
                        },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(text = actionLabel, color = MaterialTheme.colorScheme.secondary)

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
private fun Preview() {
    LifeTogetherTheme {
        AddNewIngredient(
            itemName = "Tomatoes",
            onItemNameChange = {},
            amount = "3",
            onAmountChange = {},
            measureType = MeasureType.PIECE,
            onMeasureTypeChange = {},
            onActionClick = {},
        )
    }
}
