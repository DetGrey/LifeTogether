package com.example.lifetogether.ui.feature.groceryList

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.lifetogether.domain.model.Category
import com.example.lifetogether.domain.model.grocery.GrocerySuggestion
import com.example.lifetogether.ui.theme.LifeTogetherTokens
import com.example.lifetogether.ui.theme.LifeTogetherTheme
import com.example.lifetogether.util.priceToString

@Composable
fun GrocerySuggestionPopup(
    suggestions: List<GrocerySuggestion>,
    onClick: (GrocerySuggestion) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(max = 130.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f)),
        shape = MaterialTheme.shapes.large.copy(
            bottomStart = CornerSize(0.dp),
            bottomEnd = CornerSize(0.dp),
        ),
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = LifeTogetherTokens.spacing.medium)
                .padding(bottom = LifeTogetherTokens.spacing.medium),
            contentPadding = PaddingValues(
                top = LifeTogetherTokens.spacing.small,
                bottom = LifeTogetherTokens.spacing.small,
            ),
            verticalArrangement = Arrangement.spacedBy(LifeTogetherTokens.spacing.small),
        ) {
            items(
                items = suggestions,
                key = { it.id },
            ) { suggestion ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 24.dp)
                        .clickable { onClick(suggestion) },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(LifeTogetherTokens.spacing.small),
                ) {
                    Text(
                        modifier = Modifier.weight(2f),
                        text = "${suggestion.category.emoji} ${suggestion.category.name}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        modifier = Modifier.weight(3f),
                        text = suggestion.suggestionName,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Box(
                        modifier = Modifier.width(50.dp),
                        contentAlignment = Alignment.CenterEnd,
                    ) {
                        suggestion.approxPrice?.let {
                            Text(
                                text = it.priceToString(),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
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
                    suggestionName = "Whole grain bread with chocolate",
                    category = Category(
                        emoji = "🍞",
                        name = "Bakery and very long name",
                    ),
                    approxPrice = 24.0f,
                ),
                GrocerySuggestion(
                    id = "suggestion-3",
                    suggestionName = "Bananas",
                    category = Category(
                        emoji = "🍌",
                        name = "Fruit",
                    ),
                ),
            ),
            onClick = {},
        )
    }
}
