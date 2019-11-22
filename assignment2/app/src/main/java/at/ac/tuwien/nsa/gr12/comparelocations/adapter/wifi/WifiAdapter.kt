package at.ac.tuwien.nsa.gr12.comparelocations.adapter.wifi

import android.content.Context
import android.content.IntentFilter
import android.net.wifi.WifiManager
import android.widget.Toast
import at.ac.tuwien.nsa.gr12.comparelocations.core.model.AccessPoint
import at.ac.tuwien.nsa.gr12.comparelocations.core.ports.WifiPort
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred

class WifiAdapter(private val context: Context) : WifiPort {

    private val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager

    private var deferred: CompletableDeferred<List<AccessPoint>>? = null

    init {
        val wifiScanReceiver = WifiScanReceiver(::scanSuccess, ::scanFailure)

        val intentFilter = IntentFilter()
        intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
        context.registerReceiver(wifiScanReceiver, intentFilter)
    }

    override suspend fun getAsync(): Deferred<List<AccessPoint>> {
        deferred = CompletableDeferred()

        enableWifi()

        val success = wifiManager.startScan()
        if (!success) {
            scanFailure()
        }

        return deferred!!
    }

    private fun scanSuccess() {
        if (deferred == null) {
            return
        }

        val results = wifiManager.scanResults
        val accessPoints = results.map { r ->
            val macAddress = r.BSSID
            val signalStrength = r.level
            AccessPoint(macAddress, signalStrength)
        }
        deferred!!.complete(accessPoints)
    }

    private fun scanFailure() {
        if (deferred == null) {
            return
        }

        deferred!!.completeExceptionally(RuntimeException("Scanning the nearby access points failed"))
    }

    private fun enableWifi() {
        if (wifiManager.isWifiEnabled) {
            return
        }
        Toast.makeText(context, "wifi is disabled..making it enabled", Toast.LENGTH_LONG).show()
        wifiManager.setWifiEnabled(true)
    }
}