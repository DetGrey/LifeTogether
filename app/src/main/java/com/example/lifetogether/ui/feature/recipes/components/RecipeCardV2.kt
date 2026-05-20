package com.example.lifetogether.ui.feature.recipes.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.lifetogether.domain.logic.minToHourMinString
import com.example.lifetogether.domain.model.recipe.Recipe
import com.example.lifetogether.domain.model.sealed.ImageType
import com.example.lifetogether.ui.common.image.AnimatedBitmapImage
import com.example.lifetogether.ui.common.image.rememberObservedImageBitmap
import com.example.lifetogether.ui.theme.LifeTogetherTheme
import com.example.lifetogether.ui.theme.LifeTogetherTokens
import java.util.Date

@Composable
fun RecipeCardV2(
    recipe: Recipe,
    onClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (recipe.itemName.isEmpty()) {
        return
    }

    val imageType = remember(recipe.familyId, recipe.id, recipe.imageUrl) {
        recipe.imageUrl?.let {
            ImageType.RecipeImage(recipe.familyId, recipe.id)
        }
    }
    val imageBitmap = rememberObservedImageBitmap(imageType)

    val backgroundGradient = if (imageBitmap != null) {
        Modifier.background(Brush.horizontalGradient(
            colorStops = arrayOf(
                0.0f to MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
                0.8f to Color.Transparent,
            ),
        ))
    } else Modifier

    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(88.dp)
            .clip(MaterialTheme.shapes.large)
            .clickable { onClick(recipe.id) },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        shape = MaterialTheme.shapes.large,
    ) {
        Box {
            AnimatedBitmapImage(
                bitmap = imageBitmap,
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(0.6f),
                contentDescription = recipe.itemName,
                contentScale = ContentScale.Crop,
            )
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .then(backgroundGradient)
                    .padding(LifeTogetherTokens.spacing.medium),
                horizontalArrangement = Arrangement.spacedBy(LifeTogetherTokens.spacing.medium),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(LifeTogetherTokens.spacing.xSmall),
                ) {
                    Text(
                        text = minToHourMinString(recipe.preparationTimeMin),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                    Text(
                        text = recipe.itemName,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun RecipeCardV2Preview() {
    LifeTogetherTheme {
        RecipeCardV2(
            recipe = Recipe(
                id = "recipe-soup",
                familyId = "family-1",
                itemName = "Tomato Soup",
                lastUpdated = Date(),
                description = "A simple soup.",
                ingredients = emptyList(),
                instructions = emptyList(),
                preparationTimeMin = 25,
                favourite = false,
                servings = 2,
                tags = listOf("Dinner", "Soup"),
                imageUrl = null,
            ),
            onClick = {},
        )
    }
}
