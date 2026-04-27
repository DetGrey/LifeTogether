package com.example.lifetogether.ui.common.list

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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.lifetogether.R
import com.example.lifetogether.domain.model.Category
import com.example.lifetogether.domain.model.CompletableItem
import com.example.lifetogether.domain.model.grocery.GroceryItem
import com.example.lifetogether.ui.common.ListItem
import com.example.lifetogether.ui.theme.LifeTogetherTheme
import com.example.lifetogether.ui.theme.LifeTogetherTokens
import com.example.lifetogether.ui.theme.bodyFontFamily
import com.example.lifetogether.util.priceToString
import java.util.Date

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
        if (expanded) {
            Spacer(modifier = Modifier.height(LifeTogetherTokens.spacing.small))
            Column {
                itemList.forEach { item ->
                    ListItem(
                        item = item,
                        onCompleteToggle = { onCompleteToggle(item) },
                        trailingText = if (item is GroceryItem && item.approxPrice != null) {
                            (item.approxPrice as Float).priceToString()
                        } else null,
                        onBellClick = if (item is GroceryItem) {
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
        Spacer(modifier = Modifier.height(LifeTogetherTokens.spacing.xLarge))
    }
}

@Preview(showBackground = true)
@Composable
fun ItemCategoryListPreview() {
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
