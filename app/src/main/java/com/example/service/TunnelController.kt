package com.example.service

import android.content.Context
import android.content.Intent
import com.example.model.ConnectionState
import com.example.model.LogEntry
import com.example.model.LogLevel
import com.example.model.TrafficStats
import com.example.model.TunnelServer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

object TunnelController {

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _trafficStats = MutableStateFlow(TrafficStats())
    val trafficStats: StateFlow<TrafficStats> = _trafficStats.asStateFlow()

    private val _logs = MutableStateFlow<List<LogEntry>>(
        listOf(
            LogEntry(tag = "SYSTEM", message = "Dark Tunnel Core Engine v3.2 ready", level = LogLevel.INFO),
            LogEntry(tag = "VPN", message = "Tap CONNECT to establish encrypted tunnel", level = LogLevel.DEBUG)
        )
    )
    val logs: StateFlow<List<LogEntry>> = _logs.asStateFlow()

    var activeServer: TunnelServer? = null

    fun updateState(state: ConnectionState) {
        _connectionState.value = state
    }

    fun updateStats(stats: TrafficStats) {
        _trafficStats.value = stats
    }

    fun resetStats() {
        _trafficStats.value = TrafficStats()
    }

    fun addLog(tag: String, message: String, level: LogLevel = LogLevel.INFO) {
        val entry = LogEntry(tag = tag, message = message, level = level)
        val current = _logs.value.toMutableList()
        current.add(entry)
        if (current.size > 250) {
            current.removeAt(0)
        }
        _logs.value = current
    }

    fun clearLogs() {
        _logs.value = listOf(
            LogEntry(tag = "CONSOLE", message = "Console logs cleared", level = LogLevel.INFO)
        )
    }

    fun startVpn(
        context: Context,
        server: TunnelServer,
        customTarget: String? = null,
        customProxy: String? = null,
        customPayload: String? = null
    ) {
        activeServer = server
        updateState(ConnectionState.CONNECTING)

        val targetStr = customTarget ?: server.rawConfig.ifEmpty { "${server.host}:${server.port}" }
        val (proxyHost, proxyPort) = if (customProxy != null) {
            if (customProxy.isBlank()) {
                "" to 0
            } else {
                val parts = customProxy.trim().split(":")
                val h = parts[0]
                val p = parts.getOrNull(1)?.toIntOrNull() ?: 80
                h to p
            }
        } else {
            "34.43.46.91" to 80
        }

        val intent = Intent(context, DarkTunnelVpnService::class.java).apply {
            action = DarkTunnelVpnService.ACTION_CONNECT
            putExtra(DarkTunnelVpnService.EXTRA_SERVER_HOST, server.host)
            putExtra(DarkTunnelVpnService.EXTRA_SERVER_PORT, server.port)
            putExtra(DarkTunnelVpnService.EXTRA_SERVER_NAME, server.name)
            putExtra(DarkTunnelVpnService.EXTRA_PROTOCOL, server.protocol.name)
            putExtra(DarkTunnelVpnService.EXTRA_SNI, server.sni)
            putExtra(DarkTunnelVpnService.EXTRA_PAYLOAD, customPayload ?: server.payload)
            putExtra(DarkTunnelVpnService.EXTRA_PROXY_HOST, proxyHost)
            putExtra(DarkTunnelVpnService.EXTRA_PROXY_PORT, proxyPort)
            putExtra(DarkTunnelVpnService.EXTRA_RAW_TARGET, targetStr)
        }
        try {
            context.startService(intent)
        } catch (e: Exception) {
            addLog("ERROR", "Failed to start service: ${e.message}", LogLevel.ERROR)
            updateState(ConnectionState.ERROR)
        }
    }

    fun stopVpn(context: Context) {
        val intent = Intent(context, DarkTunnelVpnService::class.java).apply {
            action = DarkTunnelVpnService.ACTION_DISCONNECT
        }
        try {
            context.startService(intent)
        } catch (_: Exception) {}
        updateState(ConnectionState.DISCONNECTED)
        addLog("DISCONNECT", "Disconnected", LogLevel.INFO)
    }
}
