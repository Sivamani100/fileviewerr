package com.arkio.officeengine.utils

import android.content.Context
import java.io.File

object FileUtils {
    
    // Get output directory for converted PDFs
    fun getOutputDir(context: Context): File {
        val dir = File(context.cacheDir, "arkio_converted")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }
    
    // Get temp directory for intermediate bitmaps
    fun getTempDir(context: Context): File {
        val dir = File(context.cacheDir, "arkio_temp")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }
    
    // Generate output PDF path from input file
    fun getOutputPdfPath(context: Context, inputPath: String): String {
        val inputFile = File(inputPath)
        val outputDir = getOutputDir(context)
        val outputName = "${inputFile.nameWithoutExtension}_${System.currentTimeMillis()}.pdf"
        return File(outputDir, outputName).absolutePath
    }
    
    // Clean old temp files older than 1 hour
    fun cleanTempFiles(context: Context) {
        val tempDir = getTempDir(context)
        val oneHourAgo = System.currentTimeMillis() - 3600000
        tempDir.listFiles()?.forEach { file ->
            if (file.lastModified() < oneHourAgo) {
                file.delete()
            }
        }
    }
    
    // Get file extension lowercase
    fun getExtension(filePath: String): String {
        return File(filePath).extension.lowercase()
    }
    
    // Check if file is readable
    fun isReadable(filePath: String): Boolean {
        val file = File(filePath)
        return file.exists() && file.canRead() && file.length() > 0
    }
    
    // Get file size in MB
    fun getFileSizeMb(filePath: String): Float {
        return File(filePath).length() / (1024f * 1024f)
    }
}
