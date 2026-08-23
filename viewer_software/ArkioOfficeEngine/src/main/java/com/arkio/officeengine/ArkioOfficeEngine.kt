package com.arkio.officeengine

import android.content.Context
import com.arkio.officeengine.model.ConversionError
import com.arkio.officeengine.model.ConversionResult
import com.arkio.officeengine.model.ErrorCode
import com.arkio.officeengine.renderer.DocxPageRenderer
import com.arkio.officeengine.renderer.PptxPageRenderer
import com.arkio.officeengine.renderer.XlsxPageRenderer
import com.arkio.officeengine.utils.FileUtils
import com.arkio.officeengine.utils.PdfBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * ArkioOfficeEngine
 * 
 * Main entry point for converting Office documents to PDF.
 */
class ArkioOfficeEngine(private val context: Context) {

    companion object {
        private val SUPPORTED_FORMATS = setOf(
            "docx", "doc", "docm", "dotx", "dot",
            "xlsx", "xls", "xlsm", "xltx", "xlt",
            "pptx", "ppt", "pptm", "potx", "pot", "ppsx", "pps"
        )
    }

    suspend fun convertToPdf(inputFilePath: String): ConversionResult {
        return withContext(Dispatchers.IO) {
            val startTime = System.currentTimeMillis()
            
            try {
                if (!FileUtils.isReadable(inputFilePath)) {
                    return@withContext ConversionResult(
                        success = false,
                        error = ConversionError(ErrorCode.FILE_NOT_FOUND, "File not found or not readable: $inputFilePath")
                    )
                }
                
                val ext = FileUtils.getExtension(inputFilePath)
                if (!SUPPORTED_FORMATS.contains(ext)) {
                    return@withContext ConversionResult(
                        success = false,
                        error = ConversionError(ErrorCode.UNSUPPORTED_FORMAT, "Unsupported format: .$ext")
                    )
                }
                
                val outputPdfPath = FileUtils.getOutputPdfPath(context, inputFilePath)
                val cachedResult = checkCache(inputFilePath, outputPdfPath)
                if (cachedResult != null) return@withContext cachedResult
                
                val bitmaps = when (ext) {
                    "docx", "docm", "dotx" -> DocxPageRenderer.renderDocx(inputFilePath)
                    "doc", "dot" -> DocxPageRenderer.renderDoc(inputFilePath)
                    "xlsx", "xlsm", "xltx" -> XlsxPageRenderer.renderXlsx(inputFilePath)
                    "xls", "xlt" -> XlsxPageRenderer.renderXls(inputFilePath)
                    "pptx", "pptm", "potx", "ppsx" -> PptxPageRenderer.renderPptx(inputFilePath)
                    "ppt", "pot", "pps" -> PptxPageRenderer.renderPpt(inputFilePath)
                    else -> emptyList()
                }
                
                if (bitmaps.isEmpty()) {
                    return@withContext ConversionResult(
                        success = false,
                        error = ConversionError(ErrorCode.CONVERSION_FAILED, "No pages could be rendered from this file")
                    )
                }
                
                val pdfSuccess = PdfBuilder.buildFromBitmaps(bitmaps, outputPdfPath)
                if (!pdfSuccess) {
                    return@withContext ConversionResult(
                        success = false,
                        error = ConversionError(ErrorCode.CONVERSION_FAILED, "Failed to build PDF from rendered pages")
                    )
                }
                
                FileUtils.cleanTempFiles(context)
                val conversionTime = System.currentTimeMillis() - startTime
                
                ConversionResult(
                    success = true,
                    outputPdfPath = outputPdfPath,
                    pageCount = bitmaps.size,
                    conversionTimeMs = conversionTime,
                    originalFormat = ext.uppercase()
                )
                
            } catch (e: OutOfMemoryError) {
                ConversionResult(success = false, error = ConversionError(ErrorCode.OUT_OF_MEMORY, "Not enough memory", e))
            } catch (e: org.apache.poi.EncryptedDocumentException) {
                ConversionResult(success = false, error = ConversionError(ErrorCode.PASSWORD_PROTECTED, "File is password protected", e))
            } catch (e: Exception) {
                ConversionResult(success = false, error = ConversionError(ErrorCode.CONVERSION_FAILED, "Conversion failed: ${e.message}", e))
            }
        }
    }
    
    private fun checkCache(inputPath: String, outputPath: String): ConversionResult? {
        val inputFile = File(inputPath)
        val cacheDir = FileUtils.getOutputDir(context)
        val baseName = File(inputPath).nameWithoutExtension
        val existing = cacheDir.listFiles { f -> f.name.startsWith(baseName) && f.name.endsWith(".pdf") && f.exists() }?.firstOrNull()
        
        if (existing != null && existing.lastModified() > inputFile.lastModified()) {
            return ConversionResult(success = true, outputPdfPath = existing.absolutePath, pageCount = -1, conversionTimeMs = 0, originalFormat = FileUtils.getExtension(inputPath).uppercase())
        }
        return null
    }
    
    fun isSupported(filePath: String): Boolean = SUPPORTED_FORMATS.contains(FileUtils.getExtension(filePath))
    fun clearCache() { FileUtils.getOutputDir(context).listFiles()?.forEach { it.delete() } }
}
