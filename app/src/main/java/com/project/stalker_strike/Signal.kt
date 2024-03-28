package com.project.stalker_strike

import android.Manifest
import android.annotation.SuppressLint
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import android.util.Log
import androidx.annotation.RequiresPermission

data class Signal(
    val ssid: String,
    val waveLevel: Int
) {
    override fun equals(other: Any?) = (other is Signal) && (ssid == other.ssid)

    override fun hashCode(): Int {
        return ssid.hashCode()
    }
}

@SuppressLint("MissingPermission")
@RequiresPermission(allOf = [Manifest.permission.ACCESS_WIFI_STATE, Manifest.permission.ACCESS_FINE_LOCATION])
fun refreshSignalList(wifiManager: WifiManager): ArrayList<Signal> {
    Log.i("WIFI_SCAN", "refreshWifiList: ")

    val networkList: ArrayList<Signal> = ArrayList()
    val scanResults: List<ScanResult> = wifiManager.scanResults
    scanResults.forEach {
        Log.i("WIFI_SCAN", "refreshWifiList: scanResults=$it")
        val ssid = it.SSID
        val waveLevel = WifiManager.calculateSignalLevel(it.level, 5)

        networkList.add(Signal(ssid, waveLevel))
    }

    return networkList
}

val SIGNAL_HEAL = Signal("heal", 0)
val SIGNAL_RADIATION = Signal("radiation", 0)
val SIGNAL_ANOMALY = Signal("anomaly", 0)

var signalList: ArrayList<Signal> = arrayListOf()