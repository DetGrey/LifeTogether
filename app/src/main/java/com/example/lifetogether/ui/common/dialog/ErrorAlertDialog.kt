package com.example.lifetogether.ui.common.dialog

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

@Composable
fun ErrorAlertDialog(error: String) {
    AlertDialog(
        onDismissRequest = { },
        title = { Text(text = "An error occurred") },
        text = { Text(text = error) },
        confirmButton = { }
    )
}