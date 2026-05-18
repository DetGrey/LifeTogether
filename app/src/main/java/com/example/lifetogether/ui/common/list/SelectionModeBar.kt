package com.example.lifetogether.ui.common.list

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.lifetogether.ui.common.text.TextDefault
import com.example.lifetogether.ui.theme.LifeTogetherTokens

@Composable
fun SelectionModeBar(
    selectedCount: Int,
    isAllSelected: Boolean,
    onToggleAll: () -> Unit,
    onCancel: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                top = LifeTogetherTokens.spacing.small,
                bottom = LifeTogetherTokens.spacing.medium,
            ),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(LifeTogetherTokens.spacing.xSmall),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CompletableBox(
                isCompleted = isAllSelected,
                onCompleteToggle = onToggleAll,
            )
            TextDefault(text = "All")
        }

        TextDefault(text = "$selectedCount selected")

        TextDefault(
            text = "Cancel",
            modifier = Modifier.combinedClickable(
                onClick = onCancel,
                onLongClick = onCancel,
            ),
        )
    }
}
