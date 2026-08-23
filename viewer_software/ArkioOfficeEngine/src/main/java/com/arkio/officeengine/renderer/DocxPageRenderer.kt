package com.arkio.officeengine.renderer

import android.graphics.*
import org.apache.poi.hwpf.HWPFDocument
import org.apache.poi.xwpf.usermodel.*
import java.io.File
import java.io.FileInputStream

object DocxPageRenderer {

    private const val PAGE_WIDTH_PX = 1240
    private const val PAGE_HEIGHT_PX = 1754
    private const val RENDER_SCALE = 2f
    private const val MARGIN_LEFT = 120f * RENDER_SCALE
    private const val MARGIN_RIGHT = 120f * RENDER_SCALE
    private const val MARGIN_TOP = 120f * RENDER_SCALE
    private const val MARGIN_BOTTOM = 120f * RENDER_SCALE
    private const val BASE_FONT_SIZE = 26f
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
            paginateElements(extractElements(doc), bitmaps)
            doc.close()
        }
        return bitmaps
    }

    fun renderDoc(filePath: String): List<Bitmap> {
        FileInputStream(File(filePath)).use { fis ->
            val doc = HWPFDocument(fis)
            val text = doc.documentText; doc.close()
            return renderPlainText(text)
        }
    }

    private sealed class DocElement {
        data class Paragraph(val text: String, val style: ParagraphStyle, val alignment: ParagraphAlignment, val runs: List<RunInfo>, val isHeading: Boolean, val headingLevel: Int, val isBullet: Boolean, val bulletIndent: Int, val spaceBefore: Float, val spaceAfter: Float) : DocElement()
        data class TableElement(val rows: List<List<CellInfo>>) : DocElement()
        data class ImageElement(val bitmap: Bitmap, val widthPx: Float, val heightPx: Float) : DocElement()
        object PageBreak : DocElement()
        object EmptyLine : DocElement()
    }

    data class RunInfo(val text: String, val bold: Boolean, val italic: Boolean, val underline: Boolean, val strike: Boolean, val fontSize: Float, val color: Int, val isMonospace: Boolean)
    data class CellInfo(val text: String, val bold: Boolean, val isHeader: Boolean, val alignment: ParagraphAlignment)
    enum class ParagraphStyle { NORMAL, HEADING1, HEADING2, HEADING3, HEADING4, TITLE, SUBTITLE, QUOTE, CODE, BULLET, NUMBERED }

    private fun extractElements(doc: XWPFDocument): List<DocElement> {
        val elements = mutableListOf<DocElement>()
        for (element in doc.bodyElements) {
            when (element) {
                is XWPFParagraph -> extractParagraph(element)?.let { elements.add(it) }
                is XWPFTable -> extractTable(element)?.let { elements.add(it) }
            }
        }
        return elements
    }

    private fun extractParagraph(para: XWPFParagraph): DocElement? {
        if (para.isPageBreak) return DocElement.PageBreak
        val runs = para.runs.map { RunInfo(it.text() ?: "", it.isBold, it.isItalic, it.underline != UnderlinePatterns.NONE, it.isStrikeThrough, (it.fontSize.takeIf { f -> f > 0 } ?: 11).toFloat(), parseColor(it.color), isMonospaceFont(it.fontFamily)) }
        val fullText = runs.joinToString("") { it.text }
        if (fullText.isBlank() && runs.isEmpty()) return DocElement.EmptyLine
        val (isH, hL, pS) = detectStyle(para.styleID ?: "", para.style ?: "")
        return DocElement.Paragraph(fullText, pS, para.alignment ?: ParagraphAlignment.LEFT, runs, isH, hL, para.numID != null && (para.numID?.toLong() ?: 0L) > 0, (para.indentationLeft / 720).coerceIn(0, 5), (para.spacingBefore / 20f).coerceAtLeast(0f), (para.spacingAfter / 20f).coerceAtLeast(4f))
    }

    private fun detectStyle(sId: String, sN: String): Triple<Boolean, Int, ParagraphStyle> {
        val id = sId.lowercase(); val name = sN.lowercase()
        return when {
            id.contains("heading1") || name.contains("heading 1") || id == "title" || name == "title" -> Triple(true, 1, ParagraphStyle.HEADING1)
            id.contains("heading2") || name.contains("heading 2") || name.contains("subtitle") -> Triple(true, 2, ParagraphStyle.HEADING2)
            id.contains("heading3") || name.contains("heading 3") -> Triple(true, 3, ParagraphStyle.HEADING3)
            name.contains("quote") -> Triple(false, 0, ParagraphStyle.QUOTE)
            name.contains("code") -> Triple(false, 0, ParagraphStyle.CODE)
            else -> Triple(false, 0, ParagraphStyle.NORMAL)
        }
    }

    private fun extractTable(table: XWPFTable): DocElement.TableElement? {
        val rows = table.rows.map { row -> row.tableCells.map { cell -> CellInfo(cell.paragraphs.joinToString("\n") { p -> p.runs.joinToString("") { it.text() ?: "" } }.trim(), cell.paragraphs.firstOrNull()?.runs?.firstOrNull()?.isBold ?: false, false, cell.paragraphs.firstOrNull()?.alignment ?: ParagraphAlignment.LEFT) } }
        return if (rows.isNotEmpty()) DocElement.TableElement(rows) else null
    }

    private fun paginateElements(elements: List<DocElement>, bitmaps: MutableList<Bitmap>) {
        var bitmap = createNewPage(); var canvas = Canvas(bitmap); var y = MARGIN_TOP
        fun newPage() { bitmaps.add(bitmap); bitmap = createNewPage(); canvas = Canvas(bitmap); y = MARGIN_TOP }
        for (element in elements) {
            when (element) {
                is DocElement.PageBreak -> newPage()
                is DocElement.EmptyLine -> { y += BASE_FONT_SIZE * LINE_HEIGHT_MULTIPLIER; if (y > canvasHeight - MARGIN_BOTTOM) newPage() }
                is DocElement.Paragraph -> { val h = measureParagraphHeight(element); if (y + h > canvasHeight - MARGIN_BOTTOM && y > MARGIN_TOP) newPage(); y = drawParagraph(canvas, element, y) }
                is DocElement.TableElement -> { val h = measureTableHeight(element); if (y + h > canvasHeight - MARGIN_BOTTOM && y > MARGIN_TOP) newPage(); y = drawTable(canvas, element, y) }
                is DocElement.ImageElement -> { if (y + element.heightPx > canvasHeight - MARGIN_BOTTOM && y > MARGIN_TOP) newPage(); canvas.drawBitmap(element.bitmap, null, RectF(MARGIN_LEFT, y, MARGIN_LEFT + element.widthPx, y + element.heightPx), null); y += element.heightPx + 16f }
            }
        }
        if (y > MARGIN_TOP) bitmaps.add(bitmap)
    }

    private fun createNewPage(): Bitmap = Bitmap.createBitmap(canvasWidth, canvasHeight, Bitmap.Config.ARGB_8888).apply { Canvas(this).drawColor(Color.WHITE) }

    private fun measureParagraphHeight(para: DocElement.Paragraph): Float {
        val fs = when (para.headingLevel) { 1 -> HEADING1_SIZE; 2 -> HEADING2_SIZE; 3 -> HEADING3_SIZE; else -> BASE_FONT_SIZE }
        return ((para.text.length / (textWidth / (fs * 0.6f)).toInt().coerceAtLeast(1)) + 1) * fs * LINE_HEIGHT_MULTIPLIER + para.spaceBefore + para.spaceAfter
    }

    private fun measureTableHeight(table: DocElement.TableElement): Float = table.rows.size * (BASE_FONT_SIZE * LINE_HEIGHT_MULTIPLIER + 16f) + 32f

    private fun drawParagraph(canvas: Canvas, para: DocElement.Paragraph, startY: Float): Float {
        var y = startY + para.spaceBefore
        if (para.text.isEmpty()) return y + BASE_FONT_SIZE * LINE_HEIGHT_MULTIPLIER + para.spaceAfter
        val fs = when (para.headingLevel) { 1 -> HEADING1_SIZE; 2 -> HEADING2_SIZE; 3 -> HEADING3_SIZE; else -> BASE_FONT_SIZE }
        val leftI = MARGIN_LEFT + (para.bulletIndent * 40f)
        if (para.style == ParagraphStyle.CODE) canvas.drawRoundRect(RectF(leftI - 8f, y - 4f, canvasWidth - MARGIN_RIGHT + 8f, y + measureParagraphHeight(para)), 8f, 8f, Paint().apply { color = Color.rgb(245, 245, 245); style = Paint.Style.FILL })
        if (para.isBullet) canvas.drawText("•", leftI - 30f, y + fs, Paint().apply { color = Color.rgb(80, 80, 80); textSize = fs; isAntiAlias = true })
        return if (para.runs.isNotEmpty()) drawRichText(canvas, para.runs, leftI, y, canvasWidth - leftI - MARGIN_RIGHT, fs, para.isHeading || para.runs.all { it.bold }, para.runs.all { it.italic }, para.style == ParagraphStyle.CODE, para.alignment) else drawWrappedText(canvas, para.text, buildTextPaint(fs, para.isHeading, false, false, Color.rgb(30, 30, 30)), leftI, y, canvasWidth - leftI - MARGIN_RIGHT, para.alignment) + para.spaceAfter
    }

    private fun drawRichText(canvas: Canvas, runs: List<RunInfo>, x: Float, startY: Float, maxWidth: Float, defFs: Float, defB: Boolean, defI: Boolean, isC: Boolean, align: ParagraphAlignment): Float {
        var y = startY; var lineWords = mutableListOf<Pair<String, RunInfo?>>(); var lineW = 0f
        val words = runs.flatMap { r -> r.text.split(" ").filter { it.isNotEmpty() }.map { it to r } }
        for ((word, run) in words) {
            val p = buildTextPaint(if (run != null && run.fontSize > 0) run.fontSize * RENDER_SCALE else defFs, run?.bold ?: defB, run?.italic ?: defI, isC, run?.color ?: Color.rgb(30, 30, 30))
            val wW = p.measureText("$word ")
            if (lineW + wW > maxWidth && lineWords.isNotEmpty()) { drawLine(canvas, lineWords, x, y, maxWidth, defFs, defB, defI, isC, align); y += defFs * LINE_HEIGHT_MULTIPLIER; lineWords = mutableListOf(); lineW = 0f }
            lineWords.add(word to run); lineW += wW
        }
        if (lineWords.isNotEmpty()) { drawLine(canvas, lineWords, x, y, maxWidth, defFs, defB, defI, isC, align); y += defFs * LINE_HEIGHT_MULTIPLIER }
        return y
    }

    private fun drawLine(canvas: Canvas, words: List<Pair<String, RunInfo?>>, x: Float, y: Float, maxWidth: Float, defFs: Float, defB: Boolean, defI: Boolean, isC: Boolean, align: ParagraphAlignment) {
        var totalW = 0f; val paints = words.map { (w, r) -> buildTextPaint(if (r != null && r.fontSize > 0) r.fontSize * RENDER_SCALE else defFs, r?.bold ?: defB, r?.italic ?: defI, isC, r?.color ?: Color.rgb(30, 30, 30)).also { totalW += it.measureText("$w ") } }
        var startX = when (align) { ParagraphAlignment.CENTER -> x + (maxWidth - totalW) / 2f; ParagraphAlignment.RIGHT -> x + maxWidth - totalW; else -> x }
        words.forEachIndexed { i, (w, r) -> val p = paints[i]; canvas.drawText(w, startX, y + defFs, p); if (r?.underline == true) canvas.drawLine(startX, y + defFs + 3f, startX + p.measureText(w), y + defFs + 3f, Paint(p).apply { style = Paint.Style.STROKE; strokeWidth = 2f }); if (r?.strike == true) canvas.drawLine(startX, y + defFs * 0.6f, startX + p.measureText(w), y + defFs * 0.6f, Paint(p).apply { style = Paint.Style.STROKE; strokeWidth = 2f }); startX += p.measureText("$w ") }
    }

    private fun drawTable(canvas: Canvas, table: DocElement.TableElement, startY: Float): Float {
        var y = startY + 12f; if (table.rows.isEmpty()) return y
        val colW = textWidth / table.rows.maxOf { it.size }; val rowH = BASE_FONT_SIZE * LINE_HEIGHT_MULTIPLIER + 16f
        for ((rI, row) in table.rows.withIndex()) {
            val isH = rI == 0; canvas.drawRect(MARGIN_LEFT, y, MARGIN_LEFT + textWidth, y + rowH, Paint().apply { color = if (isH) Color.rgb(41, 98, 255) else if (rI % 2 == 0) Color.rgb(248, 248, 248) else Color.WHITE; style = Paint.Style.FILL })
            var x = MARGIN_LEFT
            for (cell in row) { canvas.drawRect(x, y, x + colW, y + rowH, Paint().apply { color = Color.rgb(180, 180, 180); style = Paint.Style.STROKE; strokeWidth = 1f }); canvas.save(); canvas.clipRect(x + 8f, y, x + colW - 8f, y + rowH); canvas.drawText(cell.text, x + 10f, y + rowH * 0.65f, buildTextPaint(BASE_FONT_SIZE, isH || cell.bold, false, false, if (isH) Color.WHITE else Color.rgb(30, 30, 30))); canvas.restore(); x += colW }
            y += rowH
        }
        return y + 16f
    }

    private fun buildTextPaint(fs: Float, b: Boolean, i: Boolean, m: Boolean, c: Int): Paint = Paint().apply { color = c; textSize = fs; isAntiAlias = true; typeface = when { m -> Typeface.MONOSPACE; b && i -> Typeface.create(Typeface.DEFAULT, Typeface.BOLD_ITALIC); b -> Typeface.DEFAULT_BOLD; i -> Typeface.create(Typeface.DEFAULT, Typeface.ITALIC); else -> Typeface.DEFAULT } }

    private fun drawWrappedText(canvas: Canvas, text: String, p: Paint, x: Float, startY: Float, maxWidth: Float, align: ParagraphAlignment): Float {
        var y = startY; var line = ""; val lh = p.textSize * LINE_HEIGHT_MULTIPLIER
        for (word in text.split(" ")) { val test = if (line.isEmpty()) word else "$line $word"; if (p.measureText(test) > maxWidth && line.isNotEmpty()) { drawAlignedText(canvas, line, p, x, y + p.textSize, maxWidth, align); y += lh; line = word } else line = test }
        if (line.isNotEmpty()) drawAlignedText(canvas, line, p, x, y + p.textSize, maxWidth, align)
        return y + lh
    }

    private fun drawAlignedText(canvas: Canvas, text: String, p: Paint, x: Float, y: Float, maxWidth: Float, align: ParagraphAlignment) { val tw = p.measureText(text); canvas.drawText(text, when (align) { ParagraphAlignment.CENTER -> x + (maxWidth - tw) / 2f; ParagraphAlignment.RIGHT -> x + maxWidth - tw; else -> x }, y, p) }

    private fun renderPlainText(text: String): List<Bitmap> {
        val bitmaps = mutableListOf<Bitmap>(); val lines = text.split("\n"); val p = Paint().apply { color = Color.rgb(30, 30, 30); textSize = BASE_FONT_SIZE; isAntiAlias = true; typeface = Typeface.DEFAULT }
        val lh = BASE_FONT_SIZE * LINE_HEIGHT_MULTIPLIER; val lpp = ((canvasHeight - MARGIN_TOP - MARGIN_BOTTOM) / lh).toInt()
        var i = 0; while (i < lines.size) { val b = createNewPage(); val c = Canvas(b); var y = MARGIN_TOP; for (j in i until minOf(i + lpp, lines.size)) { c.drawText(lines[j], MARGIN_LEFT, y + BASE_FONT_SIZE, p); y += lh }; bitmaps.add(b); i += lpp }
        return bitmaps
    }

    private fun parseColor(cStr: String?): Int { if (cStr == null || cStr == "auto" || cStr.length != 6) return Color.rgb(30, 30, 30); return try { Color.rgb(cStr.substring(0, 2).toInt(16), cStr.substring(2, 4).toInt(16), cStr.substring(4, 6).toInt(16)) } catch (e: Exception) { Color.rgb(30, 30, 30) } }
    private fun isMonospaceFont(fF: String?): Boolean { val f = fF?.lowercase() ?: return false; return f.contains("courier") || f.contains("consola") || f.contains("mono") }
}
