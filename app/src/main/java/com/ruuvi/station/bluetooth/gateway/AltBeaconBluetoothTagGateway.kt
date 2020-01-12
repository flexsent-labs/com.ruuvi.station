package com.ruuvi.station.bluetooth.gateway

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.location.Location
import android.util.Log
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.ruuvi.station.bluetooth.gateway.listener.DefaultOnTagFoundListener
import com.ruuvi.station.service.RuuviRangeNotifier
import com.ruuvi.station.util.BackgroundScanModes
import com.ruuvi.station.util.Preferences
import com.ruuvi.station.util.ServiceUtils
import com.ruuvi.station.util.Utils
import org.altbeacon.beacon.BeaconConsumer
import org.altbeacon.beacon.BeaconManager
import org.altbeacon.beacon.Region
import org.altbeacon.bluetooth.BluetoothMedic
import java.util.HashMap

class AltBeaconBluetoothTagGateway(private val application: Application) : BluetoothTagGateway {

    private val TAG = AltBeaconBluetoothTagGateway::class.java.simpleName

    private var prefs = Preferences(application)

    private var tagLocation: Location? = null

    private var lastLogged: MutableMap<String, Long> = HashMap()

    private val fusedLocationClient: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this.application)

    private var gatewayOn: Boolean = false

    // NEW REFACTORING --------------

    private var beaconManager: BeaconManager? = null
    private var region: Region? = null
    var running = false
    private var ruuviRangeNotifier: RuuviRangeNotifier? = null
    private var foreground = false
    var medic: BluetoothMedic? = null

    private val beaconConsumer = object : BeaconConsumer {

        override fun getApplicationContext(): Context = application

        override fun unbindService(p0: ServiceConnection?) {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun bindService(p0: Intent?, p1: ServiceConnection?, p2: Int): Boolean {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
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
    }

    override fun stopScanning() {
        Log.d(TAG, "Stopping scanning")
        running = false
        try {
            beaconManager!!.stopRangingBeaconsInRegion(region!!)
        } catch (e: Exception) {
            Log.d(TAG, "Could not remove ranging region")
        }
    }

    override fun setGatewayOn(isGatewayOn: Boolean) {
        ruuviRangeNotifier?.isGatewayOn = isGatewayOn
    }

    override fun disposeStuff() {
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
        beaconManager!!.unbind(beaconConsumer)
        beaconManager = null
    }

    override fun runForegroundIfEnabled(): Boolean {
        if (prefs.backgroundScanMode === BackgroundScanModes.FOREGROUND) {
            val su = ServiceUtils(this.application)
            disposeStuff()
            su.startForegroundService()
            return true
        }
        return false
    }

    override fun startForegroundScanning() {
        if (runForegroundIfEnabled()) return
        if (foreground) return
        foreground = true

        Utils.removeStateFile(application)
        Log.d(TAG, "Starting foreground scanning")
        bindBeaconManager(beaconConsumer, application)
        beaconManager!!.backgroundMode = false
        if (ruuviRangeNotifier != null) ruuviRangeNotifier!!.isGatewayOn = false
    }

    override fun startBackgroundScanning(): Boolean {
        Log.d(TAG, "Starting background scanning")
        if (runForegroundIfEnabled()) return false
        if (prefs.backgroundScanMode !== BackgroundScanModes.BACKGROUND) {
            Log.d(TAG, "Background scanning is not enabled, ignoring")
            return false
        }
        bindBeaconManager(beaconConsumer, application)
        var scanInterval = Preferences(application).backgroundScanInterval * 1000
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
        if (medic == null) medic = setupMedic(application)
        return true
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

    override fun onAppCreated() {

        ruuviRangeNotifier = RuuviRangeNotifier(
            application,
            "BluetoothInteractor",
            DefaultOnTagFoundListener(application)
        )

        region = Region("com.ruuvi.station.leRegion", null, null, null)
    }

    private fun setupMedic(context: Context?): BluetoothMedic {
        val medic = BluetoothMedic.getInstance()
        medic.enablePowerCycleOnFailures(context)
        medic.enablePeriodicTests(context, BluetoothMedic.SCAN_TEST)
        return medic
    }
}