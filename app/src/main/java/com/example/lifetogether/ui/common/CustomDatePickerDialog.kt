package com.example.lifetogether.ui.common

import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomDatePickerDialog(
    selectedDate: Date?,
    onDismiss: () -> Unit,
    onDateSelected: (Date) -> Unit,
) {
    val datePickerDialogViewModel: DatePickerDialogViewModel = viewModel()

    val datePickerState =
        rememberDatePickerState(
            initialSelectedDateMillis = selectedDate?.time,
            initialDisplayedMonthMillis = selectedDate?.time ?: System.currentTimeMillis(),
            yearRange = 1950..2024,
        )

    DatePickerDialog(
        onDismissRequest = { onDismiss() },
        confirmButton = {
            TextButton(
                onClick = { onDateSelected(Date(datePickerDialogViewModel.selectedDate)) },
                enabled = datePickerState.selectedDateMillis != null,
            ) {
                Text(text = "Confirm")
            }
        },
        dismissButton = {
            TextButton(onClick = { onDismiss() }) {
                Text(text = "Dismiss")
            }
        },
    ) {
        // Setting the selected date
        datePickerState.selectedDateMillis?.let { millis ->
            datePickerDialogViewModel.selectedDate = millis
        }
        DatePicker(state = datePickerState)
    }
}

class DatePickerDialogViewModel : ViewModel() {
    var selectedDate by mutableLongStateOf(System.currentTimeMillis())
}
