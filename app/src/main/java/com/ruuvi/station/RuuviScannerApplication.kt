package com.ruuvi.station

import android.app.Application
import android.content.Context
import android.os.Handler
import android.util.Log
import com.raizlabs.android.dbflow.config.FlowManager
import com.ruuvi.station.bluetooth.gateway.factory.BluetoothTagGatewayFactory
import com.ruuvi.station.bluetooth.gateway.factory.DefaultBluetoothTagGatewayFactory
import com.ruuvi.station.bluetooth.gateway.listener.DefaultOnTagFoundListener
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

/**
 * Created by io53 on 10/09/17.
 */
class RuuviScannerApplication : Application(), BeaconConsumer {

    private var beaconManager: BeaconManager? = null
    private var region: Region? = null
    var running = false
    private var prefs: Preferences? = null
    private var ruuviRangeNotifier: RuuviRangeNotifier? = null
    private var foreground = false
    var medic: BluetoothMedic? = null
    var me: RuuviScannerApplication? = null

    val bluetoothRangeGatewayFactory: BluetoothTagGatewayFactory by lazy { DefaultBluetoothTagGatewayFactory(this) }

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
        if (ruuviRangeNotifier != null) ruuviRangeNotifier!!.isGatewayOn = false
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
        if (ruuviRangeNotifier != null) ruuviRangeNotifier!!.isGatewayOn = true
        if (medic == null) medic = setupMedic(applicationContext)
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
        me = this
        Log.d(TAG, "App class onCreate")
        FlowManager.init(applicationContext)
        prefs = Preferences(applicationContext)
        ruuviRangeNotifier = RuuviRangeNotifier(
            applicationContext,
            "RuuviScannerApplication",
            DefaultOnTagFoundListener(this)
        )

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
            if (ruuviRangeNotifier != null) ruuviRangeNotifier!!.isGatewayOn = false
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
            if (ruuviRangeNotifier != null) ruuviRangeNotifier!!.isGatewayOn = true
        }
    }

    override fun onBeaconServiceConnect() {
        Log.d(TAG, "onBeaconServiceConnect")
        //Toast.makeText(getApplicationContext(), "Started scanning (Application)", Toast.LENGTH_SHORT).show();
        ruuviRangeNotifier!!.isGatewayOn = !foreground
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
        @JvmStatic
        fun setupMedic(context: Context?): BluetoothMedic {
            val medic = BluetoothMedic.getInstance()
            medic.enablePowerCycleOnFailures(context)
            medic.enablePeriodicTests(context, BluetoothMedic.SCAN_TEST)
            return medic
        }
    }
}