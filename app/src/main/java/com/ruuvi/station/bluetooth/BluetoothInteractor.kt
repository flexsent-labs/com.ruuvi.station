package com.ruuvi.station.bluetooth

import android.Manifest
import android.app.Activity
import android.app.Application
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.location.Location
import android.os.Handler
import android.support.v4.content.ContextCompat
import android.util.Log
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.ruuvi.station.RuuviScannerApplication
import com.ruuvi.station.bluetooth.gateway.BluetoothTagGateway
import com.ruuvi.station.bluetooth.gateway.listener.DefaultOnTagFoundListener
import com.ruuvi.station.gateway.Http
import com.ruuvi.station.model.RuuviTag
import com.ruuvi.station.model.TagSensorReading
import com.ruuvi.station.service.AltBeaconScannerForegroundService
import com.ruuvi.station.service.RuuviRangeNotifier
import com.ruuvi.station.util.AlarmChecker
import com.ruuvi.station.util.BackgroundScanModes
import com.ruuvi.station.util.Constants
import com.ruuvi.station.util.Foreground
import com.ruuvi.station.util.Preferences
import com.ruuvi.station.util.ServiceUtils
import com.ruuvi.station.util.Utils
import org.altbeacon.beacon.BeaconConsumer
import org.altbeacon.beacon.BeaconManager
import org.altbeacon.beacon.Region
import org.altbeacon.bluetooth.BluetoothMedic
import java.util.Calendar
import java.util.Date
import java.util.HashMap

class BluetoothInteractor(private val application: Application) {

//    val isForegroundScanningActive: Boolean
//        get() = bluetoothRangeGateway.isForegroundScanningActive()

    private val TAG: String = BluetoothInteractor::class.java.simpleName

    private val bluetoothRangeGateway = (this.application as RuuviScannerApplication).bluetoothRangeGatewayFactory.create()

    fun startForegroundScanning() {
        bluetoothRangeGateway.startForegroundScanning()
//        if (runForegroundIfEnabled()) return
//        if (foreground) return
//        foreground = true
//
//        Utils.removeStateFile(application)
//        Log.d(TAG, "Starting foreground scanning")
//        bindBeaconManager(beaconConsumer, application)
//        beaconManager!!.backgroundMode = false
//        if (ruuviRangeNotifier != null) ruuviRangeNotifier!!.isGatewayOn = false
    }

    fun startBackgroundScanning() {
        bluetoothRangeGateway.startBackgroundScanning()
//
//        Log.d(TAG, "Starting background scanning")
//        if (runForegroundIfEnabled()) return
//        if (prefs!!.backgroundScanMode !== BackgroundScanModes.BACKGROUND) {
//            Log.d(TAG, "Background scanning is not enabled, ignoring")
//            return
//        }
//        bindBeaconManager(beaconConsumer, application)
//        var scanInterval = Preferences(application).backgroundScanInterval * 1000
//        val minInterval = 15 * 60 * 1000
//        if (scanInterval < minInterval) scanInterval = minInterval
//        if (scanInterval.toLong() != beaconManager!!.backgroundBetweenScanPeriod) {
//            beaconManager!!.backgroundBetweenScanPeriod = scanInterval.toLong()
//            try {
//                beaconManager!!.updateScanPeriods()
//            } catch (e: Exception) {
//                Log.e(TAG, "Could not update scan intervals")
//            }
//        }
//        beaconManager!!.backgroundMode = true
//        if (ruuviRangeNotifier != null) ruuviRangeNotifier!!.isGatewayOn = true
//        if (medic == null) medic = setupMedic(application)
    }

//    private fun bindBeaconManager(consumer: BeaconConsumer?, context: Context) {
//        if (beaconManager == null) {
//            beaconManager = BeaconManager.getInstanceForApplication(context.applicationContext)
//            Utils.setAltBeaconParsers(beaconManager)
//            beaconManager!!.backgroundScanPeriod = 5000
//            beaconManager!!.bind(consumer!!)
//        } else if (!running) {
//            running = true
//            try {
//                beaconManager!!.startRangingBeaconsInRegion(region!!)
//            } catch (e: Exception) {
//                Log.d(TAG, "Could not start ranging again")
//            }
//        }
//    }

    fun onAppCreated() {
        bluetoothRangeGateway.onAppCreated()
//
//        prefs = Preferences(application)
//        ruuviRangeNotifier = RuuviRangeNotifier(
//            application,
//            "BluetoothInteractor",
//            DefaultOnTagFoundListener(application)
//        )
////
//        Foreground.init(application)
//
//        val listener: Foreground.Listener = object : Foreground.Listener {
//            override fun onBecameForeground() {
//                Log.d(TAG, "onBecameForeground")
//                startForegroundScanning()
//                if (ruuviRangeNotifier != null) ruuviRangeNotifier!!.isGatewayOn = false
//            }
//
//            override fun onBecameBackground() {
//                Log.d(TAG, "onBecameBackground")
//                foreground = false
//                val su = ServiceUtils(application)
//                if (prefs!!.backgroundScanMode === BackgroundScanModes.DISABLED) { // background scanning is disabled so all scanning things will be killed
//                    stopScanning()
//                    su.stopForegroundService()
//                } else if (prefs!!.backgroundScanMode === BackgroundScanModes.BACKGROUND) {
//                    if (su.isRunning(AltBeaconScannerForegroundService::class.java)) {
//                        su.stopForegroundService()
//                    } else {
//                        startBackgroundScanning()
//                    }
//                } else {
//                    disposeStuff()
//                    su.startForegroundService()
//                }
//                if (ruuviRangeNotifier != null) ruuviRangeNotifier!!.isGatewayOn = true
//            }
//        }
//
//        Foreground.get().addListener(listener)
//
//        Handler().postDelayed({
//            if (!foreground) {
//                if (prefs!!.backgroundScanMode === BackgroundScanModes.FOREGROUND) {
//                    ServiceUtils(application).startForegroundService()
//                } else if (prefs!!.backgroundScanMode === BackgroundScanModes.BACKGROUND) {
//                    startBackgroundScanning()
//                }
//            }
//        }, 5000)
//
//        region = Region("com.ruuvi.station.leRegion", null, null, null)
    }
}