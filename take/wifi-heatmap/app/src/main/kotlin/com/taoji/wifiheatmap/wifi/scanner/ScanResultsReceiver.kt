/*
 * WiFiAnalyzer
 * Copyright (C) 2015 - 2026 VREM Software Development <VREMSoftwareDevelopment@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */
package com.taoji.wifiheatmap.wifi.scanner

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.WifiManager
import com.taoji.wifiheatmap.annotation.OpenClass

fun interface Callback { // Compliant, function interface used
    fun onSuccess()
}

@OpenClass
internal class ScanResultsReceiver(
    private val callback: Callback,
) : BroadcastReceiver() {
    private var registered = false

    fun register(context: Context) {
        if (!registered) {
            val intentFilter = makeIntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
            context.registerReceiver(this, intentFilter)
            registered = true
        }
    }

    fun unregister(context: Context) {
        if (registered) {
            context.unregisterReceiver(this)
            registered = false
        }
    }

    fun makeIntentFilter(action: String): IntentFilter = IntentFilter(action)

    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        if (WifiManager.SCAN_RESULTS_AVAILABLE_ACTION == intent.action &&
            intent.getBooleanExtra(WifiManager.EXTRA_RESULTS_UPDATED, false)
        ) {
            callback.onSuccess()
        }
    }
}
