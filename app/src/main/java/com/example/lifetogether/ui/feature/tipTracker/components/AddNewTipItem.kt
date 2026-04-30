package com.example.lifetogether.ui.feature.tipTracker.components

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Surface
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
import com.example.lifetogether.domain.logic.toDayOfMonthString
import com.example.lifetogether.ui.common.dialog.DatePickerDialog
import com.example.lifetogether.ui.common.textfield.CustomTextField
import com.example.lifetogether.ui.theme.LifeTogetherTheme
import com.example.lifetogether.ui.theme.LifeTogetherTokens
import java.util.Date

@Composable
fun AddNewTipItem(
    textValue: String,
    onTextChange: (String) -> Unit,
    onAddClick: () -> Unit,
    dateValue: Date,
    onDateChange: (Date) -> Unit,
) {
    var showDialog by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = MaterialTheme.shapes.medium,
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth(0.8f),
            ) {
                Box(
                    modifier = Modifier
                        .padding(LifeTogetherTokens.spacing.small)
                        .fillMaxHeight()
                        .aspectRatio(1f, true)
                        .border(
                            width = 4.dp,
                            color = MaterialTheme.colorScheme.secondary,
                            shape = MaterialTheme.shapes.small,
                        )
                        .clickable {
                            showDialog = true
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(text = dateValue.toDayOfMonthString(), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

//                VerticalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.secondary, modifier = Modifier.padding(vertical = 10.dp))

                CustomTextField(
                    value = textValue,
                    onValueChange = onTextChange,
                    label = "Add tip amount...",
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done,
                )
            }

            Row(
                modifier = Modifier
                    .padding(LifeTogetherTokens.spacing.small)
                    .fillMaxHeight()
                    .clickable {
                        onAddClick()
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

    if (showDialog) {
        DatePickerDialog(
            selectedDate = dateValue,
            onDismiss = {
                showDialog = false
            },
            onDateSelected = {
                onDateChange(it)
                showDialog = false
            },
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ListEntryCardDailyPreview() {
    LifeTogetherTheme {
        AddNewTipItem(
            textValue = "133",
            onTextChange = {},
            onAddClick = {},
            dateValue = Date(),
            onDateChange = {},
        )
    }
}
