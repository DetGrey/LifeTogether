package com.example.lifetogether.ui.feature.tipTracker.components

import androidx.compose.foundation.background
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
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.lifetogether.domain.logic.toDayOfMonthString
import com.example.lifetogether.ui.common.add.AddNewListItemViewModel
import com.example.lifetogether.ui.common.dialog.CustomDatePickerDialog
import com.example.lifetogether.ui.common.textfield.CustomTextField
import java.util.Date

@Composable
fun AddNewTipItem(
    textValue: String,
    onTextChange: (String) -> Unit,
    onAddClick: () -> Unit,
    dateValue: Date,
    onDateChange: (Date) -> Unit,
) {
    val addNewListItemViewModel: AddNewListItemViewModel = hiltViewModel()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .clip(shape = MaterialTheme.shapes.large)
            .background(color = MaterialTheme.colorScheme.onBackground),
        contentAlignment = Alignment.CenterStart,
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
                        .padding(10.dp)
                        .fillMaxHeight()
                        .aspectRatio(1f, true)
                        .border(
                            width = 4.dp,
                            color = MaterialTheme.colorScheme.secondary,
                            shape = RoundedCornerShape(10.dp),
                        )
                        .clickable {
                            addNewListItemViewModel.showDialog = true
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(text = dateValue.toDayOfMonthString(), color = Color.White)
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
                    .padding(10.dp)
                    .fillMaxHeight()
                    .clickable {
                        onAddClick()
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

    if (addNewListItemViewModel.showDialog) {
        CustomDatePickerDialog(
            selectedDate = dateValue,
            onDismiss = {
                addNewListItemViewModel.showDialog = false
            },
            onDateSelected = {
                onDateChange(it)
                addNewListItemViewModel.showDialog = false
            },
        )
    }
}
