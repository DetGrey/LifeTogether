package com.example.lifetogether.ui.common.dialog

import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import java.time.LocalDate
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomDatePickerDialog(
    selectedDate: Date?,
    onDismiss: () -> Unit,
    onDateSelected: (Date) -> Unit,
) {
    val currentYear = LocalDate.now().year

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = selectedDate?.time,
        initialDisplayedMonthMillis = selectedDate?.time ?: System.currentTimeMillis(),
        yearRange = 1920..currentYear,
    )

    DatePickerDialog(
        onDismissRequest = { onDismiss() },
        confirmButton = {
            TextButton(
                onClick = {
                    onDateSelected(Date(datePickerState.selectedDateMillis ?: System.currentTimeMillis()))
                },
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
        DatePicker(state = datePickerState)
    }
}
