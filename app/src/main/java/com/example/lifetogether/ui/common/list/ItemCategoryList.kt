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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.lifetogether.R
import com.example.lifetogether.domain.logic.toAbbreviatedDateString
import com.example.lifetogether.domain.model.Category
import com.example.lifetogether.domain.model.CompletableItem
import com.example.lifetogether.domain.model.grocery.GroceryItem
import com.example.lifetogether.ui.common.GroceryListItem
import com.example.lifetogether.ui.theme.LifeTogetherTheme
import com.example.lifetogether.ui.theme.LifeTogetherTokens
import com.example.lifetogether.ui.theme.bodyFontFamily
import com.example.lifetogether.util.priceToString
import java.util.Date

@Composable
fun ItemCategoryListHeader(
    category: Category,
    expanded: Boolean,
    onClick: () -> Unit,
    onDelete: (() -> Unit)? = null,
) {
    Column(
        modifier = Modifier
            .padding(horizontal = LifeTogetherTokens.spacing.xSmall)
            .clickable { onClick() },
        verticalArrangement = Arrangement.spacedBy(LifeTogetherTokens.spacing.xSmall),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(LifeTogetherTokens.sizing.iconLarge),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
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
                    color = MaterialTheme.colorScheme.tertiary,
                )
            }
            Row {
                if (onDelete != null) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_trashcan),
                        contentDescription = "trashcan icon",
                        tint = MaterialTheme.colorScheme.error,
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
        HorizontalDivider(thickness = 2.dp, color = MaterialTheme.colorScheme.primaryContainer)
    }
}

@Composable
fun ItemCategoryList(
    category: Category,
    itemList: List<CompletableItem>,
    expanded: Boolean,
    onClick: () -> Unit,
    onCompleteToggle: (CompletableItem) -> Unit,
    onBellClick: ((GroceryItem) -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
) {
    Column {
        ItemCategoryListHeader(
            category = category,
            expanded = expanded,
            onClick = onClick,
            onDelete = onDelete,
        )
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut(),
        ) {
            Column(modifier = Modifier.padding(top = LifeTogetherTokens.spacing.small)) {
                itemList.forEach { item ->
                    GroceryListItem(
                        item = item,
                        onCompleteToggle = { onCompleteToggle(item) },
                        trailingText = if (item.completed) {
                            item.lastUpdated.toAbbreviatedDateString()
                        } else if (item is GroceryItem && item.approxPrice != null) {
                            (item.approxPrice as Float).priceToString()
                        } else null,
                        onBellClick = if (item is GroceryItem && !item.completed) {
                            {
                                onBellClick?.invoke(item)
                            }
                        } else {
                            null
                        },
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ItemCategoryListPreview() {
    LifeTogetherTheme {
        ItemCategoryList(
            category = Category(
                "🍎",
                "Fruits and vegetables",
            ),
            itemList = groceryList,
            true,
            onClick = {},
            onCompleteToggle = {},
        )
    }
}

val groceryList = listOf(
    GroceryItem(
        id = "grocery-1",
        familyId = "dsuaihfao",
        category = Category(
            "🍎",
            "Fruits and vegetables",
        ),
        itemName = "Bananas",
        lastUpdated = Date(System.currentTimeMillis()),
        completed = false,
    ),
    GroceryItem(
        id = "grocery-2",
        familyId = "dsuaihfao",
        category = Category(
            "🍎",
            "Fruits and vegetables",
        ),
        itemName = "Potatoes",
        lastUpdated = Date(System.currentTimeMillis()),
        completed = true,
    ),
)
