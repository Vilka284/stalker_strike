package com.example.stalker_strike

data class Signal(
    val ssid: String,
    val waveLevel: Int
) {
    override fun equals(other: Any?) = (other is Signal) && (ssid == other.ssid)

    override fun hashCode(): Int {
        return ssid.hashCode()
    }
}


val SIGNAL_HEAL = Signal("heal", 0)
val SIGNAL_RADIATION = Signal("radiation", 0)
val SIGNAL_ANOMALY = Signal("anomaly", 0)
