package com.example.lifetogether.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.lifetogether.ui.theme.LifeTogetherTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DarkDropdown(
    selectedValue: String,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    options: List<String>,
    label: String? = null,
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
                focusedContainerColor = MaterialTheme.colorScheme.onBackground,
                unfocusedContainerColor = MaterialTheme.colorScheme.onBackground,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
            ),
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable, true)
                .fillMaxSize(),
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = {
                onExpandedChange(false)
            },
            modifier = Modifier.background(MaterialTheme.colorScheme.onBackground),
        ) {
            options.forEach { option: String ->
                DropdownMenuItem(
                    text = { Text(text = option, color = Color.White) },
                    onClick = {
                        onExpandedChange(false)
                        println("new value: $option")
                        onValueChangedEvent(option)
                    },
                    modifier = Modifier.background(MaterialTheme.colorScheme.onBackground),
                )
            }
        }
    }
}

@Preview
@Composable
fun DarkDropdownPreview() {
    LifeTogetherTheme {
        Box(Modifier.fillMaxWidth().height(60.dp)) {
            DarkDropdown(
                selectedValue = "g",
                expanded = false,
                onExpandedChange = {},
                options = (1..31).map { it.toString() },
                onValueChangedEvent = {},
            )
        }
    }
}
