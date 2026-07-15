package com.taoji.wifiheatmap.ui.heatmap

import android.os.Bundle
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.taoji.wifiheatmap.R
import kotlinx.coroutines.launch

class HeatmapActivity : AppCompatActivity() {
    private val viewModel: HeatmapViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_heatmap)

        val renderer = findViewById<HeatmapRenderer>(R.id.heatmap_renderer)
        val sessionSpinner = findViewById<Spinner>(R.id.spinner_session)
        val loadingIndicator = findViewById<ProgressBar>(R.id.loading_indicator)
        val emptyText = findViewById<TextView>(R.id.tv_empty)
        val deleteBtn = findViewById<Button>(R.id.btn_delete)

        lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                loadingIndicator.visibility =
                    if (state.isLoading) android.view.View.VISIBLE else android.view.View.GONE

                if (state.sessions.isNotEmpty()) {
                    sessionSpinner.visibility = android.view.View.VISIBLE
                    emptyText.visibility = android.view.View.GONE

                    val adapter = ArrayAdapter(
                        this@HeatmapActivity,
                        android.R.layout.simple_spinner_item,
                        state.sessions
                    )
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    sessionSpinner.adapter = adapter

                    if (state.selectedSessionId != null) {
                        val idx = state.sessions.indexOf(state.selectedSessionId)
                        if (idx >= 0) sessionSpinner.setSelection(idx)
                    }
                } else {
                    sessionSpinner.visibility = android.view.View.GONE
                    emptyText.visibility = android.view.View.VISIBLE
                }

                if (state.positionSummaries.isNotEmpty()) {
                    renderer.visibility = android.view.View.VISIBLE
                    renderer.setData(state.positionSummaries)
                } else if (!state.isLoading) {
                    renderer.visibility = android.view.View.GONE
                }
            }
        }

        sessionSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: android.view.View?, pos: Int, id: Long) {
                val sessionId = parent?.getItemAtPosition(pos) as? String ?: return
                viewModel.selectSession(sessionId)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        deleteBtn.setOnClickListener {
            val sessionId = viewModel.uiState.value.selectedSessionId ?: return@setOnClickListener
            viewModel.deleteSession(sessionId)
        }
    }
}
