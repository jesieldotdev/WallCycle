package com.wallcycle.app.core

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.net.Uri
import android.os.Build
import android.util.DisplayMetrics
import android.view.WindowManager
import androidx.exifinterface.media.ExifInterface
import kotlin.math.max

object BitmapUtils {

    /** Tamanho real da tela (incluindo barras do sistema), em retrato. */
    fun screenSize(context: Context): Pair<Int, Int> {
        val wm = context.getSystemService(WindowManager::class.java)
        val (w, h) = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val b = wm.maximumWindowMetrics.bounds
            b.width() to b.height()
        } else {
            val dm = DisplayMetrics()
            @Suppress("DEPRECATION")
            wm.defaultDisplay.getRealMetrics(dm)
            dm.widthPixels to dm.heightPixels
        }
        return minOf(w, h) to maxOf(w, h)
    }

    /**
     * Decodifica a imagem já reduzida (inSampleSize), corrige a rotação EXIF
     * e recorta no centro para exatamente [width] x [height].
     */
    fun decodeCenterCrop(context: Context, uri: Uri, width: Int, height: Int): Bitmap? {
        val resolver = context.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        val rotation = runCatching {
            resolver.openInputStream(uri)?.use { ExifInterface(it).rotationDegrees } ?: 0
        }.getOrDefault(0)
        val rotated = rotation == 90 || rotation == 270
        val srcW = if (rotated) bounds.outHeight else bounds.outWidth
        val srcH = if (rotated) bounds.outWidth else bounds.outHeight

        var sample = 1
        while (srcW / (sample * 2) >= width && srcH / (sample * 2) >= height) sample *= 2

        val opts = BitmapFactory.Options().apply {
            inSampleSize = sample
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val decoded = resolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, opts)
        } ?: return null

        val out = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val bw = if (rotated) decoded.height else decoded.width
        val bh = if (rotated) decoded.width else decoded.height
        val scale = max(width.toFloat() / bw, height.toFloat() / bh)

        val m = Matrix().apply {
            // Gira em torno do centro da imagem original, depois escala e centraliza.
            postTranslate(-decoded.width / 2f, -decoded.height / 2f)
            postRotate(rotation.toFloat())
            postScale(scale, scale)
            postTranslate(width / 2f, height / 2f)
        }
        Canvas(out).drawBitmap(decoded, m, Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG))
        decoded.recycle()
        return out
    }
}
