package com.example.lifetogether.ui.common.textfield

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.lifetogether.domain.converter.formatDateToString
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
            .clip(shape = RoundedCornerShape(20))
            .clickable { onClick() },
        value = date?.let { formatDateToString(it) } ?: "",
        onValueChange = { },
        label = { Text(label) },
        colors = TextFieldDefaults.colors(
            disabledContainerColor = MaterialTheme.colorScheme.onBackground,
            disabledLabelColor = MaterialTheme.colorScheme.background,
            disabledTextColor = MaterialTheme.colorScheme.background,
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
