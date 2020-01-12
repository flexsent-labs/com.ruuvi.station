package com.ruuvi.station.bluetooth

import android.app.Application
import android.os.Handler
import android.util.Log
import com.ruuvi.station.bluetooth.gateway.factory.BluetoothTagGatewayFactory
import com.ruuvi.station.service.AltBeaconScannerForegroundService
import com.ruuvi.station.util.BackgroundScanModes
import com.ruuvi.station.util.Foreground
import com.ruuvi.station.util.Preferences
import com.ruuvi.station.util.ServiceUtils

class BluetoothInteractor(
    private val application: Application,
    private val bluetoothTagGatewayFactory: BluetoothTagGatewayFactory
) {

    private val TAG = BluetoothInteractor::class.java.simpleName

    private val bluetoothRangeGateway = bluetoothTagGatewayFactory.create()

    private var prefs = Preferences(application)

    fun startForegroundScanning() {
        bluetoothRangeGateway.startForegroundScanning()
    }

    fun startBackgroundScanning() {
        bluetoothRangeGateway.startBackgroundScanning()
    }

    fun onAppCreated() {

        Foreground.init(application)

        val listener: Foreground.Listener = object : Foreground.Listener {
            override fun onBecameForeground() {
                Log.d(TAG, "onBecameForeground")
                startForegroundScanning()
                bluetoothRangeGateway.setGatewayOn(false)
            }

            override fun onBecameBackground() {
                Log.d(TAG, "onBecameBackground")
                foreground = false
                val su = ServiceUtils(application)
                if (prefs.backgroundScanMode === BackgroundScanModes.DISABLED) { // background scanning is disabled so all scanning things will be killed
                    bluetoothRangeGateway.stopScanning()
                    su.stopForegroundService()
                } else if (prefs.backgroundScanMode === BackgroundScanModes.BACKGROUND) {
                    if (su.isRunning(AltBeaconScannerForegroundService::class.java)) {
                        su.stopForegroundService()
                    } else {
                        startBackgroundScanning()
                    }
                } else {
                    bluetoothRangeGateway.disposeStuff()
                    su.startForegroundService()
                }
                bluetoothRangeGateway.setGatewayOn(true)
            }
        }

        Foreground.get().addListener(listener)

        Handler().postDelayed({
            if (!foreground) {
                if (prefs.backgroundScanMode === BackgroundScanModes.FOREGROUND) {
                    ServiceUtils(application).startForegroundService()
                } else if (prefs.backgroundScanMode === BackgroundScanModes.BACKGROUND) {
                    startBackgroundScanning()
                }
            }
        }, 5000)

        bluetoothRangeGateway.onAppCreated()

    }

    companion object {
        var foreground = false
    }
}