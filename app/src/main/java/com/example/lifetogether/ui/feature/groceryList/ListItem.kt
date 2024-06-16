package com.example.lifetogether.ui.feature.groceryList

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.lifetogether.R
import com.example.lifetogether.domain.model.Category
import com.example.lifetogether.domain.model.GroceryItem
import com.example.lifetogether.domain.model.Item
import com.example.lifetogether.ui.theme.LifeTogetherTheme
import java.util.Date

@Composable
fun ListItem(
    item: Item,
) {
    Row(
        modifier = Modifier
            .padding(horizontal = 10.dp, vertical = 5.dp)
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(shape = CircleShape)
                .border(width = 2.dp, color = MaterialTheme.colorScheme.secondary, shape = CircleShape)
                .then(
                    if (item.checked) {
                        Modifier.background(color = MaterialTheme.colorScheme.secondary)
                    } else {
                        Modifier
                    },
                ),
        ) {
            if (item.checked) {
                Image(
                    painter = painterResource(id = R.drawable.ic_back_arrow),
                    contentDescription = "",
                ) // TODO
            }
        }

        Text(
            text = item.itemName,
            textDecoration = if (item.checked) TextDecoration.LineThrough else TextDecoration.None,
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ListItemPreview() {
    LifeTogetherTheme {
        ListItem(
            GroceryItem(
                uid = "dsuaihfao",
                username = "Ane",
                category = Category(
                    "🍎",
                    "Fruits and vegetables",
                ),
                itemName = "Potatoes",
                lastUpdated = Date(System.currentTimeMillis()),
                checked = true,
            ),
        )
    }
}
