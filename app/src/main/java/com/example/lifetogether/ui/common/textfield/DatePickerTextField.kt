package com.example.lifetogether.ui.common.textfield

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.lifetogether.domain.logic.toFullDateString
import com.example.lifetogether.ui.theme.LifeTogetherTheme
import java.util.Date

@Composable
fun DatePickerTextField(
    label: String,
    date: Date?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier.clickable(
            interactionSource = interactionSource,
            indication = null,
        ) {
            onClick()
        },
    ) {
        TextField(
            modifier = Modifier.fillMaxWidth().inputFieldModifier(),
            value = date?.toFullDateString() ?: "",
            onValueChange = { },
            label = { Text(label) },
            colors = filledTextFieldColors(),
            readOnly = true,
            enabled = false,
        )
    }
}

@Preview
@Composable
private fun DatePickerPreview() {
    LifeTogetherTheme {
        DatePickerTextField(label = "Birthday", date = null, onClick = {})
    }
}
