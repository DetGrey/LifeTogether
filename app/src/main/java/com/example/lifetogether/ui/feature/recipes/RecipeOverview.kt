package com.example.lifetogether.ui.feature.recipes

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.lifetogether.domain.logic.minToHourMinString
import com.example.lifetogether.domain.model.recipe.Recipe
import com.example.lifetogether.ui.theme.LifeTogetherTheme

@Composable
fun RecipeOverview(
    recipe: Recipe,
    onClick: (String) -> Unit, // recipe id
) {
    if (recipe.itemName.isEmpty()) {
        return
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
            .clip(shape = RoundedCornerShape(20))
            .background(color = MaterialTheme.colorScheme.onBackground)
            .padding(horizontal = 15.dp, vertical = 10.dp)
            .clickable { recipe.id?.let { onClick(it) } },
        contentAlignment = Alignment.CenterStart,
    ) {
        Row {
            Row(
                modifier = Modifier
                    .fillMaxWidth(0.65f)
                    .fillMaxHeight(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = recipe.itemName,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.secondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End,
            ) {
                Text(
                    text = minToHourMinString(recipe.preparationTimeMin),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun RecipeOverviewPreview() {
    LifeTogetherTheme {
        RecipeOverview(
            Recipe(
                itemName = "Rød grød med fløde og sovs",
                preparationTimeMin = 149,
                tags = listOf("dinner", "easy", "fast"),
            ),
            onClick = {},
        )
    }
}
