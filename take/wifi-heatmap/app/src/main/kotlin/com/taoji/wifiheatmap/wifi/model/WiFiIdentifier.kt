package com.taoji.wifiheatmap.wifi.model

data class WiFiIdentifier(
    val ssid: String = "",
    val bssid: String = "",
) {
    companion object {
        val EMPTY = WiFiIdentifier()
    }
}
