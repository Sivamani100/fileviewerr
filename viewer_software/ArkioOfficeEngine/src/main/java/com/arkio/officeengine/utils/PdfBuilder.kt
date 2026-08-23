package com.arkio.officeengine.utils

import android.graphics.Bitmap
import com.itextpdf.io.image.ImageDataFactory
import com.itextpdf.kernel.geom.PageSize
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Image
import java.io.ByteArrayOutputStream
import java.io.File

object PdfBuilder {

    // Build a PDF from a list of Android Bitmaps
    // Each bitmap = one page in the PDF
    fun buildFromBitmaps(
        bitmaps: List<Bitmap>,
        outputPath: String,
        quality: Int = 95
    ): Boolean {
        return try {
            val pdfWriter = PdfWriter(outputPath)
            val pdfDoc = PdfDocument(pdfWriter)
            val document = Document(pdfDoc)
            document.setMargins(0f, 0f, 0f, 0f)
            
            bitmaps.forEachIndexed { index, bitmap ->
                // Convert bitmap to JPEG bytes
                val baos = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, quality, baos)
                val imageBytes = baos.toByteArray()
                baos.close()
                
                // Create PDF page with exact bitmap dimensions
                val widthPt = bitmap.width.toFloat() * 72f / 96f  // px to pt (96 DPI)
                val heightPt = bitmap.height.toFloat() * 72f / 96f
                
                val pageSize = PageSize(widthPt, heightPt)
                pdfDoc.addNewPage(pageSize)
                
                // Add image to page
                val imageData = ImageDataFactory.create(imageBytes)
                val pdfImage = Image(imageData)
                pdfImage.setFixedPosition(index + 1, 0f, 0f)
                pdfImage.setWidth(widthPt)
                pdfImage.setHeight(heightPt)
                document.add(pdfImage)
                
                // Recycle bitmap to free memory
                bitmap.recycle()
            }
            
            document.close()
            pdfDoc.close()
            pdfWriter.close()
            true
        } catch (e: Exception) {
            // Clean up partial file
            File(outputPath).delete()
            false
        }
    }
}
