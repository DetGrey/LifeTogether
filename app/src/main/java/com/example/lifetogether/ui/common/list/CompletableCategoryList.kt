package com.example.lifetogether.ui.common.list

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.lifetogether.R
import com.example.lifetogether.domain.model.Category
import com.example.lifetogether.domain.model.Completable
import com.example.lifetogether.ui.common.ListItem
import com.example.lifetogether.ui.theme.LifeTogetherTokens
import com.example.lifetogether.ui.theme.bodyFontFamily

@Composable
fun CompletableCategoryList(
    category: Category,
    itemList: List<Completable>,
    expanded: Boolean,
    onClick: () -> Unit,
    onCompleteToggle: (Completable) -> Unit,
    onDelete: (() -> Unit)? = null,
) {
    Column(modifier = Modifier.padding(bottom = LifeTogetherTokens.spacing.xLarge)) {
        Column(
            modifier = Modifier
                    .padding(horizontal = LifeTogetherTokens.spacing.xSmall)
                .clickable { onClick() },
                verticalArrangement = Arrangement.spacedBy(LifeTogetherTokens.spacing.xSmall),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                        .height(LifeTogetherTokens.sizing.iconMedium),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row {
                    Text(
                        text = category.emoji,
                        style = MaterialTheme.typography.labelLarge,
                        fontFamily = bodyFontFamily,
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = category.name,
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
                Row {
                    if (onDelete != null) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_trashcan),
                            contentDescription = "trashcan icon",
                            tint = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.clickable { onDelete() },
                        )
                    }
                    Icon(
                        painter = painterResource(id = if (expanded) R.drawable.ic_expanded else R.drawable.ic_expand),
                        contentDescription = "expand or expanded icon",
                        tint = MaterialTheme.colorScheme.onBackground,
                    )
                }
            }
            HorizontalDivider(thickness = 2.dp, color = MaterialTheme.colorScheme.primary)
        }
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut(),
        ) {
            Column {
                Spacer(modifier = Modifier.height(LifeTogetherTokens.spacing.small))
                itemList.forEach { item ->
                    ListItem(
                        item = item,
                        onCompleteToggle = { onCompleteToggle(item) },
                        onBellClick = {},
                    )
                }
            }
        }
    }
}
