package com.example.lifetogether.ui.common.add

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.lifetogether.ui.common.textfield.CustomTextField
import com.example.lifetogether.ui.theme.LifeTogetherTokens

@Composable
fun AddNewString(
    label: String? = null,
    onAddClick: (String) -> Unit,
    ) {
    var textValue by rememberSaveable { mutableStateOf("") }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.onBackground),
        shape = MaterialTheme.shapes.large,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = LifeTogetherTokens.spacing.small),
            horizontalArrangement = Arrangement.spacedBy(LifeTogetherTokens.spacing.small),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CustomTextField(
                modifier = Modifier
                    .weight(1f),
                value = textValue,
                onValueChange = { textValue = it },
                label = label,
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Done,
                capitalization = true,
            )

            Row(
                modifier = Modifier
                    .padding(LifeTogetherTokens.spacing.small)
                    .clickable {
                        onAddClick(textValue)
                        textValue = ""
                    },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(text = "Add", color = MaterialTheme.colorScheme.onBackground)

                Spacer(modifier = Modifier.width(LifeTogetherTokens.spacing.xSmall))

                Text(
                    text = ">",
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
        }
    }
}
