package com.taoji.wifiheatmap.wifi.scanner

import android.net.wifi.ScanResult

data class TransformedResult(
    val bssid: String,
    val ssid: String,
    val frequency: Int,
    val level: Int,
)

class Transformer {
    fun transform(scanResult: ScanResult): TransformedResult {
        return TransformedResult(
            bssid = scanResult.BSSID ?: "",
            ssid = scanResult.SSID ?: "",
            frequency = scanResult.frequency,
            level = scanResult.level,
        )
    }

    fun transformList(scanResults: List<ScanResult>): List<TransformedResult> =
        scanResults.map { transform(it) }
}
