package com.arkio.officeengine.renderer

import android.graphics.*
import org.apache.poi.hwpf.HWPFDocument
import org.apache.poi.xwpf.usermodel.*
import java.io.File
import java.io.FileInputStream

object DocxPageRenderer {

    // A4 page at 2x 96DPI
    private const val PAGE_WIDTH_PX = 1240
    private const val PAGE_HEIGHT_PX = 1754
    private const val RENDER_SCALE = 2f
    
    // Margins in scaled pixels
    private const val MARGIN_LEFT = 120f * RENDER_SCALE
    private const val MARGIN_RIGHT = 120f * RENDER_SCALE
    private const val MARGIN_TOP = 120f * RENDER_SCALE
    private const val MARGIN_BOTTOM = 120f * RENDER_SCALE
    
    // Text metrics
    private const val BASE_FONT_SIZE = 26f   // 13pt at 2x
    private const val LINE_HEIGHT_MULTIPLIER = 1.5f
    private const val HEADING1_SIZE = 48f
    private const val HEADING2_SIZE = 38f
    private const val HEADING3_SIZE = 32f
    
    private val canvasWidth = (PAGE_WIDTH_PX * RENDER_SCALE).toInt()
    private val canvasHeight = (PAGE_HEIGHT_PX * RENDER_SCALE).toInt()
    private val textWidth = canvasWidth - MARGIN_LEFT - MARGIN_RIGHT

    fun renderDocx(filePath: String): List<Bitmap> {
        val bitmaps = mutableListOf<Bitmap>()
        
        FileInputStream(File(filePath)).use { fis ->
            val doc = XWPFDocument(fis)
            
            // Extract all renderable elements in order
            val elements = extractElements(doc)
            
            // Paginate elements onto pages
            paginateElements(elements, bitmaps)
            
            doc.close()
        }
        
        return bitmaps
    }

    fun renderDoc(filePath: String): List<Bitmap> {
        // For old .doc format, extract text and render as plain text
        FileInputStream(File(filePath)).use { fis ->
            val doc = HWPFDocument(fis)
            val text = doc.documentText
            doc.close()
            
            // Render as plain text pages
            return renderPlainText(text)
        }
    }

    // ================================================================
    // ELEMENT EXTRACTION
    // ================================================================

    private sealed class DocElement {
        data class Paragraph(
            val text: String,
            val style: ParagraphStyle,
            val alignment: ParagraphAlignment,
            val runs: List<RunInfo>,
            val isHeading: Boolean,
            val headingLevel: Int,
            val isBullet: Boolean,
            val bulletIndent: Int,
            val spaceBefore: Float,
            val spaceAfter: Float,
            val listText: String? = null
        ) : DocElement()
        
        data class TableElement(
            val rows: List<List<CellInfo>>
        ) : DocElement()
        
        data class ImageElement(
            val bitmap: Bitmap,
            val widthPx: Float,
            val heightPx: Float
        ) : DocElement()
        
        object PageBreak : DocElement()
        object EmptyLine : DocElement()
    }

    data class RunInfo(
        val text: String,
        val bold: Boolean,
        val italic: Boolean,
        val underline: Boolean,
        val strike: Boolean,
        val fontSize: Float,
        val color: Int,
        val isMonospace: Boolean
    )

    data class CellInfo(
        val text: String,
        val bold: Boolean,
        val isHeader: Boolean,
        val alignment: ParagraphAlignment
    )

    enum class ParagraphStyle {
        NORMAL, HEADING1, HEADING2, HEADING3, HEADING4,
        TITLE, SUBTITLE, QUOTE, CODE, BULLET, NUMBERED
    }

    private fun extractElements(doc: XWPFDocument): List<DocElement> {
        val elements = mutableListOf<DocElement>()
        
        for (element in doc.bodyElements) {
            when (element) {
                is XWPFParagraph -> {
                    val para = extractParagraph(element)
                    if (para != null) elements.add(para)
                }
                is XWPFTable -> {
                    val table = extractTable(element)
                    if (table != null) elements.add(table)
                }
            }
        }
        
        return elements
    }

    private fun extractParagraph(para: XWPFParagraph): DocElement? {
        if (para.isPageBreak) return DocElement.PageBreak
        
        val runs = para.runs.map { run ->
            RunInfo(
                text = run.text() ?: "",
                bold = run.isBold,
                italic = run.isItalic,
                underline = run.underline != UnderlinePatterns.NONE,
                strike = run.isStrikeThrough,
                fontSize = (run.fontSize.takeIf { it > 0 } ?: 13).toFloat(),
                color = parseColor(run.color),
                isMonospace = isMonospaceFont(run.fontFamily)
            )
        }
        
        val fullText = runs.joinToString("") { it.text }
        if (fullText.isBlank() && runs.isEmpty()) {
            return DocElement.EmptyLine
        }
        
        val styleId = para.styleID ?: ""
        val styleName = para.style ?: ""
        val (isHeading, headingLevel, paraStyle) = detectStyle(styleId, styleName)
        
        val isBullet = para.numID != null && (para.numID?.toLong() ?: 0L) > 0
        val bulletIndent = para.indentationLeft / 720
        
        val listText = if (isBullet) para.numLevelText else null
        
        val spaceBefore = (para.spacingBefore / 20f).coerceAtLeast(0f)
        val spaceAfter = (para.spacingAfter / 20f).coerceAtLeast(4f)
        
        return DocElement.Paragraph(
            text = fullText,
            style = paraStyle,
            alignment = para.alignment ?: ParagraphAlignment.LEFT,
            runs = runs,
            isHeading = isHeading,
            headingLevel = headingLevel,
            isBullet = isBullet,
            bulletIndent = bulletIndent.coerceIn(0, 5),
            spaceBefore = spaceBefore,
            spaceAfter = spaceAfter,
            listText = listText
        )
    }

    private fun detectStyle(styleId: String, styleName: String): Triple<Boolean, Int, ParagraphStyle> {
        val id = styleId.lowercase()
        val name = styleName.lowercase()
        return when {
            id.contains("heading1") || name.contains("heading 1") || id == "title" -> Triple(true, 1, ParagraphStyle.HEADING1)
            id.contains("heading2") || name.contains("heading 2") || name.contains("subtitle") -> Triple(true, 2, ParagraphStyle.HEADING2)
            id.contains("heading3") || name.contains("heading 3") -> Triple(true, 3, ParagraphStyle.HEADING3)
            id.contains("heading4") || name.contains("heading 4") -> Triple(true, 4, ParagraphStyle.HEADING4)
            name.contains("quote") || name.contains("blockquote") -> Triple(false, 0, ParagraphStyle.QUOTE)
            name.contains("code") || name.contains("preformat") -> Triple(false, 0, ParagraphStyle.CODE)
            else -> Triple(false, 0, ParagraphStyle.NORMAL)
        }
    }

    private fun extractTable(table: XWPFTable): DocElement.TableElement? {
        val rows = table.rows.mapIndexed { rowIndex, row ->
            row.tableCells.map { cell ->
                val text = cell.paragraphs.joinToString("\n") { p -> p.runs.joinToString("") { it.text() ?: "" } }.trim()
                val firstRun = cell.paragraphs.firstOrNull()?.runs?.firstOrNull()
                CellInfo(
                    text = text,
                    bold = firstRun?.isBold ?: false,
                    isHeader = rowIndex == 0,
                    alignment = cell.paragraphs.firstOrNull()?.alignment ?: ParagraphAlignment.LEFT
                )
            }
        }.filter { it.isNotEmpty() }
        return if (rows.isNotEmpty()) DocElement.TableElement(rows) else null
    }

    // ================================================================
    // PAGINATION & RENDERING
    // ================================================================

    private fun paginateElements(elements: List<DocElement>, bitmaps: MutableList<Bitmap>) {
        var bitmap = createNewPage()
        var canvas = Canvas(bitmap)
        var y = MARGIN_TOP
        
        fun newPage() {
            bitmaps.add(bitmap)
            bitmap = createNewPage()
            canvas = Canvas(bitmap)
            y = MARGIN_TOP
        }
        
        for (element in elements) {
            when (element) {
                is DocElement.PageBreak -> newPage()
                is DocElement.EmptyLine -> {
                    y += BASE_FONT_SIZE * LINE_HEIGHT_MULTIPLIER
                    if (y > canvasHeight - MARGIN_BOTTOM) newPage()
                }
                is DocElement.Paragraph -> {
                    val requiredHeight = measureParagraphHeight(element)
                    if (y + requiredHeight > canvasHeight - MARGIN_BOTTOM && y > MARGIN_TOP) newPage()
                    y = drawParagraph(canvas, element, y)
                }
                is DocElement.TableElement -> {
                    val tableHeight = measureTableHeight(element)
                    if (y + tableHeight > canvasHeight - MARGIN_BOTTOM && y > MARGIN_TOP) newPage()
                    y = drawTable(canvas, element, y)
                }
                is DocElement.ImageElement -> {
                    val scaledH = element.heightPx
                    if (y + scaledH > canvasHeight - MARGIN_BOTTOM && y > MARGIN_TOP) newPage()
                    val dst = RectF(MARGIN_LEFT, y, MARGIN_LEFT + element.widthPx, y + scaledH)
                    canvas.drawBitmap(element.bitmap, null, dst, null)
                    y += scaledH + 16f
                }
            }
        }
        if (y > MARGIN_TOP) bitmaps.add(bitmap)
    }

    private fun createNewPage(): Bitmap {
        val bm = Bitmap.createBitmap(canvasWidth, canvasHeight, Bitmap.Config.ARGB_8888)
        val c = Canvas(bm)
        c.drawColor(Color.WHITE)
        return bm
    }

    private fun measureParagraphHeight(para: DocElement.Paragraph): Float {
        val fontSize = when (para.headingLevel) {
            1 -> HEADING1_SIZE
            2 -> HEADING2_SIZE
            3 -> HEADING3_SIZE
            4 -> HEADING3_SIZE * 0.9f
            else -> BASE_FONT_SIZE
        }
        val charsPerLine = (textWidth / (fontSize * 0.6f)).toInt().coerceAtLeast(1)
        val lines = (para.text.length / charsPerLine) + 1
        return (lines * fontSize * LINE_HEIGHT_MULTIPLIER) + para.spaceBefore + para.spaceAfter
    }

    private fun measureTableHeight(table: DocElement.TableElement): Float {
        return table.rows.size * (BASE_FONT_SIZE * LINE_HEIGHT_MULTIPLIER + 16f) + 32f
    }

    private fun drawParagraph(canvas: Canvas, para: DocElement.Paragraph, startY: Float): Float {
        var y = startY + para.spaceBefore
        if (para.text.isEmpty()) return y + BASE_FONT_SIZE * LINE_HEIGHT_MULTIPLIER + para.spaceAfter
        
        val fontSize = when (para.headingLevel) {
            1 -> HEADING1_SIZE
            2 -> HEADING2_SIZE
            3 -> HEADING3_SIZE
            4 -> HEADING3_SIZE * 0.9f
            else -> BASE_FONT_SIZE
        }
        
        val isBold = para.isHeading || para.runs.all { it.bold }
        val isItalic = para.runs.all { it.italic }
        val isCode = para.style == ParagraphStyle.CODE
        
        val leftIndent = MARGIN_LEFT + (para.bulletIndent * 40f)
        val availableWidth = canvasWidth - leftIndent - MARGIN_RIGHT
        
        if (para.isBullet) {
            val bulletPaint = Paint().apply {
                color = Color.rgb(80, 80, 80)
                textSize = fontSize
                isAntiAlias = true
            }
            val marker = para.listText ?: "•"
            canvas.drawText(marker, leftIndent - 40f, y + fontSize, bulletPaint)
        }
        
        if (para.runs.isNotEmpty()) {
            y = drawRichText(canvas, para.runs, leftIndent, y, availableWidth, fontSize, isBold, isItalic, isCode, para.alignment)
        } else {
            val paint = buildTextPaint(fontSize, isBold, isItalic, isCode, Color.rgb(30, 30, 30))
            y = drawWrappedText(canvas, para.text, paint, leftIndent, y, availableWidth, para.alignment)
        }
        return y + para.spaceAfter
    }

    private fun drawRichText(
        canvas: Canvas, runs: List<RunInfo>, x: Float, startY: Float,
        maxWidth: Float, defaultFontSize: Float, defaultBold: Boolean, defaultItalic: Boolean,
        isCode: Boolean, alignment: ParagraphAlignment
    ): Float {
        var y = startY
        val lineHeight = defaultFontSize * LINE_HEIGHT_MULTIPLIER
        
        val words = mutableListOf<Pair<String, RunInfo?>>()
        for (run in runs) {
            if (run.text.isEmpty()) continue
            val runWords = run.text.split(" ")
            runWords.forEachIndexed { idx, word ->
                val w = if (idx < runWords.size - 1) "$word " else word
                if (w.isNotEmpty()) words.add(Pair(w, run))
            }
        }
        
        var lineWords = mutableListOf<Pair<String, RunInfo?>>()
        var lineWidth = 0f
        
        for ((word, run) in words) {
            val runFontSize = if (run != null && run.fontSize > 0) run.fontSize * RENDER_SCALE else defaultFontSize
            val paint = buildTextPaint(runFontSize, run?.bold ?: defaultBold, run?.italic ?: defaultItalic, isCode, run?.color ?: Color.rgb(30, 30, 30))
            val wordWidth = paint.measureText(word)
            
            if (lineWidth + wordWidth > maxWidth && lineWords.isNotEmpty()) {
                drawLine(canvas, lineWords, x, y, maxWidth, defaultFontSize, defaultBold, defaultItalic, isCode, alignment)
                y += lineHeight
                lineWords = mutableListOf()
                lineWidth = 0f
            }
            lineWords.add(Pair(word, run))
            lineWidth += wordWidth
        }
        if (lineWords.isNotEmpty()) {
            drawLine(canvas, lineWords, x, y, maxWidth, defaultFontSize, defaultBold, defaultItalic, isCode, alignment)
            y += lineHeight
        }
        return y
    }

    private fun drawLine(
        canvas: Canvas, words: List<Pair<String, RunInfo?>>, x: Float, y: Float, maxWidth: Float,
        defaultFontSize: Float, defaultBold: Boolean, defaultItalic: Boolean, isCode: Boolean, alignment: ParagraphAlignment
    ) {
        var totalWidth = 0f
        val paints = words.map { (word, run) ->
            val fs = if (run != null && run.fontSize > 0) run.fontSize * RENDER_SCALE else defaultFontSize
            val p = buildTextPaint(fs, run?.bold ?: defaultBold, run?.italic ?: defaultItalic, isCode, run?.color ?: Color.rgb(30, 30, 30))
            totalWidth += p.measureText(word)
            p
        }
        
        var startX = when (alignment) {
            ParagraphAlignment.CENTER -> x + (maxWidth - totalWidth) / 2f
            ParagraphAlignment.RIGHT -> x + maxWidth - totalWidth
            else -> x
        }
        
        words.forEachIndexed { index, (word, run) ->
            val p = paints[index]
            canvas.drawText(word, startX, y + defaultFontSize, p)
            val w = p.measureText(word)
            
            if (run?.underline == true) {
                val underlinePaint = Paint(p).apply { style = Paint.Style.STROKE; strokeWidth = 2f }
                canvas.drawLine(startX, y + defaultFontSize + 3f, startX + w, y + defaultFontSize + 3f, underlinePaint)
            }
            if (run?.strike == true) {
                val strikePaint = Paint(p).apply { style = Paint.Style.STROKE; strokeWidth = 2f }
                canvas.drawLine(startX, y + defaultFontSize * 0.6f, startX + w, y + defaultFontSize * 0.6f, strikePaint)
            }
            startX += w
        }
    }

    private fun drawTable(canvas: Canvas, table: DocElement.TableElement, startY: Float): Float {
        var y = startY + 12f
        if (table.rows.isEmpty()) return y
        val colCount = table.rows.maxOf { it.size }
        val colWidth = textWidth / colCount
        val rowHeight = BASE_FONT_SIZE * LINE_HEIGHT_MULTIPLIER + 16f
        
        for ((rowIndex, row) in table.rows.withIndex()) {
            val isHeader = rowIndex == 0
            val bgPaint = Paint().apply {
                color = if (isHeader) Color.rgb(200, 200, 200) else Color.WHITE
                style = Paint.Style.FILL
            }
            canvas.drawRect(MARGIN_LEFT, y, MARGIN_LEFT + textWidth, y + rowHeight, bgPaint)
            
            var x = MARGIN_LEFT
            for (cell in row) {
                val borderPaint = Paint().apply {
                    color = Color.rgb(180, 180, 180)
                    style = Paint.Style.STROKE
                    strokeWidth = 1f
                }
                canvas.drawRect(x, y, x + colWidth, y + rowHeight, borderPaint)
                val textColor = Color.rgb(30, 30, 30)
                val cellPaint = buildTextPaint(BASE_FONT_SIZE * 0.9f, isHeader || cell.bold, false, false, textColor)
                
                canvas.save()
                canvas.clipRect(x + 8f, y, x + colWidth - 8f, y + rowHeight)
                canvas.drawText(cell.text.take(30), x + 10f, y + rowHeight * 0.65f, cellPaint)
                canvas.restore()
                x += colWidth
            }
            y += rowHeight
        }
        return y + 16f
    }

    private fun buildTextPaint(fontSize: Float, bold: Boolean, italic: Boolean, monospace: Boolean, color: Int): Paint {
        return Paint().apply {
            this.color = color
            this.textSize = fontSize
            this.isAntiAlias = true
            typeface = when {
                monospace -> Typeface.MONOSPACE
                bold && italic -> Typeface.create(Typeface.DEFAULT, Typeface.BOLD_ITALIC)
                bold -> Typeface.DEFAULT_BOLD
                italic -> Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
                else -> Typeface.DEFAULT
            }
        }
    }

    private fun drawWrappedText(canvas: Canvas, text: String, paint: Paint, x: Float, startY: Float, maxWidth: Float, alignment: ParagraphAlignment): Float {
        val words = text.split(" ")
        var y = startY
        var line = StringBuilder()
        val lineHeight = paint.textSize * LINE_HEIGHT_MULTIPLIER
        
        for (word in words) {
            val testLine = if (line.isEmpty()) word else "$line $word"
            if (paint.measureText(testLine) > maxWidth && line.isNotEmpty()) {
                drawAlignedText(canvas, line.toString(), paint, x, y + paint.textSize, maxWidth, alignment)
                y += lineHeight
                line = StringBuilder(word)
            } else {
                line = StringBuilder(testLine)
            }
        }
        if (line.isNotEmpty()) {
            drawAlignedText(canvas, line.toString(), paint, x, y + paint.textSize, maxWidth, alignment)
            y += lineHeight
        }
        return y
    }

    private fun drawAlignedText(canvas: Canvas, text: String, paint: Paint, x: Float, y: Float, maxWidth: Float, alignment: ParagraphAlignment) {
        val textW = paint.measureText(text)
        val drawX = when (alignment) {
            ParagraphAlignment.CENTER -> x + (maxWidth - textW) / 2f
            ParagraphAlignment.RIGHT -> x + maxWidth - textW
            else -> x
        }
        canvas.drawText(text, drawX, y, paint)
    }

    private fun renderPlainText(text: String): List<Bitmap> {
        val bitmaps = mutableListOf<Bitmap>()
        val lines = text.split("\n")
        val paint = Paint().apply {
            color = Color.rgb(30, 30, 30)
            textSize = BASE_FONT_SIZE
            isAntiAlias = true
            typeface = Typeface.DEFAULT
        }
        val lineHeight = BASE_FONT_SIZE * LINE_HEIGHT_MULTIPLIER
        val linesPerPage = ((canvasHeight - MARGIN_TOP - MARGIN_BOTTOM) / lineHeight).toInt()
        
        var lineIndex = 0
        while (lineIndex < lines.size) {
            val bitmap = createNewPage()
            val canvas = Canvas(bitmap)
            var y = MARGIN_TOP
            for (i in lineIndex until minOf(lineIndex + linesPerPage, lines.size)) {
                canvas.drawText(lines[i], MARGIN_LEFT, y + BASE_FONT_SIZE, paint)
                y += lineHeight
            }
            bitmaps.add(bitmap)
            lineIndex += linesPerPage
        }
        return bitmaps
    }

    private fun parseColor(colorStr: String?): Int {
        if (colorStr == null || colorStr == "auto" || colorStr.length != 6) return Color.rgb(30, 30, 30)
        return try {
            val r = colorStr.substring(0, 2).toInt(16)
            val g = colorStr.substring(2, 4).toInt(16)
            val b = colorStr.substring(4, 6).toInt(16)
            Color.rgb(r, g, b)
        } catch (e: Exception) { Color.rgb(30, 30, 30) }
    }

    private fun isMonospaceFont(fontFamily: String?): Boolean {
        val f = fontFamily?.lowercase() ?: return false
        return f.contains("courier") || f.contains("consolas") || f.contains("mono")
    }
}
