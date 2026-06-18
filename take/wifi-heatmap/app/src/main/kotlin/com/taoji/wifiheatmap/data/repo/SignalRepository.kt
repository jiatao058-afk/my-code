package com.taoji.wifiheatmap.data.repo

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File

data class SignalReading(
    val bssid: String,
    val ssid: String,
    val frequency: Int,
    val level: Int,
    val positionName: String,
    val timestamp: Long,
    val sessionId: String,
)

data class PositionSummary(
    val positionName: String,
    val avgLevel: Double,
    val sampleCount: Int,
    val color: Int,
)

data class NetworkSignal(
    val ssid: String,
    val bssid: String,
    val levels: Map<String, Double>, // positionName -> avgLevel
)

class SignalRepository(private val context: Context) {
    private val gson = Gson()
    private val dataDir = File(context.filesDir, "heatmap_data").also { it.mkdirs() }

    fun saveScanResults(
        scanResults: List<com.taoji.wifiheatmap.wifi.scanner.TransformedResult>,
        positionName: String,
        sessionId: String,
    ): Int {
        val timestamp = System.currentTimeMillis()
        val readings = scanResults.map { r ->
            SignalReading(
                bssid = r.bssid,
                ssid = r.ssid.ifEmpty { "<隐藏>" },
                frequency = r.frequency,
                level = r.level,
                positionName = positionName,
                timestamp = timestamp,
                sessionId = sessionId,
            )
        }
        val file = File(dataDir, "$sessionId.json")
        val existing = if (file.exists()) {
            gson.fromJson<List<SignalReading>>(file.readText(), listType) ?: emptyList()
        } else emptyList()
        val all = existing + readings
        file.writeText(gson.toJson(all))
        return readings.size
    }

    fun getPositionSummaries(sessionId: String): List<PositionSummary> {
        val readings = loadReadings(sessionId)
        if (readings.isEmpty()) return emptyList()
        val grouped = readings.groupBy { it.positionName }
        return grouped.map { (pos, list) ->
            val avg = list.map { it.level }.average()
            PositionSummary(
                positionName = pos,
                avgLevel = avg,
                sampleCount = list.size,
                color = levelToColor(avg),
            )
        }.sortedBy { it.positionName }
    }

    fun getNetworkSignals(sessionId: String): List<NetworkSignal> {
        val readings = loadReadings(sessionId)
        if (readings.isEmpty()) return emptyList()
        val byBssid = readings.groupBy { it.bssid }
        return byBssid.map { (bssid, list) ->
            val byPos = list.groupBy { it.positionName }
            NetworkSignal(
                ssid = list.first().ssid,
                bssid = bssid,
                levels = byPos.mapValues { (_, rds) -> rds.map { it.level }.average() },
            )
        }.sortedByDescending { it.levels.values.average() }
    }

    fun getSessions(): List<String> {
        return dataDir.listFiles()
            ?.filter { it.extension == "json" }
            ?.map { it.nameWithoutExtension }
            ?.sortedDescending()
            ?: emptyList()
    }

    fun deleteSession(sessionId: String) {
        File(dataDir, "$sessionId.json").delete()
    }

    fun getScanCount(sessionId: String): Int {
        return loadReadings(sessionId).size
    }

    private fun loadReadings(sessionId: String): List<SignalReading> {
        val file = File(dataDir, "$sessionId.json")
        return if (file.exists()) {
            gson.fromJson(file.readText(), listType) ?: emptyList()
        } else emptyList()
    }

    companion object {
        private val listType = object : TypeToken<List<SignalReading>>() {}.type

        fun levelToColor(avg: Double): Int = when {
            avg >= -50 -> 0xFF00E676.toInt()
            avg >= -67 -> 0xFF76FF03.toInt()
            avg >= -70 -> 0xFFFFEA00.toInt()
            avg >= -80 -> 0xFFFF9100.toInt()
            else -> 0xFFFF1744.toInt()
        }
    }
}
