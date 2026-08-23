package com.arkio.officeengine.utils

import android.content.Context
import java.io.File

object FileUtils {
    fun getOutputDir(context: Context): File {
        val dir = File(context.cacheDir, "arkio_converted")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }
    
    fun getOutputPdfPath(context: Context, inputPath: String): String {
        val inputFile = File(inputPath)
        val outputDir = getOutputDir(context)
        val outputName = "${inputFile.nameWithoutExtension}_${System.currentTimeMillis()}.pdf"
        return File(outputDir, outputName).absolutePath
    }
    
    fun getExtension(filePath: String): String {
        return File(filePath).extension.lowercase()
    }
    
    fun isReadable(filePath: String): Boolean {
        val file = File(filePath)
        return file.exists() && file.canRead() && file.length() > 0
    }
}
