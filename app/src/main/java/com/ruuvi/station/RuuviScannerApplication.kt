package com.ruuvi.station

import android.app.Application
import android.content.Context
import android.os.Handler
import android.util.Log
import com.raizlabs.android.dbflow.config.FlowManager
import com.ruuvi.station.bluetooth.BluetoothInteractor
import com.ruuvi.station.bluetooth.gateway.BluetoothTagGateway
import com.ruuvi.station.bluetooth.gateway.factory.BackgroundBluetoothScannerGatewayFactory
import com.ruuvi.station.bluetooth.gateway.factory.BluetoothScanningGatewayFactory
import com.ruuvi.station.bluetooth.gateway.factory.BluetoothTagGatewayFactory
import com.ruuvi.station.bluetooth.gateway.factory.DefaultBackgroundBluetoothScannerGatewayFactory
import com.ruuvi.station.bluetooth.gateway.factory.DefaultBluetoothScanningGatewayFactory
import com.ruuvi.station.bluetooth.gateway.factory.DefaultBluetoothTagGatewayFactory
import com.ruuvi.station.bluetooth.model.factory.DefaultLeScanResultFactory
import com.ruuvi.station.bluetooth.model.factory.LeScanResultFactory
import com.ruuvi.station.model.RuuviTag
import com.ruuvi.station.service.AltBeaconScannerForegroundService
import com.ruuvi.station.service.RuuviRangeNotifier
import com.ruuvi.station.util.BackgroundScanModes
import com.ruuvi.station.util.Foreground
import com.ruuvi.station.util.Preferences
import com.ruuvi.station.util.ServiceUtils
import com.ruuvi.station.util.Utils
import org.altbeacon.beacon.BeaconConsumer
import org.altbeacon.beacon.BeaconManager
import org.altbeacon.beacon.Region
import org.altbeacon.bluetooth.BluetoothMedic

class RuuviScannerApplication : Application(), BeaconConsumer {

    val leScanResultFactory: LeScanResultFactory by lazy { DefaultLeScanResultFactory(this) }

    val scannerGatewayFactory: BackgroundBluetoothScannerGatewayFactory by lazy { DefaultBackgroundBluetoothScannerGatewayFactory(this) }

    val bluetoothScanningGatewayFactory: BluetoothScanningGatewayFactory by lazy { DefaultBluetoothScanningGatewayFactory(this) }

    val bluetoothRangeGatewayFactory: BluetoothTagGatewayFactory by lazy { DefaultBluetoothTagGatewayFactory(this) }

    val bluetoothInteractor by lazy { BluetoothInteractor(this) }

    private var beaconManager: BeaconManager? = null
    private var region: Region? = null
    var running = false
    private var prefs: Preferences? = null
    private var ruuviRangeNotifier: RuuviRangeNotifier? = null
    private var foreground = false
    var medic: BluetoothMedic? = null
    var me: com.ruuvi.station.RuuviScannerApplication? = null
    fun stopScanning() {
        Log.d(TAG, "Stopping scanning")
        running = false
        try {
            beaconManager!!.stopRangingBeaconsInRegion(region!!)
        } catch (e: Exception) {
            Log.d(TAG, "Could not remove ranging region")
        }
    }

    fun disposeStuff() {
        Log.d(TAG, "Stopping scanning")
        medic = null
        if (beaconManager == null) return
        running = false
        beaconManager!!.removeRangeNotifier(ruuviRangeNotifier!!)
        try {
            beaconManager!!.stopRangingBeaconsInRegion(region!!)
        } catch (e: Exception) {
            Log.d(TAG, "Could not remove ranging region")
        }
        beaconManager!!.unbind(this)
        beaconManager = null
    }

    private fun runForegroundIfEnabled(): Boolean {
        if (prefs!!.backgroundScanMode === BackgroundScanModes.FOREGROUND) {
            val su = ServiceUtils(applicationContext)
            disposeStuff()
            su.startForegroundService()
            return true
        }
        return false
    }

    fun startForegroundScanning() {
        if (runForegroundIfEnabled()) return
        if (foreground) return
        foreground = true
        Utils.removeStateFile(applicationContext)
        Log.d(TAG, "Starting foreground scanning")
        bindBeaconManager(this, applicationContext)
        beaconManager!!.backgroundMode = false
            if (ruuviRangeNotifier != null) ruuviRangeNotifier.gatewayOn = false
    }

    fun startBackgroundScanning() {
        Log.d(TAG, "Starting background scanning")
        if (runForegroundIfEnabled()) return
        if (prefs!!.backgroundScanMode !== BackgroundScanModes.BACKGROUND) {
            Log.d(TAG, "Background scanning is not enabled, ignoring")
            return
        }
        bindBeaconManager(me, applicationContext)
        var scanInterval = Preferences(applicationContext).backgroundScanInterval * 1000
        val minInterval = 15 * 60 * 1000
        if (scanInterval < minInterval) scanInterval = minInterval
        if (scanInterval.toLong() != beaconManager!!.backgroundBetweenScanPeriod) {
            beaconManager!!.backgroundBetweenScanPeriod = scanInterval.toLong()
            try {
                beaconManager!!.updateScanPeriods()
            } catch (e: Exception) {
                Log.e(TAG, "Could not update scan intervals")
            }
        }
        beaconManager!!.backgroundMode = true
//            if (ruuviRangeNotifier != null) ruuviRangeNotifier.gatewayOn = true
        if (medic == null) medic = Companion.setupMedic(applicationContext)
    }

    private fun bindBeaconManager(consumer: BeaconConsumer?, context: Context) {
        if (beaconManager == null) {
            beaconManager = BeaconManager.getInstanceForApplication(context.applicationContext)
            Utils.setAltBeaconParsers(beaconManager)
            beaconManager!!.backgroundScanPeriod = 5000
            beaconManager!!.bind(consumer!!)
        } else if (!running) {
            running = true
            try {
                beaconManager!!.startRangingBeaconsInRegion(region!!)
            } catch (e: Exception) {
                Log.d(TAG, "Could not start ranging again")
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "App class onCreate")
        bluetoothInteractor.onAppCreated()
        FlowManager.init(applicationContext)
//    }
//        override fun onCreate() {
//            super.onCreate()
        me = this
        Log.d(TAG, "App class onCreate")
//            FlowManager.init(applicationContext)
        prefs = Preferences(applicationContext)
        ruuviRangeNotifier = RuuviRangeNotifier(applicationContext, "RuuviScannerApplication", object : BluetoothTagGateway.OnTagsFoundListener {
            override fun onFoundTags(allTags: List<RuuviTag>) {
                Log.e(TAG, "onFoundTags $allTags")
            }
        })
        Foreground.init(this)
        Foreground.get().addListener(listener)
        Handler().postDelayed({
            if (!foreground) {
                if (prefs!!.backgroundScanMode === BackgroundScanModes.FOREGROUND) {
                    ServiceUtils(applicationContext).startForegroundService()
                } else if (prefs!!.backgroundScanMode === BackgroundScanModes.BACKGROUND) {
                    startBackgroundScanning()
                }
            }
        }, 5000)
        region = Region("com.ruuvi.station.leRegion", null, null, null)
    }

    var listener: Foreground.Listener = object : Foreground.Listener {
        override fun onBecameForeground() {
            Log.d(TAG, "onBecameForeground")
            startForegroundScanning()
//                if (ruuviRangeNotifier != null) ruuviRangeNotifier.gatewayOn = false
        }

        override fun onBecameBackground() {
            Log.d(TAG, "onBecameBackground")
            foreground = false
            val su = ServiceUtils(applicationContext)
            if (prefs!!.backgroundScanMode === BackgroundScanModes.DISABLED) { // background scanning is disabled so all scanning things will be killed
                stopScanning()
                su.stopForegroundService()
            } else if (prefs!!.backgroundScanMode === BackgroundScanModes.BACKGROUND) {
                if (su.isRunning(AltBeaconScannerForegroundService::class.java)) {
                    su.stopForegroundService()
                } else {
                    startBackgroundScanning()
                }
            } else {
                disposeStuff()
                su.startForegroundService()
            }
//                if (ruuviRangeNotifier != null) ruuviRangeNotifier.gatewayOn = true
        }
    }

    override fun onBeaconServiceConnect() {
        Log.d(TAG, "onBeaconServiceConnect")
        //Toast.makeText(getApplicationContext(), "Started scanning (Application)", Toast.LENGTH_SHORT).show();
//            ruuviRangeNotifier.gatewayOn = !foreground
        if (!beaconManager!!.rangingNotifiers.contains(ruuviRangeNotifier)) {
            beaconManager!!.addRangeNotifier(ruuviRangeNotifier!!)
        }
        running = true
        try {
            beaconManager!!.startRangingBeaconsInRegion(region!!)
        } catch (e: Exception) {
            Log.e(TAG, "Could not start ranging")
        }
    }

    companion object {
        private const val TAG = "RuuviScannerApplication"
        fun setupMedic(context: Context?): BluetoothMedic {
            val medic = BluetoothMedic.getInstance()
            medic.enablePowerCycleOnFailures(context)
            medic.enablePeriodicTests(context, BluetoothMedic.SCAN_TEST)
            return medic
        }
    }
}