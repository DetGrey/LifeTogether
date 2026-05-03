package com.example.lifetogether.ui.common.button

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.lifetogether.ui.theme.LifeTogetherTheme

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
) {
    val disabledContentColor = if (loading) MaterialTheme.colorScheme.inverseOnSurface
        else MaterialTheme.colorScheme.inverseSurface

    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled && !loading,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            disabledContainerColor = MaterialTheme.colorScheme.inverseOnSurface,
            disabledContentColor = disabledContentColor,
        ),
    ) {
        Box(
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
            )
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(14.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.inverseSurface,
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PrimaryButtonPreview() {
    LifeTogetherTheme {
        Column {
            PrimaryButton(
                text = "Confirm",
                onClick = {},
            )
            PrimaryButton(
                text = "Confirm",
                onClick = {},
                loading = true
            )
        }
    }
}
