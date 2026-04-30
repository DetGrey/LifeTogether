package com.example.lifetogether.ui.common

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem as M3ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.example.lifetogether.R
import com.example.lifetogether.domain.model.Category
import com.example.lifetogether.domain.model.Completable
import com.example.lifetogether.domain.model.grocery.GroceryItem
import com.example.lifetogether.domain.model.recipe.Ingredient
import com.example.lifetogether.ui.common.list.CompletableBox
import com.example.lifetogether.ui.theme.LifeTogetherTheme
import com.example.lifetogether.ui.theme.LifeTogetherTokens
import java.text.DecimalFormat
import java.util.Date

@Composable
fun ListItem(
    item: Completable,
    onCompleteToggle: () -> Unit,
    trailingText: String? = null,
    onBellClick: (() -> Unit)? = null,
) {
    var text = item.itemName

    if (item is Ingredient && item.amount > 0) {
        val formattedAmount = if (item.amount % 1.0 == 0.0) {
            item.amount.toInt()
        } else {
            DecimalFormat("#.##").format(item.amount)
        }
        text = "$formattedAmount ${item.measureType.unit} ${item.itemName}"
    }

    M3ListItem(
        modifier = Modifier
            .padding(
                horizontal = LifeTogetherTokens.spacing.small,
                vertical = LifeTogetherTokens.spacing.xSmall
            )
            .fillMaxWidth(),
        leadingContent = {
            CompletableBox(
                isCompleted = item.completed,
                onCompleteToggle = onCompleteToggle,
            )
        },
        headlineContent = {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                textDecoration = if (item.completed) TextDecoration.LineThrough else TextDecoration.None,
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
            )
        },
        trailingContent = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                trailingText?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyLarge,
                        textDecoration = if (item.completed) TextDecoration.LineThrough else TextDecoration.None,
                    )
                }

                if (item is GroceryItem && onBellClick != null) {
                    IconButton(
                        onClick = onBellClick,
                        modifier = Modifier.padding(start = LifeTogetherTokens.spacing.xSmall),
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_bell),
                            contentDescription = "bell notification icon",
                            tint = MaterialTheme.colorScheme.onBackground,
                        )
                    }
                }
            }
        },
        colors = ListItemDefaults.colors(
            containerColor = Color.Transparent,
        ),
    )
}

@Preview(showBackground = true)
@Composable
fun ListItemPreview() {
    LifeTogetherTheme {
        Column {
            ListItem(
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
                trailingText = "9 kr.",
                onCompleteToggle = {},
                onBellClick = {}
            )
            ListItem(
                GroceryItem(
                    category = Category(
                        "🍎",
                        "Fruits and vegetables",
                    ),
                    itemName = "Tomato",
                    lastUpdated = Date(System.currentTimeMillis()),
                    completed = false,
                ),
                trailingText = "9 kr.",
                onCompleteToggle = {},
                onBellClick = {}
            )
        }
    }
}
