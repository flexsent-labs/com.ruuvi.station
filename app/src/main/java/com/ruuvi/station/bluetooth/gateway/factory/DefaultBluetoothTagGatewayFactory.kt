package com.ruuvi.station.bluetooth.gateway.factory

import android.app.Application
import com.ruuvi.station.bluetooth.gateway.BluetoothTagGateway
import com.ruuvi.station.bluetooth.gateway.AltBeaconBluetoothTagGateway

class DefaultBluetoothTagGatewayFactory(private val application: Application) : BluetoothTagGatewayFactory {

    override fun create(): BluetoothTagGateway =
        AltBeaconBluetoothTagGateway(application)
}