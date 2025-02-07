package com.example.lifetogether.ui.common.list

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.lifetogether.R
import com.example.lifetogether.domain.logic.groceryListNotificationOptions
import com.example.lifetogether.domain.model.Category
import com.example.lifetogether.domain.model.CompletableItem
import com.example.lifetogether.domain.model.grocery.GroceryItem
import com.example.lifetogether.ui.common.ListItem
import com.example.lifetogether.ui.navigation.AppRoutes
import com.example.lifetogether.ui.theme.LifeTogetherTheme
import com.example.lifetogether.ui.theme.bodyFontFamily
import com.example.lifetogether.ui.viewmodel.NotificationViewModel
import com.example.lifetogether.util.Constants
import java.util.Date

@Composable
fun ItemCategoryList(
    category: Category,
    itemList: List<CompletableItem>,
    expanded: Boolean,
    onClick: () -> Unit,
    onCompleteToggle: (CompletableItem) -> Unit,
    onDelete: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    val notificationViewModel: NotificationViewModel = hiltViewModel()
    Column {
        Column(
            modifier = Modifier
                .padding(horizontal = 5.dp)
                .clickable { onClick() },
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(25.dp),
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
                        Image(
                            painter = painterResource(id = R.drawable.ic_trashcan_black),
                            contentDescription = "trashcan icon",
                            modifier = Modifier.clickable { onDelete() },
                        )
                    }
                    Image(
                        painter = painterResource(id = if (expanded) R.drawable.ic_expanded else R.drawable.ic_expand),
                        contentDescription = "expand or expanded icon",
                    )
                }
            }
            HorizontalDivider(thickness = 2.dp, color = MaterialTheme.colorScheme.primary)
        }
        if (expanded) {
            Spacer(modifier = Modifier.height(10.dp))
            Column {
                itemList.forEach { item ->
                    ListItem(
                        item = item,
                        onCompleteToggle = { onCompleteToggle(item) },
                        onBellClick = if (item is GroceryItem) {
                            {
                                println("bell clicked")
                                val option = groceryListNotificationOptions(item.itemName, item.category?.emoji ?: "")

                                notificationViewModel.sendNotification(
                                    context,
                                    familyId = item.familyId,
                                    title = option.title,
                                    message = option.message,
                                    channelId = Constants.GROCERY_LIST_CHANNEL,
                                    destination = AppRoutes.GROCERY_LIST_SCREEN,
                                )
                            }
                        } else {
                            null
                        },
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(30.dp))
    }
}

@RequiresApi(Build.VERSION_CODES.S)
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
