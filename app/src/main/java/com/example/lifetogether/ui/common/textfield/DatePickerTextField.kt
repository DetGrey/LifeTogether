package com.example.lifetogether.ui.common.textfield

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.lifetogether.domain.logic.toFullDateString
import com.example.lifetogether.ui.theme.LifeTogetherTheme
import java.util.Date

@Composable
fun DatePickerTextField(
    label: String,
    date: Date?,
    onClick: () -> Unit,
) {
    TextField(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .clip(shape = MaterialTheme.shapes.large)
            .clickable { onClick() },
        value = date?.toFullDateString() ?: "",
        onValueChange = { },
        label = { Text(label) },
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            disabledContainerColor = MaterialTheme.colorScheme.primaryContainer,
            disabledLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
            disabledTextColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
        enabled = false,
    )
}

@Preview
@Composable
fun DatePickerPreview() {
    LifeTogetherTheme {
        DatePickerTextField(label = "Birthday", date = null, onClick = {})
    }
}
