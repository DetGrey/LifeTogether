package com.example.lifetogether.ui.common.add

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.lifetogether.ui.common.textfield.CustomTextField
import com.example.lifetogether.ui.theme.LifeTogetherTheme
import com.example.lifetogether.ui.viewmodel.AddNewListItemViewModel

@Composable
fun AddNewString(
    label: String? = null,
    onAddClick: (String) -> Unit,
) {
    val addNewListItemViewModel: AddNewListItemViewModel = hiltViewModel()

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
                CustomTextField(
                    value = addNewListItemViewModel.textValue,
                    onValueChange = { addNewListItemViewModel.textValue = it },
                    label = label,
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
                        onAddClick(addNewListItemViewModel.textValue)
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
fun AddNewStringPreview() {
    LifeTogetherTheme {
        AddNewString(
            onAddClick = {},
        )
    }
}
