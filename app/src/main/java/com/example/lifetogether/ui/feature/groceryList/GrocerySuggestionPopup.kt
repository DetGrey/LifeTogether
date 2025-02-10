package com.example.lifetogether.ui.feature.groceryList

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.lifetogether.domain.model.grocery.GrocerySuggestion

@Composable
fun GrocerySuggestionPopup(
    suggestions: List<GrocerySuggestion>,
    onClick: (GrocerySuggestion) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height((75 + 30 * (suggestions.size - 1)).dp)
            .clip(shape = RoundedCornerShape(20))
            .background(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.9f))
            .padding(horizontal = 20.dp)
            .padding(top = 15.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            for (suggestion in suggestions) {
                Text(
                    modifier = Modifier.clickable { onClick(suggestion) },
                    text = "${suggestion.category?.emoji} ${suggestion.category?.name} - ${suggestion.suggestionName}",
                    color = Color.White,
                )
            }
        }
    }
}
