package com.arkio.officeengine.renderer

import android.graphics.*
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Composite
import java.awt.Font
import java.awt.FontMetrics
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.GraphicsConfiguration
import java.awt.Image
import java.awt.Paint
import java.awt.Rectangle
import java.awt.RenderingHints
import java.awt.Shape
import java.awt.Stroke
import java.awt.font.FontRenderContext
import java.awt.font.GlyphVector
import java.awt.geom.*
import java.awt.image.BufferedImage
import java.awt.image.BufferedImageOp
import java.awt.image.ImageObserver
import java.awt.image.RenderedImage
import java.awt.image.renderable.RenderableImage
import java.text.AttributedCharacterIterator

/**
 * AndroidGraphics2DAdapter
 * 
 * This class bridges Apache POI's Java AWT Graphics2D rendering
 * to Android's native Canvas/Paint system.
 */
class AndroidGraphics2DAdapter private constructor(
    private val canvas: android.graphics.Canvas,
    private val scaleX: Float,
    private val scaleY: Float
) : Graphics2D() {

    private val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
    private var currentColor: android.graphics.Color = android.graphics.Color.valueOf(0f, 0f, 0f)
    private var currentFont: android.graphics.Typeface = android.graphics.Typeface.DEFAULT
    private var currentFontSize: Float = 12f
    private var currentStrokeWidth: Float = 1f
    private var currentAlpha: Int = 255
    private val clipStack = mutableListOf<android.graphics.RectF>()
    private var transform = android.graphics.Matrix()
    
    companion object {
        fun create(
            canvas: android.graphics.Canvas,
            canvasWidth: Int,
            canvasHeight: Int,
            docWidth: Float,
            docHeight: Float
        ): AndroidGraphics2DAdapter {
            val scaleX = canvasWidth / docWidth
            val scaleY = canvasHeight / docHeight
            return AndroidGraphics2DAdapter(canvas, scaleX, scaleY)
        }
    }

    private fun sx(x: Double): Float = (x * scaleX).toFloat()
    private fun sy(y: Double): Float = (y * scaleY).toFloat()
    private fun sw(w: Double): Float = (w * scaleX).toFloat()
    private fun sh(h: Double): Float = (h * scaleY).toFloat()
    private fun sx(x: Float): Float = x * scaleX
    private fun sy(y: Float): Float = y * scaleY
    private fun sw(w: Float): Float = w * scaleX
    private fun sh(h: Float): Float = h * scaleY
    private fun sx(x: Int): Float = x * scaleX
    private fun sy(y: Int): Float = y * scaleY
    private fun sw(w: Int): Float = w * scaleX
    private fun sh(h: Int): Float = h * scaleY

    override fun setColor(c: Color?) {
        c ?: return
        currentColor = android.graphics.Color.valueOf(
            c.red / 255f, c.green / 255f, c.blue / 255f, c.alpha / 255f
        )
        currentAlpha = c.alpha
        paint.color = android.graphics.Color.argb(c.alpha, c.red, c.green, c.blue)
    }

    override fun getColor(): Color {
        return Color(
            (currentColor.red() * 255).toInt(),
            (currentColor.green() * 255).toInt(),
            (currentColor.blue() * 255).toInt(),
            currentAlpha
        )
    }

    override fun setPaint(paint: Paint?) {
        when (paint) {
            is Color -> setColor(paint)
            is java.awt.GradientPaint -> setColor(paint.color1)
            else -> setColor(Color.BLACK)
        }
    }

    override fun getPaint(): Paint = getColor()
    override fun setBackground(color: Color?) { color?.let { setColor(it) } }
    override fun getBackground(): Color = getColor()

    override fun fillRect(x: Int, y: Int, width: Int, height: Int) {
        paint.style = android.graphics.Paint.Style.FILL
        canvas.drawRect(sx(x), sy(y), sx(x) + sw(width), sy(y) + sh(height), paint)
    }

    override fun drawRect(x: Int, y: Int, width: Int, height: Int) {
        paint.style = android.graphics.Paint.Style.STROKE
        paint.strokeWidth = currentStrokeWidth
        canvas.drawRect(sx(x), sy(y), sx(x) + sw(width), sy(y) + sh(height), paint)
    }

    override fun fillOval(x: Int, y: Int, width: Int, height: Int) {
        paint.style = android.graphics.Paint.Style.FILL
        val oval = RectF(sx(x), sy(y), sx(x) + sw(width), sy(y) + sh(height))
        canvas.drawOval(oval, paint)
    }

    override fun drawOval(x: Int, y: Int, width: Int, height: Int) {
        paint.style = android.graphics.Paint.Style.STROKE
        paint.strokeWidth = currentStrokeWidth
        val oval = RectF(sx(x), sy(y), sx(x) + sw(width), sy(y) + sh(height))
        canvas.drawOval(oval, paint)
    }

    override fun drawLine(x1: Int, y1: Int, x2: Int, y2: Int) {
        paint.style = android.graphics.Paint.Style.STROKE
        paint.strokeWidth = currentStrokeWidth
        canvas.drawLine(sx(x1), sy(y1), sx(x2), sy(y2), paint)
    }

    override fun drawPolyline(xPoints: IntArray?, yPoints: IntArray?, nPoints: Int) {
        if (xPoints == null || yPoints == null || nPoints < 2) return
        paint.style = android.graphics.Paint.Style.STROKE
        paint.strokeWidth = currentStrokeWidth
        val path = android.graphics.Path()
        path.moveTo(sx(xPoints[0]), sy(yPoints[0]))
        for (i in 1 until nPoints) path.lineTo(sx(xPoints[i]), sy(yPoints[i]))
        canvas.drawPath(path, paint)
    }

    override fun fillPolygon(xPoints: IntArray?, yPoints: IntArray?, nPoints: Int) {
        if (xPoints == null || yPoints == null || nPoints < 3) return
        paint.style = android.graphics.Paint.Style.FILL
        val path = android.graphics.Path()
        path.moveTo(sx(xPoints[0]), sy(yPoints[0]))
        for (i in 1 until nPoints) path.lineTo(sx(xPoints[i]), sy(yPoints[i]))
        path.close()
        canvas.drawPath(path, paint)
    }

    override fun drawPolygon(xPoints: IntArray?, yPoints: IntArray?, nPoints: Int) {
        if (xPoints == null || yPoints == null || nPoints < 3) return
        paint.style = android.graphics.Paint.Style.STROKE
        paint.strokeWidth = currentStrokeWidth
        val path = android.graphics.Path()
        path.moveTo(sx(xPoints[0]), sy(yPoints[0]))
        for (i in 1 until nPoints) path.lineTo(sx(xPoints[i]), sy(yPoints[i]))
        path.close()
        canvas.drawPath(path, paint)
    }

    override fun draw(s: Shape?) {
        s ?: return
        paint.style = android.graphics.Paint.Style.STROKE
        paint.strokeWidth = currentStrokeWidth
        val path = shapeToPath(s)
        canvas.drawPath(path, paint)
    }

    override fun fill(s: Shape?) {
        s ?: return
        paint.style = android.graphics.Paint.Style.FILL
        val path = shapeToPath(s)
        canvas.drawPath(path, paint)
    }

    private fun shapeToPath(shape: Shape): android.graphics.Path {
        val path = android.graphics.Path()
        val pi = shape.getPathIterator(null)
        val coords = FloatArray(6)
        while (!pi.isDone) {
            when (pi.currentSegment(coords)) {
                PathIterator.SEG_MOVETO -> path.moveTo(sx(coords[0]), sy(coords[1]))
                PathIterator.SEG_LINETO -> path.lineTo(sx(coords[0]), sy(coords[1]))
                PathIterator.SEG_QUADTO -> path.quadTo(sx(coords[0]), sy(coords[1]), sx(coords[2]), sy(coords[3]))
                PathIterator.SEG_CUBICTO -> path.cubicTo(sx(coords[0]), sy(coords[1]), sx(coords[2]), sy(coords[3]), sx(coords[4]), sy(coords[5]))
                PathIterator.SEG_CLOSE -> path.close()
            }
            pi.next()
        }
        return path
    }

    override fun drawString(str: String?, x: Int, y: Int) = drawString(str, x.toFloat(), y.toFloat())
    override fun drawString(str: String?, x: Float, y: Float) {
        str ?: return
        paint.style = android.graphics.Paint.Style.FILL
        paint.typeface = currentFont
        paint.textSize = currentFontSize * scaleY
        canvas.drawText(str, sx(x), sy(y), paint)
    }

    override fun drawString(iterator: AttributedCharacterIterator?, x: Int, y: Int) = drawString(iterator, x.toFloat(), y.toFloat())
    override fun drawString(iterator: AttributedCharacterIterator?, x: Float, y: Float) {
        iterator ?: return
        val sb = StringBuilder()
        var c = iterator.first()
        while (c != AttributedCharacterIterator.DONE) {
            sb.append(c); c = iterator.next()
        }
        drawString(sb.toString(), x, y)
    }

    override fun drawGlyphVector(g: GlyphVector?, x: Float, y: Float) {
        g ?: return
        fill(g.getOutline(x, y))
    }

    override fun setFont(f: Font?) {
        f ?: return
        currentFontSize = f.size.toFloat()
        val isBold = f.isBold
        val isItalic = f.isItalic
        currentFont = when {
            isBold && isItalic -> android.graphics.Typeface.create(mapFontName(f.name), android.graphics.Typeface.BOLD_ITALIC)
            isBold -> android.graphics.Typeface.create(mapFontName(f.name), android.graphics.Typeface.BOLD)
            isItalic -> android.graphics.Typeface.create(mapFontName(f.name), android.graphics.Typeface.ITALIC)
            else -> android.graphics.Typeface.create(mapFontName(f.name), android.graphics.Typeface.NORMAL)
        }
        paint.typeface = currentFont
        paint.textSize = currentFontSize * scaleY
    }

    private fun mapFontName(name: String): String {
        return when (name.lowercase()) {
            "times new roman", "times" -> "serif"
            "calibri", "segoe ui", "tahoma", "verdana", "arial", "helvetica" -> "sans-serif"
            "courier new", "courier", "consolas", "monaco", "lucida console" -> "monospace"
            "georgia" -> "serif"
            "comic sans ms" -> "casual"
            else -> "sans-serif"
        }
    }

    override fun getFont(): Font = Font("sans-serif", Font.PLAIN, currentFontSize.toInt())
    override fun getFontMetrics(f: Font?): FontMetrics {
        return object : FontMetrics(f ?: getFont()) {
            override fun getHeight(): Int = (currentFontSize * 1.2f).toInt()
            override fun getAscent(): Int = (currentFontSize * 0.8f).toInt()
            override fun getDescent(): Int = (currentFontSize * 0.2f).toInt()
            override fun stringWidth(str: String): Int {
                paint.textSize = currentFontSize * scaleX
                return paint.measureText(str).toInt()
            }
        }
    }

    override fun getFontRenderContext(): FontRenderContext = FontRenderContext(null, true, true)

    override fun drawImage(img: Image?, x: Int, y: Int, observer: ImageObserver?): Boolean = drawImage(img, x, y, img?.getWidth(null) ?: 0, img?.getHeight(null) ?: 0, observer)
    override fun drawImage(img: Image?, x: Int, y: Int, width: Int, height: Int, observer: ImageObserver?): Boolean {
        img ?: return false
        try {
            if (img is BufferedImage) {
                val bitmap = bufferedImageToBitmap(img)
                val dst = RectF(sx(x), sy(y), sx(x) + sw(width), sy(y) + sh(height))
                canvas.drawBitmap(bitmap, null, dst, null)
                bitmap.recycle()
                return true
            }
        } catch (e: Exception) {}
        return false
    }

    override fun drawImage(img: Image?, op: BufferedImageOp?, x: Int, y: Int) { img?.let { drawImage(it, x, y, null) } }
    override fun drawRenderedImage(img: RenderedImage?, xform: AffineTransform?) {}
    override fun drawRenderableImage(img: RenderableImage?, xform: AffineTransform?) {}

    private fun bufferedImageToBitmap(img: BufferedImage): android.graphics.Bitmap {
        val bitmap = android.graphics.Bitmap.createBitmap(img.width, img.height, android.graphics.Bitmap.Config.ARGB_8888)
        for (x in 0 until img.width) for (y in 0 until img.height) bitmap.setPixel(x, y, img.getRGB(x, y))
        return bitmap
    }

    override fun getTransform(): AffineTransform = AffineTransform()
    override fun setTransform(Tx: AffineTransform?) { Tx?.let { canvas.matrix = affineToMatrix(it) } }
    override fun transform(Tx: AffineTransform?) { Tx?.let { canvas.concat(affineToMatrix(it)) } }
    override fun translate(x: Int, y: Int) = canvas.translate(sx(x), sy(y))
    override fun translate(tx: Double, ty: Double) = canvas.translate(sx(tx.toFloat()), sy(ty.toFloat()))
    override fun rotate(theta: Double) = canvas.rotate(Math.toDegrees(theta).toFloat())
    override fun rotate(theta: Double, x: Double, y: Double) = canvas.rotate(Math.toDegrees(theta).toFloat(), sx(x.toFloat()), sy(y.toFloat()))
    override fun scale(sx: Double, sy: Double) = canvas.scale(sx.toFloat(), sy.toFloat())
    override fun shear(shx: Double, shy: Double) {
        val matrix = android.graphics.Matrix()
        matrix.setSkew(shx.toFloat(), shy.toFloat())
        canvas.concat(matrix)
    }

    private fun affineToMatrix(at: AffineTransform): android.graphics.Matrix {
        val matrix = android.graphics.Matrix()
        val vals = DoubleArray(6)
        at.getMatrix(vals)
        matrix.setValues(floatArrayOf(vals[0].toFloat(), vals[2].toFloat(), vals[4].toFloat(), vals[1].toFloat(), vals[3].toFloat(), vals[5].toFloat(), 0f, 0f, 1f))
        return matrix
    }

    override fun setClip(x: Int, y: Int, width: Int, height: Int) { canvas.clipRect(sx(x), sy(y), sx(x) + sw(width), sy(y) + sh(height)) }
    override fun setClip(clip: Shape?) {
        clip ?: return
        val bounds = clip.bounds2D
        canvas.clipRect(sx(bounds.x), sy(bounds.y), sx(bounds.x) + sw(bounds.width), sy(bounds.y) + sh(bounds.height))
    }
    override fun clip(s: Shape?) { s?.let { canvas.clipPath(shapeToPath(it)) } }
    override fun getClip(): Shape {
        val cb = canvas.clipBounds
        return Rectangle(cb.left, cb.top, cb.width(), cb.height())
    }
    override fun getClipBounds(): Rectangle {
        val cb = canvas.clipBounds
        return Rectangle((cb.left / scaleX).toInt(), (cb.top / scaleY).toInt(), (cb.width() / scaleX).toInt(), (cb.height() / scaleY).toInt())
    }

    override fun setStroke(s: Stroke?) {
        if (s is BasicStroke) {
            currentStrokeWidth = s.lineWidth * ((scaleX + scaleY) / 2f)
            paint.strokeWidth = currentStrokeWidth
            paint.strokeJoin = when (s.lineJoin) {
                BasicStroke.JOIN_BEVEL -> android.graphics.Paint.Join.BEVEL
                BasicStroke.JOIN_ROUND -> android.graphics.Paint.Join.ROUND
                else -> android.graphics.Paint.Join.MITER
            }
            paint.strokeCap = when (s.endCap) {
                BasicStroke.CAP_ROUND -> android.graphics.Paint.Cap.ROUND
                BasicStroke.CAP_SQUARE -> android.graphics.Paint.Cap.SQUARE
                else -> android.graphics.Paint.Cap.BUTT
            }
            val dashArray = s.dashArray
            if (dashArray != null && dashArray.isNotEmpty()) {
                val scaledDash = dashArray.map { it * ((scaleX + scaleY) / 2f) }.toFloatArray()
                paint.pathEffect = DashPathEffect(scaledDash, s.dashPhase * ((scaleX + scaleY) / 2f))
            } else paint.pathEffect = null
        }
    }

    override fun getStroke(): Stroke = BasicStroke(currentStrokeWidth)

    override fun setComposite(comp: Composite?) {
        if (comp is java.awt.AlphaComposite) {
            val alpha = (comp.alpha * 255).toInt()
            paint.alpha = alpha; currentAlpha = alpha
        }
    }
    override fun getComposite(): Composite = java.awt.AlphaComposite.getInstance(java.awt.AlphaComposite.SRC_OVER, currentAlpha / 255f)

    override fun setRenderingHint(hintKey: RenderingHints.Key?, hintValue: Any?) {
        if (hintKey == RenderingHints.KEY_ANTIALIASING || hintKey == RenderingHints.KEY_TEXT_ANTIALIASING) {
            paint.isAntiAlias = (hintValue == RenderingHints.VALUE_ANTIALIAS_ON)
        }
    }
    override fun getRenderingHint(hintKey: RenderingHints.Key?): Any? = RenderingHints.VALUE_ANTIALIAS_ON
    override fun setRenderingHints(hints: Map<*, *>?) {}
    override fun addRenderingHints(hints: Map<*, *>?) {}
    override fun getRenderingHints(): RenderingHints = RenderingHints(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

    override fun clearRect(x: Int, y: Int, width: Int, height: Int) {
        val savedColor = paint.color
        paint.color = android.graphics.Color.WHITE
        paint.style = android.graphics.Paint.Style.FILL
        canvas.drawRect(sx(x), sy(y), sx(x) + sw(width), sy(y) + sh(height), paint)
        paint.color = savedColor
    }

    override fun copyArea(x: Int, y: Int, width: Int, height: Int, dx: Int, dy: Int) {}
    override fun create(): Graphics = this
    override fun dispose() {}

    override fun drawArc(x: Int, y: Int, width: Int, height: Int, startAngle: Int, arcAngle: Int) {
        paint.style = android.graphics.Paint.Style.STROKE
        paint.strokeWidth = currentStrokeWidth
        val oval = RectF(sx(x), sy(y), sx(x) + sw(width), sy(y) + sh(height))
        canvas.drawArc(oval, -startAngle.toFloat(), -arcAngle.toFloat(), false, paint)
    }

    override fun fillArc(x: Int, y: Int, width: Int, height: Int, startAngle: Int, arcAngle: Int) {
        paint.style = android.graphics.Paint.Style.FILL
        val oval = RectF(sx(x), sy(y), sx(x) + sw(width), sy(y) + sh(height))
        canvas.drawArc(oval, -startAngle.toFloat(), -arcAngle.toFloat(), true, paint)
    }

    override fun drawRoundRect(x: Int, y: Int, width: Int, height: Int, arcWidth: Int, arcHeight: Int) {
        paint.style = android.graphics.Paint.Style.STROKE
        paint.strokeWidth = currentStrokeWidth
        val rect = RectF(sx(x), sy(y), sx(x) + sw(width), sy(y) + sh(height))
        canvas.drawRoundRect(rect, sw(arcWidth).toFloat(), sh(arcHeight).toFloat(), paint)
    }

    override fun fillRoundRect(x: Int, y: Int, width: Int, height: Int, arcWidth: Int, arcHeight: Int) {
        paint.style = android.graphics.Paint.Style.FILL
        val rect = RectF(sx(x), sy(y), sx(x) + sw(width), sy(y) + sh(height))
        canvas.drawRoundRect(rect, sw(arcWidth).toFloat(), sh(arcHeight).toFloat(), paint)
    }

    override fun setPaintMode() {}
    override fun setXORMode(c1: Color?) {}
    override fun getDeviceConfiguration(): GraphicsConfiguration? = null

    override fun drawImage(img: Image?, x: Int, y: Int, bgcolor: Color?, observer: ImageObserver?): Boolean = drawImage(img, x, y, observer)
    override fun drawImage(img: Image?, x: Int, y: Int, width: Int, height: Int, bgcolor: Color?, observer: ImageObserver?): Boolean = drawImage(img, x, y, width, height, observer)
    override fun drawImage(img: Image?, dx1: Int, dy1: Int, dx2: Int, dy2: Int, sx1: Int, sy1: Int, sx2: Int, sy2: Int, observer: ImageObserver?): Boolean = false
    override fun drawImage(img: Image?, dx1: Int, dy1: Int, dx2: Int, dy2: Int, sx1: Int, sy1: Int, sx2: Int, sy2: Int, bgcolor: Color?, observer: ImageObserver?): Boolean = false
    override fun drawImage(img: BufferedImage?, op: BufferedImageOp?, x: Int, y: Int) { img?.let { drawImage(it as Image, x, y, null) } }
}
