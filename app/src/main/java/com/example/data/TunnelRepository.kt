package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.model.PayloadConfig
import com.example.model.TunnelProtocol
import com.example.model.TunnelServer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

class TunnelRepository(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("dark_tunnel_prefs", Context.MODE_PRIVATE)

    private val _servers = MutableStateFlow<List<TunnelServer>>(emptyList())
    val servers: StateFlow<List<TunnelServer>> = _servers.asStateFlow()

    private val _selectedServer = MutableStateFlow<TunnelServer>(DefaultServers.list.first())
    val selectedServer: StateFlow<TunnelServer> = _selectedServer.asStateFlow()

    private val _payloadConfig = MutableStateFlow(PayloadConfig())
    val payloadConfig: StateFlow<PayloadConfig> = _payloadConfig.asStateFlow()

    // Settings
    private val _dnsServer = MutableStateFlow(prefs.getString("dns_server", "1.1.1.1") ?: "1.1.1.1")
    val dnsServer: StateFlow<String> = _dnsServer.asStateFlow()

    private val _udpForwarding = MutableStateFlow(prefs.getBoolean("udp_forwarding", true))
    val udpForwarding: StateFlow<Boolean> = _udpForwarding.asStateFlow()

    private val _wakeLock = MutableStateFlow(prefs.getBoolean("wake_lock", true))
    val wakeLock: StateFlow<Boolean> = _wakeLock.asStateFlow()

    init {
        loadServers()
    }

    private fun loadServers() {
        val savedJson = prefs.getString("custom_servers", null)
        val list = mutableListOf<TunnelServer>()

        // Add default servers
        list.addAll(DefaultServers.list)

        // Add saved custom/extracted servers
        if (!savedJson.isNullOrEmpty()) {
            try {
                val array = JSONArray(savedJson)
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    list.add(
                        TunnelServer(
                            id = obj.getString("id"),
                            name = obj.getString("name"),
                            country = obj.getString("country"),
                            countryCode = obj.optString("countryCode", "XX"),
                            flagEmoji = obj.optString("flagEmoji", "🌐"),
                            host = obj.getString("host"),
                            port = obj.getInt("port"),
                            protocol = TunnelProtocol.fromString(obj.getString("protocol")),
                            isExtracted = obj.optBoolean("isExtracted", true),
                            isCustom = obj.optBoolean("isCustom", true),
                            username = obj.optString("username", "darktunnel"),
                            password = obj.optString("password", "free"),
                            sni = obj.optString("sni", ""),
                            payload = obj.optString("payload", ""),
                            wsPath = obj.optString("wsPath", "/darktunnel-ws"),
                            uuid = obj.optString("uuid", ""),
                            dnsPublicKey = obj.optString("dnsPublicKey", ""),
                            dnsNameServer = obj.optString("dnsNameServer", ""),
                            description = obj.optString("description", ""),
                            rawConfig = obj.optString("rawConfig", "")
                        )
                    )
                }
            } catch (_: Exception) {}
        }

        _servers.value = list

        val selectedId = prefs.getString("selected_server_id", null)
        val selected = list.find { it.id == selectedId } ?: list.first()
        _selectedServer.value = selected
    }

    fun selectServer(server: TunnelServer) {
        _selectedServer.value = server
        prefs.edit().putString("selected_server_id", server.id).apply()
    }

    fun addServer(server: TunnelServer) {
        val current = _servers.value.toMutableList()
        // Replace if exists, or append
        val index = current.indexOfFirst { it.id == server.id }
        if (index >= 0) {
            current[index] = server
        } else {
            current.add(0, server)
        }
        _servers.value = current
        saveCustomServers(current.filter { it.isCustom || it.isExtracted })
        selectServer(server)
    }

    fun removeServer(serverId: String) {
        val current = _servers.value.toMutableList()
        current.removeAll { it.id == serverId }
        _servers.value = current
        saveCustomServers(current.filter { it.isCustom || it.isExtracted })
        if (_selectedServer.value.id == serverId) {
            _selectedServer.value = current.firstOrNull() ?: DefaultServers.list.first()
        }
    }

    fun updateServerPing(serverId: String, ping: Int) {
        val current = _servers.value.map {
            if (it.id == serverId) it.copy(pingMs = ping) else it
        }
        _servers.value = current
        if (_selectedServer.value.id == serverId) {
            _selectedServer.value = _selectedServer.value.copy(pingMs = ping)
        }
    }

    fun updatePayloadConfig(config: PayloadConfig) {
        _payloadConfig.value = config
    }

    fun setDnsServer(dns: String) {
        _dnsServer.value = dns
        prefs.edit().putString("dns_server", dns).apply()
    }

    fun setUdpForwarding(enabled: Boolean) {
        _udpForwarding.value = enabled
        prefs.edit().putBoolean("udp_forwarding", enabled).apply()
    }

    fun setWakeLock(enabled: Boolean) {
        _wakeLock.value = enabled
        prefs.edit().putBoolean("wake_lock", enabled).apply()
    }

    private fun saveCustomServers(servers: List<TunnelServer>) {
        val array = JSONArray()
        for (s in servers) {
            val obj = JSONObject()
            obj.put("id", s.id)
            obj.put("name", s.name)
            obj.put("country", s.country)
            obj.put("countryCode", s.countryCode)
            obj.put("flagEmoji", s.flagEmoji)
            obj.put("host", s.host)
            obj.put("port", s.port)
            obj.put("protocol", s.protocol.name)
            obj.put("isExtracted", s.isExtracted)
            obj.put("isCustom", s.isCustom)
            obj.put("username", s.username)
            obj.put("password", s.password)
            obj.put("sni", s.sni)
            obj.put("payload", s.payload)
            obj.put("wsPath", s.wsPath)
            obj.put("uuid", s.uuid)
            obj.put("dnsPublicKey", s.dnsPublicKey)
            obj.put("dnsNameServer", s.dnsNameServer)
            obj.put("description", s.description)
            obj.put("rawConfig", s.rawConfig)
            array.put(obj)
        }
        prefs.edit().putString("custom_servers", array.toString()).apply()
    }
}
