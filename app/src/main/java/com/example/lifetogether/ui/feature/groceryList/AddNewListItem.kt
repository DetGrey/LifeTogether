package com.example.lifetogether.ui.feature.groceryList

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.lifetogether.ui.common.CustomTextField
import com.example.lifetogether.ui.theme.LifeTogetherTheme

@Composable
fun AddNewListItem(
    textValue: String,
    onTextChange: (String) -> Unit,
    onAddClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .clip(shape = RoundedCornerShape(20))
            .background(color = MaterialTheme.colorScheme.onBackground),
        contentAlignment = Alignment.CenterStart,
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth(0.8f),
            ) {
                Box(
                    modifier = Modifier // TODO make clickable category icon to choose category instead of circle
                        .padding(15.dp)
                        .fillMaxHeight()
                        .aspectRatio(1f, true)
                        .clip(shape = CircleShape)
                        .border(
                            width = 2.dp,
                            color = MaterialTheme.colorScheme.secondary,
                            shape = CircleShape,
                        ),
                )

                CustomTextField(
                    value = textValue,
                    onValueChange = onTextChange,
                    label = "Add an item...",
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Done,
                    capitalization = true,
                )
            }

            Row(
                modifier = Modifier
                    .padding(10.dp)
                    .fillMaxHeight()
                    .clickable {
                        onAddClick()
                    },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(text = "Add", color = Color.White)

                Spacer(modifier = Modifier.width(5.dp))

                Text(
                    text = ">",
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AddNewListItemPreview() {
    LifeTogetherTheme {
        AddNewListItem(
            textValue = "hello",
            onTextChange = {},
            onAddClick = {},
        )
    }
}
