package com.wallcycle.app.service

import android.app.WallpaperManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.os.Bundle
import android.os.SystemClock
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
import androidx.core.content.ContextCompat
import com.wallcycle.app.core.BitmapUtils
import com.wallcycle.app.core.Effects
import com.wallcycle.app.core.WallpaperChanger
import com.wallcycle.app.data.Repository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Papel de parede "animado" que mostra a imagem atual da coleção e permite:
 *  - toque duplo na tela inicial para trocar;
 *  - trocar sempre que a tela desliga (nova imagem ao desbloquear).
 */
class GestureWallpaperService : WallpaperService() {

    override fun onCreateEngine(): Engine = WallEngine()

    private inner class WallEngine : Engine(), SharedPreferences.OnSharedPreferenceChangeListener {

        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        private val paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
        private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textAlign = Paint.Align.CENTER
            textSize = 42f
        }
        private var bitmap: Bitmap? = null
        private var surfaceW = 0
        private var surfaceH = 0
        private var loadJob: Job? = null
        private var lastTap = 0L
        private var lastChange = 0L
        private var receiverRegistered = false

        private val screenOffReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (Repository.settings.value.changeOnScreenOff) changeNext(force = true)
            }
        }

        override fun onCreate(surfaceHolder: SurfaceHolder) {
            super.onCreate(surfaceHolder)
            setTouchEventsEnabled(false) // usamos os "taps" enviados pelo launcher (onCommand)
            Repository.prefs.registerOnSharedPreferenceChangeListener(this)
            if (!isPreview) {
                ContextCompat.registerReceiver(
                    this@GestureWallpaperService,
                    screenOffReceiver,
                    IntentFilter(Intent.ACTION_SCREEN_OFF),
                    ContextCompat.RECEIVER_NOT_EXPORTED
                )
                receiverRegistered = true
            }
        }

        override fun onDestroy() {
            Repository.prefs.unregisterOnSharedPreferenceChangeListener(this)
            if (receiverRegistered) {
                runCatching { unregisterReceiver(screenOffReceiver) }
                receiverRegistered = false
            }
            scope.cancel()
            bitmap?.recycle()
            bitmap = null
            super.onDestroy()
        }

        override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            super.onSurfaceChanged(holder, format, width, height)
            if (width != surfaceW || height != surfaceH || bitmap == null) {
                surfaceW = width
                surfaceH = height
                reload()
            } else {
                draw()
            }
        }

        override fun onSurfaceRedrawNeeded(holder: SurfaceHolder) {
            super.onSurfaceRedrawNeeded(holder)
            draw()
        }

        override fun onVisibilityChanged(visible: Boolean) {
            if (visible) draw()
        }

        /** O launcher envia "android.wallpaper.tap" a cada toque em área vazia da tela inicial. */
        override fun onCommand(
            action: String?, x: Int, y: Int, z: Int, extras: Bundle?, resultRequested: Boolean
        ): Bundle? {
            if (action == WallpaperManager.COMMAND_TAP && Repository.settings.value.doubleTap) {
                val now = SystemClock.uptimeMillis()
                if (now - lastTap < DOUBLE_TAP_MS) {
                    lastTap = 0L
                    changeNext()
                } else {
                    lastTap = now
                }
            }
            return null
        }

        override fun onSharedPreferenceChanged(prefs: SharedPreferences?, key: String?) {
            if (key == Repository.KEY_CURRENT || key in Repository.EFFECT_KEYS) reload()
        }

        private fun changeNext(force: Boolean = false) {
            val now = SystemClock.uptimeMillis()
            if (!force && now - lastChange < 1000) return
            lastChange = now
            scope.launch { WallpaperChanger.next(applicationContext) }
        }

        private fun reload() {
            if (surfaceW == 0 || surfaceH == 0) return
            loadJob?.cancel()
            loadJob = scope.launch {
                val uri = Repository.current.value
                if (uri == null) {
                    // Primeira vez: escolhe uma imagem (o listener chamará reload de novo).
                    if (!WallpaperChanger.next(applicationContext)) draw()
                    return@launch
                }
                val settings = Repository.settings.value
                val w = surfaceW
                val h = surfaceH
                val newBmp = withContext(Dispatchers.IO) {
                    runCatching {
                        BitmapUtils.decodeCenterCrop(applicationContext, uri, w, h)
                            ?.let { Effects.apply(it, settings) }
                    }.getOrNull()
                }
                if (newBmp != null) {
                    val old = bitmap
                    bitmap = newBmp
                    draw()
                    old?.recycle()
                } else {
                    draw()
                }
            }
        }

        private fun draw() {
            val holder = surfaceHolder
            val canvas = runCatching { holder.lockCanvas() }.getOrNull() ?: return
            try {
                canvas.drawColor(Color.rgb(28, 27, 32))
                val bmp = bitmap
                if (bmp != null && !bmp.isRecycled) {
                    canvas.drawBitmap(bmp, null, Rect(0, 0, canvas.width, canvas.height), paint)
                } else if (Repository.collections.value.isEmpty()) {
                    canvas.drawText(
                        "Abra o WallCycle e crie uma coleção",
                        canvas.width / 2f, canvas.height / 2f, textPaint
                    )
                }
            } finally {
                runCatching { holder.unlockCanvasAndPost(canvas) }
            }
        }
    }

    private companion object {
        const val DOUBLE_TAP_MS = 400L
    }
}
