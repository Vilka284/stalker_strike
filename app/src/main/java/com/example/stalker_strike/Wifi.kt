package com.example.stalkerstrike

data class Wifi(
    val ssid: String,
    val waveLevel: Int
) {
    override fun equals(other: Any?) = (other is Wifi) && (ssid == other.ssid)

    override fun hashCode(): Int {
        return ssid.hashCode()
    }
}


val WIFI_HEAL = Wifi("heal", 0)
val WIFI_RADIATION = Wifi("radiation", 0)
val WIFI_ANOMALY = Wifi("anomaly", 0)
