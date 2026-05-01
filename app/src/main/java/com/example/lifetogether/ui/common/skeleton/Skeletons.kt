package com.example.lifetogether.ui.common.skeleton

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.lifetogether.ui.theme.LifeTogetherTheme
import com.example.lifetogether.ui.theme.LifeTogetherTokens

object Skeletons {
    @Composable
    fun ListDetail(
        modifier: Modifier = Modifier,
    ) {
        Column(
            modifier = modifier.padding(LifeTogetherTokens.spacing.small),
            verticalArrangement = Arrangement.spacedBy(LifeTogetherTokens.spacing.medium),
        ) {
            SkeletonLine(
                modifier = Modifier.fillMaxWidth(0.52f),
                height = 24.dp,
            )

            SkeletonLine(
                modifier = Modifier.fillMaxWidth(0.34f),
                height = 14.dp,
            )

            repeat(5) {
                SkeletonSurface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp),
                    shape = MaterialTheme.shapes.large,
                )
            }
        }
    }

    @Composable
    fun SectionDetail(
        modifier: Modifier = Modifier,
    ) {
        Column(
            modifier = modifier.padding(LifeTogetherTokens.spacing.small),
            verticalArrangement = Arrangement.spacedBy(LifeTogetherTokens.spacing.medium),
        ) {
            SkeletonLine(
                modifier = Modifier.fillMaxWidth(0.58f),
                height = 24.dp,
            )

            SkeletonSurface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                shape = MaterialTheme.shapes.extraLarge,
            )

            SkeletonSurface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = MaterialTheme.shapes.large,
            )

            repeat(3) {
                SkeletonSurface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(88.dp),
                    shape = MaterialTheme.shapes.large,
                )
            }
        }
    }

    @Composable
    fun FormEdit(
        modifier: Modifier = Modifier,
    ) {
        Column(
            modifier = modifier.padding(LifeTogetherTokens.spacing.small),
            verticalArrangement = Arrangement.spacedBy(LifeTogetherTokens.spacing.medium),
        ) {
            SkeletonSurface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(212.dp),
                shape = MaterialTheme.shapes.extraLarge,
            )

            SkeletonLine(
                modifier = Modifier.fillMaxWidth(0.52f),
                height = 24.dp,
            )

            SkeletonLine(
                modifier = Modifier.fillMaxWidth(0.84f),
            )

            repeat(3) {
                SkeletonSurface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = MaterialTheme.shapes.medium,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(LifeTogetherTokens.spacing.small),
            ) {
                SkeletonPill(modifier = Modifier.weight(1f))
                SkeletonPill(modifier = Modifier.weight(1f))
                SkeletonPill(modifier = Modifier.weight(1f))
            }

            SkeletonSurface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = MaterialTheme.shapes.large,
            )
        }
    }

    @Composable
    fun GalleryGrid(
        modifier: Modifier = Modifier,
    ) {
        Column(
            modifier = modifier.padding(LifeTogetherTokens.spacing.small),
            verticalArrangement = Arrangement.spacedBy(LifeTogetherTokens.spacing.medium),
        ) {
            SkeletonLine(
                modifier = Modifier.fillMaxWidth(0.44f),
                height = 24.dp,
            )

            SkeletonLine(
                modifier = Modifier.fillMaxWidth(0.32f),
                height = 14.dp,
            )

            SkeletonSurface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(24.dp),
                shape = MaterialTheme.shapes.large,
            )

            repeat(4) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(LifeTogetherTokens.spacing.small),
                ) {
                    SkeletonSurface(
                        modifier = Modifier
                            .weight(1f)
                            .height(112.dp),
                        shape = MaterialTheme.shapes.large,
                    )
                    SkeletonSurface(
                        modifier = Modifier
                            .weight(1f)
                            .height(112.dp),
                        shape = MaterialTheme.shapes.large,
                    )
                }
            }
        }
    }

    @Composable
    fun GridCollection(
        modifier: Modifier = Modifier,
    ) {
        Column(
            modifier = modifier.padding(LifeTogetherTokens.spacing.small),
            verticalArrangement = Arrangement.spacedBy(LifeTogetherTokens.spacing.medium),
        ) {
            SkeletonLine(
                modifier = Modifier.fillMaxWidth(0.42f),
                height = 24.dp,
            )

            SkeletonSurface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(204.dp),
                shape = MaterialTheme.shapes.large,
            )

            SkeletonSurface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp),
                shape = MaterialTheme.shapes.large,
            )

            repeat(3) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(LifeTogetherTokens.spacing.small),
                ) {
                    SkeletonSurface(
                        modifier = Modifier
                            .weight(1f)
                            .height(108.dp),
                        shape = MaterialTheme.shapes.large,
                    )
                    SkeletonSurface(
                        modifier = Modifier
                            .weight(1f)
                            .height(108.dp),
                        shape = MaterialTheme.shapes.large,
                    )
                }
            }
        }
    }
}

@Composable
private fun SkeletonSurface(
    modifier: Modifier,
    shape: Shape,
) {
    Box(
        modifier = modifier.skeletonSurface(shape = shape),
    )
}

@Composable
private fun SkeletonLine(
    modifier: Modifier,
    height: Dp = 14.dp,
    centerAlign: Boolean = false,
) {
    val contentAlignment = if (centerAlign) Alignment.Center else Alignment.TopStart
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = contentAlignment,
    ) {
        SkeletonSurface(
            modifier = modifier.height(height),
            shape = MaterialTheme.shapes.small,
        )
    }
}

@Composable
private fun SkeletonPill(
    modifier: Modifier,
) {
    SkeletonSurface(
        modifier = modifier.height(24.dp),
        shape = MaterialTheme.shapes.large,
    )
}

private fun Modifier.skeletonSurface(
    shape: Shape,
): Modifier = composed {
    val transition = rememberInfiniteTransition(label = "skeleton-shimmer")
    val shimmerPosition by transition.animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "skeleton-shimmer-position",
    )

    val baseColor = MaterialTheme.colorScheme.surfaceVariant
    val lowColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f)
    val highlightColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f)

    clip(shape)
        .background(baseColor)
        .drawWithCache {
            val sweepWidth = size.width * 0.45f
            val startX = (size.width * shimmerPosition) - sweepWidth
            val brush = Brush.linearGradient(
                colors = listOf(lowColor, highlightColor, lowColor),
                start = Offset(startX, 0f),
                end = Offset(startX + sweepWidth, size.height),
            )
            onDrawWithContent {
                drawContent()
                drawRect(brush = brush)
            }
        }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun ListDetailSkeletonPreview() {
    LifeTogetherTheme {
        Skeletons.ListDetail()
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun SectionDetailSkeletonPreview() {
    LifeTogetherTheme {
        Skeletons.SectionDetail()
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun FormEditSkeletonPreview() {
    LifeTogetherTheme {
        Skeletons.FormEdit()
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun GalleryGridSkeletonPreview() {
    LifeTogetherTheme {
        Skeletons.GalleryGrid()
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun GridCollectionSkeletonPreview() {
    LifeTogetherTheme {
        Skeletons.GridCollection()
    }
}
