package com.example.lifetogether.ui.feature.guides.details.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.example.lifetogether.domain.model.guides.Guide
import com.example.lifetogether.domain.model.enums.Visibility
import com.example.lifetogether.ui.common.text.TextDefault
import com.example.lifetogether.ui.common.text.TextSubHeadingMedium
import com.example.lifetogether.ui.theme.LifeTogetherTokens

@Composable
fun GuideHeroCard(guide: Guide) {
    val completedSections = guide.sections.count { it.completed }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(LifeTogetherTokens.spacing.medium),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(LifeTogetherTokens.spacing.small)) {
            TextSubHeadingMedium(guide.itemName)

            if (guide.description.isNotBlank()) {
                TextDefault(text = guide.description)
            }

            Text(
                text = buildString {
                    append(if (guide.visibility == Visibility.FAMILY) "Family shared" else "Private")
                    append("  •  ")
                    append("Sections: $completedSections/${guide.sections.size}")
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground,
            )

            Text(
                text = if (guide.started) "Started" else "Not started",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}
