package com.arkio.officeengine.renderer

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import org.apache.poi.hslf.usermodel.HSLFSlideShow
import org.apache.poi.xslf.usermodel.XMLSlideShow
import java.io.File
import java.io.FileInputStream

object PptxPageRenderer {

    private const val RENDER_SCALE = 2.0f
    private const val BASE_WIDTH = 960
    
    fun renderPptx(filePath: String): List<Bitmap> {
        val bitmaps = mutableListOf<Bitmap>()
        FileInputStream(File(filePath)).use { fis ->
            val pptx = XMLSlideShow(fis)
            val pageSize = pptx.pageSize
            val aspectRatio = pageSize.height.toFloat() / pageSize.width.toFloat()
            val renderWidth = (BASE_WIDTH * RENDER_SCALE).toInt()
            val renderHeight = (renderWidth * aspectRatio).toInt()
            
            for (slide in pptx.slides) {
                try {
                    val bitmap = Bitmap.createBitmap(renderWidth, renderHeight, Bitmap.Config.ARGB_8888)
                    val canvas = Canvas(bitmap)
                    canvas.drawColor(Color.WHITE)
                    val graphics = AndroidGraphics2DAdapter.create(canvas, renderWidth, renderHeight, pageSize.width.toFloat(), pageSize.height.toFloat())
                    slide.draw(graphics)
                    bitmaps.add(bitmap)
                } catch (e: Exception) {
                    bitmaps.add(createErrorSlide(renderWidth, renderHeight, "Slide render error: ${e.message}"))
                }
            }
            pptx.close()
        }
        return bitmaps
    }
    
    fun renderPpt(filePath: String): List<Bitmap> {
        val bitmaps = mutableListOf<Bitmap>()
        FileInputStream(File(filePath)).use { fis ->
            val ppt = HSLFSlideShow(fis)
            val pageSize = ppt.pageSize
            val aspectRatio = pageSize.height.toFloat() / pageSize.width.toFloat()
            val renderWidth = (BASE_WIDTH * RENDER_SCALE).toInt()
            val renderHeight = (renderWidth * aspectRatio).toInt()
            
            for (slide in ppt.slides) {
                try {
                    val bitmap = Bitmap.createBitmap(renderWidth, renderHeight, Bitmap.Config.ARGB_8888)
                    val canvas = Canvas(bitmap)
                    canvas.drawColor(Color.WHITE)
                    val graphics = AndroidGraphics2DAdapter.create(canvas, renderWidth, renderHeight, pageSize.width.toFloat(), pageSize.height.toFloat())
                    slide.draw(graphics)
                    bitmaps.add(bitmap)
                } catch (e: Exception) {
                    bitmaps.add(createErrorSlide(renderWidth, renderHeight, "Slide render error: ${e.message}"))
                }
            }
            ppt.close()
        }
        return bitmaps
    }
    
    private fun createErrorSlide(width: Int, height: Int, message: String): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)
        val paint = Paint().apply { color = Color.RED; textSize = 32f; typeface = Typeface.DEFAULT }
        canvas.drawText(message, 50f, height / 2f, paint)
        return bitmap
    }
}
