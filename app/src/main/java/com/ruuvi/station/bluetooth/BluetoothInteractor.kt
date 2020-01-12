package com.ruuvi.station.bluetooth

import com.ruuvi.station.bluetooth.gateway.factory.BluetoothTagGatewayFactory

class BluetoothInteractor(private val bluetoothTagGatewayFactory: BluetoothTagGatewayFactory) {

    private val bluetoothRangeGateway = bluetoothTagGatewayFactory.create()

    fun startForegroundScanning() {
        bluetoothRangeGateway.startForegroundScanning()
    }

    fun startBackgroundScanning() {
        bluetoothRangeGateway.startBackgroundScanning()
    }

    fun onAppCreated() {
        bluetoothRangeGateway.onAppCreated()
    }
}