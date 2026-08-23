package com.arkio.officeengine

import android.content.Context
import com.arkio.officeengine.model.*
import com.arkio.officeengine.renderer.OfficeRenderers
import com.arkio.officeengine.renderer.DocxPageRenderer
import com.arkio.officeengine.utils.FileUtils
import com.arkio.officeengine.utils.PdfBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ArkioOfficeEngine(private val context: Context) {
    suspend fun convertToPdf(inputPath: String): ConversionResult = withContext(Dispatchers.IO) {
        try {
            if (!FileUtils.isReadable(inputPath)) return@withContext ConversionResult(false, error = ConversionError(ErrorCode.FILE_NOT_FOUND, "File not found"))
            val ext = FileUtils.getExtension(inputPath)
            
            val bitmaps = when (ext) {
                "docx", "docm", "dotx" -> DocxPageRenderer.renderDocx(inputPath)
                "doc", "dot" -> DocxPageRenderer.renderDoc(inputPath)
                "xlsx", "xlsm", "xltx", "xls", "xlt" -> OfficeRenderers.renderXlsx(inputPath)
                "pptx", "pptm", "potx", "ppsx", "ppt", "pot", "pps" -> OfficeRenderers.renderPptx(inputPath)
                else -> emptyList()
            }
            
            if (bitmaps.isEmpty()) return@withContext ConversionResult(false, error = ConversionError(ErrorCode.CONVERSION_FAILED, "Render failed or format unsupported"))
            
            val out = FileUtils.getOutputPdfPath(context, inputPath)
            if (PdfBuilder.buildFromBitmaps(bitmaps, out)) {
                ConversionResult(true, out, bitmaps.size, originalFormat = ext.uppercase())
            } else {
                ConversionResult(false, error = ConversionError(ErrorCode.CONVERSION_FAILED, "PDF build failed"))
            }
        } catch (e: Exception) { 
            ConversionResult(false, error = ConversionError(ErrorCode.CONVERSION_FAILED, e.message ?: "Unknown error")) 
        }
    }
}
