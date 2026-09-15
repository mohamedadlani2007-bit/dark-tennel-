package com.example.model

data class TunnelServer(
    val id: String,
    val name: String,
    val country: String,
    val countryCode: String,
    val flagEmoji: String,
    val host: String,
    val port: Int,
    val protocol: TunnelProtocol,
    val pingMs: Int = -1, // -1 means unmeasured
    val isExtracted: Boolean = false,
    val isCustom: Boolean = false,
    val username: String = "darktunnel",
    val password: String = "free",
    val sni: String = "",
    val payload: String = "",
    val wsPath: String = "/darktunnel-ws",
    val uuid: String = "",
    val dnsPublicKey: String = "",
    val dnsNameServer: String = "",
    val description: String = "",
    val rawConfig: String = ""
) {
    val displayHostPort: String
        get() = "$host:$port"

    val statusPingText: String
        get() = when {
            pingMs < 0 -> "Not Tested"
            pingMs >= 999 -> "OFFLINE"
            else -> "$pingMs ms"
        }

    val isOnline: Boolean
        get() = pingMs in 1..998

    val isDead: Boolean
        get() = pingMs >= 999

    val isTested: Boolean
        get() = pingMs >= 0
}
