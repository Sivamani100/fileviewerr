package com.arkio.officeengine.renderer

import android.graphics.*
import org.apache.poi.hssf.usermodel.HSSFWorkbook
import org.apache.poi.ss.usermodel.*
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.io.FileInputStream

object XlsxPageRenderer {

    private const val PAGE_WIDTH_PX = 1240
    private const val PAGE_HEIGHT_PX = 1754
    private const val RENDER_SCALE = 2f
    private const val PADDING = 60f
    private const val HEADER_HEIGHT = 40f
    private const val ROW_HEIGHT = 32f
    private const val MIN_COL_WIDTH = 80f
    private const val MAX_COL_WIDTH = 300f

    fun renderXlsx(filePath: String): List<Bitmap> {
        FileInputStream(File(filePath)).use { fis ->
            val workbook = XSSFWorkbook(fis)
            return renderWorkbook(workbook)
        }
    }

    fun renderXls(filePath: String): List<Bitmap> {
        FileInputStream(File(filePath)).use { fis ->
            val workbook = HSSFWorkbook(fis)
            return renderWorkbook(workbook)
        }
    }

    private fun renderWorkbook(workbook: Workbook): List<Bitmap> {
        val allBitmaps = mutableListOf<Bitmap>()
        val evaluator = workbook.creationHelper.createFormulaEvaluator()
        for (sheetIndex in 0 until workbook.numberOfSheets) {
            val sheet = workbook.getSheetAt(sheetIndex)
            if (workbook.isSheetHidden(sheetIndex)) continue
            val lastRow = sheet.lastRowNum
            if (lastRow < 0) continue
            var maxCols = 0
            for (r in sheet.firstRowNum..lastRow) {
                val row = sheet.getRow(r) ?: continue
                if (row.lastCellNum > maxCols) maxCols = row.lastCellNum.toInt()
            }
            if (maxCols == 0) continue
            val colWidths = calculateColumnWidths(sheet, maxCols)
            allBitmaps.addAll(renderSheet(sheet, evaluator, workbook.getSheetName(sheetIndex), sheet.firstRowNum, lastRow, maxCols, colWidths, colWidths.sum()))
        }
        workbook.close()
        return allBitmaps
    }

    private fun calculateColumnWidths(sheet: Sheet, maxCols: Int): FloatArray {
        val widths = FloatArray(maxCols)
        for (col in 0 until maxCols) {
            widths[col] = ((sheet.getColumnWidth(col) / 256f) * 7f * RENDER_SCALE).coerceIn(MIN_COL_WIDTH * RENDER_SCALE, MAX_COL_WIDTH * RENDER_SCALE)
        }
        return widths
    }

    private fun renderSheet(sheet: Sheet, evaluator: FormulaEvaluator, sheetName: String, firstRow: Int, lastRow: Int, maxCols: Int, colWidths: FloatArray, totalWidth: Float): List<Bitmap> {
        val bitmaps = mutableListOf<Bitmap>()
        val canvasWidth = (PAGE_WIDTH_PX * RENDER_SCALE).toInt()
        val canvasHeight = (PAGE_HEIGHT_PX * RENDER_SCALE).toInt()
        val usableWidth = canvasWidth - (PADDING * 2 * RENDER_SCALE)
        val usableHeight = canvasHeight - (PADDING * 2 * RENDER_SCALE) - (HEADER_HEIGHT * RENDER_SCALE)
        val scaleToFit = if (totalWidth > usableWidth) usableWidth / totalWidth else 1f
        val scaledColWidths = colWidths.map { it * scaleToFit }.toFloatArray()
        val scaledRowHeight = ROW_HEIGHT * RENDER_SCALE * scaleToFit
        val scaledHeaderHeight = HEADER_HEIGHT * RENDER_SCALE
        val rowsPerPage = (usableHeight / scaledRowHeight).toInt()
        
        var currentRow = firstRow
        var pageNum = 1
        while (currentRow <= lastRow) {
            val bitmap = Bitmap.createBitmap(canvasWidth, canvasHeight, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            canvas.drawColor(Color.WHITE)
            val pad = PADDING * RENDER_SCALE
            drawSheetHeader(canvas, sheetName, pageNum, pad, scaledHeaderHeight, canvasWidth)
            var y = pad + scaledHeaderHeight
            val endRow = minOf(currentRow + rowsPerPage - 1, lastRow)
            for (rowNum in currentRow..endRow) {
                val row = sheet.getRow(rowNum)
                var x = pad
                val rowBg = if ((rowNum - firstRow) % 2 == 0) Color.rgb(250, 250, 250) else Color.WHITE
                canvas.drawRect(pad, y, canvasWidth - pad, y + scaledRowHeight, Paint().apply { color = rowBg; style = Paint.Style.FILL })
                for (colNum in 0 until maxCols) {
                    val cellWidth = scaledColWidths.getOrElse(colNum) { MIN_COL_WIDTH }
                    canvas.drawRect(x, y, x + cellWidth, y + scaledRowHeight, Paint().apply { color = Color.rgb(200, 200, 200); style = Paint.Style.STROKE; strokeWidth = 1f })
                    val cell = row?.getCell(colNum)
                    if (cell != null) drawCellContent(canvas, getCellValue(cell, evaluator), cell.cellStyle, x, y, cellWidth, scaledRowHeight, scaleToFit)
                    x += cellWidth
                }
                y += scaledRowHeight
            }
            bitmaps.add(bitmap); currentRow = endRow + 1; pageNum++
        }
        return bitmaps
    }

    private fun drawSheetHeader(canvas: Canvas, sheetName: String, pageNum: Int, padding: Float, headerHeight: Float, canvasWidth: Int) {
        canvas.drawRect(padding, padding, canvasWidth - padding, padding + headerHeight, Paint().apply { color = Color.rgb(70, 130, 180); style = Paint.Style.FILL })
        canvas.drawText("$sheetName — Page $pageNum", padding + 20f, padding + headerHeight * 0.7f, Paint().apply { color = Color.WHITE; textSize = 28f * RENDER_SCALE; typeface = Typeface.DEFAULT_BOLD; isAntiAlias = true })
    }

    private fun drawCellContent(canvas: Canvas, value: String, style: CellStyle?, x: Float, y: Float, width: Float, height: Float, scale: Float) {
        if (value.isEmpty()) return
        val isBold = style?.font?.bold ?: false
        val fontSize = ((style?.font?.fontHeightInPoints ?: 10).toFloat() * RENDER_SCALE * scale).coerceIn(16f, 36f)
        val alignment = style?.alignment ?: HorizontalAlignment.LEFT
        val paint = Paint().apply { color = Color.rgb(30, 30, 30); textSize = fontSize; typeface = if (isBold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT; isAntiAlias = true }
        canvas.save(); canvas.clipRect(x + 4f, y, x + width - 4f, y + height)
        val textWidth = paint.measureText(value)
        val textX = when (alignment) { HorizontalAlignment.CENTER -> x + (width - textWidth) / 2f; HorizontalAlignment.RIGHT -> x + width - textWidth - 8f; else -> x + 8f }
        canvas.drawText(value, textX, y + height * 0.65f, paint); canvas.restore()
    }

    private fun getCellValue(cell: Cell, evaluator: FormulaEvaluator): String {
        return try {
            when (cell.cellType) {
                CellType.STRING -> cell.stringCellValue
                CellType.NUMERIC -> if (DateUtil.isCellDateFormatted(cell)) cell.dateCellValue.toString() else { val num = cell.numericCellValue; if (num == num.toLong().toDouble()) num.toLong().toString() else "%.2f".format(num) }
                CellType.BOOLEAN -> cell.booleanCellValue.toString()
                CellType.FORMULA -> try { val eval = evaluator.evaluate(cell); when (eval.cellType) { CellType.STRING -> eval.stringValue; CellType.NUMERIC -> "%.2f".format(eval.numberValue); CellType.BOOLEAN -> eval.booleanValue.toString(); else -> "" } } catch (e: Exception) { cell.toString() }
                else -> ""
            }
        } catch (e: Exception) { "" }
    }
}
