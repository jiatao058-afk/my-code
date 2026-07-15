package com.taoji.wifiheatmap

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.taoji.wifiheatmap.ui.collect.CollectActivity
import com.taoji.wifiheatmap.ui.heatmap.HeatmapActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<android.widget.Button>(R.id.btn_new_collection)?.setOnClickListener {
            startActivity(Intent(this, CollectActivity::class.java))
        }

        findViewById<android.widget.Button>(R.id.btn_view_heatmap)?.setOnClickListener {
            startActivity(Intent(this, HeatmapActivity::class.java))
        }
    }
}
