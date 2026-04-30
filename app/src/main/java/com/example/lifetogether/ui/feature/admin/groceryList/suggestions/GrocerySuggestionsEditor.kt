package com.example.lifetogether.ui.feature.admin.groceryList.suggestions

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.lifetogether.R
import com.example.lifetogether.ui.theme.LifeTogetherTokens
import com.example.lifetogether.domain.model.grocery.GrocerySuggestion
import com.example.lifetogether.ui.common.text.TextDefault
import com.example.lifetogether.ui.theme.LifeTogetherTheme
import com.example.lifetogether.util.UNCATEGORIZED_CATEGORY
import com.example.lifetogether.util.priceToString

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GrocerySuggestionsEditor(
    suggestions: List<GrocerySuggestion>,
    expandedCategories: Set<String>,
    onToggleExpand: (String) -> Unit,
    onEditItem: (GrocerySuggestion) -> Unit,
    onDeleteItem: (GrocerySuggestion) -> Unit,
) {
    // 1. Group data logically
    val grouped = suggestions.groupBy { it.category?.name ?: "Uncategorized" }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(LifeTogetherTokens.spacing.medium),
    ) {
        grouped.forEach { (categoryName, items) ->
            val isExpanded = expandedCategories.contains(categoryName)
            val categoryEmoji = items.firstOrNull()?.category?.emoji ?: "🛒"

            // --- CATEGORY HEADER ---
            item(key = categoryName) {
                CategoryHeader(
                    name = categoryName,
                    emoji = categoryEmoji,
                    isExpanded = isExpanded,
                    onToggle = { onToggleExpand(categoryName) },
                )
            }

            // --- ITEMS (Visible only if expanded) ---
            if (isExpanded) {
                items(items, key = { it.id ?: it.suggestionName }) { suggestion ->
                    GrocerySuggestionRow(
                        suggestion = suggestion,
                        onEdit = { onEditItem(suggestion) },
                        onDelete = { onDeleteItem(suggestion) },
                    )
                }

                // Add spacing after an expanded section
                item { Spacer(modifier = Modifier.height(LifeTogetherTokens.spacing.small)) }
            }
        }
    }
}

@Composable
fun CategoryHeader(
    name: String,
    emoji: String,
    isExpanded: Boolean,
    onToggle: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
            .clickable { onToggle() },
    ) {
        Column(
            modifier = Modifier.padding(vertical = LifeTogetherTokens.spacing.small),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextDefault(
                    text = "$emoji  $name",
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                )

                Icon(
                    painter = painterResource(
                        id = if (isExpanded) R.drawable.ic_expanded else R.drawable.ic_expand,
                    ),
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
            HorizontalDivider(
                modifier = Modifier.padding(top = LifeTogetherTokens.spacing.small),
                thickness = 2.dp,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
fun GrocerySuggestionRow(
    suggestion: GrocerySuggestion,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
            .padding(
                start = LifeTogetherTokens.spacing.medium,
                top = LifeTogetherTokens.spacing.xSmall,
                bottom = LifeTogetherTokens.spacing.xSmall,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextDefault(
            text = suggestion.suggestionName,
            modifier = Modifier.weight(1f),
            maxLines = 1,
        )
        suggestion.approxPrice?.let {
            TextDefault(
                text = suggestion.approxPrice.priceToString(),
            )
        }
        Icon(
            painter = painterResource(id = R.drawable.ic_edit),
            contentDescription = "edit icon",
            tint = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier
                .fillMaxHeight(0.9f)
                .clickable { onEdit() },
        )
        Icon(
            painter = painterResource(id = R.drawable.ic_trashcan),
            contentDescription = "trashcan icon",
            tint = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier
                .fillMaxHeight(0.9f)
                .clickable { onDelete() },
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun Preview() {
    LifeTogetherTheme {
        GrocerySuggestionRow(
            suggestion = GrocerySuggestion(
                id = "1",
                category = UNCATEGORIZED_CATEGORY,
                suggestionName = "Broccoli",
                approxPrice = 17.5f,
            ),
            onEdit = {},
            onDelete = {}
        )
    }
}
