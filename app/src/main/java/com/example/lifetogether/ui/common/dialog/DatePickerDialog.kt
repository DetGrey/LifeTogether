package com.example.lifetogether.ui.common.dialog

import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import com.example.lifetogether.ui.common.button.PrimaryButton
import com.example.lifetogether.ui.common.button.SecondaryButton
import java.time.LocalDate
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerDialog(
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
        onDismissRequest = onDismiss,
        confirmButton = {
            PrimaryButton(
                text = "Confirm",
                enabled = datePickerState.selectedDateMillis != null,
                onClick = {
                    onDateSelected(Date(datePickerState.selectedDateMillis ?: System.currentTimeMillis()))
                },
            )
        },
        dismissButton = {
            SecondaryButton(
                text = "Dismiss",
                onClick = onDismiss,
            )
        },
    ) {
        DatePicker(state = datePickerState)
    }
}
