package com.example.lifetogether.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.lifetogether.ui.common.text.TextDefault

@Composable
fun OverflowMenu(
    onDismiss: () -> Unit,
    actionsList: List<Map<String, () -> Unit>>,
) {
    Box(
        modifier = Modifier
            .padding(top = 50.dp)
            .fillMaxSize()
            .padding(10.dp)
            .clickable {
                onDismiss()
            },
    ) {
        Box(
            modifier = Modifier
                .width(125.dp)
                .align(Alignment.TopEnd)
//                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.onBackground)
                .padding(10.dp),
        ) {
            var currentlyClickedItemName: String? by remember { mutableStateOf(null) }

            Column {
                actionsList.forEachIndexed { index, actionMap ->
                    actionMap.forEach { (name, onActionClick) ->
                        val isThisItemSelected = currentlyClickedItemName == name
                        val textColor = if (isThisItemSelected) MaterialTheme.colorScheme.tertiary else Color.White

                        TextDefault(
                            text = name,
                            color = textColor,
                            modifier = Modifier
                                .clickable {
                                    currentlyClickedItemName = name
                                    onActionClick()
                                },
                        )
                        // Add a Spacer or Divider if you have multiple items and want separation
                        if (index < actionsList.size - 1) {
                            HorizontalDivider(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 10.dp, bottom = 5.dp)
                                    .height(5.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}
