package com.example.lifetogether.ui.common.list

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import com.example.lifetogether.R
import com.example.lifetogether.ui.common.text.TextHeadingMedium
import com.example.lifetogether.ui.theme.LifeTogetherTokens

@Composable
fun CompletedSectionHeader(
    text: String,
    count: Int,
    expanded: Boolean,
    onToggle: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onToggle, onLongClick = onToggle),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(LifeTogetherTokens.sizing.iconLarge),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextHeadingMedium(text = "$text ($count)")
            Icon(
                painter = painterResource(id = if (expanded) R.drawable.ic_expanded else R.drawable.ic_expand),
                contentDescription = if (expanded) "collapse completed" else "expand completed",
                tint = MaterialTheme.colorScheme.onBackground,
            )
        }
        HorizontalDivider(
            modifier = Modifier.padding(top = LifeTogetherTokens.spacing.xSmall),
            color = MaterialTheme.colorScheme.primary,
        )
    }
}