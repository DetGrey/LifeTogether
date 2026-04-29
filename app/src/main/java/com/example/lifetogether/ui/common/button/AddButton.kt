package com.example.lifetogether.ui.common.button

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.lifetogether.R
import com.example.lifetogether.ui.theme.LifeTogetherTheme

@Composable
fun AddButton(
    onClick: () -> Unit,
) {
    FloatingActionButton(
        onClick = onClick,
        modifier = Modifier.size(60.dp),
        containerColor = MaterialTheme.colorScheme.tertiary,
        contentColor = MaterialTheme.colorScheme.onTertiary,
        shape = CircleShape,
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_plus),
            contentDescription = "Add",
        )
    }
}

@Preview(showBackground = true)
@Composable
fun AddButtonPreview() {
    LifeTogetherTheme {
        AddButton(onClick = {})
    }
}
