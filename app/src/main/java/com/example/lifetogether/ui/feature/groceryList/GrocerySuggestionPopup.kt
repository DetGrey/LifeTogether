package com.example.lifetogether.ui.feature.groceryList

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.lifetogether.domain.model.Category
import com.example.lifetogether.domain.model.grocery.GrocerySuggestion
import com.example.lifetogether.ui.theme.LifeTogetherTheme
import com.example.lifetogether.util.priceToString
import com.example.lifetogether.ui.theme.LifeTogetherTokens

@Composable
fun GrocerySuggestionPopup(
    suggestions: List<GrocerySuggestion>,
    onClick: (GrocerySuggestion) -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height((75 + 30 * (suggestions.size - 1)).dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f)),
        shape = MaterialTheme.shapes.large.copy(
            bottomStart = CornerSize(0.dp),
            bottomEnd = CornerSize(0.dp),
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = LifeTogetherTokens.spacing.medium)
                .padding(top = LifeTogetherTokens.spacing.medium),
            verticalArrangement = Arrangement.spacedBy(LifeTogetherTokens.spacing.small),
        ) {
            for (suggestion in suggestions) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onClick(suggestion) },
                    horizontalArrangement = Arrangement.spacedBy(LifeTogetherTokens.spacing.xSmall)
                ) {
                    Text(
                        modifier = Modifier.fillMaxWidth(0.45f),
                        text = "${suggestion.category?.emoji} ${suggestion.category?.name}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        modifier = Modifier.weight(1f),
                        text = suggestion.suggestionName,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    suggestion.approxPrice?.let {
                        Text(
                            modifier = Modifier
                                .fillMaxWidth(0.3f),
                            text = it.priceToString(),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun GrocerySuggestionPopupPreview() {
    LifeTogetherTheme {
        GrocerySuggestionPopup(
            suggestions = listOf(
                GrocerySuggestion(
                    id = "suggestion-1",
                    suggestionName = "Apples",
                    category = Category(
                        emoji = "🍎",
                        name = "Fruit",
                    ),
                    approxPrice = 12.5f,
                ),
                GrocerySuggestion(
                    id = "suggestion-2",
                    suggestionName = "Bananas",
                    category = Category(
                        emoji = "🍌",
                        name = "Fruit",
                    ),
                ),
                GrocerySuggestion(
                    id = "suggestion-3",
                    suggestionName = "Whole grain bread",
                    category = Category(
                        emoji = "🍞",
                        name = "Bakery",
                    ),
                    approxPrice = 24.0f,
                ),
            ),
            onClick = {},
        )
    }
}
