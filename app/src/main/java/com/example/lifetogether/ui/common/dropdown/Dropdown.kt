package com.example.lifetogether.ui.common.dropdown

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import com.example.lifetogether.ui.theme.LifeTogetherTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Dropdown(
    selectedValue: String,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    options: List<String>,
    label: String?,
    onValueChangedEvent: (String) -> Unit,
) {
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { onExpandedChange(it) },
    ) {
        TextField(
            readOnly = true,
            value = selectedValue,
            onValueChange = {},
            label = if (label != null) {
                { Text(label) }
            } else {
                null
            },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
            ),
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = {
                onExpandedChange(false)
            },
        ) {
            options.forEach { option: String ->
                DropdownMenuItem(
                    text = { Text(text = option) },
                    onClick = {
                        onExpandedChange(false)
                        println("new value: $option")
                        onValueChangedEvent(option)
                    },
                )
            }
        }
    }
}

@Preview
@Composable
fun DropdownPreview() {
    LifeTogetherTheme {
        Box(Modifier.fillMaxSize()) {
            Dropdown(
                selectedValue = "",
                expanded = false,
                onExpandedChange = {},
                options = (1..31).map { it.toString() },
                label = "Day",
                onValueChangedEvent = {},
            )
        }
    }
}
