package com.example.lifetogether.ui.feature.guides.stepplayer.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import com.example.lifetogether.domain.model.guides.GuideStep
import com.example.lifetogether.domain.model.guides.GuideStepType

@Composable
fun GuideStepRowText(
    step: GuideStep,
    textColor: Color,
    modifier: Modifier = Modifier,
    textDecoration: TextDecoration = TextDecoration.None,
    numberedIndex: Int? = null,
    roundTitleOverride: String = "",
) {
    when (step.type) {
        GuideStepType.ROUND -> {
            GuideStepRoundText(
                title = roundTitleOverride.ifBlank { guideStepRoundLabel(step) },
                content = step.content,
                textColor = textColor,
                modifier = modifier,
                textDecoration = textDecoration,
            )
        }

        GuideStepType.COMMENT, GuideStepType.UNKNOWN -> {
            GuideStepCommentText(
                text = guideStepCommentText(step),
                textColor = textColor,
                modifier = modifier,
                textDecoration = textDecoration,
            )
        }

        GuideStepType.NUMBERED -> {
            GuideStepNumberedText(
                index = numberedIndex ?: 1,
                text = guideStepNumberedText(step),
                textColor = textColor,
                modifier = modifier,
                textDecoration = textDecoration,
            )
        }

        GuideStepType.SUBSECTION -> {
            GuideStepSubsectionText(
                title = guideStepSubsectionLabel(step),
                content = step.content,
                textColor = textColor,
                modifier = modifier,
                textDecoration = textDecoration,
            )
        }
    }
}

@Composable
fun GuideStepCommentText(
    text: String,
    textColor: Color,
    modifier: Modifier = Modifier,
    textDecoration: TextDecoration = TextDecoration.None,
) {
    Text(
        text = text,
        modifier = modifier,
        color = textColor,
        style = MaterialTheme.typography.bodySmall,
        fontStyle = FontStyle.Italic,
        textDecoration = textDecoration,
    )
}

@Composable
fun GuideStepRoundText(
    title: String,
    content: String,
    textColor: Color,
    modifier: Modifier = Modifier,
    textDecoration: TextDecoration = TextDecoration.None,
) {
    Text(
        text = buildAnnotatedString {
            withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                append(title)
            }
            if (content.isNotBlank()) {
                append(": ")
                append(content)
            }
        },
        modifier = modifier,
        color = textColor,
        style = MaterialTheme.typography.bodySmall,
        textDecoration = textDecoration,
    )
}

@Composable
fun GuideStepNumberedText(
    index: Int,
    text: String,
    textColor: Color,
    modifier: Modifier = Modifier,
    textDecoration: TextDecoration = TextDecoration.None,
) {
    Text(
        text = "$index. $text",
        modifier = modifier,
        color = textColor,
        style = MaterialTheme.typography.bodySmall,
        textDecoration = textDecoration,
    )
}

@Composable
fun GuideStepSubsectionText(
    title: String,
    content: String,
    textColor: Color,
    modifier: Modifier = Modifier,
    textDecoration: TextDecoration = TextDecoration.None,
) {
    Text(
        text = buildAnnotatedString {
            withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                append(title)
            }
            if (content.isNotBlank()) {
                append(": ")
                append(content)
            }
        },
        modifier = modifier,
        color = textColor,
        style = MaterialTheme.typography.bodySmall,
        textDecoration = textDecoration,
    )
}

fun guideStepCommentText(step: GuideStep): String {
    if (step.content.isNotBlank()) return step.content
    if (step.name.isNotBlank()) return step.name
    if (step.title.isNotBlank()) return step.title
    return "Comment"
}

fun guideStepRoundLabel(step: GuideStep): String {
    if (step.name.isNotBlank()) return step.name
    if (step.title.isNotBlank()) return step.title
    return "Round"
}

fun guideStepSubsectionLabel(step: GuideStep): String {
    if (step.title.isNotBlank()) return step.title
    if (step.name.isNotBlank()) return step.name
    return "Subsection"
}

fun guideStepNumberedText(step: GuideStep): String {
    if (step.content.isNotBlank()) return step.content
    if (step.title.isNotBlank()) return step.title
    if (step.name.isNotBlank()) return step.name
    return "Step"
}
