package com.example.lifetogether.ui.common

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.lifetogether.ui.theme.LifeTogetherTheme

@Composable
fun CustomTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String?,
    keyboardType: KeyboardType,
    imeAction: ImeAction,
    capitalization: Boolean = false,
) {
    var visualTransformation: VisualTransformation = VisualTransformation.None
    if (keyboardType == KeyboardType.Password) {
        visualTransformation = PasswordVisualTransformation()
    }

    TextField(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .clip(shape = RoundedCornerShape(20)),
        value = value,
        onValueChange = { onValueChange(it) },
        label = if (label != null) {
            { Text(label) }
        } else {
            null
        },
        visualTransformation = visualTransformation,
        keyboardOptions = KeyboardOptions(
            capitalization = if (capitalization) KeyboardCapitalization.Sentences else KeyboardCapitalization.None,
            keyboardType = keyboardType,
            imeAction = imeAction,
        ),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.onBackground,
            unfocusedContainerColor = MaterialTheme.colorScheme.onBackground,
            focusedTextColor = MaterialTheme.colorScheme.background,
            unfocusedTextColor = MaterialTheme.colorScheme.background,
            focusedLabelColor = MaterialTheme.colorScheme.background,
            unfocusedLabelColor = MaterialTheme.colorScheme.background,
            focusedIndicatorColor = MaterialTheme.colorScheme.secondary,
            cursorColor = MaterialTheme.colorScheme.secondary,
        ),
    )
}

@Preview
@Composable
fun CustomTextFieldPreview() {
    LifeTogetherTheme {
        CustomTextField(
            value = "email@email.com",
            onValueChange = {},
            label = "Email",
            keyboardType = KeyboardType.Text,
            imeAction = ImeAction.Next,
        )
    }
}
