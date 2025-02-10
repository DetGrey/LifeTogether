package com.example.lifetogether.ui.common

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.lifetogether.R
import com.example.lifetogether.domain.model.Category
import com.example.lifetogether.domain.model.Completable
import com.example.lifetogether.domain.model.grocery.GroceryItem
import com.example.lifetogether.domain.model.recipe.Ingredient
import com.example.lifetogether.ui.theme.LifeTogetherTheme
import java.text.DecimalFormat
import java.util.Date

@Composable
fun ListItem(
    item: Completable,
    onCompleteToggle: () -> Unit,
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

    Row(
        modifier = Modifier
            .padding(horizontal = 10.dp, vertical = 5.dp)
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier = Modifier
                    .height(30.dp)
                    .aspectRatio(1f)
                    .clip(shape = CircleShape)
                    .border(
                        width = 2.dp,
                        color = MaterialTheme.colorScheme.secondary,
                        shape = CircleShape,
                    )
                    .clickable { onCompleteToggle() }
                    .then(
                        if (item.completed) {
                            Modifier.background(color = MaterialTheme.colorScheme.secondary)
                        } else {
                            Modifier
                        },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                if (item.completed) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_checkmark),
                        contentDescription = "checkmark icon",
                    )
                }
            }

            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                textDecoration = if (item.completed) TextDecoration.LineThrough else TextDecoration.None,
            )
        }

        if (item is GroceryItem && onBellClick != null) {
            Box(
                modifier = Modifier
                    .height(30.dp)
                    .aspectRatio(1f)
                    .clickable { onBellClick() },
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_bell_black),
                    contentDescription = "bell notification icon",
                )
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.S)
@Preview(showBackground = true)
@Composable
fun ListItemPreview() {
    LifeTogetherTheme {
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
            onCompleteToggle = {},
        )
    }
}
