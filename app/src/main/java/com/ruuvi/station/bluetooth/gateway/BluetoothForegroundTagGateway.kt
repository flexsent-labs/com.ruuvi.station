package com.ruuvi.station.bluetooth.gateway

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.RemoteException
import android.util.Log
import com.ruuvi.station.bluetooth.gateway.listener.DefaultOnTagFoundListener
import com.ruuvi.station.service.RuuviRangeNotifier
import com.ruuvi.station.util.Foreground
import com.ruuvi.station.util.Preferences
import com.ruuvi.station.util.Utils
import org.altbeacon.beacon.BeaconConsumer
import org.altbeacon.beacon.BeaconManager
import org.altbeacon.beacon.Region
import org.altbeacon.bluetooth.BluetoothMedic

class BluetoothForegroundTagGateway(private val application: Application) {

    private var beaconManager: BeaconManager? = null
    private var region: Region? = null
    private var ruuviRangeNotifier: RuuviRangeNotifier? = null
    private var medic: BluetoothMedic? = null

    private var foregroundListener: Foreground.Listener = object : Foreground.Listener {
        override fun onBecameForeground() {
            Utils.removeStateFile(application)
            beaconManager?.backgroundMode = false
        }

        override fun onBecameBackground() {
            setBackground()
        }
    }

    private val beaconConsumer = object : BeaconConsumer {

        override fun getApplicationContext(): Context = application

        override fun unbindService(serviceConnection: ServiceConnection?) {
            application.unbindService(serviceConnection)
        }

        override fun bindService(
            intent: Intent?,
            serviceConnection
            : ServiceConnection?,
            flags: Int
        ): Boolean =
            application.bindService(intent, serviceConnection, flags)

        override fun onBeaconServiceConnect() {
            Log.d(TAG, "onBeaconServiceConnect")
            //Toast.makeText(getapplication(), "Started scanning (Service)", Toast.LENGTH_SHORT).show();
            RuuviRangeNotifier.gatewayOn = true
            if (!(beaconManager?.rangingNotifiers?.contains(ruuviRangeNotifier) == true)) {
                ruuviRangeNotifier?.let {
                    beaconManager?.addRangeNotifier(it)
                }
            }
            try {
                region?.let {
                    beaconManager?.startRangingBeaconsInRegion(it)
                }
            } catch (e: RemoteException) {
                Log.e(TAG, "Could not start ranging")
            }
        }
    }

    fun initOnServiceCreate() {

        beaconManager = BeaconManager.getInstanceForApplication(application)
        Utils.setAltBeaconParsers(beaconManager)
        beaconManager?.backgroundScanPeriod = 5000

        ruuviRangeNotifier = RuuviRangeNotifier(
            application,
            "AltBeaconFGScannerService",
            DefaultOnTagFoundListener(application)
        )
        region = Region("com.ruuvi.station.leRegion", null, null, null)
        beaconManager?.setEnableScheduledScanJobs(false)
        beaconManager?.bind(beaconConsumer)
        medic = setupMedic(application)

        Foreground.init(application)
        Foreground.get().addListener(foregroundListener)
    }

    private fun setupMedic(context: Context): BluetoothMedic {
        val medic = BluetoothMedic.getInstance()
        medic.enablePowerCycleOnFailures(context)
        medic.enablePeriodicTests(context, BluetoothMedic.SCAN_TEST)
        return medic
    }

    fun setBackground() {
        if (shouldUpdateScanPeriod()) {

            val scanInterval = Preferences(application).backgroundScanInterval * 1000L
            beaconManager?.backgroundBetweenScanPeriod = scanInterval
            try {
                beaconManager?.updateScanPeriods()
            } catch (e: Throwable) {
                Log.e(TAG, "Could not update scan intervals")
            }
        }
        beaconManager?.backgroundMode = true
    }

    fun shouldUpdateScanPeriod(): Boolean {
        val scanInterval = Preferences(application).backgroundScanInterval * 1000
        return scanInterval.toLong() != beaconManager?.backgroundBetweenScanPeriod
    }

    fun onServiceDestroy() {
        Log.d(TAG, "onDestroy =======")
        ruuviRangeNotifier?.let {
            beaconManager?.removeRangeNotifier(it)
        }
        try {
            region?.let {
                beaconManager?.stopRangingBeaconsInRegion(it)
            }
        } catch (e: Exception) {
            Log.d(TAG, "Could not stop ranging region")
        }
        medic = null
        beaconManager?.unbind(beaconConsumer)
        beaconManager = null
        ruuviRangeNotifier = null
        if (foregroundListener != null) Foreground.get().removeListener(foregroundListener)
    }

    companion object {
        private const val TAG = "BlForegroundTagGw"
    }
}