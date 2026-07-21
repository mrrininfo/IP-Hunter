package com.ipwatcher.app

import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService

class QsTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        updateTile()
    }

    override fun onClick() {
        super.onClick()
        val running = Prefs.isRunning(this)
        if (running) {
            startService(Intent(this, IpCheckService::class.java).apply {
                action = IpCheckService.ACTION_STOP
            })
        } else {
            val intent = Intent(this, IpCheckService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
        }
        updateTile()
    }

    private fun updateTile() {
        val tile = qsTile ?: return
        val running = Prefs.isRunning(this)
        tile.state = if (running) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.label = if (running) "IP Watcher: ON" else "IP Watcher: OFF"
        tile.updateTile()
    }
}
