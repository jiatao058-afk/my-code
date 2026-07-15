package com.taoji.wifiheatmap.wifi.model

data class WiFiSignal(
    val primaryFrequency: Int = 0,
    val centerFrequency: Int = 0,
    val level: Int = 0,
) {
    companion object {
        val EMPTY = WiFiSignal()
    }
}
