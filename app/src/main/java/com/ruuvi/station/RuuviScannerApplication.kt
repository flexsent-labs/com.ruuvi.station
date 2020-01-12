package com.ruuvi.station

import android.app.Application
import android.util.Log
import com.raizlabs.android.dbflow.config.FlowManager
import com.ruuvi.station.bluetooth.BluetoothInteractor
import com.ruuvi.station.bluetooth.gateway.factory.BluetoothTagGatewayFactory
import com.ruuvi.station.bluetooth.gateway.factory.DefaultBluetoothTagGatewayFactory

/**
 * Created by io53 on 10/09/17.
 */
class RuuviScannerApplication : Application() {

    private val bluetoothRangeGatewayFactory: BluetoothTagGatewayFactory by lazy { DefaultBluetoothTagGatewayFactory(this) }

    val bluetoothInteractor by lazy { BluetoothInteractor(bluetoothRangeGatewayFactory) }

    fun startForegroundScanning() {
        bluetoothInteractor.startForegroundScanning()
    }

    fun startBackgroundScanning() {
        bluetoothInteractor.startBackgroundScanning()
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "App class onCreate")

        FlowManager.init(applicationContext)

        bluetoothInteractor.onAppCreated()
    }

    companion object {
        private const val TAG = "RuuviScannerApplication"
    }
}