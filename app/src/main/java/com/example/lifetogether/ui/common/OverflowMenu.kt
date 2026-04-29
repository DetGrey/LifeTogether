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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.lifetogether.ui.common.text.TextDefault
import com.example.lifetogether.ui.theme.LifeTogetherTheme
import com.example.lifetogether.ui.theme.LifeTogetherTokens

data class ActionSheetItem(
    val label: String,
    val onClick: () -> Unit,
    val isDestructive: Boolean = false,
    val isEnabled: Boolean = true,
)

@Composable
fun OverflowMenu(
    onDismiss: () -> Unit,
    actionsList: List<Map<String, () -> Unit>>,
) {
    Box(
        modifier = Modifier
            .padding(top = LifeTogetherTokens.spacing.xxxLarge)
            .fillMaxSize()
            .padding(LifeTogetherTokens.spacing.small)
            .clickable {
                onDismiss()
            },
    ) {
        Box(
            modifier = Modifier
                .width(125.dp)
                .align(Alignment.TopEnd)
                .background(MaterialTheme.colorScheme.onBackground)
                .padding(LifeTogetherTokens.spacing.small),
        ) {
            Column {
                actionsList.forEachIndexed { index, actionMap ->
                    actionMap.forEach { (name, onActionClick) ->
                        TextDefault(
                            text = name,
                            color = MaterialTheme.colorScheme.background,
                            modifier = Modifier.clickable {
                                onActionClick()
                            },
                        )
                        if (index < actionsList.lastIndex) {
                            HorizontalDivider(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = LifeTogetherTokens.spacing.small, bottom = LifeTogetherTokens.spacing.xSmall)
                                    .height(LifeTogetherTokens.spacing.xSmall),
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActionSheet(
    onDismiss: () -> Unit,
    actionsList: List<ActionSheetItem>,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.onBackground,
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = LifeTogetherTokens.spacing.medium, vertical = LifeTogetherTokens.spacing.small)
                .padding(bottom = LifeTogetherTokens.spacing.large + LifeTogetherTokens.spacing.small),
        ) {
            actionsList.forEachIndexed { index, action ->
                val textColor = when {
                    action.isDestructive -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.background
                }

                TextDefault(
                    text = action.label,
                    color = textColor,
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            alpha = if (action.isEnabled) 1f else 0.45f
                        }
                        .clickable(enabled = action.isEnabled) {
                            action.onClick()
                        }
                        .padding(vertical = LifeTogetherTokens.spacing.small + LifeTogetherTokens.spacing.xSmall),
                )

                if (index < actionsList.lastIndex) {
                    HorizontalDivider(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = LifeTogetherTokens.spacing.xSmall)
                            .height(1.dp),
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun OverflowMenuPreview() {
    LifeTogetherTheme {
        Box {
            OverflowMenu(
                onDismiss = {},
                actionsList = listOf(
                    mapOf("Rename" to {}),
                    mapOf("Delete" to {}),
                ),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ActionSheetPreview() {
    LifeTogetherTheme {
        Box {
            ActionSheet(
                onDismiss = {},
                actionsList = listOf(
                    ActionSheetItem(label = "Select entries", onClick = {}),
                    ActionSheetItem(label = "Delete selected", onClick = {}, isDestructive = true),
                ),
            )
        }
    }
}
