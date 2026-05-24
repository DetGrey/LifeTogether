package com.example.lifetogether.ui.common.textfield

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.sp

/**
 * Hides `# ` / `## ` line-start markers and inserts a blank line before each
 * non-first heading, producing space above the heading with none added below.
 * Cursor mapping accounts for both the removed prefix chars and the inserted blank line.
 */
class MarkdownVisualTransformation : VisualTransformation {

    private data class LineInfo(
        val startOrig: Int,
        val prefixLen: Int,
        val insertBlankLineBefore: Boolean,
        val raw: String,
    )

    override fun filter(text: AnnotatedString): TransformedText {
        val original = text.text

        val lineInfos = mutableListOf<LineInfo>()
        var pos = 0
        original.split("\n").forEachIndexed { idx, line ->
            val prefix = when {
                line.startsWith("## ") -> 3
                line.startsWith("# ") -> 2
                else -> 0
            }
            lineInfos.add(
                LineInfo(
                    startOrig = pos,
                    prefixLen = prefix,
                    insertBlankLineBefore = prefix > 0 && idx > 0,
                    raw = line,
                ),
            )
            pos += line.length + 1
        }

        val builder = AnnotatedString.Builder()
        val origToTrans = IntArray(original.length + 1)
        val transToOrigList = mutableListOf<Int>()
        var transIdx = 0

        lineInfos.forEachIndexed { lineIdx, info ->
            // Insert blank line before non-first headings → space above only
            if (info.insertBlankLineBefore) { //todo maybe just add some padding or similar instead since I always want a bit extra space below but not as much as above
                transToOrigList.add(info.startOrig)
                builder.append("\n")
                transIdx++
            }

            // All hidden prefix chars map to where the first visible char will land
            for (i in 0 until info.prefixLen) {
                origToTrans[info.startOrig + i] = transIdx
            }

            val visible = info.raw.substring(info.prefixLen)

            val spanStyle: SpanStyle? = when (info.prefixLen) {
                2 -> SpanStyle(fontWeight = FontWeight.Bold, fontSize = 22.sp)
                3 -> SpanStyle(fontWeight = FontWeight.Bold, fontSize = 18.sp)
                else -> if (info.raw.startsWith("- ")) SpanStyle(fontWeight = FontWeight.Normal) else null
            }

            if (spanStyle != null) builder.pushStyle(spanStyle)

            visible.forEachIndexed { charIdx, _ ->
                val origPos = info.startOrig + info.prefixLen + charIdx
                origToTrans[origPos] = transIdx
                transToOrigList.add(origPos)
                transIdx++
            }
            builder.append(visible)

            if (spanStyle != null) builder.pop()

            if (lineIdx < lineInfos.lastIndex) {
                val nlOrigPos = info.startOrig + info.raw.length
                origToTrans[nlOrigPos] = transIdx
                transToOrigList.add(nlOrigPos)
                builder.append("\n")
                transIdx++
            }
        }

        origToTrans[original.length] = transIdx
        transToOrigList.add(original.length)

        val transToOrig = transToOrigList.toIntArray()

        val mapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int =
                origToTrans[offset.coerceIn(0, original.length)]

            override fun transformedToOriginal(offset: Int): Int =
                transToOrig[offset.coerceIn(0, transIdx)]
        }

        return TransformedText(builder.toAnnotatedString(), mapping)
    }
}
