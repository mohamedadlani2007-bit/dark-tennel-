package com.example.model

enum class TunnelProtocol(val displayName: String, val badgeColorHex: Long) {
    SSH_DIRECT("SSH Direct", 0xFF00E5FF),
    SSH_SSL("SSH + SSL/TLS", 0xFF7C4DFF),
    SSH_WEBSOCKET("SSH + WebSocket", 0xFF00E676),
    VMESS("V2Ray VMess", 0xFFFF9100),
    VLESS("V2Ray VLESS", 0xFFFF4081),
    TROJAN("Trojan GFW", 0xFFE040FB),
    SLOWDNS("SlowDNS Tunnel", 0xFF00B0FF);

    companion object {
        fun fromString(str: String): TunnelProtocol {
            return entries.find {
                it.name.equals(str, ignoreCase = true) ||
                it.displayName.equals(str, ignoreCase = true)
            } ?: SSH_SSL
        }
    }
}
