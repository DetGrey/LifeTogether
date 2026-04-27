package com.example.lifetogether.ui.feature.groceryList

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.lifetogether.domain.model.grocery.GrocerySuggestion
import com.example.lifetogether.util.priceToString
import com.example.lifetogether.ui.theme.LifeTogetherTokens

@Composable
fun GrocerySuggestionPopup(
    suggestions: List<GrocerySuggestion>,
    onClick: (GrocerySuggestion) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height((75 + 30 * (suggestions.size - 1)).dp)
            .background(
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.9f),
                shape = RoundedCornerShape(topStart = LifeTogetherTokens.spacing.large, topEnd = LifeTogetherTokens.spacing.large, bottomStart = 0.dp, bottomEnd = 0.dp)
            )
            .padding(horizontal = LifeTogetherTokens.spacing.medium)
            .padding(top = LifeTogetherTokens.spacing.medium),
        contentAlignment = Alignment.CenterStart,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize(),
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
                        color = MaterialTheme.colorScheme.background,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        modifier = Modifier.weight(1f),
                        text = suggestion.suggestionName,
                        color = MaterialTheme.colorScheme.background,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                    )
                    suggestion.approxPrice?.let {
                        Text(
                            modifier = Modifier
                                .fillMaxWidth(0.3f),
                            text = it.priceToString(),
                            color = MaterialTheme.colorScheme.background,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}
