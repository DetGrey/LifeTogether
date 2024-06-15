package com.example.lifetogether.domain.model

import androidx.compose.runtime.Composable

data class ConfirmationDialogDetails(
    val dialogTitle: String,
    val dialogMessage: String,
    val dismissButtonMessage: String = "Cancel",
    val confirmButtonMessage: String,
    val onConfirm: @Composable () -> Unit,
)
