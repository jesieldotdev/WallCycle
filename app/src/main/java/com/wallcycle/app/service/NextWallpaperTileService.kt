package com.wallcycle.app.service

import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.wallcycle.app.work.Scheduler

/** Bloco "Próximo wallpaper" nas Configurações rápidas. */
class NextWallpaperTileService : TileService() {

    override fun onStartListening() {
        qsTile?.apply {
            state = Tile.STATE_INACTIVE
            updateTile()
        }
    }

    override fun onClick() {
        Scheduler.changeNow(applicationContext)
    }
}
