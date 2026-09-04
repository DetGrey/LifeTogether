package com.example.lifetogether.data.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.content.FileProvider
import androidx.core.graphics.withSave
import androidx.core.graphics.withTranslation
import com.example.lifetogether.ui.feature.recipes.details.RecipeDetailsUiState
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

data class GeneratedRecipePdf(
    val uri: Uri,
    val fileName: String,
)

@Singleton
class RecipePdfGenerator @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    companion object {
        private const val PAGE_WIDTH = 595
        private const val PAGE_HEIGHT = 842
        private const val MARGIN = 48
        private const val CONTENT_WIDTH = PAGE_WIDTH - MARGIN * 2
    }

    private val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 28f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        color = Color.BLACK
    }

    private val sectionPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 15f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        color = Color.BLACK
    }

    private val bodyPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 11f
        color = Color.BLACK
    }

    private val metaPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 10f
        color = Color.DKGRAY
    }

    private val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.LTGRAY
        strokeWidth = 1f
    }

    fun generate(content: RecipeDetailsUiState.Content, bitmap: Bitmap?): GeneratedRecipePdf {
        val document = PdfDocument()
        var pageNumber = 1
        var page = document.startPage(pageInfo(pageNumber))
        var canvas: Canvas = page.canvas
        var y = MARGIN.toFloat() + 8f

        fun finishAndNewPage() {
            document.finishPage(page)
            pageNumber++
            page = document.startPage(pageInfo(pageNumber))
            canvas = page.canvas
            y = MARGIN.toFloat() + 8f
        }

        fun ensureSpace(neededHeight: Float) {
            if (y + neededHeight > PAGE_HEIGHT - MARGIN) finishAndNewPage()
        }

        fun drawText(text: String, paint: TextPaint) {
            val layout = buildLayout(text, paint)
            var firstLineIndex = 0
            while (firstLineIndex < layout.lineCount) {
                val firstLineHeight = layout.lineHeight(firstLineIndex)
                ensureSpace(firstLineHeight)

                val availableHeight = PAGE_HEIGHT - MARGIN - y
                val chunkTop = layout.getLineTop(firstLineIndex)
                var lastLineIndexExclusive = firstLineIndex
                while (
                    lastLineIndexExclusive < layout.lineCount &&
                    layout.getLineBottom(lastLineIndexExclusive) - chunkTop <= availableHeight
                ) {
                    lastLineIndexExclusive++
                }
                if (lastLineIndexExclusive == firstLineIndex) {
                    lastLineIndexExclusive++
                }

                val chunkBottom = layout.getLineBottom(lastLineIndexExclusive - 1)
                canvas.withTranslation(MARGIN.toFloat(), y - chunkTop) {
                    withSave {
                        clipRect(
                            0f,
                            chunkTop.toFloat(),
                            CONTENT_WIDTH.toFloat(),
                            chunkBottom.toFloat(),
                        )
                        layout.draw(this)
                    }
                }
                y += (chunkBottom - chunkTop).toFloat()

                firstLineIndex = lastLineIndexExclusive
                if (firstLineIndex < layout.lineCount) {
                    finishAndNewPage()
                }
            }
        }

        // Title
        drawText(content.itemName, titlePaint)
        y += 8f

        // Divider
        ensureSpace(2f)
        canvas.drawLine(MARGIN.toFloat(), y, (MARGIN + CONTENT_WIDTH).toFloat(), y, dividerPaint)
        y += 14f

        if (bitmap != null) {
            val dstWidth = CONTENT_WIDTH.toFloat()
            val dstHeight = dstWidth / 2f
            val dstAspect = dstWidth / dstHeight
            val srcAspect = bitmap.width.toFloat() / bitmap.height.toFloat()
            val srcRect = if (srcAspect > dstAspect) {
                val cropWidth = (bitmap.height * dstAspect).toInt()
                val cropLeft = (bitmap.width - cropWidth) / 2
                Rect(cropLeft, 0, cropLeft + cropWidth, bitmap.height)
            } else {
                val cropHeight = (bitmap.width / dstAspect).toInt()
                val cropTop = (bitmap.height - cropHeight) / 2
                Rect(0, cropTop, bitmap.width, cropTop + cropHeight)
            }
            ensureSpace(dstHeight + 12f)
            canvas.drawBitmap(bitmap, srcRect, RectF(MARGIN.toFloat(), y, MARGIN + dstWidth, y + dstHeight), null)
            y += dstHeight + 14f
        }

        // Metadata
        val metaParts = mutableListOf<String>()
        if (content.preparationTimeMin.isNotBlank() && content.preparationTimeMin != "0") {
            metaParts.add("Prep time: ${content.preparationTimeMin} min")
        }
        metaParts.add("Servings: ${content.servings}")
        drawText(metaParts.joinToString("     "), metaPaint)
        y += 12f

        // Description
        if (content.description.isNotBlank()) {
            drawText(content.description, bodyPaint)
            y += 14f
        }

        // Ingredients
        if (content.ingredientsByServings.isNotEmpty()) {
            ensureSpace(30f)
            drawText("Ingredients", sectionPaint)
            y += 6f
            for (ingredient in content.ingredientsByServings) {
                val amount = if (ingredient.amount % 1.0 == 0.0) {
                    ingredient.amount.toInt().toString()
                } else {
                    ingredient.amount.toString()
                }
                drawText("•  $amount ${ingredient.measureType.unit}  ${ingredient.itemName}", bodyPaint)
                y += 3f
            }
            y += 12f
        }

        // Instructions
        if (content.instructions.isNotEmpty()) {
            ensureSpace(30f)
            drawText("Instructions", sectionPaint)
            y += 6f
            content.instructions.forEachIndexed { index, instruction ->
                drawText("${index + 1}.  ${instruction.itemName}", bodyPaint)
                y += 5f
            }
        }

        document.finishPage(page)

        val pdfDir = File(context.cacheDir, "pdfs").apply { mkdirs() }
        val fileName = recipePdfFileName(content.itemName)
        val pdfFile = File(pdfDir, fileName)
        pdfFile.outputStream().use { document.writeTo(it) }
        document.close()

        return GeneratedRecipePdf(
            uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                pdfFile,
                fileName,
            ),
            fileName = fileName,
        )
    }

    private fun pageInfo(pageNumber: Int) =
        PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()

    private fun buildLayout(text: String, paint: TextPaint): StaticLayout =
        StaticLayout.Builder.obtain(text, 0, text.length, paint, CONTENT_WIDTH)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setLineSpacing(2f, 1.15f)
            .build()

    private fun StaticLayout.lineHeight(lineIndex: Int): Float =
        (getLineBottom(lineIndex) - getLineTop(lineIndex)).toFloat()

    private fun recipePdfFileName(recipeName: String): String {
        val displayName = recipeName.trim().ifBlank { "Recipe" }
        val safeName = "$displayName Recipe"
            .replace(Regex("""[\\/:*?"<>|]"""), "")
            .replace(Regex("\\s+"), " ")
            .trim()
            .ifBlank { "Recipe" }

        return "$safeName.pdf"
    }
}
