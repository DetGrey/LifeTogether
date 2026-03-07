package com.example.lifetogether.ui.feature.guides.details.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.lifetogether.domain.model.guides.Guide
import com.example.lifetogether.domain.model.guides.GuideVisibility

@Composable
fun GuideHeroCard(guide: Guide) {
    val completedSections = guide.sections.count { it.completed }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.onBackground, RoundedCornerShape(20.dp))
            .padding(16.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = guide.itemName,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.background,
                fontWeight = FontWeight.Bold,
            )

            if (guide.description.isNotBlank()) {
                Text(
                    text = guide.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.background,
                )
            }

            Text(
                text = buildString {
                    append(if (guide.visibility == GuideVisibility.FAMILY) "Family shared" else "Private")
                    append("  •  ")
                    append("Sections: $completedSections/${guide.sections.size}")
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.background,
            )

            Text(
                text = if (guide.started) "Started" else "Not started",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.background,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}