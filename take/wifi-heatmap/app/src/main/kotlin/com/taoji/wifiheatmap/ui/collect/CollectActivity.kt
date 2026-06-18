package com.taoji.wifiheatmap.ui.collect

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.taoji.wifiheatmap.R
import kotlinx.coroutines.launch

class CollectActivity : AppCompatActivity() {
    private val viewModel: CollectViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_collect)

        val positionInput = findViewById<EditText>(R.id.et_position)
        val markBtn = findViewById<Button>(R.id.btn_mark)
        val positionList = findViewById<LinearLayout>(R.id.ll_positions)
        val startBtn = findViewById<Button>(R.id.btn_start)
        val stopBtn = findViewById<Button>(R.id.btn_stop)
        val readingsText = findViewById<TextView>(R.id.tv_readings)
        val timeText = findViewById<TextView>(R.id.tv_time)
        val wifiText = findViewById<TextView>(R.id.tv_wifi_status)
        val scanInfoText = findViewById<TextView>(R.id.tv_scan_info)

        // Check WiFi
        val wifiOk = viewModel.checkWiFi()
        wifiText.text = if (wifiOk) "WiFi: 已开启" else "WiFi: 未开启！请打开WiFi"
        wifiText.setTextColor(if (wifiOk) 0xFF00E676.toInt() else 0xFFFF1744.toInt())

        markBtn.setOnClickListener {
            val name = positionInput.text.toString().trim()
            if (name.isEmpty()) {
                Toast.makeText(this, "请输入位置名称", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            viewModel.markPosition(name)
            positionInput.text?.clear()
            refreshPositionList(positionList)
        }

        startBtn.setOnClickListener {
            if (!viewModel.checkWiFi()) {
                Toast.makeText(this, "请先打开WiFi", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (viewModel.uiState.value.currentPosition.isEmpty()) {
                Toast.makeText(this, "请先标记当前位置", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            viewModel.startScanning()
        }

        stopBtn.setOnClickListener {
            viewModel.stopScanning()
        }

        lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                readingsText.text = "已采集: ${state.totalReadings} 条"
                timeText.text = "扫描: ${state.elapsedSeconds}s"

                val pos = state.currentPosition
                scanInfoText.text = if (pos.isNotEmpty()) "当前位置: $pos" else "尚未标记位置"

                if (state.scanThrottled) {
                    scanInfoText.text = "${scanInfoText.text} ⚠扫描限速"
                }

                startBtn.isEnabled = !state.isScanning && state.currentPosition.isNotEmpty()
                stopBtn.isEnabled = true // Always enable stop (Bug A fix)
                markBtn.isEnabled = !state.isScanning

                if (state.error != null) {
                    Toast.makeText(this@CollectActivity, state.error, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun refreshPositionList(container: LinearLayout) {
        container.removeAllViews()
        for (pos in viewModel.uiState.value.markedPositions) {
            val chip = Button(this).apply {
                text = pos
                isAllCaps = false
                setOnClickListener {
                    viewModel.setCurrentPosition(pos)
                }
                val isCurrent = pos == viewModel.uiState.value.currentPosition
                setBackgroundColor(if (isCurrent) 0xFF4CAF50.toInt() else 0xFF607D8B.toInt())
                setTextColor(0xFFFFFFFF.toInt())
            }
            container.addView(chip)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.stopScanning()
    }
}
