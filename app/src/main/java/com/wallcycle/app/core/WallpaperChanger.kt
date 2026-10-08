package com.wallcycle.app.core

import android.app.WallpaperManager
import android.content.Context
import android.net.Uri
import android.util.Log
import com.wallcycle.app.data.ChangeOrder
import com.wallcycle.app.data.Repository
import com.wallcycle.app.data.WallTarget
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

object WallpaperChanger {

    private const val TAG = "WallpaperChanger"
    private val mutex = Mutex()

    /** O papel de parede animado do WallCycle está ativo no sistema? */
    fun isLiveActive(context: Context): Boolean =
        WallpaperManager.getInstance(context).wallpaperInfo?.packageName == context.packageName

    /** Escolhe a próxima imagem da coleção ativa (aleatória ou sequencial) e aplica. */
    suspend fun next(context: Context): Boolean = mutex.withLock {
        val collection = Repository.activeCollection() ?: return false
        val images = Repository.images(collection)
        if (images.isEmpty()) return false

        val settings = Repository.settings.value
        val chosen: Uri = when (settings.order) {
            ChangeOrder.SEQUENTIAL -> {
                val idx = (Repository.sequenceIndex + 1).mod(images.size)
                Repository.sequenceIndex = idx
                images[idx]
            }
            ChangeOrder.RANDOM -> {
                // Evita repetir as últimas imagens (até metade da coleção).
                val avoid = Repository.history()
                    .takeLast(minOf(images.size / 2, 50))
                    .toSet()
                val pool = images.filterNot { it.toString() in avoid }.ifEmpty {
                    images.filterNot { it == Repository.current.value }.ifEmpty { images }
                }
                pool.random()
            }
        }
        applyInternal(context, chosen)
    }

    /** Aplica uma imagem específica (ex.: tocada na grade). */
    suspend fun apply(context: Context, uri: Uri): Boolean = mutex.withLock {
        applyInternal(context, uri)
    }

    /** Reaplica a imagem atual (ex.: depois de mudar os efeitos). */
    suspend fun reapply(context: Context): Boolean {
        val uri = Repository.current.value ?: return next(context)
        return apply(context, uri)
    }

    private suspend fun applyInternal(context: Context, uri: Uri): Boolean =
        withContext(Dispatchers.IO) {
            try {
                // No modo animado o serviço observa a imagem atual e redesenha sozinho.
                if (isLiveActive(context)) {
                    Repository.setCurrent(uri)
                    return@withContext true
                }
                val (w, h) = BitmapUtils.screenSize(context)
                val raw = BitmapUtils.decodeCenterCrop(context, uri, w, h) ?: return@withContext false
                val bmp = Effects.apply(raw, Repository.settings.value)
                val wm = WallpaperManager.getInstance(context)
                val flags = when (Repository.settings.value.target) {
                    WallTarget.HOME -> WallpaperManager.FLAG_SYSTEM
                    WallTarget.LOCK -> WallpaperManager.FLAG_LOCK
                    WallTarget.BOTH -> WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK
                }
                wm.setBitmap(bmp, null, true, flags)
                bmp.recycle()
                Repository.setCurrent(uri)
                true
            } catch (e: Exception) {
                Log.e(TAG, "Falha ao aplicar $uri", e)
                false
            }
        }
}
