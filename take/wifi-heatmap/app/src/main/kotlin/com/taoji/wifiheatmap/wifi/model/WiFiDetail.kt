package com.taoji.wifiheatmap.wifi.model

data class WiFiDetail(
    val wiFiIdentifier: WiFiIdentifier = WiFiIdentifier.EMPTY,
    val wiFiSignal: WiFiSignal = WiFiSignal.EMPTY,
)
