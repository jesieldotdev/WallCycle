package com.wallcycle.app.core

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import com.wallcycle.app.data.AppSettings
import kotlin.math.max

/** Efeitos aplicados ao bitmap antes de virar papel de parede. */
object Effects {

    fun hasAny(s: AppSettings) = s.blur > 0 || s.dim > 0 || s.grayscale

    fun apply(src: Bitmap, s: AppSettings): Bitmap {
        if (!hasAny(s)) return src
        var bmp = src
        if (s.blur > 0) {
            val blurred = blur(bmp, s.blur)
            if (blurred !== bmp) bmp.recycle()
            bmp = blurred
        }
        if (s.grayscale || s.dim > 0) {
            val out = if (bmp.isMutable) bmp else bmp.copy(Bitmap.Config.ARGB_8888, true)
            val canvas = Canvas(out)
            if (s.grayscale) {
                val paint = Paint().apply {
                    colorFilter = ColorMatrixColorFilter(ColorMatrix().apply { setSaturation(0f) })
                }
                val copy = out.copy(Bitmap.Config.ARGB_8888, false)
                canvas.drawBitmap(copy, 0f, 0f, paint)
                copy.recycle()
            }
            if (s.dim > 0) {
                canvas.drawColor(Color.argb((s.dim * 255 / 100).coerceIn(0, 255), 0, 0, 0))
            }
            if (out !== bmp) bmp.recycle()
            bmp = out
        }
        return bmp
    }

    /**
     * Desfoque rápido: reduz a imagem, aplica box blur (2 passadas ≈ gaussiano)
     * e amplia de volta com filtragem.
     */
    private fun blur(src: Bitmap, level: Int): Bitmap {
        val factor = 2 + level / 3
        val w = max(1, src.width / factor)
        val h = max(1, src.height / factor)
        val small = Bitmap.createScaledBitmap(src, w, h, true)
        val px = IntArray(w * h)
        small.getPixels(px, 0, w, 0, 0, w, h)
        if (small !== src) small.recycle()

        val radius = 1 + level / 4
        repeat(2) {
            boxBlurH(px, w, h, radius)
            boxBlurV(px, w, h, radius)
        }
        val blurredSmall = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        blurredSmall.setPixels(px, 0, w, 0, 0, w, h)
        val out = Bitmap.createScaledBitmap(blurredSmall, src.width, src.height, true)
        if (out !== blurredSmall) blurredSmall.recycle()
        return out
    }

    private fun boxBlurH(p: IntArray, w: Int, h: Int, r: Int) {
        val row = IntArray(w)
        val d = 2 * r + 1
        for (y in 0 until h) {
            val off = y * w
            var a = 0; var rr = 0; var g = 0; var b = 0
            for (i in -r..r) {
                val c = p[off + i.coerceIn(0, w - 1)]
                a += c ushr 24; rr += c shr 16 and 0xff; g += c shr 8 and 0xff; b += c and 0xff
            }
            for (x in 0 until w) {
                row[x] = (a / d shl 24) or (rr / d shl 16) or (g / d shl 8) or (b / d)
                val cOut = p[off + (x - r).coerceIn(0, w - 1)]
                val cIn = p[off + (x + r + 1).coerceIn(0, w - 1)]
                a += (cIn ushr 24) - (cOut ushr 24)
                rr += (cIn shr 16 and 0xff) - (cOut shr 16 and 0xff)
                g += (cIn shr 8 and 0xff) - (cOut shr 8 and 0xff)
                b += (cIn and 0xff) - (cOut and 0xff)
            }
            System.arraycopy(row, 0, p, off, w)
        }
    }

    private fun boxBlurV(p: IntArray, w: Int, h: Int, r: Int) {
        val col = IntArray(h)
        val d = 2 * r + 1
        for (x in 0 until w) {
            var a = 0; var rr = 0; var g = 0; var b = 0
            for (i in -r..r) {
                val c = p[i.coerceIn(0, h - 1) * w + x]
                a += c ushr 24; rr += c shr 16 and 0xff; g += c shr 8 and 0xff; b += c and 0xff
            }
            for (y in 0 until h) {
                col[y] = (a / d shl 24) or (rr / d shl 16) or (g / d shl 8) or (b / d)
                val cOut = p[(y - r).coerceIn(0, h - 1) * w + x]
                val cIn = p[(y + r + 1).coerceIn(0, h - 1) * w + x]
                a += (cIn ushr 24) - (cOut ushr 24)
                rr += (cIn shr 16 and 0xff) - (cOut shr 16 and 0xff)
                g += (cIn shr 8 and 0xff) - (cOut shr 8 and 0xff)
                b += (cIn and 0xff) - (cOut and 0xff)
            }
            for (y in 0 until h) p[y * w + x] = col[y]
        }
    }
}
