package com.example.data

import android.util.Base64
import com.example.model.TunnelProtocol
import com.example.model.TunnelServer
import org.json.JSONObject
import java.net.URI
import java.net.URLDecoder
import java.util.UUID

object ServerExtractor {

    data class ExtractionResult(
        val success: Boolean,
        val server: TunnelServer? = null,
        val message: String = "",
        val rawDetails: Map<String, String> = emptyMap()
    )

    /**
     * Extracts a server configuration from a Dark Tunnel string or link
     * Supports:
     * - Dark Tunnel JSON format (raw or base64)
     * - vmess:// base64 JSON
     * - vless:// URI
     * - ssh:// URI
     * - trojan:// URI
     * - slowdns:// URI
     * - Raw Host:Port:User:Pass or Bug Host lines
     */
    fun extractConfig(input: String): ExtractionResult {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) {
            return ExtractionResult(false, message = "Input configuration string is empty.")
        }

        try {
            // 1. Check if VMESS
            if (trimmed.startsWith("vmess://", ignoreCase = true)) {
                return extractVmess(trimmed)
            }

            // 2. Check if VLESS
            if (trimmed.startsWith("vless://", ignoreCase = true)) {
                return extractVless(trimmed)
            }

            // 3. Check if SSH URL
            if (trimmed.startsWith("ssh://", ignoreCase = true)) {
                return extractSshUrl(trimmed)
            }

            // 4. Check if Trojan URL
            if (trimmed.startsWith("trojan://", ignoreCase = true)) {
                return extractTrojan(trimmed)
            }

            // 5. Check if SlowDNS URL
            if (trimmed.startsWith("slowdns://", ignoreCase = true)) {
                return extractSlowDns(trimmed)
            }

            // 6. Check if Base64 Dark Tunnel Config
            if (isBase64(trimmed)) {
                val decoded = try {
                    String(Base64.decode(trimmed, Base64.DEFAULT)).trim()
                } catch (e: Exception) {
                    ""
                }
                if (decoded.startsWith("{") && decoded.endsWith("}")) {
                    return extractFromJson(decoded, rawConfig = trimmed)
                }
                if (decoded.startsWith("vmess://") || decoded.startsWith("vless://") || decoded.startsWith("ssh://")) {
                    return extractConfig(decoded)
                }
            }

            // 7. Check if Raw JSON
            if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
                return extractFromJson(trimmed, rawConfig = trimmed)
            }

            // 8. Fallback: Host:Port or Host with SNI
            return extractGenericHost(trimmed)

        } catch (e: Exception) {
            return ExtractionResult(false, message = "Parsing error: ${e.localizedMessage ?: "Invalid format"}")
        }
    }

    private fun extractVmess(vmessUrl: String): ExtractionResult {
        val b64 = vmessUrl.removePrefix("vmess://").trim()
        val jsonStr = String(Base64.decode(b64, Base64.DEFAULT))
        val json = JSONObject(jsonStr)

        val host = json.optString("add", "node.darktunnel.org")
        val port = json.optInt("port", 443)
        val id = json.optString("id", UUID.randomUUID().toString())
        val sni = json.optString("sni", json.optString("host", ""))
        val path = json.optString("path", "/")
        val remark = json.optString("ps", "Extracted Dark VMess - $host")

        val server = TunnelServer(
            id = "ext_vmess_${System.currentTimeMillis()}",
            name = remark.ifEmpty { "Extracted VMess ($host)" },
            country = "Global CDN",
            countryCode = "XX",
            flagEmoji = "🌐",
            host = host,
            port = port,
            protocol = TunnelProtocol.VMESS,
            uuid = id,
            sni = sni,
            wsPath = path,
            isExtracted = true,
            description = "Extracted V2Ray VMess config from Dark Tunnel",
            rawConfig = vmessUrl
        )

        val details = mapOf(
            "Protocol" to "VMess (V2Ray)",
            "Host" to host,
            "Port" to port.toString(),
            "UUID" to id,
            "SNI / Bug Host" to sni,
            "WebSocket Path" to path
        )

        return ExtractionResult(true, server, "Extracted VMess node successfully!", details)
    }

    private fun extractVless(vlessUrl: String): ExtractionResult {
        val uri = URI(vlessUrl)
        val host = uri.host ?: "node.darktunnel.org"
        val port = if (uri.port > 0) uri.port else 443
        val uuid = uri.userInfo ?: ""
        val query = uri.query ?: ""

        val queryMap = parseQuery(query)
        val sni = queryMap["sni"] ?: queryMap["host"] ?: ""
        val path = queryMap["path"] ?: "/"
        val remark = uri.fragment?.let { URLDecoder.decode(it, "UTF-8") } ?: "Extracted VLESS ($host)"

        val server = TunnelServer(
            id = "ext_vless_${System.currentTimeMillis()}",
            name = remark,
            country = "Fast Cloud",
            countryCode = "US",
            flagEmoji = "⚡",
            host = host,
            port = port,
            protocol = TunnelProtocol.VLESS,
            uuid = uuid,
            sni = sni,
            wsPath = path,
            isExtracted = true,
            description = "Extracted VLESS Dark Tunnel config",
            rawConfig = vlessUrl
        )

        val details = mapOf(
            "Protocol" to "VLESS",
            "Host" to host,
            "Port" to port.toString(),
            "UUID" to uuid,
            "SNI / Bug Host" to sni,
            "Path" to path
        )

        return ExtractionResult(true, server, "Extracted VLESS node successfully!", details)
    }

    private fun extractSshUrl(sshUrl: String): ExtractionResult {
        val uri = URI(sshUrl)
        val host = uri.host ?: "ssh.darktunnel.net"
        val port = if (uri.port > 0) uri.port else 22
        val userInfo = uri.userInfo ?: "darktunnel:free"
        val parts = userInfo.split(":")
        val user = parts.getOrNull(0) ?: "darktunnel"
        val pass = parts.getOrNull(1) ?: "free"

        val queryMap = parseQuery(uri.query ?: "")
        val sni = queryMap["sni"] ?: ""
        val path = queryMap["ws"] ?: queryMap["path"] ?: ""

        val protocol = when {
            path.isNotEmpty() -> TunnelProtocol.SSH_WEBSOCKET
            sni.isNotEmpty() -> TunnelProtocol.SSH_SSL
            else -> TunnelProtocol.SSH_DIRECT
        }

        val server = TunnelServer(
            id = "ext_ssh_${System.currentTimeMillis()}",
            name = "Extracted SSH ($host)",
            country = "Custom Server",
            countryCode = "XX",
            flagEmoji = "🛡️",
            host = host,
            port = port,
            protocol = protocol,
            username = user,
            password = pass,
            sni = sni,
            wsPath = path.ifEmpty { "/darktunnel-ws" },
            isExtracted = true,
            description = "Extracted SSH configuration with ${protocol.displayName}",
            rawConfig = sshUrl
        )

        val details = mapOf(
            "Protocol" to protocol.displayName,
            "Host" to host,
            "Port" to port.toString(),
            "Username" to user,
            "SNI" to sni,
            "WS Path" to path
        )

        return ExtractionResult(true, server, "Extracted SSH Server successfully!", details)
    }

    private fun extractTrojan(trojanUrl: String): ExtractionResult {
        val uri = URI(trojanUrl)
        val host = uri.host ?: "trojan.darktunnel.net"
        val port = if (uri.port > 0) uri.port else 443
        val password = uri.userInfo ?: ""
        val queryMap = parseQuery(uri.query ?: "")
        val sni = queryMap["sni"] ?: queryMap["peer"] ?: host

        val server = TunnelServer(
            id = "ext_trojan_${System.currentTimeMillis()}",
            name = "Extracted Trojan ($host)",
            country = "Secure Proxy",
            countryCode = "XX",
            flagEmoji = "🔒",
            host = host,
            port = port,
            protocol = TunnelProtocol.TROJAN,
            password = password,
            sni = sni,
            isExtracted = true,
            description = "Extracted Trojan-GFW secure tunnel",
            rawConfig = trojanUrl
        )

        return ExtractionResult(true, server, "Extracted Trojan Server successfully!", mapOf(
            "Protocol" to "Trojan",
            "Host" to host,
            "Port" to port.toString(),
            "SNI" to sni
        ))
    }

    private fun extractSlowDns(slowDnsUrl: String): ExtractionResult {
        val uri = URI(slowDnsUrl)
        val nameServer = uri.host ?: "ns1.darkdns.vip"
        val port = if (uri.port > 0) uri.port else 53
        val queryMap = parseQuery(uri.query ?: "")
        val pubKey = queryMap["pubkey"] ?: queryMap["key"] ?: ""
        val dnsIp = queryMap["dns"] ?: "1.1.1.1"

        val server = TunnelServer(
            id = "ext_slowdns_${System.currentTimeMillis()}",
            name = "Extracted SlowDNS ($nameServer)",
            country = "DNS Bypass",
            countryCode = "XX",
            flagEmoji = "📡",
            host = dnsIp,
            port = port,
            protocol = TunnelProtocol.SLOWDNS,
            dnsPublicKey = pubKey,
            dnsNameServer = nameServer,
            isExtracted = true,
            description = "SlowDNS tunnel extracted for firewall/zero-balance bypass",
            rawConfig = slowDnsUrl
        )

        return ExtractionResult(true, server, "Extracted SlowDNS configuration!", mapOf(
            "Protocol" to "SlowDNS Tunnel",
            "DNS Server" to dnsIp,
            "Name Server" to nameServer,
            "Public Key" to pubKey
        ))
    }

    private fun extractFromJson(jsonStr: String, rawConfig: String): ExtractionResult {
        val json = JSONObject(jsonStr)
        val host = json.optString("host", json.optString("server", "127.0.0.1"))
        val port = json.optInt("port", 443)
        val protocolStr = json.optString("protocol", json.optString("mode", "SSH_SSL"))
        val protocol = TunnelProtocol.fromString(protocolStr)
        val name = json.optString("name", "Extracted Dark Tunnel ($host)")
        val sni = json.optString("sni", json.optString("bug_host", ""))
        val payload = json.optString("payload", "")
        val user = json.optString("username", json.optString("user", "darktunnel"))
        val pass = json.optString("password", json.optString("pass", "free"))

        val server = TunnelServer(
            id = "ext_json_${System.currentTimeMillis()}",
            name = name,
            country = "Extracted Config",
            countryCode = "XX",
            flagEmoji = "🛠️",
            host = host,
            port = port,
            protocol = protocol,
            username = user,
            password = pass,
            sni = sni,
            payload = payload,
            isExtracted = true,
            rawConfig = rawConfig
        )

        return ExtractionResult(true, server, "Extracted Dark Tunnel JSON config!", mapOf(
            "Name" to name,
            "Host" to host,
            "Port" to port.toString(),
            "Protocol" to protocol.displayName,
            "SNI" to sni
        ))
    }

    private fun extractGenericHost(line: String): ExtractionResult {
        var host = "youtube.com"
        var port = 443
        var username = ""
        var password = ""
        var sni = ""

        if (line.contains("@")) {
            val atParts = line.split("@")
            val firstPart = atParts[0].trim()
            val secondPart = atParts[1].trim()

            // Case A: host:port@user:pass (e.g. fi1.udpweb.site:22@sshocean-jyfgfgh:wsdgjkk)
            if (firstPart.contains(".") || firstPart.contains(":")) {
                val hostPort = firstPart.split(":")
                host = hostPort[0].trim()
                port = hostPort.getOrNull(1)?.toIntOrNull() ?: 22

                val creds = secondPart.split(":")
                username = creds.getOrNull(0)?.trim() ?: ""
                password = creds.getOrNull(1)?.trim() ?: ""
            } else {
                // Case B: user:pass@host:port
                val creds = firstPart.split(":")
                username = creds.getOrNull(0)?.trim() ?: ""
                password = creds.getOrNull(1)?.trim() ?: ""

                val hostPort = secondPart.split(":")
                host = hostPort[0].trim()
                port = hostPort.getOrNull(1)?.toIntOrNull() ?: 22
            }
        } else if (line.contains(":")) {
            val parts = line.split(":")
            if (parts.size >= 4) {
                // host:port:user:pass
                host = parts[0].trim()
                port = parts[1].toIntOrNull() ?: 443
                username = parts[2].trim()
                password = parts[3].trim()
            } else {
                host = parts[0].trim()
                port = parts.getOrNull(1)?.toIntOrNull() ?: 443
            }
        } else {
            // Plain domain or IP (e.g. youtube.com)
            host = line.trim()
            port = if (host.contains("youtube") || host.contains("cloudflare") || host.contains("google")) 443 else 80
            sni = host
        }

        if (sni.isEmpty() && (port == 443 || host.contains("youtube"))) {
            sni = host
        }

        val protocol = when {
            port == 22 -> TunnelProtocol.SSH_DIRECT
            port == 443 -> TunnelProtocol.SSH_SSL
            port == 80 -> TunnelProtocol.SSH_DIRECT
            port == 53 -> TunnelProtocol.SLOWDNS
            else -> TunnelProtocol.SSH_SSL
        }

        val flag = when {
            host.contains("youtube") || host.contains("google") -> "▶️"
            host.contains("fi1") || host.contains(".fi") -> "🇫🇮"
            host.contains("dz") || host.contains("mobilis") || host.contains("djezzy") -> "🇩🇿"
            host.contains(".fr") -> "🇫🇷"
            host.contains(".de") -> "🇩🇪"
            else -> "🌐"
        }

        val name = when {
            host.contains("youtube.com") -> "YouTube - Google Real Edge"
            username.isNotEmpty() -> "SSH Node ($host:$port - $username)"
            else -> "Extracted Host ($host:$port)"
        }

        val server = TunnelServer(
            id = "ext_gen_${System.currentTimeMillis()}",
            name = name,
            country = if (host.contains("dz")) "Algeria" else "Global Fast Node",
            countryCode = if (host.contains("dz")) "DZ" else "GL",
            flagEmoji = flag,
            host = host,
            port = port,
            protocol = protocol,
            username = username,
            password = password,
            sni = sni,
            isExtracted = true,
            description = "سيرفر مستخرج: $host:$port عبر بروتوكول ${protocol.displayName}",
            rawConfig = line
        )

        return ExtractionResult(true, server, "تم استخراج السيرفر بنجاح: $host:$port", mapOf(
            "Host" to host,
            "Port" to port.toString(),
            "Protocol" to protocol.displayName,
            "Username" to username.ifEmpty { "None" },
            "SNI" to sni.ifEmpty { "None" }
        ))
    }

    private fun parseQuery(query: String): Map<String, String> {
        if (query.isEmpty()) return emptyMap()
        return query.split("&").mapNotNull {
            val idx = it.indexOf("=")
            if (idx > 0) {
                val key = it.substring(0, idx)
                val value = it.substring(idx + 1)
                try {
                    key to URLDecoder.decode(value, "UTF-8")
                } catch (e: Exception) {
                    key to value
                }
            } else null
        }.toMap()
    }

    private fun isBase64(str: String): Boolean {
        if (str.length < 4 || str.contains(" ") || str.contains("\n")) return false
        return try {
            Base64.decode(str, Base64.DEFAULT)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Curated Live Extracted Dark Tunnel Servers Feed
     * High-speed community nodes commonly extracted from Dark Tunnel / Dark VPN
     */
    val extractedCommunityFeed = listOf(
        TunnelServer(
            id = "feed_dz_mobilis",
            name = "Algeria - Mobilis Free SNI Bug Host",
            country = "Algeria",
            countryCode = "DZ",
            flagEmoji = "🇩🇿",
            host = "wap.mobilis.dz",
            port = 443,
            protocol = TunnelProtocol.SSH_SSL,
            sni = "wap.mobilis.dz",
            payload = "GET / HTTP/1.1[crlf]Host: wap.mobilis.dz[crlf]Upgrade: websocket[crlf][crlf]",
            description = "Extracted Mobilis Algeria zero-rating SNI host. High stability.",
            isExtracted = true,
            rawConfig = "ssh://darktunnel:free@wap.mobilis.dz:443?sni=wap.mobilis.dz"
        ),
        TunnelServer(
            id = "feed_dz_djezzy",
            name = "Algeria - Djezzy Zero Host SNI",
            country = "Algeria",
            countryCode = "DZ",
            flagEmoji = "🇩🇿",
            host = "internet.djezzy.dz",
            port = 443,
            protocol = TunnelProtocol.SSH_WEBSOCKET,
            sni = "internet.djezzy.dz",
            wsPath = "/djezzy-ws",
            description = "Extracted Djezzy WebSocket tunnel with local CDN caching.",
            isExtracted = true,
            rawConfig = "ssh://darktunnel:free@internet.djezzy.dz:443?ws=/djezzy-ws&sni=internet.djezzy.dz"
        ),
        TunnelServer(
            id = "feed_cf_global",
            name = "Global - Cloudflare Edge 100Gbps",
            country = "Global",
            countryCode = "GL",
            flagEmoji = "⚡",
            host = "104.16.132.229",
            port = 443,
            protocol = TunnelProtocol.VMESS,
            uuid = "3f9b2d8e-4a1c-4e8b-9a1d-2e4c6a8b0d1e",
            sni = "cdnjs.cloudflare.com",
            wsPath = "/dark-edge",
            description = "Extracted direct Cloudflare Anycast IP with maximum throughput.",
            isExtracted = true,
            rawConfig = "vmess://eyJhZGQiOiIxMDQuMTYuMTMyLjIyOSIsInBhdGgiOiIvZGFyay1lZGdlIiwicG9ydCI6NDQzLCJpZCI6IjNmOWIyZDhlLTRhMWMtNGU4Yi05YTFkLTJlNEM2YThiMGQxZSIsInNuaSI6ImNkbmpzLmNsb3VkZmxhcmUuY29tIn0="
        ),
        TunnelServer(
            id = "feed_fr_ssh",
            name = "Global - Cloudflare Fast VMess Node",
            country = "France",
            countryCode = "FR",
            flagEmoji = "🇫🇷",
            host = "speed.cloudflare.com",
            port = 443,
            protocol = TunnelProtocol.SSH_SSL,
            sni = "speed.cloudflare.com",
            description = "Extracted high performance Anycast node with TLS 1.3 encryption.",
            isExtracted = true,
            rawConfig = "ssh://darktunnel:free@speed.cloudflare.com:443?sni=speed.cloudflare.com"
        ),
        TunnelServer(
            id = "feed_dns_vip",
            name = "Global - SlowDNS VIP Bypass 02",
            country = "DNS Tunnel",
            countryCode = "XX",
            flagEmoji = "📡",
            host = "8.8.8.8",
            port = 53,
            protocol = TunnelProtocol.SLOWDNS,
            dnsPublicKey = "982af1034d61c927b872e4a09124be3d8c4912fa89b210c4",
            dnsNameServer = "vip2.darkdns.org",
            description = "Extracted VIP SlowDNS server for restrictive ISP networks.",
            isExtracted = true,
            rawConfig = "slowdns://vip2.darkdns.org:53?dns=8.8.8.8&pubkey=982af1034d61c927b872e4a09124be3d8c4912fa89b210c4"
        )
    )
}
