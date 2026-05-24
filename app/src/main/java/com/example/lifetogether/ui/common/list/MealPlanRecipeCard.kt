package com.example.lifetogether.ui.common.list

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.lifetogether.domain.logic.minToHourMinString
import com.example.lifetogether.domain.model.sealed.ImageType
import com.example.lifetogether.ui.common.image.AnimatedBitmapImage
import com.example.lifetogether.ui.common.image.rememberObservedImageBitmap
import com.example.lifetogether.ui.common.text.TextDefault
import com.example.lifetogether.ui.common.text.TextLabel
import com.example.lifetogether.ui.theme.LifeTogetherTokens

@Composable
fun MealPlanRecipeCard(
    familyId: String,
    recipeId: String,
    recipeName: String,
    mealType: String,
    prepTimeMin: Int?,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val imageType = ImageType.RecipeImage(familyId, recipeId)
    val bitmap = rememberObservedImageBitmap(imageType)

    Card(
        modifier = modifier.then(
            if (onClick != null) {
                Modifier.clickable(onClick = onClick)
            } else {
                Modifier
            },
        ),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        shape = MaterialTheme.shapes.large,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(190.dp),
        ) {
            AnimatedBitmapImage(
                bitmap = bitmap,
                modifier = Modifier.fillMaxSize(),
                contentDescription = "recipe image",
            )
            if (bitmap != null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colorStops = arrayOf(
                                    0.0f to Color.Transparent,
                                    0.35f to Color.Transparent,
                                    1.0f to MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f),
                                ),
                            ),
                        ),
                )
            }

            TextLabel(
                text = mealType,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(LifeTogetherTokens.spacing.medium),
            )

            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(LifeTogetherTokens.spacing.medium),
            ) {
                Text(
                    text = recipeName,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (prepTimeMin != null && prepTimeMin > 0) {
                    TextDefault(
                        text = "Prep time: ${minToHourMinString(prepTimeMin)}",
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
        }
    }
}
