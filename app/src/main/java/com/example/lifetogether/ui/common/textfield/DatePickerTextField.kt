package com.example.lifetogether.ui.common.textfield

import androidx.compose.foundation.clickable
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
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
    TextField(
        modifier = modifier
            .inputFieldModifier()
            .clickable { onClick() },
        value = date?.toFullDateString() ?: "",
        onValueChange = { },
        label = { Text(label) },
        colors = filledTextFieldColors(),
        readOnly = true,
    )
}

@Preview
@Composable
fun DatePickerPreview() {
    LifeTogetherTheme {
        DatePickerTextField(label = "Birthday", date = null, onClick = {})
    }
}
