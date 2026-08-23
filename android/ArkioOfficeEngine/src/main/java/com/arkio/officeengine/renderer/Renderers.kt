package com.arkio.officeengine.renderer

import android.graphics.*
import org.apache.poi.xslf.usermodel.*
import org.apache.poi.ss.usermodel.DataFormatter
import org.apache.poi.ss.usermodel.FormulaEvaluator
import org.apache.poi.ss.usermodel.Sheet as ExcelSheet
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.apache.poi.xwpf.usermodel.*
import java.io.File
import java.io.FileInputStream

object OfficeRenderers {
    private const val RENDER_SCALE = 2.0f
    private const val PAGE_WIDTH_PX = 1240
    private const val PAGE_HEIGHT_PX = 1754

    fun renderPptx(path: String): List<Bitmap> {
        val bitmaps = mutableListOf<Bitmap>()
        FileInputStream(File(path)).use { fis ->
            val pptx = XMLSlideShow(fis)
            val pgW = 720f; val pgH = 540f
            val rW = (pgW * RENDER_SCALE).toInt()
            val rH = (pgH * RENDER_SCALE).toInt()

            pptx.slides.forEach { slide ->
                val b = Bitmap.createBitmap(rW, rH, Bitmap.Config.ARGB_8888)
                val c = Canvas(b)
                c.drawColor(android.graphics.Color.WHITE)
                
                slide.shapes.forEach { shape ->
                    try {
                        val shapeClass = shape.javaClass
                        if (XSLFTextShape::class.java.isAssignableFrom(shapeClass)) {
                            drawTextShape(c, shape as XSLFTextShape)
                        } else if (XSLFPictureShape::class.java.isAssignableFrom(shapeClass)) {
                            drawPictureShape(c, shape as XSLFPictureShape)
                        } else if (XSLFConnectorShape::class.java.isAssignableFrom(shapeClass)) {
                            drawConnectorShape(c, shape as XSLFConnectorShape)
                        } else if (XSLFSimpleShape::class.java.isAssignableFrom(shapeClass)) {
                            drawSimpleShape(c, shape as XSLFSimpleShape)
                        }
                    } catch (e: Exception) {}
                }
                bitmaps.add(b)
            }
        }
        return bitmaps
    }

    private fun getRefVal(obj: Any?, methodName: String): Double {
        if (obj == null) return 0.0
        return try {
            val method = obj.javaClass.getMethod(methodName)
            (method.invoke(obj) as Number).toDouble()
        } catch (e: Exception) { 0.0 }
    }

    private fun drawTextShape(canvas: Canvas, shape: XSLFTextShape) {
        try {
            val anchor = shape.javaClass.getMethod("getAnchor").invoke(shape) ?: return
            val ax = getRefVal(anchor, "getX").toFloat()
            val ay = getRefVal(anchor, "getY").toFloat()
            val x = ax * RENDER_SCALE
            val y = ay * RENDER_SCALE
            
            val paint = Paint().apply {
                isAntiAlias = true
                color = android.graphics.Color.BLACK
            }

            shape.getTextParagraphs().forEachIndexed { index, para ->
                val py = y + (index * 40f * RENDER_SCALE)
                para.getTextRuns().forEach { run ->
                    paint.textSize = (run.getFontSize() ?: 18.0).toFloat() * RENDER_SCALE
                    try {
                        val runPaint = run.javaClass.getMethod("getFontColor").invoke(run)
                        if (runPaint != null && runPaint.javaClass.name.contains("SolidPaint")) {
                            val colorStyle = runPaint.javaClass.getMethod("getSolidColor").invoke(runPaint)
                            if (colorStyle != null) {
                                val alpha = getRefVal(colorStyle, "getAlpha")
                                val red = getRefVal(colorStyle, "getRed")
                                val green = getRefVal(colorStyle, "getGreen")
                                val blue = getRefVal(colorStyle, "getBlue")
                                paint.color = android.graphics.Color.argb(
                                    (alpha * 255).toInt(),
                                    (red * 255).toInt(),
                                    (green * 255).toInt(),
                                    (blue * 255).toInt()
                                )
                            }
                        }
                    } catch (e: Exception) {}
                    canvas.drawText(run.getRawText(), x, py, paint)
                }
            }
        } catch (e: Exception) {}
    }

    private fun drawPictureShape(canvas: Canvas, shape: XSLFPictureShape) {
        try {
            val anchor = shape.javaClass.getMethod("getAnchor").invoke(shape) ?: return
            val data = shape.getPictureData().getData()
            val bitmap = BitmapFactory.decodeByteArray(data, 0, data.size)
            if (bitmap != null) {
                val ax = getRefVal(anchor, "getX").toFloat()
                val ay = getRefVal(anchor, "getY").toFloat()
                val aw = getRefVal(anchor, "getWidth").toFloat()
                val ah = getRefVal(anchor, "getHeight").toFloat()
                
                val rect = RectF(
                    ax * RENDER_SCALE,
                    ay * RENDER_SCALE,
                    (ax + aw) * RENDER_SCALE,
                    (ay + ah) * RENDER_SCALE
                )
                canvas.drawBitmap(bitmap, null, rect, Paint(Paint.FILTER_BITMAP_FLAG))
                bitmap.recycle()
            }
        } catch (e: Exception) {}
    }

    private fun drawConnectorShape(canvas: Canvas, shape: XSLFConnectorShape) {
        try {
            val anchor = shape.javaClass.getMethod("getAnchor").invoke(shape) ?: return
            val paint = Paint().apply {
                color = android.graphics.Color.GRAY
                strokeWidth = 2f * RENDER_SCALE
                style = Paint.Style.STROKE
            }
            val ax = getRefVal(anchor, "getX").toFloat()
            val ay = getRefVal(anchor, "getY").toFloat()
            val aw = getRefVal(anchor, "getWidth").toFloat()
            val ah = getRefVal(anchor, "getHeight").toFloat()

            canvas.drawLine(
                ax * RENDER_SCALE,
                ay * RENDER_SCALE,
                (ax + aw) * RENDER_SCALE,
                (ay + ah) * RENDER_SCALE,
                paint
            )
        } catch (e: Exception) {}
    }

    private fun drawSimpleShape(canvas: Canvas, shape: XSLFSimpleShape) {
        try {
            val anchor = shape.javaClass.getMethod("getAnchor").invoke(shape) ?: return
            val paint = Paint().apply {
                color = android.graphics.Color.LTGRAY
                style = Paint.Style.STROKE
                strokeWidth = 1f * RENDER_SCALE
            }
            val ax = getRefVal(anchor, "getX").toFloat()
            val ay = getRefVal(anchor, "getY").toFloat()
            val aw = getRefVal(anchor, "getWidth").toFloat()
            val ah = getRefVal(anchor, "getHeight").toFloat()

            canvas.drawRect(
                ax * RENDER_SCALE,
                ay * RENDER_SCALE,
                (ax + aw) * RENDER_SCALE,
                (ay + ah) * RENDER_SCALE,
                paint
            )
        } catch (e: Exception) {}
    }

    fun renderXlsx(path: String): List<Bitmap> {
        val bitmaps = mutableListOf<Bitmap>()
        FileInputStream(File(path)).use { fis ->
            val wb = XSSFWorkbook(fis); val eval = wb.creationHelper.createFormulaEvaluator()
            for (i in 0 until wb.numberOfSheets) { 
                val s = wb.getSheetAt(i)
                if (wb.isSheetHidden(i) || s.lastRowNum < 0) continue
                bitmaps.addAll(renderSheet(s, eval)) 
            }
            wb.close()
        }
        return bitmaps
    }

    private fun renderSheet(s: ExcelSheet, ev: FormulaEvaluator): List<Bitmap> {
        val bitmaps = mutableListOf<Bitmap>()
        val cW = (PAGE_WIDTH_PX * RENDER_SCALE).toInt()
        val cH = (PAGE_HEIGHT_PX * RENDER_SCALE).toInt()
        
        var maxC = 0
        for (rn in s.firstRowNum..minOf(s.lastRowNum, 200)) {
            s.getRow(rn)?.let { if (it.lastCellNum > maxC) maxC = it.lastCellNum.toInt() }
        }
        if (maxC == 0) return emptyList()
        
        val cWs = FloatArray(maxC) { c -> ((s.getColumnWidth(c) / 256f) * 7f * RENDER_SCALE).coerceIn(100f, 800f) }
        val rH = 50f * RENDER_SCALE
        val rpp = (cH / rH).toInt()
        var cR = s.firstRowNum
        
        while (cR <= s.lastRowNum && bitmaps.size < 50) {
            val b = Bitmap.createBitmap(cW, cH, Bitmap.Config.ARGB_8888)
            val c = Canvas(b).apply { drawColor(android.graphics.Color.WHITE) }
            var y = 40f * RENDER_SCALE
            val df = DataFormatter()
            val eR = minOf(cR + rpp - 1, s.lastRowNum)
            
            for (rn in cR..eR) {
                val r = s.getRow(rn)
                var x = 40f * RENDER_SCALE
                for (cn in 0 until maxC) {
                    val w = cWs[cn]
                    c.drawRect(x, y, x + w, y + rH, Paint().apply { color = android.graphics.Color.LTGRAY; style = Paint.Style.STROKE })
                    r?.getCell(cn)?.let { cell ->
                        val valStr = df.formatCellValue(cell, ev)
                        if (valStr.isNotEmpty()) {
                            c.drawText(valStr, x + 10f, y + (rH * 0.7f), Paint().apply { textSize = 14f * RENDER_SCALE; isAntiAlias = true })
                        }
                    }
                    x += w
                }
                y += rH
            }
            bitmaps.add(b)
            cR = eR + 1
        }
        return bitmaps
    }

    fun renderDocx(path: String): List<Bitmap> {
        val bitmaps = mutableListOf<Bitmap>()
        FileInputStream(File(path)).use { fis ->
            val doc = XWPFDocument(fis)
            val buffer = StringBuilder()
            doc.getParagraphs().forEach { buffer.append(it.getText()).append("\n") }
            bitmaps.addAll(renderText(buffer.toString()))
            doc.close()
        }
        return bitmaps
    }

    private fun renderText(text: String): List<Bitmap> {
        val bitmaps = mutableListOf<Bitmap>()
        val cW = (PAGE_WIDTH_PX * RENDER_SCALE).toInt()
        val cH = (PAGE_HEIGHT_PX * RENDER_SCALE).toInt()
        val paint = Paint().apply { textSize = 16f * RENDER_SCALE; isAntiAlias = true }
        val lh = 24f * RENDER_SCALE
        val lpp = ((cH - 200f) / lh).toInt()
        val lines = text.split("\n")
        var i = 0
        while (i < lines.size && bitmaps.size < 100) {
            val b = Bitmap.createBitmap(cW, cH, Bitmap.Config.ARGB_8888)
            val c = Canvas(b).apply { drawColor(android.graphics.Color.WHITE) }
            var y = 100f * RENDER_SCALE
            for (j in i until minOf(i + lpp, lines.size)) {
                c.drawText(lines[j], 100f * RENDER_SCALE, y, paint)
                y += lh
            }
            bitmaps.add(b)
            i += lpp
        }
        return bitmaps
    }
}
