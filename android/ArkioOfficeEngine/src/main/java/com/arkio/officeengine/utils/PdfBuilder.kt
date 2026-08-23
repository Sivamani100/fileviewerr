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
    fun buildFromBitmaps(bitmaps: List<Bitmap>, outputPath: String): Boolean {
        return try {
            val pdfWriter = PdfWriter(outputPath)
            val pdfDoc = PdfDocument(pdfWriter)
            val document = Document(pdfDoc)
            document.setMargins(0f, 0f, 0f, 0f)
            
            bitmaps.forEach { bitmap ->
                val baos = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, baos)
                val imageData = ImageDataFactory.create(baos.toByteArray())
                val pdfImage = Image(imageData)
                
                val widthPt = bitmap.width.toFloat() * 72f / 96f
                val heightPt = bitmap.height.toFloat() * 72f / 96f
                
                pdfDoc.addNewPage(PageSize(widthPt, heightPt))
                pdfImage.setFixedPosition(pdfDoc.numberOfPages, 0f, 0f)
                pdfImage.setWidth(widthPt)
                pdfImage.setHeight(heightPt)
                document.add(pdfImage)
                
                bitmap.recycle()
            }
            
            document.close()
            true
        } catch (e: Exception) {
            File(outputPath).delete()
            false
        }
    }
}
